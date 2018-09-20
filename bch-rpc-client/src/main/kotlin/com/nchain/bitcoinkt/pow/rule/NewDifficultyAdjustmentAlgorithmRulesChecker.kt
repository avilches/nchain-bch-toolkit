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

import com.google.common.base.Preconditions
import com.nchain.bitcoinkt.core.*
import com.nchain.bitcoinkt.pow.AbstractPowRulesChecker
import com.nchain.bitcoinkt.store.BlockStore
import com.nchain.bitcoinkt.store.BlockStoreException
import com.nchain.params.NetworkParameters
import com.nchain.shared.VerificationException
import java.math.BigInteger

/**
 * The new DAA algorithm seeks to accomplish the following objectives:
 * - Adjust difficulty to hash rate to target a mean block interval of 600 seconds.
 * - Avoid sudden changes in difficulty when hash rate is fairly stable.
 * - Adjust difficulty rapidly when hash rate changes rapidly.
 * - Avoid oscillations from feedback between hash rate and difficulty.
 * - Be resilient to attacks such as timestamp manipulation.
 *
 *
 * https://www.bitcoinabc.org/november
 */
class NewDifficultyAdjustmentAlgorithmRulesChecker(networkParameters: NetworkParameters) : AbstractPowRulesChecker(networkParameters) {

    @Throws(VerificationException::class, BlockStoreException::class)
    override fun checkRules(storedPrev: StoredBlock, nextBlock: Block, blockStore: BlockStore, blockChain: AbstractBlockChain) {
        checkNextCashWorkRequired(storedPrev, nextBlock, blockStore)
    }

    /**
     * Compute the next required proof of work using a weighted average of the
     * estimated hashrate per block.
     *
     *
     * Using a weighted average ensure that the timestamp parameter cancels out in
     * most of the calculation - except for the timestamp of the first and last
     * block. Because timestamps are the least trustworthy information we have as
     * input, this ensures the algorithm is more resistant to malicious inputs.
     */
    private fun checkNextCashWorkRequired(storedPrev: StoredBlock, nextBlock: Block, blockStore: BlockStore) {
        val prevHeight = storedPrev.height
        Preconditions.checkState(prevHeight >= networkParameters.interval)

        try {
            val last = GetMostSuitableBlock(storedPrev, blockStore)
            val first = getFirst(storedPrev, blockStore)

            val nextTarget = ComputeTarget(first, last)
            verifyDifficulty(nextTarget, nextBlock)
        } catch (x: BlockStoreException) {
            // We don't have enough blocks, yet
        }
    }


    fun ComputeTarget(firstBlock: StoredBlock?,
                      lastBlock: StoredBlock): BigInteger {
        check(lastBlock.height > firstBlock!!.height)

        /*
         * From the total work done and the time it took to produce that much work,
         * we can deduce how much work we expect to be produced in the targeted time
         * between blocks.
         * */


        var work = lastBlock.chainWork.subtract(firstBlock.chainWork)
        work = work.multiply(BigInteger.valueOf(NetworkParameters.TARGET_SPACING.toLong()))

        // In order to avoid difficulty cliffs, we bound the amplitude of the
        // adjustment we are going to do.
        check(lastBlock.header.timeSeconds > firstBlock.header.timeSeconds)
        var nActualTimespan = lastBlock.header.timeSeconds - firstBlock.header.timeSeconds
        if (nActualTimespan > 288 * NetworkParameters.TARGET_SPACING) {
            nActualTimespan = (288 * NetworkParameters.TARGET_SPACING).toLong()
        } else if (nActualTimespan < 72 * NetworkParameters.TARGET_SPACING) {
            nActualTimespan = (72 * NetworkParameters.TARGET_SPACING).toLong()
        }

        work = work.divide(BigInteger.valueOf(nActualTimespan))

        /*
         * We need to compute T = (2^256 / W) - 1.
         * This code differs from Bitcoin-ABC in that we are using
         * BigIntegers instead of a data type that is limited to 256 bits.
         */


        return NetworkParameters.LARGEST_HASH.divide(work).subtract(BigInteger.ONE)
    }


    /**
     * To reduce the impact of timestamp manipulation, we select the block we are
     * basing our computation on via a median of 3.
     */
    @Throws(BlockStoreException::class)
    private fun GetMostSuitableBlock(pindex: StoredBlock, blockStore: BlockStore): StoredBlock {
        /**
         * In order to avoid a block is a very skewed timestamp to have too much
         * influence, we select the median of the 3 top most blocks as a starting
         * point.
         */
        val blocks = arrayOfNulls<StoredBlock>(3)
        blocks[2] = pindex
        blocks[1] = pindex.getPrev(blockStore)
        if (blocks[1] == null)
            throw BlockStoreException("Not enough blocks in blockStore to calculate difficulty")
        blocks[0] = blocks[1]!!.getPrev(blockStore)
        if (blocks[0] == null)
            throw BlockStoreException("Not enough blocks in blockStore to calculate difficulty")

        // Sorting network.
        if (blocks[0]!!.header.timeSeconds > blocks[2]!!.header.timeSeconds) {
            //std::swap(blocks[0], blocks[2]);
            val temp = blocks[0]
            blocks[0] = blocks[2]
            blocks[2] = temp
        }

        if (blocks[0]!!.header.timeSeconds > blocks[1]!!.header.timeSeconds) {
            //std::swap(blocks[0], blocks[1]);
            val temp = blocks[0]
            blocks[0] = blocks[1]
            blocks[1] = temp
        }

        if (blocks[1]!!.header.timeSeconds > blocks[2]!!.header.timeSeconds) {
            //std::swap(blocks[1], blocks[2]);
            val temp = blocks[1]
            blocks[1] = blocks[2]
            blocks[2] = temp
        }

        // We should have our candidate in the middle now.
        return blocks[1]!!
    }

    @Throws(BlockStoreException::class)
    private fun getFirst(storedPrev: StoredBlock, blockStore: BlockStore): StoredBlock {
        var first: StoredBlock? = storedPrev
        for (i in AVERAGE_BLOCKS_PER_DAY downTo 1) {
            first = first!!.getPrev(blockStore)
            if (first == null) {
                throw BlockStoreException("The previous block no longer exists")
            }
        }
        return GetMostSuitableBlock(first!!, blockStore)
    }

    companion object {

        private val AVERAGE_BLOCKS_PER_DAY = 144
    }

}
