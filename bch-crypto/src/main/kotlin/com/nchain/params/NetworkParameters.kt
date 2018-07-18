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

package com.nchain.bitcoinkt.params

import java.math.BigInteger
import java.util.*

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
        val spendableCoinbaseDepth: Int,

        val subsidyDecreaseBlockCount: Int,

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
        val dnsSeeds: Array<String>,
        /** Returns IP address of known seed peers. */
        val addrSeeds: Array<Int>
) {

    /**
     *
     * Genesis block for this chain.
     *
     * The first block in every chain is a well known constant shared between all Bitcoin implemenetations. For a
     * block to be valid, it must be eventually possible to work backwards to the genesis block by following the
     * prevBlockHash pointers in the block headers.
     *
     * The genesis blocks for both test and main networks contain the timestamp of when they were created,
     * and a message in the coinbase transaction. It says, *"The Times 03/Jan/2009 Chancellor on brink of second
     * bailout for banks"*.
     */
//    val genesisBlock: Block = createGenesis(this)

//    protected var checkpoints: MutableMap<Int, Sha256Hash> = HashMap()

/*
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

*/

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        return if (o == null || javaClass != o.javaClass) false else id == (o as NetworkParameters).id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    /**
     * Returns true if the block height is either not a checkpoint, or is a checkpoint and the hash matches.
     */
//    fun passesCheckpoint(height: Int, hash: Sha256Hash): Boolean {
//        val checkpointHash = checkpoints[height]
//        return checkpointHash == null || checkpointHash == hash
//    }

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
    open fun allowEmptyPeerChain(): Boolean {
        return true
    }

    /**
     * The flags indicating which block validation tests should be applied to
     * the given block. Enables support for alternative blockchains which enable
     * tests based on different criteria.
     *
     * @param block block to determine flags for.
     * @param height height of the block, if known, null otherwise. Returned
     * tests should be a safe subset if block height is unknown.
     */
//    fun getBlockVerificationFlags(block: Block,
//                                  tally: VersionTally, height: Int?): EnumSet<Block.VerifyFlag> {
//        val flags = EnumSet.noneOf<Block.VerifyFlag>(Block.VerifyFlag::class.java)
//
//        if (block.isBIP34) {
//            val count = tally.getCountAtOrAbove(Block.BLOCK_VERSION_BIP34)
//            if (null != count && count >= majorityEnforceBlockUpgrade) {
//                flags.add(Block.VerifyFlag.HEIGHT_IN_COINBASE)
//            }
//        }
//        return flags
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
    val uriScheme: String
        get() = BITCOIN_SCHEME

    /**
     * Checks if we are at a difficulty transition point.
     * @param storedPrev The previous stored block
     * @return If this is a difficulty transition point
     */
/*
    fun isDifficultyTransitionPoint(storedPrev: StoredBlock): Boolean {
        return (storedPrev.height + 1) % this.interval == 0
    }

    fun verifyDifficulty(newTarget: BigInteger?, nextBlock: Block) {
        var newTarget = newTarget
        if (newTarget!!.compareTo(this.maxTarget!!) > 0) {
            newTarget = this.maxTarget
        }

        val accuracyBytes = nextBlock.getDifficultyTarget().ushr(24).toInt() - 3
        val receivedTargetCompact = nextBlock.getDifficultyTarget()

        // The calculated difficulty is to a higher precision than received, so reduce here.
        val mask = BigInteger.valueOf(0xFFFFFFL).shiftLeft(accuracyBytes * 8)
        newTarget = newTarget!!.and(mask)
        val newTargetCompact = Utils.encodeCompactBits(newTarget!!)

        if (newTargetCompact != receivedTargetCompact)
            throw VerificationException("Network provided difficulty bits do not match what was calculated: " +
                    java.lang.Long.toHexString(newTargetCompact) + " vs " + java.lang.Long.toHexString(receivedTargetCompact))
    }

*/
    /**
     * Compute the a target based on the work done between 2 blocks and the time
     * required to produce that work.
     */
/*
    fun ComputeTarget(firstBlock: StoredBlock?,
                      lastBlock: StoredBlock): BigInteger {
        check(lastBlock.height > firstBlock!!.height)

*
         * From the total work done and the time it took to produce that much work,
         * we can deduce how much work we expect to be produced in the targeted time
         * between blocks.


        var work = lastBlock.chainWork.subtract(firstBlock.chainWork)
        work = work.multiply(BigInteger.valueOf(TARGET_SPACING.toLong()))

        // In order to avoid difficulty cliffs, we bound the amplitude of the
        // adjustment we are going to do.
        Preconditions.checkState(lastBlock.header.timeSeconds > firstBlock.header.timeSeconds)
        var nActualTimespan = lastBlock.header.timeSeconds - firstBlock.header.timeSeconds
        if (nActualTimespan > 288 * TARGET_SPACING) {
            nActualTimespan = (288 * TARGET_SPACING).toLong()
        } else if (nActualTimespan < 72 * TARGET_SPACING) {
            nActualTimespan = (72 * TARGET_SPACING).toLong()
        }

        work = work.divide(BigInteger.valueOf(nActualTimespan))

*
         * We need to compute T = (2^256 / W) - 1.
         * This code differs from Bitcoin-ABC in that we are using
         * BigIntegers instead of a data type that is limited to 256 bits.



        return LARGEST_HASH.divide(work).subtract(BigInteger.ONE)
    }

    fun getProtocolVersionNum(version: ProtocolVersion): Int {
        return version.bitcoinProtocolVersion
    }
*/

    /**
     * Construct and return a custom serializer.
     */
//    fun getSerializer(parseRetain: Boolean): BitcoinSerializer {
//        return BitcoinSerializer(this, parseRetain)
//    }

    /**
     * Returns whether this network has a maximum number of coins (finite supply) or
     * not. Always returns true for Bitcoin, but exists to be overriden for other
     * networks.
     */
    fun hasMaxMoney(): Boolean {
        return true
    }

    companion object {
        val ID_MAINNET = "com.nchain.bitcoinkt.production"
        val ID_TESTNET = "com.nchain.bitcoinkt.test"
        val ID_REGTEST = "com.nchain.bitcoinkt.regtest"
        val ID_UNITTESTNET = "com.nchain.bitcoinkt.unittest"

        /**
         * Scheme part for Bitcoin URIs.
         */
        val BITCOIN_SCHEME = "bitcoincash"

        /**
         * The number that is one greater than the largest representable SHA-256 hash.
         */
        private val LARGEST_HASH = BigInteger.ONE.shiftLeft(256)

        val TARGET_TIMESPAN = 14 * 24 * 60 * 60  // 2 weeks per difficulty cycle, on average.
        val TARGET_SPACING = 10 * 60  // 10 minutes per block.
        val INTERVAL = TARGET_TIMESPAN / TARGET_SPACING // blocks per difficulty cycle

        /**
         * Blocks with a timestamp after this should enforce BIP 16, aka "Pay to script hash". This BIP changed the
         * network rules in a soft-forking manner, that is, blocks that don't follow the rules are accepted but not
         * mined upon and thus will be quickly re-orged out as long as the majority are enforcing the rule.
         */
        val BIP16_ENFORCE_TIME = 1333238400

        /**
         * The maximum number of coins to be generated
         */
        val MAX_COINS: Long = 21000000

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
/*

        private fun createGenesis(n: NetworkParameters): Block {
            val genesisBlock = Block(n, Block.BLOCK_VERSION_GENESIS)
            val t = Transaction(n)
            // A script containing the difficulty bits and the following message:
            //   "The Times 03/Jan/2009 Chancellor on brink of second bailout for banks"
            val bytes = Utils.HEX.decode("04ffff001d0104455468652054696d65732030332f4a616e2f32303039204368616e63656c6c6f72206f6e206272696e6b206f66207365636f6e64206261696c6f757420666f722062616e6b73")
            t.addInput(TransactionInput(n, t, bytes))
            val scriptPubKeyBytes = ByteArrayOutputStream()
            Script.writeBytes(scriptPubKeyBytes, Utils.HEX.decode("04678afdb0fe5548271967f1a67130b7105cd6a828e03909a67962e0ea1f61deb649f6bc3f4cef38c4f35504e51ec112de5c384df7ba0b8d578a4c702b6bf11d5f"))
            scriptPubKeyBytes.write(ScriptOpCodes.OP_CHECKSIG)
            t.addOutput(TransactionOutput(n, t, Coin.FIFTY_COINS, scriptPubKeyBytes.toByteArray()))
            genesisBlock.addTransaction(t)
            return genesisBlock
        }
*/

    }
}
