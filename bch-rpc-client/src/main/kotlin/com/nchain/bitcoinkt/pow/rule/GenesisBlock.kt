package com.nchain.bitcoinkt.pow.rule

import com.nchain.bitcoinkt.core.Block
import com.nchain.params.*
import com.nchain.script.Script
import com.nchain.script.ScriptOpCodes
import com.nchain.tools.HEX
import com.nchain.tx.Coin
import com.nchain.tx.TransactionBuilder
import com.nchain.tx.TransactionInput
import com.nchain.tx.TransactionOutput
import java.io.ByteArrayOutputStream

/*
 * @author Alberto Vilches
 * @date 24/09/2018
 */
object GenesisBlock {
    private val map = mapOf(
            MainNetParams to createMainNetGenesisBlock(),
            TestNet3Params to createTestNetGenesisBlock(),
            RegTestParams to createRegTestGenesisBlock(),
            UnitTestParams to createUnitTestGenesisBlock()
    )

    @JvmStatic
    fun of(params: NetworkParameters): Block {
        return map[params]!!
    }

    private fun createMainNetGenesisBlock(): Block {
        val genesisBlock = createGenesis(MainNetParams)
        genesisBlock.setDifficultyTarget(0x1d00ffffL)
        genesisBlock.setTime(1231006505L)
        genesisBlock.setNonce(2083236893)

        if (genesisBlock.hashAsString != "000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f")
            throw RuntimeException("genesis block hash incorrect")

        return genesisBlock
    }

    private fun createTestNetGenesisBlock(): Block {
        val genesisBlock = createGenesis(TestNet3Params)
        genesisBlock.setTime(1296688602L)
        genesisBlock.setDifficultyTarget(0x1d00ffffL)
        genesisBlock.setNonce(414098458)
        if (genesisBlock.hashAsString != "000000000933ea01ad0ee984209779baaec3ced90fa3f408719526f8d77f4943")
            throw RuntimeException("genesis block hash incorrect")

        return genesisBlock
    }

    private fun createRegTestGenesisBlock(): Block {
        val genesisBlock = createGenesis(RegTestParams)
        genesisBlock.setNonce(2)
        genesisBlock.setDifficultyTarget(0x207fFFFFL)
        genesisBlock.setTime(1296688602L)
        if (genesisBlock.hashAsString.toLowerCase() != "0f9188f13cb7b2c71f2a335e3a4fc328bf5beb436012afca590b1a11466e2206")
            throw RuntimeException("genesis block hash is incorrect")
        return genesisBlock
    }

    private fun createUnitTestGenesisBlock(): Block {
        val genesisBlock = createGenesis(UnitTestParams)
        genesisBlock.setTime(System.currentTimeMillis() / 1000)
        genesisBlock.setDifficultyTarget(Block.EASIEST_DIFFICULTY_TARGET)
        genesisBlock.solve()
        return genesisBlock
    }

    private fun createGenesis(n: NetworkParameters): com.nchain.bitcoinkt.core.Block {
        val genesisBlock = com.nchain.bitcoinkt.core.Block(n, com.nchain.bitcoinkt.core.Block.BLOCK_VERSION_GENESIS)
        val t = TransactionBuilder()
        // A script containing the difficulty bits and the following message:
        //   "The Times 03/Jan/2009 Chancellor on brink of second bailout for banks"
        val bytes = HEX.decode("04ffff001d0104455468652054696d65732030332f4a616e2f32303039204368616e63656c6c6f72206f6e206272696e6b206f66207365636f6e64206261696c6f757420666f722062616e6b73")
        t.addInput(TransactionInput(bytes))
        val scriptPubKeyBytes = ByteArrayOutputStream()
        Script.writeBytes(scriptPubKeyBytes, HEX.decode("04678afdb0fe5548271967f1a67130b7105cd6a828e03909a67962e0ea1f61deb649f6bc3f4cef38c4f35504e51ec112de5c384df7ba0b8d578a4c702b6bf11d5f"))
        scriptPubKeyBytes.write(ScriptOpCodes.OP_CHECKSIG)
        t.addOutput(TransactionOutput(Coin.FIFTY_COINS, scriptPubKeyBytes.toByteArray()))
        genesisBlock.addTransaction(t.build())
        return genesisBlock
    }

}