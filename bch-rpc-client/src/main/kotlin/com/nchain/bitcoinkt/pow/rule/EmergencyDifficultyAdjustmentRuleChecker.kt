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

package com.nchain.bitcoinkt.pow.rule

import com.nchain.bitcoinkt.core.*
import com.nchain.bitcoinkt.pow.AbstractPowRulesChecker
import com.nchain.bitcoinkt.store.BlockStore
import com.nchain.bitcoinkt.store.BlockStoreException
import com.nchain.params.NetworkParameters
import com.nchain.shared.VerificationException
import com.nchain.tools.ByteUtils

import java.math.BigInteger

class EmergencyDifficultyAdjustmentRuleChecker(networkParameters: NetworkParameters) : AbstractPowRulesChecker(networkParameters) {

    @Throws(VerificationException::class, BlockStoreException::class)
    override fun checkRules(storedPrev: StoredBlock, nextBlock: Block, blockStore: BlockStore,
                            blockChain: AbstractBlockChain) {

        try {
            val lastBlocksMPTinSeconds = getMedianProducingTimeInSeconds(REFERENCE_OF_BLOCKS_PRODUCED_SIZE,
                    storedPrev, blockStore)
            checkEDARules(storedPrev, nextBlock, lastBlocksMPTinSeconds)
        } catch (ex: NullPointerException) {
            // We don't have enough blocks, yet
        } catch (ex: BlockStoreException) {
            // We don't have enough blocks, yet
        }

    }

    @Throws(BlockStoreException::class)
    private fun getMedianProducingTimeInSeconds(sizeOfBlocks: Int, storedPrev: StoredBlock, blockStore: BlockStore): Long {
        var cursor = blockStore[storedPrev.header.hash!!]
        for (i in 0 until sizeOfBlocks) {
            if (cursor == null) {
                throw NullPointerException("Not enough blocks to check difficulty.")
            }
            cursor = blockStore[cursor.header.getPrevBlockHash()!!]
        }
        //Check to see if there are enough blocks before cursor to correctly calculate the median time
        var beforeCursor = cursor
        for (i in 0..9) {
            beforeCursor = blockStore[beforeCursor!!.header.getPrevBlockHash()!!]
            if (beforeCursor == null)
                throw NullPointerException("Not enough blocks to check difficulty.")
        }
        return AbstractBlockChain.getMedianTimestampOfRecentBlocks(storedPrev, blockStore) - AbstractBlockChain.getMedianTimestampOfRecentBlocks(cursor, blockStore)
    }

    private fun checkEDARules(storedPrev: StoredBlock, nextBlock: Block, lastBlocksMPTinSeconds: Long) {
        val prevBlock = storedPrev.header
        if (needToReduceTheDifficulty(lastBlocksMPTinSeconds)) {
            val nPow = calculateReducedDifficulty(prevBlock)
            if (!hasEqualDifficulty(nextBlock.getDifficultyTarget(), nPow)) {
                throwUnexpectedReducedDifficultyException(storedPrev, nextBlock, nPow)
            }
        } else {
            if (!hasEqualDifficulty(prevBlock, nextBlock)) {
                throwUnexpectedDifficultyChangedException(prevBlock, nextBlock, storedPrev)
            }
        }
    }

    private fun needToReduceTheDifficulty(lastBlocksMPTinSeconds: Long): Boolean {
        return lastBlocksMPTinSeconds >= TARGET_PRODUCTION_TIME_IN_SECONDS
    }

    private fun calculateReducedDifficulty(prevBlock: Block): BigInteger {
        var pow: BigInteger = prevBlock.difficultyTargetAsInteger
        // Divide difficulty target by 1/4 (which reduces the difficulty by 20%)
        pow = pow.add(pow.shiftRight(2))

        if (pow.compareTo(networkParameters.maxTarget!!) > 0) {
            pow = networkParameters.maxTarget!!
        }
        return pow
    }

    private fun throwUnexpectedReducedDifficultyException(storedPrev: StoredBlock, nextBlock: Block, nPow: BigInteger) {
        throw VerificationException("Unexpected change in difficulty [6 blocks >12 hours] at height " + storedPrev.height +
                ": " + java.lang.Long.toHexString(nextBlock.getDifficultyTarget()) + " vs " +
                 ByteUtils.encodeCompactBits(nPow))
    }

    private fun throwUnexpectedDifficultyChangedException(prevBlock: Block, nextBlock: Block, storedPrev: StoredBlock) {
        throw VerificationException("Unexpected change in difficulty at height " + storedPrev.height +
                ": " + java.lang.Long.toHexString(nextBlock.getDifficultyTarget()) + " vs " +
                java.lang.Long.toHexString(prevBlock.getDifficultyTarget()))
    }

    companion object {

        private val TARGET_PRODUCTION_TIME_IN_SECONDS = (12 * 60 * 60).toLong() // 12 hours
        private val REFERENCE_OF_BLOCKS_PRODUCED_SIZE = 6
    }

}
