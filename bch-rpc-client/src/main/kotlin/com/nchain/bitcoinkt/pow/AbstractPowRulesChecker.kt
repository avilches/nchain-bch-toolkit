/*
 * Copyright 2018 the bitcoinj-cash developers
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

package com.nchain.bitcoinkt.pow

import com.nchain.bitcoinkt.core.*
import com.nchain.params.NetworkParameters
import com.nchain.bitcoinkt.store.BlockStore
import com.nchain.bitcoinkt.store.BlockStoreException
import com.nchain.shared.VerificationException
import com.nchain.tools.ByteUtils

import java.math.BigInteger

abstract class AbstractPowRulesChecker(protected var networkParameters: NetworkParameters) {

    @Throws(VerificationException::class, BlockStoreException::class)
    abstract fun checkRules(storedPrev: StoredBlock, nextBlock: Block, blockStore: BlockStore,
                            blockChain: AbstractBlockChain)

    companion object {

        @JvmStatic
        fun hasEqualDifficulty(prevBlock: Block, nextBlock: Block): Boolean {
            return prevBlock.getDifficultyTarget() == nextBlock.getDifficultyTarget()
        }

        @JvmStatic
        fun hasEqualDifficulty(a: Long, b: BigInteger): Boolean {
            return a == ByteUtils.encodeCompactBits(b)
        }

    }

    // TODO vilches remove the ? and the !!
    fun verifyDifficulty(newTarget: BigInteger?, nextBlock: Block) {
        var newTarget = newTarget
        if (newTarget!!.compareTo(networkParameters.maxTarget!!) > 0) {
            newTarget = networkParameters.maxTarget
        }

        val accuracyBytes = nextBlock.getDifficultyTarget().ushr(24).toInt() - 3
        val receivedTargetCompact = nextBlock.getDifficultyTarget()

        // The calculated difficulty is to a higher precision than received, so reduce here.
        val mask = BigInteger.valueOf(0xFFFFFFL).shiftLeft(accuracyBytes * 8)
        newTarget = newTarget!!.and(mask)
        val newTargetCompact = ByteUtils.encodeCompactBits(newTarget!!)

        if (newTargetCompact != receivedTargetCompact)
            throw VerificationException("Network provided difficulty bits do not match what was calculated: " +
                    java.lang.Long.toHexString(newTargetCompact) + " vs " + java.lang.Long.toHexString(receivedTargetCompact))
    }


}
