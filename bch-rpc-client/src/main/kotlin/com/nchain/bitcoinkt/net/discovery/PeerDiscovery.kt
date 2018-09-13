package com.nchain.bitcoinkt.net.discovery

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

/**
 * A PeerDiscovery object is responsible for finding addresses of other nodes in the Bitcoin P2P network. Note that
 * the addresses returned may or may not be accepting connections.
 */
interface PeerDiscovery {
    @Throws(PeerDiscoveryException::class)
    fun getPeers(): Array<InetSocketAddress>
}
