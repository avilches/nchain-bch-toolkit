/*
 * Copyright 2014 Mike Hearn
 * Copyright 2015 Andreas Schildbach
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

import com.google.common.collect.Lists

import com.nchain.bitcoinkt.net.discovery.DnsDiscovery.DnsSeedDiscovery
import org.slf4j.LoggerFactory

import java.net.InetSocketAddress
import java.util.Collections
import java.util.concurrent.*

import com.google.common.base.Preconditions.checkArgument
import com.nchain.params.NetworkParameters

/**
 * MultiplexingDiscovery queries multiple PeerDiscovery objects, shuffles their responses and then returns the results,
 * thus selecting randomly between them and reducing the influence of any particular seed. Any that don't respond
 * within the timeout are ignored. Backends are queried in parallel. Backends may block.
 */
open class MultiplexingDiscovery
/**
 * Will query the given seeds in parallel before producing a merged response.
 */
constructor (protected val netParams: NetworkParameters, protected val seeds: List<PeerDiscovery>) : PeerDiscovery {
    @Volatile
    private var vThreadPool: ExecutorService? = null

    init {
        checkArgument(!seeds.isEmpty())
    }

    @Throws(PeerDiscoveryException::class)
    override fun getPeers(services: Long, timeoutValue: Long, timeoutUnit: TimeUnit): Array<InetSocketAddress> {
        vThreadPool = createExecutor()
        try {
            val tasks = Lists.newArrayList<Callable<Array<InetSocketAddress>>>()
            for (seed in seeds) {
                tasks.add(Callable { seed.getPeers(services, timeoutValue, timeoutUnit) })
            }
            val futures = vThreadPool!!.invokeAll(tasks, timeoutValue, timeoutUnit)
            val addrs = Lists.newArrayList<InetSocketAddress>()
            for (i in futures.indices) {
                val future = futures[i]
                if (future.isCancelled) {
                    log.warn("Seed {}: timed out", seeds[i])
                    continue  // Timed out.
                }
                val inetAddresses: Array<InetSocketAddress>
                try {
                    inetAddresses = future.get()
                } catch (e: ExecutionException) {
                    log.warn("Seed {}: failed to look up: {}", seeds[i], e.message)
                    continue
                }

                Collections.addAll(addrs, *inetAddresses)
            }
            if (addrs.size == 0)
                throw PeerDiscoveryException("No peer discovery returned any results in "
                        + timeoutUnit.toMillis(timeoutValue) + "ms. Check internet connection?")
            Collections.shuffle(addrs)
            vThreadPool!!.shutdownNow()
            return addrs.toTypedArray()
        } catch (e: InterruptedException) {
            throw PeerDiscoveryException(e)
        } finally {
            vThreadPool!!.shutdown()
        }
    }

    protected open fun createExecutor(): ExecutorService {
        return Executors.newFixedThreadPool(seeds.size, ContextPropagatingThreadFactory("Multiplexing discovery"))
    }

    override fun shutdown() {
        val tp = vThreadPool
        tp?.shutdown()
    }

    companion object {
        private val log = LoggerFactory.getLogger(MultiplexingDiscovery::class.java)

        /**
         * Builds a suitable set of peer discoveries. Will query them in parallel before producing a merged response.
         * If specific services are required, DNS is not used as the protocol can't handle it.
         * @param params Network to use.
         * @param services Required services as a bitmask, e.g. [VersionMessage.NODE_NETWORK].
         */
        fun forServices(params: NetworkParameters, services: Long): MultiplexingDiscovery {
            val discoveries = Lists.newArrayList<PeerDiscovery>()
            // Use DNS seeds if there is no specific service requirement
            if (services == 0L) {
                val dnsSeeds = params.dnsSeeds
                if (dnsSeeds != null)
                    for (dnsSeed in dnsSeeds)
                        discoveries.add(DnsSeedDiscovery(params, dnsSeed))
            }
            return MultiplexingDiscovery(params, discoveries)
        }
    }
}
