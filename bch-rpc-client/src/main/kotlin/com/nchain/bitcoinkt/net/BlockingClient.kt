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

import com.google.common.util.concurrent.*
import org.slf4j.*

import javax.net.*
import java.io.*
import java.net.*
import java.nio.*

import com.google.common.base.Preconditions.*

/**
 *
 * Creates a simple connection to a server using a [StreamConnection] to process data.
 *
 *
 * Generally, using [NioClient] and [NioClientManager] should be preferred over [BlockingClient]
 * and [BlockingClientManager], unless you wish to connect over a proxy or use some other network settings that
 * cannot be set using NIO.
 */
class BlockingClient
/**
 *
 * Creates a new client to the given server address using the given [StreamConnection] to decode the data.
 * The given connection **MUST** be unique to this object. This does not block while waiting for the connection to
 * open, but will call either the [StreamConnection.connectionOpened] or
 * [StreamConnection.connectionClosed] callback on the created network event processing thread.
 *
 * @param connectTimeoutMillis The connect timeout set on the connection (in milliseconds). 0 is interpreted as no
 * timeout.
 * @param socketFactory An object that creates [Socket] objects on demand, which may be customised to control
 * how this client connects to the internet. If not sure, use SocketFactory.getDefault()
 * @param clientSet A set which this object will add itself to after initialization, and then remove itself from
 */
@Throws(IOException::class)
constructor(serverAddress: SocketAddress, connection: StreamConnection,
            connectTimeoutMillis: Int, socketFactory: SocketFactory,
            clientSet: MutableSet<BlockingClient>?) : MessageWriteTarget {

    private val socket: Socket
    @Volatile
    private var vCloseRequested = false
    internal val connectFuture: SettableFuture<SocketAddress>

    init {
        connectFuture = SettableFuture.create()
        // Try to fit at least one message in the network buffer, but place an upper and lower limit on its size to make
        // sure it doesnt get too large or have to call read too often.
        connection.writeTarget = this
        socket = socketFactory.createSocket()

        // TODO Context
//        val context = Context.get()
        val t = object : Thread() {
            override fun run() {
                // TODO Context
//                Context.propagate(context!!)
                clientSet?.add(this@BlockingClient)
                try {
                    socket.connect(serverAddress, connectTimeoutMillis)
                    connection.connectionOpened()
                    connectFuture.set(serverAddress)
                    val stream = socket.getInputStream()
                    runReadLoop(stream, connection)
                } catch (e: Exception) {
                    if (!vCloseRequested) {
                        log.error("Error trying to open/read from connection: {}: {}", serverAddress, e.message)
                        connectFuture.setException(e)
                    }
                } finally {
                    try {
                        socket.close()
                    } catch (e1: IOException) {
                        // At this point there isn't much we can do, and we can probably assume the channel is closed
                    }

                    clientSet?.remove(this@BlockingClient)
                    connection.connectionClosed()
                }
            }
        }
        t.name = "BlockingClient network thread for $serverAddress"
        t.isDaemon = true
        t.start()
    }

    /**
     * Closes the connection to the server, triggering the [StreamConnection.connectionClosed]
     * event on the network-handling thread where all callbacks occur.
     */
    override fun closeConnection() {
        // Closes the channel, triggering an exception in the network-handling thread triggering connectionClosed()
        try {
            vCloseRequested = true
            socket.close()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }

    @Synchronized
    @Throws(IOException::class)
    override fun writeBytes(message: ByteArray) {
        try {
            val stream = socket.getOutputStream()
            stream.write(message)
            stream.flush()
        } catch (e: IOException) {
            log.error("Error writing message to connection, closing connection", e)
            closeConnection()
            throw e
        }

    }

    /** Returns a future that completes once connection has occurred at the socket level or with an exception if failed to connect.  */
    fun getConnectFuture(): ListenableFuture<SocketAddress> {
        return connectFuture
    }

    companion object {
        private val log = LoggerFactory.getLogger(BlockingClient::class.java)

        private val BUFFER_SIZE_LOWER_BOUND = 4096
        private val BUFFER_SIZE_UPPER_BOUND = 65536

        /**
         * A blocking call that never returns, except by throwing an exception. It reads bytes from the input stream
         * and feeds them to the provided [StreamConnection], for example, a [Peer].
         */
        @Throws(Exception::class)
        fun runReadLoop(stream: InputStream, connection: StreamConnection) {
            val dbuf = ByteBuffer.allocateDirect(Math.min(Math.max(connection.maxMessageSize, BUFFER_SIZE_LOWER_BOUND), BUFFER_SIZE_UPPER_BOUND))
            val readBuff = ByteArray(dbuf.capacity())
            while (true) {
                // TODO Kill the message duplication here
                checkState(dbuf.remaining() > 0 && dbuf.remaining() <= readBuff.size)
                val read = stream.read(readBuff, 0, Math.max(1, Math.min(dbuf.remaining(), stream.available())))
                if (read == -1)
                    return
                dbuf.put(readBuff, 0, read)
                // "flip" the buffer - setting the limit to the current position and setting position to 0
                dbuf.flip()
                // Use connection.receiveBytes's return value as a double-check that it stopped reading at the right
                // location
                val bytesConsumed = connection.receiveBytes(dbuf)
                checkState(dbuf.position() == bytesConsumed)
                // Now drop the bytes which were read by compacting dbuf (resetting limit and keeping relative
                // position)
                dbuf.compact()
            }
        }
    }
}
