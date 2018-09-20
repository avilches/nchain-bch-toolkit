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
import java.lang.Long

/**
 * Walk backwards until we find a block that doesn't have the easiest proof of work,
 * then check that difficulty is equal to that one.
 */
class LastNonMinimalDifficultyRuleChecker(networkParameters: NetworkParameters) : AbstractPowRulesChecker(networkParameters) {

    @Throws(VerificationException::class, BlockStoreException::class)
    override fun checkRules(storedPrev: StoredBlock, nextBlock: Block, blockStore: BlockStore,
                            blockChain: AbstractBlockChain) {
        val prevBlock = storedPrev.header
        if (isUnderPeriod(prevBlock, nextBlock)) {
            checkLastNonMinimalDifficultyIsSet(storedPrev, blockStore, nextBlock)
        }
    }

    private fun isUnderPeriod(prevBlock: Block, nextBlock: Block): Boolean {
        val timeDelta = nextBlock.timeSeconds - prevBlock.timeSeconds
        return timeDelta >= 0 && timeDelta <= NetworkParameters.TARGET_SPACING * 2
    }

    @Throws(BlockStoreException::class)
    private fun checkLastNonMinimalDifficultyIsSet(storedPrev: StoredBlock, blockStore: BlockStore, nextBlock: Block) {
        try {
            val lastNotEasiestPowBlock = findLastNotEasiestPowBlock(storedPrev, blockStore)
            if (!hasEqualDifficulty(lastNotEasiestPowBlock, nextBlock))
                throw VerificationException("Testnet block transition that is not allowed: " +
                        Long.toHexString(lastNotEasiestPowBlock.getDifficultyTarget()) + " vs " +
                        Long.toHexString(nextBlock.getDifficultyTarget()))
        } catch (ex: BlockStoreException) {
            // we don't have enough blocks, yet
        }

    }

    @Throws(BlockStoreException::class)
    private fun findLastNotEasiestPowBlock(storedPrev: StoredBlock, blockStore: BlockStore): Block {
        var cursor: StoredBlock? = storedPrev
        val easiestDifficulty = networkParameters.maxTarget!!
        while (cursor!!.header != networkParameters.genesisBlock &&
                cursor.height % networkParameters.interval != 0 &&
                hasEqualDifficulty(cursor.header.getDifficultyTarget(), easiestDifficulty)) {
            cursor = cursor.getPrev(blockStore)
        }
        return cursor.header
    }

}
