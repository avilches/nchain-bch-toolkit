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

import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Throwables
import com.google.common.util.concurrent.AbstractExecutionThreadService
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.spi.SelectorProvider

/**
 * Creates a simple server listener which listens for incoming client connections and uses a [StreamConnection] to
 * process data.
 */
class NioServer
/**
 * Creates a new server which is capable of listening for incoming connections and processing client provided data
 * using [StreamConnection]s created by the given [StreamConnectionFactory]
 *
 * @throws IOException If there is an issue opening the server socket or binding fails for some reason
 */
@Throws(IOException::class)
constructor(private val connectionFactory: StreamConnectionFactory, bindAddress: InetSocketAddress) : AbstractExecutionThreadService() {

    private val sc: ServerSocketChannel
    @VisibleForTesting
    internal val selector: Selector

    // Handle a SelectionKey which was selected
    @Throws(IOException::class)
    private fun handleKey(selector: Selector, key: SelectionKey) {
        if (key.isValid && key.isAcceptable) {
            // Accept a new connection, give it a stream connection as an attachment
            val newChannel = sc.accept()
            newChannel.configureBlocking(false)
            val newKey = newChannel.register(selector, SelectionKey.OP_READ)
            try {
                val handler = ConnectionHandler(connectionFactory, newKey)
                newKey.attach(handler)
                handler.connection!!.connectionOpened()
            } catch (e: IOException) {
                // This can happen if ConnectionHandler's call to get a new handler returned null
                log.error("Error handling new connection", Throwables.getRootCause(e).message)
                newKey.channel().close()
            }

        } else { // Got a closing channel or a channel to a client connection
            ConnectionHandler.handleKey(key)
        }
    }

    init {

        sc = ServerSocketChannel.open()
        sc.configureBlocking(false)
        sc.socket().bind(bindAddress)
        selector = SelectorProvider.provider().openSelector()
        sc.register(selector, SelectionKey.OP_ACCEPT)
    }

    @Throws(Exception::class)
    override fun run() {
        try {
            while (isRunning) {
                selector.select()

                val keyIterator = selector.selectedKeys().iterator()
                while (keyIterator.hasNext()) {
                    val key = keyIterator.next()
                    keyIterator.remove()

                    handleKey(selector, key)
                }
            }
        } catch (e: Exception) {
            log.error("Error trying to open/read from connection: {}", e)
        } finally {
            // Go through and close everything, without letting IOExceptions get in our way
            for (key in selector.keys()) {
                try {
                    key.channel().close()
                } catch (e: IOException) {
                    log.error("Error closing channel", e)
                }

                try {
                    key.cancel()
                    handleKey(selector, key)
                } catch (e: IOException) {
                    log.error("Error closing selection key", e)
                }

            }
            try {
                selector.close()
            } catch (e: IOException) {
                log.error("Error closing server selector", e)
            }

            try {
                sc.close()
            } catch (e: IOException) {
                log.error("Error closing server channel", e)
            }

        }
    }

    /**
     * Invoked by the Execution service when it's time to stop.
     * Calling this method directly will NOT stop the service, call
     * [com.google.common.util.concurrent.AbstractExecutionThreadService.stop] instead.
     */
    public override fun triggerShutdown() {
        // Wake up the selector and let the selection thread break its loop as the ExecutionService !isRunning()
        selector.wakeup()
    }

    companion object {
        private val log = LoggerFactory.getLogger(NioServer::class.java)
    }
}
