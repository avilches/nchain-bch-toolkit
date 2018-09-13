package com.nchain.bitcoinkt.net.discovery

import com.nchain.params.NetworkParameters

import java.net.*
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Will query the given seeds producing a merged response in one single thread.
 */
object DiscoveryResolver {

    @JvmStatic
    @Throws(PeerDiscoveryException::class)
    fun getPeers(seeds: List<PeerDiscovery>): Array<InetSocketAddress> {
        val addrs = ArrayList<InetSocketAddress>()
        for (seed in seeds) {
            try {
                val inetAddresses: Array<InetSocketAddress> = seed.getPeers()
                addrs.addAll(inetAddresses)
            } catch (e: UnknownHostException) {
                // TODO: log warn
            }
        }
        if (addrs.size == 0)
            throw PeerDiscoveryException("No peer discovery returned any results. Check internet connection?")
        addrs.shuffle()
        return addrs.toTypedArray()
    }

    @JvmStatic
    @Throws(PeerDiscoveryException::class)
    fun getPeersParallel(seeds: List<PeerDiscovery>,
                         timeoutValue: Long,
                         timeoutUnit: TimeUnit): Array<InetSocketAddress> {
        val executor: ExecutorService = Executors.newFixedThreadPool(seeds.size)
        try {
            val addrs = ArrayList<InetSocketAddress>()
            val tasks = seeds.map {
                Callable { it.getPeers() }
            }
            executor.invokeAll(tasks, timeoutValue, timeoutUnit)
                    .filter { !it.isCancelled }
                    .forEach {
                        try {
                            val inetAddresses = it.get()
                            Collections.addAll(addrs, *inetAddresses)
                        } catch (e: ExecutionException) {
                        }
                    }
            if (addrs.size == 0)
                throw PeerDiscoveryException("No peer discovery returned any results in ${timeoutUnit.toMillis(timeoutValue)}ms. Check internet connection?")
            addrs.shuffle()
            return addrs.toTypedArray()
        } catch (e: InterruptedException) {
            throw PeerDiscoveryException(e)
        } finally {
            executor.shutdownNow()
        }
    }

}
