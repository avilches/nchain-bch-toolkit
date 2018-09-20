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
import com.nchain.bitcoinkt.pow.AbstractRuleCheckerFactory
import com.nchain.bitcoinkt.pow.RulesPoolChecker
import com.nchain.bitcoinkt.pow.rule.RegTestRuleChecker
import com.nchain.params.NetworkParameters

class RuleCheckerFactory private constructor(params: NetworkParameters) : AbstractRuleCheckerFactory(params) {

    private val regTestChecker: RulesPoolChecker
    private val daaRulesFactory: AbstractRuleCheckerFactory
    private val edaRulesFactory: AbstractRuleCheckerFactory

    init {
        this.regTestChecker = RulesPoolChecker(params)
        this.regTestChecker.addRule(RegTestRuleChecker(params))
        this.daaRulesFactory = DAARuleCheckerFactory(params)
        this.edaRulesFactory = EDARuleCheckerFactory(params)
    }

    override fun getRuleChecker(storedPrev: StoredBlock, nextBlock: Block): RulesPoolChecker {
        return if (NetworkParameters.ID_REGTEST.equals(params.id)) {
            regTestChecker
        } else if (isNewDaaActivated(storedPrev, params)) {
            daaRulesFactory.getRuleChecker(storedPrev, nextBlock)
        } else {
            edaRulesFactory.getRuleChecker(storedPrev, nextBlock)
        }
    }

    private fun isNewDaaActivated(storedPrev: StoredBlock, parameters: NetworkParameters): Boolean {
        return storedPrev.height >= parameters.daaUpdateHeight
    }

    companion object {

        fun create(parameters: NetworkParameters): RuleCheckerFactory {
            return RuleCheckerFactory(parameters)
        }
    }

}
