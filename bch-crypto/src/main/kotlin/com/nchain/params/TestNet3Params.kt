/*
 * Copyright 2013 Google Inc.
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

const val TESTNET_ADDRESS_HEADER = 111
const val TESTNET_P2SH_HEADER = 196

/**
 * Parameters for the testnet, a separate public instance of Bitcoin that has relaxed rules suitable for development
 * and testing of applications and new Bitcoin versions.
 */
object TestNet3Params : NetworkParameters(
        id = NetworkParameters.ID_TESTNET,
        cashAddrPrefix = "bchtest",
        addressHeader = TESTNET_ADDRESS_HEADER,
        p2SHHeader = TESTNET_P2SH_HEADER,
        dumpedPrivateKeyHeader = 239,
        acceptableAddressCodes = intArrayOf(TESTNET_ADDRESS_HEADER, TESTNET_P2SH_HEADER),
        bip32HeaderPub = 0x043587CF,
        bip32HeaderPriv = 0x04358394

//        maxTarget = ByteUtils.decodeCompactBits(0x1d00ffffL),
//        majorityEnforceBlockUpgrade = 51,
//        majorityRejectBlockOutdated = 75,
//        majorityWindow = 100,
//        subsidyDecreaseBlockCount = 210000,
//        spendableCoinbaseDepth = 100,
//
//        uahfHeight = 1155876,
//        daaUpdateHeight = 1188697,
//        monolithHeight = 1233078,
//
//        packetMagic = 0xf4e5f3f4L,
//        port = 18333,
//        dnsSeeds = arrayOf("testnet-seed.bitcoinabc.org", "testnet-seed-abc.bitcoinforks.org",
//                "testnet-seed.bitcoinunlimited.info", "testnet-seed.bitprim.org", "testnet-seed.deadalnix.me",
//                "testnet-seeder.criptolayer.net"),
//        addrSeeds = emptyArray()
)
{

    // February 16th 2012
//    private val testnetDiffDate = Date(1329264000000L)
/*

    init {
        genesisBlock.setTime(1296688602L)
        genesisBlock.setDifficultyTarget(0x1d00ffffL)
        genesisBlock.setNonce(414098458)
        if (genesisBlock.hashAsString != "000000000933ea01ad0ee984209779baaec3ced90fa3f408719526f8d77f4943")
            throw RuntimeException("genesis block hash incorrect")
    }

    fun isValidTestnetDateBlock(block: Block): Boolean {
        return block.time.after(testnetDiffDate)
    }
*/
}
