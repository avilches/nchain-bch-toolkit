/*
 * Copyright 2013 Google Inc.
 * Copyright 2015 Andreas Schildbach
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

import com.nchain.tools.ByteUtils


const val MAINNET_ADDRESS_HEADER = 0
const val MAINNET_P2SH_HEADER = 5

/**
 * Parameters for the main production network
 */
object MainNetParams : NetworkParameters(
        id = NetworkParameters.ID_MAINNET,
        cashAddrPrefix = "bitcoincash",
        addressHeader = MAINNET_ADDRESS_HEADER,
        p2SHHeader = MAINNET_P2SH_HEADER,
        dumpedPrivateKeyHeader = 128,
        acceptableAddressCodes = intArrayOf(MAINNET_ADDRESS_HEADER, MAINNET_P2SH_HEADER),
        bip32HeaderPub = 0x0488B21E, //The 4 byte header that serializes in base58 to "xpub".
        bip32HeaderPriv = 0x0488ADE4, //The 4 byte header that serializes in base58 to "xprv"

        maxTarget = ByteUtils.decodeCompactBits(0x1d00ffffL),
        majorityEnforceBlockUpgrade = 750,
        majorityRejectBlockOutdated = 950,
        majorityWindow = 1000,
        subsidyDecreaseBlockCount = 210000,
        spendableCoinbaseDepth = 100,

        uahfHeight = 478559,
        daaUpdateHeight = 504031,
        monolithHeight = 530359,

        packetMagic = 0xe3e1f3e8L,
        port = 8333,
        dnsSeeds = arrayOf("seed.bitcoinabc.org", "seed-abc.bitcoinforks.org", "btccash-seeder.bitcoinunlimited.info",
                "seed.bitprim.org", "seed.deadalnix.me", "seeder.criptolayer.net"),
        addrSeeds = emptyArray()

) {

    init {
//        genesisBlock.setDifficultyTarget(0x1d00ffffL)
//        genesisBlock.setTime(1231006505L)
//        genesisBlock.setNonce(2083236893)

//        if (genesisBlock.hashAsString != "000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f")
//                throw RuntimeException("genesis block hash incorrect")

        // This contains (at a minimum) the blocks which are not BIP30 compliant. BIP30 changed how duplicate
        // transactions are handled. Duplicated transactions could occur in the case where a coinbase had the same
        // extraNonce and the same outputs but appeared at different heights, and greatly complicated re-org handling.
        // Having these here simplifies block connection logic considerably.
//        checkpoints[91722] = Sha256Hash.wrap("00000000000271a2dc26e7667f8419f2e15416dc6955e5a6c6cdf3f2574dd08e")
//        checkpoints[91812] = Sha256Hash.wrap("00000000000af0aed4792b1acee3d966af36cf5def14935db8de83d6f9306f2f")
//        checkpoints[91842] = Sha256Hash.wrap("00000000000a4d0a398161ffc163c503763b1f4360639393e0e4c8e300e0caec")
//        checkpoints[91880] = Sha256Hash.wrap("00000000000743f190a18c5577a3c2d2a1f610ae9601ac046a38084ccb7cd721")
//        checkpoints[200000] = Sha256Hash.wrap("000000000000034a7dedef4a161fa058a2d67a173a90155f3a2fe6fc132e0ebf")
//        checkpoints[478559] = Sha256Hash.wrap("000000000000000000651ef99cb9fcbe0dadde1d424bd9f15ff20136191a5eec")
    }
}
