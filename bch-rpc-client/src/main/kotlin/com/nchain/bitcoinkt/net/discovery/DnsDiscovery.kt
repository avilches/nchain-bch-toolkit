/*
 * Copyright 2011 John Sample
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

package com.nchain.bitcoinkt.net.discovery

import com.nchain.bitcoinkt.exception.PeerDiscoveryException
import com.nchain.bitcoinkt.params.NetworkParameters
import com.nchain.bitcoinkt.utils.*
import com.nchain.params.NetworkParameters

import java.net.*
import java.util.*
import java.util.concurrent.*

/**
 *
 * Supports peer discovery through DNS.
 *
 *
 * Failure to resolve individual host names will not cause an Exception to be thrown.
 * However, if all hosts passed fail to resolve a PeerDiscoveryException will be thrown during getPeers().
 *
 *
 *
 * DNS seeds do not attempt to enumerate every peer on the network. [DnsDiscovery.getPeers]
 * will return up to 30 random peers from the set of those returned within the timeout period. If you want more peers
 * to connect to, you need to discover them via other means (like addr broadcasts).
 */
class DnsDiscovery: MultiplexingDiscovery {
    /**
     * Supports finding peers through DNS A records.
     *
     * @param netParams Network parameters to be used for port information.
     * @param dnsSeeds Host names to be examined for seed addresses.
     */
    constructor(netParams: NetworkParameters, dnsSeeds: Array<String>) : super(netParams, DnsDiscovery.buildDiscoveries(netParams, dnsSeeds))

    /**
     * Supports finding peers through DNS A records. Community run DNS entry points will be used.
     *
     * @param netParams Network parameters to be used for port information.
     */
    constructor(netParams: NetworkParameters) : super(netParams, DnsDiscovery.buildDiscoveries(netParams, netParams.dnsSeeds))

    companion object {
        fun buildDiscoveries(params: NetworkParameters, seeds: Array<String>?): List<PeerDiscovery> {
            return if (seeds != null) seeds.map {
                DnsSeedDiscovery(params, it)
            } else ArrayList<PeerDiscovery>()
        }
    }

    override fun createExecutor(): ExecutorService {
        // Attempted workaround for reported bugs on Linux in which gethostbyname does not appear to be properly
        // thread safe and can cause segfaults on some libc versions.
        return if (System.getProperty("os.name").toLowerCase().contains("linux"))
            Executors.newSingleThreadExecutor(ContextPropagatingThreadFactory("DNS seed lookups"))
        else
            Executors.newFixedThreadPool(seeds.size, DaemonThreadFactory("DNS seed lookups"))
    }

    /** Implements discovery from a single DNS host.  */
    class DnsSeedDiscovery(private val params: NetworkParameters, private val hostname: String) : PeerDiscovery {

        @Throws(PeerDiscoveryException::class)
        override fun getPeers(services: Long, timeoutValue: Long, timeoutUnit: TimeUnit): Array<InetSocketAddress> {
            if (services != 0L)
                throw PeerDiscoveryException("DNS seeds cannot filter by services: $services")
            try {
                val response = InetAddress.getAllByName(hostname)
                return Array(response.size) {
                    InetSocketAddress(response[it], params.port)
                }
            } catch (e: UnknownHostException) {
                throw PeerDiscoveryException(e)
            }

        }

        override fun shutdown() {}

        override fun toString(): String {
            return hostname
        }
    }
}
