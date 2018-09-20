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

import com.google.common.base.Preconditions.checkNotNull
import com.google.common.util.concurrent.AbstractIdleService
import com.google.common.util.concurrent.ListenableFuture
import java.io.IOException
import java.net.SocketAddress
import java.util.*
import javax.net.SocketFactory

/**
 *
 * A thin wrapper around a set of [BlockingClient]s.
 *
 *
 * Generally, using [NioClient] and [NioClientManager] should be preferred over [BlockingClient]
 * and [BlockingClientManager] as they scale significantly better, unless you wish to connect over a proxy or use
 * some other network settings that cannot be set using NIO.
 */
class BlockingClientManager : AbstractIdleService, ClientConnectionManager {
    private val socketFactory: SocketFactory
    private val clients = Collections.synchronizedSet(HashSet<BlockingClient>())

    private var connectTimeoutMillis = 1000

    override val connectedClientCount: Int
        get() = clients.size

    constructor() {
        socketFactory = SocketFactory.getDefault()
    }

    /**
     * Creates a blocking client manager that will obtain sockets from the given factory. Useful for customising how
     * bitcoinj connects to the P2P network.
     */
    constructor(socketFactory: SocketFactory) {
        this.socketFactory = checkNotNull(socketFactory)
    }

    override fun openConnection(serverAddress: SocketAddress, connection: StreamConnection): ListenableFuture<SocketAddress> {
        try {
            if (!isRunning)
                throw IllegalStateException()
            return BlockingClient(serverAddress, connection, connectTimeoutMillis, socketFactory, clients).connectFuture
        } catch (e: IOException) {
            throw RuntimeException(e) // This should only happen if we are, eg, out of system resources
        }

    }

    /** Sets the number of milliseconds to wait before giving up on a connect attempt  */
    fun setConnectTimeoutMillis(connectTimeoutMillis: Int) {
        this.connectTimeoutMillis = connectTimeoutMillis
    }

    @Throws(Exception::class)
    override fun startUp() {
    }

    @Throws(Exception::class)
    override fun shutDown() {
        synchronized(clients) {
            for (client in clients)
                client.closeConnection()
        }
    }

    override fun closeConnections(n: Int) {
        var n = n
        if (!isRunning)
            throw IllegalStateException()
        synchronized(clients) {
            val it = clients.iterator()
            while (n-- > 0 && it.hasNext())
                it.next().closeConnection()
        }
    }
}
