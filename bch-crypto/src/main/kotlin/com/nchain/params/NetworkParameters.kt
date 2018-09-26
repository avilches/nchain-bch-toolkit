/*
 * Copyright 2011 Google Inc.
 * Copyright 2014 Andreas Schildbach
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

package com.nchain.params

import com.nchain.bitcoinkt.core.BitcoinSerializer
import com.nchain.bitcoinkt.core.MessageSerializer
import java.math.BigInteger

/**
 * NetworkParameters contains the data needed for working with an instantiation of a Bitcoin chain.
 *
 * This is an abstract class, concrete instantiations can be found in the params package. There are four:
 * one for the main network ([MainNetParams]), one for the public test network ([TestNet3Params], and two others
 * ([RegTestParams] and [UnitTestParams]) that are intended for local app and unit testing development purposes.
 */
abstract class NetworkParameters protected constructor(

        /**
         * identification
         */
        val id: String,  // A Java package style string acting as unique ID for these parameters
        val cashAddrPrefix: String,                     // the prefix to use for cash addresses
        /**
         * First byte of a base58 encoded address. See [com.nchain.bitcoinkt.core.Address]. This is the same as
         * acceptableAddressCodes[0] and is the one used for "normal" addresses. Other types of address may be encountered
         * with version codes found in the acceptableAddressCodes array.
         */
        val addressHeader: Int = 0,
        /**
         * First byte of a base58 encoded P2SH address.  P2SH addresses are defined as part of BIP0013.
         */
        val p2SHHeader: Int = 0,
        /**
         * First byte of a base58 encoded dumped private key. See [com.nchain.bitcoinkt.core.DumpedPrivateKey].
         */
        val dumpedPrivateKeyHeader: Int,
        /**
         * The version codes that prefix addresses which are acceptable on this network. Although Satoshi intended these to
         * be used for "versioning", in fact they are today used to discriminate what kind of data is contained in the
         * address and to prevent accidentally sending coins across chains which would destroy them.
         */
        val acceptableAddressCodes: IntArray,
        /** Returns the 4 byte header for BIP32 (HD) wallet - public key part.  */
        val bip32HeaderPub: Int,
        /** Returns the 4 byte header for BIP32 (HD) wallet - private key part.  */
        val bip32HeaderPriv: Int,


        /**
         * block chain related
         */

        /** Maximum target represents the easiest allowable proof of work.  */
        val maxTarget: BigInteger,

        /**
         * The number of blocks in the last [] blocks
         * at which to trigger a notice to the user to upgrade their client, where
         * the client does not understand those blocks.
         */
        val majorityEnforceBlockUpgrade: Int,

        /**
         * The number of blocks in the last [] blocks
         * at which to enforce the requirement that all new blocks are of the
         * newer type (i.e. outdated blocks are rejected).
         */
        val majorityRejectBlockOutdated: Int,

        /**
         * The sampling window from which the version numbers of blocks are taken
         * in order to determine if a new block version is now the majority.
         */
        val majorityWindow: Int,

        /**
         * The depth of blocks required for a coinbase transaction to be spendable.
         */
//        val spendableCoinbaseDepth: Int,

//        val subsidyDecreaseBlockCount: Int,

        /**
         * How many blocks pass between difficulty adjustment periods. Bitcoin standardises this to be 2015.
         */
        val interval: Int = INTERVAL,

        /**
         * How much time in seconds is supposed to pass between "interval" blocks. If the actual elapsed time is
         * significantly different from this value, the network difficulty formula will produce a different value. Both
         * test and main Bitcoin networks use 2 weeks (1209600 seconds).
         */
        val targetTimespan: Int = TARGET_TIMESPAN,


        /**
         * protocol update heights
         * the following are block heights when particular features became enabled
         * they are the height of the first block where the feature is active
         */
        val uahfHeight: Int,                            // 1 aug 2018 split from BTC
        val daaUpdateHeight: Int,                       // 13 nov 2018 DAA upgrade
        val monolithHeight: Int,                         // 15 may 2018 upgrade

        /**
         * network related
         */

        /** Default TCP port on which to connect to nodes.  */
        val port: Int,
        /** The header bytes that identify the start of a packet on this network.  */
        val packetMagic: Long,
        /** Returns DNS names that when resolved, give IP addresses of active peers.  */
        val dnsSeeds: Array<String>
        /** Returns IP address of known seed peers. */
//        val addrSeeds: Array<Int>
) {


    @Transient
    var defaultSerializer: MessageSerializer? = null
        get(): MessageSerializer? {
            // Construct a default serializer if we don't have one
            if (field == null) {
                // Don't grab a lock unless we absolutely need it
                synchronized(this) {
                    // Now we have a lock, double check there's still no serializer
                    // and create one if so.
                    if (field == null) {
                        // As the serializers are intended to be immutable, creating
                        // two due to a race condition should not be a problem, however
                        // to be safe we ensure only one exists for each network.
                        field = getSerializer(false)
                    }
                }
            }
            return field
        }
        protected set



    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return if (other != null && other is NetworkParameters)
            id == other.id
        else
            false
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }


    /**
     * Returns true if the given height has a recorded checkpoint.
     */
