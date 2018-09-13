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

package com.nchain.params

import java.math.BigInteger

const val UNITTEST_ADDRESS_HEADER = 111
const val UNITTEST_P2SH_HEADER = 196

/**
 * Network parameters used by the bitcoinj unit tests (and potentially your own). This lets you solve a block using
 * [com.nchain.bitcoinkt.core.Block.solve] by setting difficulty to the easiest possible.
 */
object UnitTestParams : NetworkParameters(
        id = ID_UNITTESTNET,
        cashAddrPrefix = "bchtest",
        addressHeader = UNITTEST_ADDRESS_HEADER,
        p2SHHeader = UNITTEST_P2SH_HEADER,
        dumpedPrivateKeyHeader = 239,
        acceptableAddressCodes = intArrayOf(UNITTEST_ADDRESS_HEADER, UNITTEST_P2SH_HEADER),
        bip32HeaderPub = 0x043587CF,
        bip32HeaderPriv = 0x04358394,

//        maxTarget = BigInteger("00ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16),
//        majorityEnforceBlockUpgrade = 3,
//        majorityRejectBlockOutdated = 4,
//        majorityWindow = 7,
//        subsidyDecreaseBlockCount = 100,
//        spendableCoinbaseDepth = 5,
//        interval = 10,
//        targetTimespan = 200000000,
//
//        uahfHeight = 0,
//        daaUpdateHeight = 1000,
//        monolithHeight = 2000,
//
        packetMagic = 0xf4e5f3f4L,
        port = 18333,
        dnsSeeds = emptyArray()
//        addrSeeds = emptyArray()
)
{

/*
    init {
        genesisBlock.setTime(System.currentTimeMillis() / 1000)
        genesisBlock.setDifficultyTarget(Block.EASIEST_DIFFICULTY_TARGET)
        genesisBlock.solve()
    }
*/
}
