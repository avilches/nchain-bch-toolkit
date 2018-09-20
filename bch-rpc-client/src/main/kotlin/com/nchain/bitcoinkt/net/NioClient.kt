/*
 * Copyright 2013 Google Inc.
 * Copyright 2014 Andreas Schildbach
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
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.SocketAddress
import java.nio.ByteBuffer

/**
 * Creates a simple connection to a server using a [StreamConnection] to process data.
 */
class NioClient
/**
 *
 * Creates a new client to the given server address using the given [StreamConnection] to decode the data.
 * The given connection **MUST** be unique to this object. This does not block while waiting for the connection to
 * open, but will call either the [StreamConnection.connectionOpened] or
 * [StreamConnection.connectionClosed] callback on the created network event processing thread.
 *
 * @param connectTimeoutMillis The connect timeout set on the connection (in milliseconds). 0 is interpreted as no
 * timeout.
 */
@Throws(IOException::class)
constructor(serverAddress: SocketAddress, parser: StreamConnection,
            connectTimeoutMillis: Int) : MessageWriteTarget {

    private val handler: Handler
    private val manager = NioClientManager()

    internal inner class Handler(private val upstreamConnection: StreamConnection, connectTimeoutMillis: Int) : AbstractTimeoutHandler(), StreamConnection {
        override var writeTarget: MessageWriteTarget? = null
            @Synchronized set(writeTarget) = if (closeOnOpen)
                writeTarget!!.closeConnection()
            else {
                setTimeoutEnabled(false)
                field = writeTarget
                upstreamConnection.writeTarget = writeTarget
            }
        private var closeOnOpen = false
        private var closeCalled = false

        override val maxMessageSize: Int
            get() = upstreamConnection.maxMessageSize

        init {
            setSocketTimeout(connectTimeoutMillis)
            setTimeoutEnabled(true)
        }

        @Synchronized
        override fun timeoutOccurred() {
            closeOnOpen = true
            connectionClosed()
        }

        @Synchronized
        override fun connectionClosed() {
            manager.stopAsync()
            if (!closeCalled) {
                closeCalled = true
                upstreamConnection.connectionClosed()
            }
        }

        @Synchronized
        override fun connectionOpened() {
            if (!closeOnOpen)
                upstreamConnection.connectionOpened()
        }

        @Throws(Exception::class)
        override fun receiveBytes(buff: ByteBuffer): Int {
            return upstreamConnection.receiveBytes(buff)
        }
    }

    init {
        manager.startAsync()
        manager.awaitRunning()
        handler = Handler(parser, connectTimeoutMillis)
        Futures.addCallback(manager.openConnection(serverAddress, handler)!!, object : FutureCallback<SocketAddress> {
            override fun onSuccess(result: SocketAddress?) {}

            override fun onFailure(t: Throwable) {
                log.error("Connect to {} failed: {}", serverAddress, Throwables.getRootCause(t))
            }
        })
    }

    override fun closeConnection() {
        handler.writeTarget!!.closeConnection()
    }

    @Synchronized
    @Throws(IOException::class)
    override fun writeBytes(message: ByteArray) {
        handler.writeTarget!!.writeBytes(message)
    }

    companion object {
        private val log = LoggerFactory.getLogger(NioClient::class.java)
    }
}
