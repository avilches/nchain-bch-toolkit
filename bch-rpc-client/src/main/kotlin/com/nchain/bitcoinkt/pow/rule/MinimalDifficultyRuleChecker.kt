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
import java.lang.Long

/**
 * After 15th February 2012 the rules on the testnet change to avoid people running up the difficulty
 * and then leaving, making it too hard to mine a block. On non-difficulty transition points, easy
 * blocks are allowed if there has been a span of 20 minutes without one.
 */
class MinimalDifficultyRuleChecker(networkParameters: NetworkParameters) : AbstractPowRulesChecker(networkParameters) {

    @Throws(VerificationException::class, BlockStoreException::class)
    override fun checkRules(storedPrev: StoredBlock, nextBlock: Block, blockStore: BlockStore,
                            blockChain: AbstractBlockChain) {
        val prevBlock = storedPrev.header
        if (isPeriodExceed(prevBlock, nextBlock)) {
            checkMinimalDifficultyIsSet(nextBlock)
        }
    }

    /**
     * There is an integer underflow bug in bitcoin-qt that means mindiff blocks are accepted
     * when time goes backwards.
     */
    private fun isPeriodExceed(prevBlock: Block, nextBlock: Block): Boolean {
        val timeDelta = nextBlock.timeSeconds - prevBlock.timeSeconds
        return timeDelta >= 0 && timeDelta > NetworkParameters.TARGET_SPACING * 2
    }

    private fun checkMinimalDifficultyIsSet(nextBlock: Block) {
        val maxTarget = networkParameters.maxTarget!!
        if (!hasEqualDifficulty(nextBlock.getDifficultyTarget(), maxTarget)) {
            throw VerificationException("Testnet block transition that is not allowed: " +
                    Long.toHexString(ByteUtils.encodeCompactBits(maxTarget!!)) + " (required min difficulty) vs " +
                    Long.toHexString(nextBlock.getDifficultyTarget()))
        }
    }

}
