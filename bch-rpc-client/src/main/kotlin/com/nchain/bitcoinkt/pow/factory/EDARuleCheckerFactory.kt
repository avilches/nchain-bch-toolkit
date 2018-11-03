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

package com.nchain.bitcoinkt.pow.factory

import com.nchain.bitcoinkt.core.Block
import com.nchain.bitcoinkt.core.StoredBlock
import com.nchain.bitcoinkt.pow.AbstractPowRulesChecker
import com.nchain.bitcoinkt.pow.AbstractRuleCheckerFactory
import com.nchain.bitcoinkt.pow.RulesPoolChecker
import com.nchain.bitcoinkt.pow.rule.DifficultyTransitionPointRuleChecker
import com.nchain.bitcoinkt.pow.rule.EmergencyDifficultyAdjustmentRuleChecker
import com.nchain.bitcoinkt.pow.rule.LastNonMinimalDifficultyRuleChecker
import com.nchain.bitcoinkt.pow.rule.MinimalDifficultyNoChangedRuleChecker
import com.nchain.params.NetworkParameters
import com.nchain.params.TestNet3Params

class EDARuleCheckerFactory(params: NetworkParameters) : AbstractRuleCheckerFactory(params) {

    private val transitionPointRulesChecker: RulesPoolChecker
        get() {
            val rulesChecker = RulesPoolChecker(params)
            rulesChecker.addRule(DifficultyTransitionPointRuleChecker(params))
            return rulesChecker
        }

    override fun getRuleChecker(storedPrev: StoredBlock, nextBlock: Block): RulesPoolChecker {
        return if (params.isDifficultyTransitionPoint(storedPrev.height)) {
            transitionPointRulesChecker
        } else {
            getNoTransitionPointRulesChecker(storedPrev, nextBlock)
        }
    }

    private fun getNoTransitionPointRulesChecker(storedPrev: StoredBlock, nextBlock: Block): RulesPoolChecker {
        val rulesChecker = RulesPoolChecker(params)
        if (isTestNet && TestNet3Params.isValidTestnetDateBlock(nextBlock.time)) {
            rulesChecker.addRule(LastNonMinimalDifficultyRuleChecker(params))
        } else {
            if (AbstractPowRulesChecker.hasEqualDifficulty(
                            storedPrev.header.getDifficultyTarget(), params.maxTarget)) {
                rulesChecker.addRule(MinimalDifficultyNoChangedRuleChecker(params))
            } else {
                rulesChecker.addRule(EmergencyDifficultyAdjustmentRuleChecker(params))
            }
        }
        return rulesChecker
    }

}