package com.nchain.bitcoinkt.net

import com.nchain.bitcoinkt.net.discovery.DiscoveryResolver
import com.nchain.bitcoinkt.net.discovery.PeerDiscovery
import com.nchain.bitcoinkt.net.discovery.PeerDiscoveryException
import com.nchain.params.NetworkParameters
import java.net.InetAddress
import java.net.InetSocketAddress

import java.util.concurrent.TimeUnit

/**
 *
 * Supports peer discovery through DNS.
 *
 * Failure to resolve individual host names will not cause an Exception to be thrown.
 * However, if all hosts passed fail to resolve a PeerDiscoveryException will be thrown during getPeers().
 *
 * DNS seeds do not attempt to enumerate every peer on the network. [DiscoveryResolver.getPeersParallel]
 * will return up to 30 random peers from the set of those returned within the timeout period. If you want more peers
 * to connect to, you need to discover them via other means (like addr broadcasts).
 */
object DnsDiscovery {
    /**
     * Supports finding peers through DNS A records. Community run DNS entry points will be used.
     *
     * @param params Network parameters to be used for port information.
     */
    @JvmStatic
    @Throws(PeerDiscoveryException::class)
    fun getPeers(params: NetworkParameters): Array<InetSocketAddress> {
        val seeds = list(params)
        return DiscoveryResolver.getPeers(seeds)
    }

    @JvmStatic
    @Throws(PeerDiscoveryException::class)
    fun getPeersParallels(params: NetworkParameters, timeoutValue: Long, timeoutUnit: TimeUnit): Array<InetSocketAddress> {
        val seeds = list(params)
        return DiscoveryResolver.getPeersParallel(seeds, timeoutValue, timeoutUnit)
    }

    private fun list(params: NetworkParameters): List<PeerDiscovery> {
        return params.dnsSeeds.map {
            object: PeerDiscovery {
                override fun getPeers(): Array<InetSocketAddress> {
                    val response = InetAddress.getAllByName(it)
                    return Array(response.size) {
                        InetSocketAddress(response[it], params.port)
                    }
                }
            }

        }
    }

}