//    fun isCheckpoint(height: Int): Boolean {
//        val checkpointHash = checkpoints[height]
//        return checkpointHash != null
//    }

    /**
     * If we are running in testnet-in-a-box mode, we allow connections to nodes with 0 non-genesis blocks.
     */
//    open fun allowEmptyPeerChain(): Boolean {
//        return true
//    }

    enum class ProtocolVersion private constructor(val bitcoinProtocolVersion: Int) {
        MINIMUM(70000),
        PONG(60001),
        BLOOM_FILTER(70000),
        CURRENT(70013)
    }

    /**
     * Returns the number of coins that will be produced in total, on this
     * network.
     */
//    val maxMoney: Coin
//        get() = MAX_MONEY
//
    /**
     * Any standard (ie pay-to-address) output smaller than this value will
     * most likely be rejected by the network.
     */
//    val minNonDustOutput: Coin
//        get() = Transaction.MIN_NONDUST_OUTPUT

    /**
     * The monetary object for this currency.
     */
//    val monetaryFormat: MonetaryFormat
//        get() = MonetaryFormat()

    /**
     * Scheme part for URIs, for example "bitcoincash".
     */
//    val uriScheme: String
//        get() = BITCOIN_SCHEME
//
    /**
     * Checks if we are at a difficulty transition point.
     * @param storedPrevBlockHeight The previous stored block
     * @return If this is a difficulty transition point
     */
    fun isDifficultyTransitionPoint(storedPrevBlockHeight: Int): Boolean {
        return (storedPrevBlockHeight + 1) % this.interval == 0
    }

    /**
     * Compute the a target based on the work done between 2 blocks and the time
     * required to produce that work.
     */

    fun getProtocolVersionNum(version: ProtocolVersion): Int {
        return version.bitcoinProtocolVersion
    }


    /**
     * Construct and return a custom serializer.
     */
    fun getSerializer(parseRetain: Boolean): BitcoinSerializer {
        return BitcoinSerializer(this, parseRetain)
    }

    /**
     * Returns whether this network has a maximum number of coins (finite supply) or
     * not. Always returns true for Bitcoin, but exists to be overriden for other
     * networks.
     */
//    fun hasMaxMoney(): Boolean {
//        return true
//    }

    companion object {
        val ID_MAINNET = "com.nchain.bitcoinkt.production"
        val ID_TESTNET = "com.nchain.bitcoinkt.test"
        val ID_REGTEST = "com.nchain.bitcoinkt.regtest"
        val ID_UNITTESTNET = "com.nchain.bitcoinkt.unittest"

        /**
         * Scheme part for Bitcoin URIs.
         */
//        val BITCOIN_SCHEME = "bitcoincash"

        /**
         * The number that is one greater than the largest representable SHA-256 hash.
         */
        val LARGEST_HASH = BigInteger.ONE.shiftLeft(256)

        val TARGET_TIMESPAN = 14 * 24 * 60 * 60  // 2 weeks per difficulty cycle, on average.
        val TARGET_SPACING = 10 * 60  // 10 minutes per block.
        val INTERVAL = TARGET_TIMESPAN / TARGET_SPACING // blocks per difficulty cycle

        /**
         * Blocks with a timestamp after this should enforce BIP 16, aka "Pay to script hash". This BIP changed the
         * network rules in a soft-forking manner, that is, blocks that don't follow the rules are accepted but not
         * mined upon and thus will be quickly re-orged out as long as the majority are enforcing the rule.
         */
//        val BIP16_ENFORCE_TIME = 1333238400

        /**
         * The maximum number of coins to be generated
         */
        const val MAX_COINS: Long = 21000000

        /**
         * A constant shared by the entire network: how large in bytes a block is allowed to be.
         */
        const val MAX_BLOCK_SIZE = 32 * 1000 * 1000

        /**
         * The maximum money to be generated
         */
//        val MAX_MONEY = Coin.COIN.multiply(MAX_COINS)

        /** Returns the network parameters for the given string ID or NULL if not recognized.  */
        fun fromID(id: String): NetworkParameters? {
            return if (id == ID_MAINNET) {
                MainNetParams
            } else if (id == ID_TESTNET) {
                TestNet3Params
            } else if (id == ID_UNITTESTNET) {
                UnitTestParams
            } else if (id == ID_REGTEST) {
                RegTestParams
            } else {
                null
            }
        }
    }
}
