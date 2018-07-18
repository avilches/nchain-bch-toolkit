/*
 * Copyright 2013 Google Inc.
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


/**
 * Network parameters for the regression test mode of bitcoind in which all blocks are trivially solvable.
 */
object RegTestParams : NetworkParameters(
        id = ID_REGTEST,
        cashAddrPrefix = "bchreg",
        addressHeader = TESTNET_ADDRESS_HEADER,
        p2SHHeader = TESTNET_P2SH_HEADER,
        dumpedPrivateKeyHeader = 239,
        acceptableAddressCodes = intArrayOf(TESTNET_ADDRESS_HEADER, TESTNET_P2SH_HEADER),
        bip32HeaderPub = 0x043587CF, //The 4 byte header that serializes in base58 to "xpub".
        bip32HeaderPriv = 0x04358394, //The 4 byte header that serializes in base58 to "xprv"

        maxTarget = BigInteger("7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16),
        majorityEnforceBlockUpgrade = 750,
        majorityRejectBlockOutdated = 950,
        majorityWindow = 1000,
        subsidyDecreaseBlockCount = 150,
        spendableCoinbaseDepth = 100,
        interval = Integer.MAX_VALUE, // Difficulty adjustments are disabled for regtest.

        uahfHeight = 0,
        daaUpdateHeight = 0,
        monolithHeight = 0,

        packetMagic = 0xe3e1f3e8L,
        port = 18444,
        dnsSeeds = emptyArray(),
        addrSeeds = emptyArray()
)
{

    init {
//        genesisBlock.setNonce(2)
//        genesisBlock.setDifficultyTarget(0x207fFFFFL)
//        genesisBlock.setTime(1296688602L)
//        if (genesisBlock.hashAsString.toLowerCase() != "0f9188f13cb7b2c71f2a335e3a4fc328bf5beb436012afca590b1a11466e2206")
//            throw RuntimeException("genesis block hash is incorrect")
    }

    override fun allowEmptyPeerChain(): Boolean {
        return true
    }
}
