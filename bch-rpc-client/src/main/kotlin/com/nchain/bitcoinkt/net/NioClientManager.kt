/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nchain.bitcoinkt.net

import com.google.common.base.Throwables
import com.google.common.util.concurrent.*
import org.slf4j.LoggerFactory

import java.io.IOException
import java.net.ConnectException
import java.net.SocketAddress
import java.nio.channels.*
import java.nio.channels.spi.SelectorProvider
import java.util.*
import java.util.concurrent.*

/**
 * A class which manages a set of client connections. Uses Java NIO to select network events and processes them in a
 * single network processing thread.
 */
class NioClientManager : AbstractExecutionThreadService(), ClientConnectionManager {

    private val selector: Selector
    internal val newConnectionChannels: Queue<PendingConnect> = LinkedBlockingQueue()

    // Added to/removed from by the individual ConnectionHandler's, thus must by synchronized on its own.
    private val connectedHandlers = Collections.synchronizedSet(HashSet<ConnectionHandler>())

    override val connectedClientCount: Int
        get() = connectedHandlers.size

    internal inner class PendingConnect(var sc: SocketChannel, var connection: StreamConnection, var address: SocketAddress) {
        var future: SettableFuture<SocketAddress>? = SettableFuture.create()
    }

    // Handle a SelectionKey which was selected
    @Throws(IOException::class)
    private fun handleKey(key: SelectionKey) {
        // We could have a !isValid() key here if the connection is already closed at this point
        if (key.isValid && key.isConnectable) { // ie a client connection which has finished the initial connect process
            // Create a ConnectionHandler and hook everything together
            val data = key.attachment() as PendingConnect
            val connection = data.connection
            val sc = key.channel() as SocketChannel
            val handler = ConnectionHandler(connection, key, connectedHandlers)
            try {
                if (sc.finishConnect()) {
                    log.info("Connected to {}", sc.socket().remoteSocketAddress)
                    key.interestOps(key.interestOps() or SelectionKey.OP_READ and SelectionKey.OP_CONNECT.inv()).attach(handler)
                    connection.connectionOpened()
                    data.future!!.set(data.address)
                } else {
                    log.warn("Failed to connect to {}", sc.socket().remoteSocketAddress)
                    handler.closeConnection() // Failed to connect for some reason
                    data.future!!.setException(ConnectException("Unknown reason"))
                    data.future = null
                }
            } catch (e: Exception) {
                // If e is a CancelledKeyException, there is a race to get to interestOps after finishConnect() which
                // may cause this. Otherwise it may be any arbitrary kind of connection failure.
                // Calling sc.socket().getRemoteSocketAddress() here throws an exception, so we can only log the error itself
                val cause = Throwables.getRootCause(e)
                log.warn("Failed to connect with exception: {}: {}", cause.javaClass.name, cause.message, e)
                handler.closeConnection()
                data.future!!.setException(cause)
                data.future = null
            }

        } else
        // Process bytes read
            ConnectionHandler.handleKey(key)
    }

    /**
     * Creates a new client manager which uses Java NIO for socket management. Uses a single thread to handle all select
     * calls.
     */
    init {
        try {
            selector = SelectorProvider.provider().openSelector()
        } catch (e: IOException) {
            throw RuntimeException(e) // Shouldn't ever happen
        }

    }

    public override fun run() {
        try {
            Thread.currentThread().priority = Thread.MIN_PRIORITY
            while (isRunning) {
                var conn: PendingConnect? = newConnectionChannels.poll()
                while (conn != null) {
                    try {
                        val key = conn.sc.register(selector, SelectionKey.OP_CONNECT)
                        key.attach(conn)
                    } catch (e: ClosedChannelException) {
                        log.warn("SocketChannel was closed before it could be registered")
                    } finally {
                        conn = newConnectionChannels.poll()
                    }

                }

                selector.select()

                val keyIterator = selector.selectedKeys().iterator()
                while (keyIterator.hasNext()) {
                    val key = keyIterator.next()
                    keyIterator.remove()
                    handleKey(key)
                }
            }
        } catch (e: Exception) {
            log.warn("Error trying to open/read from connection: ", e)
        } finally {
            // Go through and close everything, without letting IOExceptions get in our way
            for (key in selector.keys()) {
                try {
                    key.channel().close()
                } catch (e: IOException) {
                    log.warn("Error closing channel", e)
                }

                key.cancel()
                if (key.attachment() is ConnectionHandler)
                    ConnectionHandler.handleKey(key) // Close connection if relevant
            }
            try {
                selector.close()
            } catch (e: IOException) {
                log.warn("Error closing client manager selector", e)
            }

        }
    }

    override fun openConnection(serverAddress: SocketAddress, connection: StreamConnection): ListenableFuture<SocketAddress>? {
        if (!isRunning)
            throw IllegalStateException()
        // Create a new connection, give it a connection as an attachment
        try {
            val sc = SocketChannel.open()
            sc.configureBlocking(false)
            sc.connect(serverAddress)
            val data = PendingConnect(sc, connection, serverAddress)
            newConnectionChannels.offer(data)
            selector.wakeup()
            return data.future
        } catch (e: Throwable) {
            return Futures.immediateFailedFuture(e)
        }

    }

    public override fun triggerShutdown() {
        selector.wakeup()
    }

    override fun closeConnections(n: Int) {
        var n = n
        while (n-- > 0) {
            var handler: ConnectionHandler? = null
            synchronized(connectedHandlers) {
                handler = connectedHandlers.iterator().next()
            }
            if (handler != null)
                handler!!.closeConnection() // Removes handler from connectedHandlers before returning
        }
    }

    override fun executor(): Executor {
        return Executors.newFixedThreadPool(10)
//        return Executor { command -> ContextPropagatingThreadFactory("NioClientManager").newThread(command).start() }
    }

    companion object {
        private val log = LoggerFactory.getLogger(NioClientManager::class.java)
    }
}
