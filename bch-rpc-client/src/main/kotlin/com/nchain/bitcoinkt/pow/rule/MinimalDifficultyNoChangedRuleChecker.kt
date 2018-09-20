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

class MinimalDifficultyNoChangedRuleChecker(networkParameters: NetworkParameters) : AbstractPowRulesChecker(networkParameters) {

    @Throws(VerificationException::class, BlockStoreException::class)
    override fun checkRules(storedPrev: StoredBlock, nextBlock: Block, blockStore: BlockStore,
                            blockChain: AbstractBlockChain) {
        val prevBlock = storedPrev.header
        val minDifficulty = networkParameters.maxTarget!!

        if (hasEqualDifficulty(prevBlock.getDifficultyTarget(), minDifficulty)) {
            if (!hasEqualDifficulty(prevBlock, nextBlock)) {
                throw VerificationException("Unexpected change in difficulty at height " +
                        storedPrev.height + ": " +
                        Long.toHexString(nextBlock.getDifficultyTarget()) + " vs " +
                        Long.toHexString(prevBlock.getDifficultyTarget()))
            }
        }
    }

}
