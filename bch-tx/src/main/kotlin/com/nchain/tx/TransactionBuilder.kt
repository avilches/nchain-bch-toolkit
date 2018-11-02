/*
 * Copyright 2011 Google Inc.
 * Copyright 2014 Andreas Schildbach
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
 */

package com.nchain.tx

import com.nchain.address.CashAddress
import com.nchain.key.ECKey
import com.nchain.shared.Sha256Hash
import com.nchain.shared.VarInt
import com.nchain.tools.MessageReader
import com.nchain.tools.hexStringToByteArray
import com.nchain.script.Script
import com.nchain.script.ScriptBuilder
import com.nchain.script.ScriptException
import com.nchain.shared.ProtocolException
import com.nchain.tools.ByteArrayMessageReader
import com.nchain.tools.InputStreamMessageReader
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 *
 * A transaction represents the movement of coins from some addresses to some other addresses. It can also represent
 * the minting of new coins. A Transaction object corresponds to the equivalent in the Bitcoin C++ implementation.
 *
 *
 * Transactions are the fundamental atoms of Bitcoin and have many powerful features. Read
 * ["Working with transactions"](https://bitcoinj.github.io/working-with-transactions) in the
 * documentation to learn more about how to use this class.
 *
 *
 * All Bitcoin transactions are at risk of being reversed, though the risk is much less than with traditional payment
 * systems. Transactions have *confidence levels*, which help you decide whether to trust a transaction or not.
 * Whether to trust a transaction is something that needs to be decided on a case by case basis - a rule that makes
 * sense for selling MP3s might not make sense for selling cars, or accepting payments from a family member. If you
 * are building a wallet, how to present confidence to your users is something to consider carefully.
 *
 *
 * Instances of this class are not safe for use by multiple threads.
 */
class TransactionBuilder

    @JvmOverloads
    constructor(var version: Long = 1,
                var lockTime: Long = 0,
                inputs: List<TransactionInput> = emptyList(),
                outputs: List<TransactionOutput> = emptyList()) {

    val inputs: MutableList<TransactionInput> = mutableListOf()
    val outputs: MutableList<TransactionOutput> = mutableListOf()

    init {
        // Ensure we have a different reference of inputs from inputs/outputs fields from parameters
        this.inputs.addAll(inputs)
        this.outputs.addAll(outputs)
    }

    constructor(transaction: Transaction) :
            this(transaction.version,
                transaction.lockTime,
                transaction.inputs,
                transaction.outputs)

    fun clearInputs():TransactionBuilder {
        inputs.clear()
        return this
    }

//    fun addInput(fromTx:Transaction, from: TransactionOutput): MutableTransaction {
//        addInput(TransactionInput.create(params, from, fromTx))
//        return this
//    }

    fun addInput(fromTx:Transaction, outputIndex: Long): TransactionBuilder {
        addInput(TransactionInput.create(fromTx, outputIndex))
        return this
    }

    fun findIndex(from: TransactionOutput): Int {
        for (i in outputs.indices) {
            if (outputs[i] === from)
                return i
        }
        throw IllegalStateException("Output linked to wrong parent transaction?")
    }


    fun addInput(input: TransactionInput): TransactionBuilder {
        inputs.add(input)
        return this
    }

    fun addInput(scriptBytes: ByteArray, outpoint: TransactionOutPoint? = null, sequenceNumber: Long?): TransactionBuilder {
        inputs.add(TransactionInput(scriptBytes, outpoint, sequenceNumber))
        return this
    }

    fun addInput(spendTxHash: Sha256Hash, outputIndex: Long, script: Script): TransactionBuilder {
        addInput(TransactionInput(script.listProgram(), TransactionOutPoint(outputIndex, spendTxHash)))
        return this
    }

    /**
     * Adds a new and fully signed input for the given parameters. Note that this method is **not** thread safe
     * and requires external synchronization. Please refer to general documentation on Bitcoin scripting and contracts
     * to understand the values of sigHash and anyoneCanPay: otherwise you can use the other form of this method
     * that sets them to typical defaults.
     *
     * @throws ScriptException if the scriptPubKey is not a pay to address or pay to pubkey script.
     */

/*
    @Throws(ScriptException::class)
    @JvmOverloads
    fun addSignedInput(prevOut: TransactionOutPoint, scriptPubKey: Script, sigKey: ECKey,
                       sigHash: Transaction.SigHash = Transaction.SigHash.ALL, anyoneCanPay: Boolean = false): TransactionInput {
        // Verify the API user didn't try to do operations out of order.
        check(!outputs.isEmpty(), {"Attempting to sign tx without outputs."})
        val hash = TransactionSignatureBuilder(this).hashForSignature(inputs.size - 1, scriptPubKey, sigHash, anyoneCanPay)
        val ecSig = sigKey.sign(hash)
        val txSig = TransactionSignature(ecSig, sigHash, anyoneCanPay, false)
        val scriptBytes = if (scriptPubKey.isSentToRawPubKey)
            ScriptBuilder.createInputScript(txSig).listProgram()
        else if (scriptPubKey.isSentToAddress)
            ScriptBuilder.createInputScript(txSig, sigKey).listProgram()
        else
            throw ScriptException("Don't know how to sign for this kind of scriptPubKey: " + scriptPubKey)

        val input = TransactionInput(params, this, scriptBytes, prevOut)
        addInput(input)

        return input
    }

*/

    /**
     * Adds a new and fully signed input for the given parameters. Note that this method is **not** thread safe
     * and requires external synchronization. Please refer to general documentation on Bitcoin scripting and contracts
     * to understand the values of sigHash and anyoneCanPay: otherwise you can use the other form of this method
     * that sets them to typical defaults.
     *
     * @throws ScriptException if the scriptPubKey is not a pay to address or pay to pubkey script.
     */
/*
    @Throws(ScriptException::class)
    fun addSignedInput(prevOut: TransactionOutPoint, scriptPubKey: Script, sigKey: ECKey,
                       sigHash: SigHash, anyoneCanPay: Boolean, forkId: Boolean): TransactionInput {
        // Verify the API user didn't try to do operations out of order.
        checkState(!outputs.isEmpty(), "Attempting to sign tx without outputs.")
        val input = TransactionInput(params, this, byteArrayOf(), prevOut)
        addInput(input)
        val hash = if (forkId)
            TransactionSignatureBuilder(this).hashForSignatureWitness(inputs.size - 1, scriptPubKey, prevOut.getConnectedOutput()!!.getValue(), sigHash, anyoneCanPay)
        else
            TransactionSignatureBuilder(this).hashForSignature(inputs.size - 1, scriptPubKey, sigHash, anyoneCanPay)

        val ecSig = sigKey.sign(hash)
        val txSig = TransactionSignature(ecSig, sigHash, anyoneCanPay, forkId)
        if (scriptPubKey.isSentToRawPubKey)
            input.setScriptSig( ScriptBuilder.createInputScript(txSig) )
        else if (scriptPubKey.isSentToAddress)
            input.setScriptSig( ScriptBuilder.createInputScript(txSig, sigKey) )
        else
            throw ScriptException("Don't know how to sign for this kind of scriptPubKey: " + scriptPubKey)
        return input
    }
*/

    /**
     * Adds an input that points to the given output and contains a valid signature for it, calculated using the
     * signing key.
     */
//    fun addSignedInput(output: TransactionOutput, signingKey: ECKey): TransactionInput {
//        return addSignedInput(output.outPointFor, output.getScriptPubKey(), signingKey)
//    }

    /**
     * Adds an input that points to the given output and contains a valid signature for it, calculated using the
     * signing key.
     */
//    fun addSignedInput(output: TransactionOutput, signingKey: ECKey, sigHash: SigHash, anyoneCanPay: Boolean): TransactionInput {
//        return addSignedInput(output.outPointFor, output.getScriptPubKey(), signingKey, sigHash, anyoneCanPay)
//    }

    /**
     * Removes all the outputs from this transaction.
     * Note that this also invalidates the length attribute
     */
    fun clearOutputs():TransactionBuilder {
        outputs.clear()
        return this
    }

    fun addOutputs(outputs: List<TransactionOutput>):TransactionBuilder {
        this.outputs.addAll(outputs)
        return this
    }

    /**
     * Adds the given output to this transaction. The output must be completely initialized. Returns the given output.
     */
    fun addOutput(to: TransactionOutput): TransactionBuilder {
        outputs.add(to)
        return this
    }

    /**
     * Creates an output based on the given address and value, adds it to this transaction, and returns the new output.
     */
    fun addOutput(value: Coin, address: CashAddress): TransactionBuilder {
        addOutput(TransactionOutput(value, address))
        return this
    }

    /**
     * Creates an output that pays to the given pubkey directly (no address) with the given value, adds it to this
     * transaction, and returns the new output.
     */
    fun addOutput(value: Coin, pubkey: ECKey): TransactionBuilder {
        addOutput(TransactionOutput(value, pubkey))
        return this
    }

    /**
     * Creates an output that pays to the given script. The address and key forms are specialisations of this method,
     * you won't normally need to use it unless you're doing unusual things.
     */
    fun addOutput(value: Coin, script: Script): TransactionBuilder {
        addOutput(TransactionOutput(value, script.listProgram()))
        return this
    }

    fun addOutput(value: Coin, scriptBytes: ByteArray): TransactionBuilder {
        addOutput(TransactionOutput(value, scriptBytes))
        return this
    }

    fun addData(data:ByteArray): TransactionBuilder {
        val script = ScriptBuilder.createOpReturnScript(data)
        addOutput(TransactionOutput(Coin.ZERO, script.listProgram()))
        return this
    }

    fun build(): Transaction {
        return Transaction(version, lockTime, inputs, outputs)
    }


    /**
     * Check block height is in coinbase input script, for use after BIP 34
     * enforcement is enabled.
     */
/*
    @Throws(VerificationException::class)
    fun checkCoinBaseHeight(height: Int) {
        checkArgument(height >= Block.BLOCK_HEIGHT_GENESIS)
        checkState(isCoinBase)

        // Check block height is in coinbase input script
        val `in` = this.getInputs()[0]
        val builder = ScriptBuilder()
        builder.number(height.toLong())
        val expected = builder.build().listProgram()
        val actual = `in`.getScriptBytes()
        if (actual!!.size < expected.size) {
            throw VerificationException.CoinbaseHeightMismatch("Block height mismatch in coinbase.")
        }
        for (scriptIdx in expected.indices) {
            if (actual[scriptIdx] != expected[scriptIdx]) {
                throw VerificationException.CoinbaseHeightMismatch("Block height mismatch in coinbase.")
            }
        }
    }
*/


    /**
     *
     * Returns true if this transaction is considered finalized and can be placed in a block. Non-finalized
     * transactions won't be included by miners and can be replaced with newer versions using sequence numbers.
     * This is useful in certain types of [contracts](http://en.bitcoinkt.it/wiki/Contracts), such as
     * micro payment channels.
     *
     *
     * Note that currently the replacement feature is disabled in Bitcoin Core and will need to be
     * re-activated before this functionality is useful.
     */
//    fun isFinal(height: Int, blockTimeSeconds: Long): Boolean {
//        val time = getLockTime()
//        return time < (if (time < LOCKTIME_THRESHOLD) height.toLong() else blockTimeSeconds) || !isTimeLocked
//    }

    /**
     * Returns either the lock time as a date, if it was specified in seconds, or an estimate based on the time in
     * the current head block if it was specified as a block time.
     */
/*
    fun estimateLockTime(chain: AbstractBlockChain): Date {
        return if (lockTime < LOCKTIME_THRESHOLD)
            chain.estimateBlockTime(getLockTime().toInt())
        else
            Date(getLockTime() * 1000)
    }
*/
    companion object {
        private val log = LoggerFactory.getLogger(TransactionBuilder::class.java)

        @JvmStatic
        @Throws(ProtocolException::class)
        fun parse(rawHex: String):TransactionBuilder {
            return parse(rawHex.hexStringToByteArray())
        }

        @JvmOverloads
        @JvmStatic
        fun parse(payload:ByteArray, offset:Int = 0):TransactionBuilder {
            return parse(ByteArrayMessageReader(payload, offset))
        }

        @JvmStatic
        fun parse(inputStream: InputStream):TransactionBuilder {
            return parse(InputStreamMessageReader(inputStream))
        }

        @JvmStatic
        @Throws(ProtocolException::class)
        fun parse(reader:MessageReader):TransactionBuilder {
//            val offset = reader.cursor
            val version = reader.readUint32()
            var optimalEncodingMessageSize = 4

            val numInputs = reader.readVarInt()

            optimalEncodingMessageSize += VarInt.sizeOf(numInputs)

            val inputs = ArrayList<TransactionInput>(numInputs.toInt())
            for (i in 0 until numInputs) {
                val input = TransactionInput.parse(reader)
                inputs.add(input)
                optimalEncodingMessageSize += input.length
            }
            val numOutputs = reader.readVarInt()
            optimalEncodingMessageSize += VarInt.sizeOf(numOutputs)

            val outputs = ArrayList<TransactionOutput>(numOutputs.toInt())
            for (i in 0 until numOutputs) {
                val output = TransactionOutput.parse(reader)
                outputs.add(output)
                val scriptLen = output.scriptBytes.size.toLong()
                optimalEncodingMessageSize += (8 + VarInt.sizeOf(scriptLen).toLong() + scriptLen).toInt()
            }
            val lockTime = reader.readUint32()
            optimalEncodingMessageSize += 4
//            var length = reader.cursor - offset
//            check(length == optimalEncodingMessageSize)
            return TransactionBuilder(version, lockTime, inputs, outputs)
        }

    }
}