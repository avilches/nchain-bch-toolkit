/*
 * Copyright 2011 Micheal Swiggs
 * Copyright 2018 nChain Ltd.
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
 *
 * This file has been modified for the bitcoinkt project.
 * The original file was from the bitcoinj-cash project (https://github.com/bitcoinj-cash/bitcoinj).
 */

package com.nchain.bitcoinkt.net.discovery

import com.nchain.bitcoinkt.exception.PeerDiscoveryException
import com.nchain.bitcoinkt.params.NetworkParameters
import com.nchain.params.NetworkParameters
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * SeedPeers stores a pre-determined list of Bitcoin node addresses. These nodes are selected based on being
 * active on the network for a long period of time. The intention is to be a last resort way of finding a connection
 * to the network, in case IRC and DNS fail. The list comes from the Bitcoin C++ source code.
 */
class SeedPeers
/**
 * Supports finding peers by IP addresses
 *
 * @param seedAddrs IP addresses for seed addresses.
 * @param params Network parameters to be used for port information.
 */
(private val seedAddrs: Array<Int>, private val params: NetworkParameters) : PeerDiscovery {
    private var pnseedIndex: Int = 0

    /**
     * Acts as an iterator, returning the address of each node in the list sequentially.
     * Once all the list has been iterated, null will be returned for each subsequent query.
     *
     * @return InetSocketAddress - The address/port of the next node.
     * @throws PeerDiscoveryException
     */
    val peer: InetSocketAddress?
        @Throws(PeerDiscoveryException::class)
        get() {
            try {
                return nextPeer()
            } catch (e: UnknownHostException) {
                throw PeerDiscoveryException(e)
            }

        }

    /**
     * Supports finding peers by IP addresses
     *
     * @param params Network parameters to be used for port information.
     */
    constructor(params: NetworkParameters) : this(params.addrSeeds, params) {}

    @Throws(UnknownHostException::class, PeerDiscoveryException::class)
    private fun nextPeer(): InetSocketAddress? {
        if (seedAddrs.size == 0)
            throw PeerDiscoveryException("No IP address seeds configured; unable to find any peers")

        return if (pnseedIndex >= seedAddrs.size) null else InetSocketAddress(convertAddress(seedAddrs[pnseedIndex++]),
                params.port)
    }

    /**
     * Returns an array containing all the Bitcoin nodes within the list.
     */
    @Throws(PeerDiscoveryException::class)
    override fun getPeers(services: Long, timeoutValue: Long, timeoutUnit: TimeUnit): Array<InetSocketAddress> {
        if (services != 0L)
            throw PeerDiscoveryException("Pre-determined peers cannot be filtered by services: $services")
        try {
            return allPeers()
        } catch (e: UnknownHostException) {
            throw PeerDiscoveryException(e)
        }

    }

    @Throws(UnknownHostException::class)
    private fun allPeers(): Array<InetSocketAddress> {
        return Array(seedAddrs.size) {
            InetSocketAddress(convertAddress(seedAddrs[it]), params.port)
        }
    }

    @Throws(UnknownHostException::class)
    private fun convertAddress(seed: Int): InetAddress {
        val v4addr = ByteArray(4)
        v4addr[0] = (0xFF and seed).toByte()
        v4addr[1] = (0xFF and (seed shr 8)).toByte()
        v4addr[2] = (0xFF and (seed shr 16)).toByte()
        v4addr[3] = (0xFF and (seed shr 24)).toByte()
        return InetAddress.getByAddress(v4addr)
    }

    override fun shutdown() {}
}
