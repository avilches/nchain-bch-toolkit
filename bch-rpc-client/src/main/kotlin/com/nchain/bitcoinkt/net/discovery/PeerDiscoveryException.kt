package com.nchain.bitcoinkt.net.discovery

class PeerDiscoveryException : Exception {
    constructor(message: String) : super(message)
    constructor(e: Throwable) : super(e)
}
