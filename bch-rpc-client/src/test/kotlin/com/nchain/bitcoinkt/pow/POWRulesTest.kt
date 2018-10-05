package com.nchain.bitcoinkt.pow

import com.nchain.bitcoinkt.core.Block
import com.nchain.bitcoinkt.core.BlockChain
import com.nchain.bitcoinkt.core.Context
import com.nchain.bitcoinkt.core.StoredBlock
import com.nchain.bitcoinkt.pow.factory.RuleCheckerFactory
import com.nchain.bitcoinkt.store.MemoryBlockStore
import com.nchain.params.RegTestParams
import org.junit.Test
import java.math.BigInteger


class POWRulesTest {
    private val regTestParams = RegTestParams

    // regtest network does not have pow rule
    @Test
    fun testRegtestPOW() {
        val block = Block(regTestParams, 1)
        val storedBlock = StoredBlock(block, BigInteger.valueOf(1), 1)
        val blockStore = MemoryBlockStore(regTestParams)
        val context = Context(regTestParams)
        val blockChain = BlockChain(context, blockStore)
        val ruleCheckerFactory = RuleCheckerFactory.create(regTestParams)
        val rulesChecker = ruleCheckerFactory.getRuleChecker(storedBlock, block)
        rulesChecker.checkRules(storedBlock, block, blockStore, blockChain)
    }
}
