package com.nchain.bitcoinkt.pow.rule

import com.nchain.bitcoinkt.core.AbstractBlockChain
import com.nchain.bitcoinkt.core.Block
import com.nchain.bitcoinkt.core.StoredBlock
import com.nchain.bitcoinkt.pow.AbstractPowRulesChecker
import com.nchain.bitcoinkt.store.BlockStore
import com.nchain.params.NetworkParameters


class RegTestRuleChecker(networkParameters: NetworkParameters) : AbstractPowRulesChecker(networkParameters) {

    override fun checkRules(storedPrev: StoredBlock, nextBlock: Block, blockStore: BlockStore,
                   blockChain: AbstractBlockChain) {
        // always pass
    }
}
