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


import java.io.IOException
import java.io.OutputStream
import java.lang.ref.WeakReference

import com.nchain.shared.VerificationException
import com.nchain.shared.Sha256Hash
import com.nchain.shared.VarInt
import com.nchain.tools.ByteUtils
import com.nchain.tools.MessageReader
import com.nchain.tools.UnsafeByteArrayOutputStream
import com.nchain.script.ProtocolException
import com.nchain.script.Script
import com.nchain.script.ScriptException
import java.util.*

/**
 *
 * A transfer of coins from one address to another creates a transaction in which the outputs
 * can be claimed by the recipient in the input of another transaction. You can imagine a
 * transaction as being a module which is wired up to others, the inputs of one have to be wired
 * to the outputs of another. The exceptions are coinbase transactions, which create new coins.
 *
 *
 * Instances of this class are not safe for use by multiple threads.
 */

open class TransactionInput

    @JvmOverloads
    constructor(val scriptBytes: ByteArray,
                outpoint: TransactionOutPoint? = null,
                sequenceNumber: Long? = null) {

    val length:Int
    val outpoint: TransactionOutPoint

    /**
     * Sequence numbers allow participants in a multi-party transaction signing protocol to create new versions of the
     * transaction independently of each other. Newer versions of a transaction can replace an existing version that's
     * in nodes memory pools if the existing version is time locked. See the Contracts page on the Bitcoin wiki for
     * examples of how you can use this feature to build contract protocols.
     */
    val sequenceNumber: Long

    init {
        this.outpoint = if (outpoint != null) outpoint else TransactionOutPoint.UNCONNECTED;
        this.sequenceNumber = if (sequenceNumber != null) sequenceNumber else NO_SEQUENCE
        length = 40 + if (scriptBytes == null) 1 else VarInt.sizeOf(scriptBytes?.size?.toLong()?:0) + (scriptBytes.size?:1)
    }

    // The "script bytes" might not actually be a script. In coinbase transactions where new coins are minted there
    // is no input transaction, so instead the scriptBytes contains some extra stuff (like a rollover nonce) that we
    // don't care about much. The bytes are turned into a Script object (cached below) on demand via a getter.
    // The Script object obtained from parsing scriptBytes. Only filled in on demand and if the transaction is not
    // coinbase.
    private var _scriptSig: WeakReference<Script>? = null

    /**
     * Returns the script that is fed to the referenced output (scriptPubKey) script in order to satisfy it: usually
     * contains signatures and maybe keys, but can contain arbitrary data if the output script accepts it.
     */
    open val scriptSig:Script
        @Throws(ScriptException::class)
        get() {
            // Transactions that generate new coins don't actually have a script. Instead this
            // parameter is overloaded to be something totally different.
            var script: Script? = if (_scriptSig == null) null else _scriptSig!!.get()
            if (script == null) {
                // can be null because is the first time (no WeakReference) or because the WeakReference is empty
                script = Script(scriptBytes)
                _scriptSig = WeakReference(script)
            }
            return script
        }

    companion object {
        /** Magic sequence number that indicates there is no sequence number.  */
        const val NO_SEQUENCE = 0xFFFFFFFFL

        /**
         * BIP68: If this flag set, sequence is NOT interpreted as a relative lock-time.
         */
        const val SEQUENCE_LOCKTIME_DISABLE_FLAG = 1L shl 31
        /**
         * BIP68: If sequence encodes a relative lock-time and this flag is set, the relative lock-time has units of 512
         * seconds, otherwise it specifies blocks with a granularity of 1.
         */
        const val SEQUENCE_LOCKTIME_TYPE_FLAG = 1L shl 22
        /**
         * BIP68: If sequence encodes a relative lock-time, this mask is applied to extract that lock-time from the sequence
         * field.
         */
        const val SEQUENCE_LOCKTIME_MASK: Long = 0x0000ffff

        /**
         * Creates an UNSIGNED input that links to the given output
         */
        fun create(fromTx:Transaction, outputIndex: Long): TransactionInput {
            val outpoint = TransactionOutPoint.create(outputIndex, fromTx)
            val input = TransactionInput(ByteUtils.EMPTY_BYTE_ARRAY, outpoint, NO_SEQUENCE)
            check(input.length == 41)
            return input
        }

/*
        fun create(params: NetworkParameters,output: TransactionOutput): TransactionInput {
            val outpoint = TransactionOutPoint.create(params, output)
            val input = TransactionInput(params, ByteUtils.EMPTY_BYTE_ARRAY, outpoint, output.getValue(), NO_SEQUENCE)
            check(input.length == 41)
            return input
        }
*/


        @Throws(ProtocolException::class)
        fun parse(payload:ByteArray, offset:Int = 0):TransactionInput {
            return parse(MessageReader(payload, offset))
        }

        @Throws(ProtocolException::class)
        fun parse(reader:MessageReader):TransactionInput {
            val offset = reader.cursor

            val outpoint = TransactionOutPoint.parse(reader)

            val scriptLen = reader.readVarInt().toInt()
            val length = reader.cursor - offset + scriptLen + 4
            val scriptBytes = reader.readBytes(scriptLen)
            val sequence = reader.readUint32()

            var otherLength = reader.cursor - offset
            check(otherLength == length)
            // es igual a length?
            return TransactionInput(scriptBytes, outpoint, sequence)
        }
    }



    /**
     * Coinbase transactions have special inputs with hashes of zero. If this is such an input, returns true.
     */
    // -1 but all is serialized to the wire as unsigned int.
    val isCoinBase: Boolean
        get() = outpoint!!.hash == Sha256Hash.ZERO_HASH && outpoint!!.index and 0xFFFFFFFFL == 0xFFFFFFFFL

    /**
     * Convenience method that returns the from address of this input by parsing the scriptSig. The concept of a
     * "from address" is not well defined in Bitcoin and you should not assume that senders of a transaction can
     * actually receive coins on the same address they used to sign (e.g. this is not true for shared wallets).
     */
/*
    val fromAddress: CashAddress
        @Deprecated("")
        @Throws(ScriptException::class)
        get() {
            if (isCoinBase) {
                throw ScriptException(
                        "This is a coinbase transaction which generates new coins. It does not have a from address.")
            }
            return getScriptSig().getFromAddress(params!!)
        }
*/

    fun changeSequence(newSequence:Long):TransactionInput {
        return TransactionInput(scriptBytes, outpoint, newSequence)
    }

    /**
     * Returns the connected output, assuming the input was connected with
     * [TransactionInput.connect] or variants at some point. If it wasn't connected, then
     * this method returns null.
     */
    val connectedOutput: TransactionOutput?
        get() = outpoint!!.connectedOutput

    /**
     * Returns the connected transaction, assuming the input was connected with
     * [TransactionInput.connect] or variants at some point. If it wasn't connected, then
     * this method returns null.
     */
//    val connectedTransaction: Transaction?
//        get() = outpoint!!.fromTx
//
    /**
     *
     * Returns either RuleViolation.NONE if the input is standard, or which rule makes it non-standard if so.
     * The "IsStandard" rules control whether the default Bitcoin Core client blocks relay of a tx / refuses to mine it,
     * however, non-standard transactions can still be included in blocks and will be accepted as valid if so.
     *
     *
     * This method simply calls <tt>DefaultRiskAnalysis.isInputStandard(this)</tt>.
     */
//    val isStandard: DefaultRiskAnalysis.RuleViolation
//        get() = DefaultRiskAnalysis.isInputStandard(this)

    /**
     * Deserializes an input message. This is usually part of a transaction message.
     * @param params NetworkParameters object.
     * @param payload Bitcoin protocol formatted byte array containing message content.
     * @param offset The location of the first payload byte within the array.
     * @param serializer the serializer to use for this message.
     * @throws ProtocolException
     */
//    @Throws(ProtocolException::class)
//    constructor(params: NetworkParameters, parentTransaction: Transaction, payload: ByteArray, offset: Int, serializer: MessageSerializer) : super(params, payload, offset, parentTransaction, serializer, Message.UNKNOWN_LENGTH) {
//        this.value = null
//    }
//
    @Throws(IOException::class)
    fun bitcoinSerialize():ByteArray {
        val stream = UnsafeByteArrayOutputStream()
        bitcoinSerializeToStream(stream)
        stream.close()
        return stream.toByteArray()
    }

    @Throws(IOException::class)
    fun bitcoinSerializeToStream(stream: OutputStream) {
        outpoint.bitcoinSerializeToStream(stream)
        stream.write(VarInt(scriptBytes.size.toLong()).encode())
        stream.write(scriptBytes)
        ByteUtils.uint32ToByteStreamLE(sequenceNumber, stream)
    }

    /** Clear input scripts, e.g. in preparation for signing.  */
    fun clearScriptBytes():TransactionInput {
        return changeScriptBytes(ByteUtils.EMPTY_BYTE_ARRAY)
    }

    fun changeScriptBytes(bytes:ByteArray):TransactionInput {
        return TransactionInput(bytes, outpoint, sequenceNumber)
    }

    /**
     * Connects this input to the relevant output of the referenced transaction if it's in the given map.
     * Connecting means updating the internal pointers and spent flags. If the mode is to ABORT_ON_CONFLICT then
     * the spent output won't be changed, but the outpoint.fromTx pointer will still be updated.
     *
     * @param transactions Map of txhash->transaction.
     * @param mode   Whether to abort if there's a pre-existing connection or not.
     * @return NO_SUCH_TX if the prevtx wasn't found, ALREADY_SPENT if there was a conflict, SUCCESS if not.
     */
/*
    fun connect(transactions: Map<Sha256Hash, Transaction>, mode: ConnectMode): ConnectionResult {
        val tx = transactions[outpoint!!.hash] ?: return TransactionInput.ConnectionResult.NO_SUCH_TX
        return connect(tx, mode)
    }
*/

    /**
     * Connects this input to the relevant output of the referenced transaction.
     * Connecting means updating the internal pointers and spent flags. If the mode is to ABORT_ON_CONFLICT then
     * the spent output won't be changed, but the outpoint.fromTx pointer will still be updated.
     *
     * @param transaction The transaction to try.
     * @param mode   Whether to abort if there's a pre-existing connection or not.
     * @return NO_SUCH_TX if transaction is not the prevtx, ALREADY_SPENT if there was a conflict, SUCCESS if not.
     */
/*
    fun connect(transaction: Transaction, mode: ConnectMode): ConnectionResult {
        if (transaction.hash != outpoint!!.hash)
            return ConnectionResult.NO_SUCH_TX
        checkElementIndex(outpoint!!.index.toInt(), transaction.getOutputs().size, "Corrupt transaction")
        val out = transaction.getOutput(outpoint!!.index.toInt().toLong())
        if (!out.isAvailableForSpending) {
            if (parentTransaction == outpoint!!.fromTx) {
                // Already connected.
                return ConnectionResult.SUCCESS
            } else if (mode == ConnectMode.DISCONNECT_ON_CONFLICT) {
                out.markAsUnspent()
            } else if (mode == ConnectMode.ABORT_ON_CONFLICT) {
                outpoint!!.fromTx = out.parentTransaction
                return TransactionInput.ConnectionResult.ALREADY_SPENT
            }
        }
        connect(out)
        return TransactionInput.ConnectionResult.SUCCESS
    }
*/

    /** Internal use only: connects this TransactionInput to the given output (updates pointers and spent flags)  */
/*
    fun connect(out: TransactionOutput) {
        outpoint!!.fromTx = out.parentTransaction
        out.markAsSpent(this)
        value = out.getValue()
    }
*/

    /**
     * If this input is connected, check the output is connected back to this input and release it if so, making
     * it spendable once again.
     *
     * @return true if the disconnection took place, false if it was not connected.
     */
/*
    fun disconnect(): Boolean {
        if (outpoint!!.fromTx == null) return false
        val output = outpoint!!.fromTx!!.getOutput(outpoint!!.index.toInt().toLong())
        if (output.spentBy === this) {
            output.markAsUnspent()
            outpoint!!.fromTx = null
            return true
        } else {
            return false
        }
    }
*/

    /**
     * @return true if this transaction's sequence number is set (ie it may be a part of a time-locked transaction)
     */
    fun hasSequence(): Boolean {
        return sequenceNumber != NO_SEQUENCE
    }

    /**
     * For a connected transaction, runs the script against the connected pubkey and verifies they are correct.
     * @throws ScriptException if the script did not verify.
     * @throws VerificationException If the outpoint doesn't match the given output.
     */
/*
    @Throws(VerificationException::class)
    fun verify() {
        val fromTx = outpoint!!.fromTx
        val spendingIndex = outpoint!!.index
        checkNotNull(fromTx, "Not connected")
        val output = fromTx!!.getOutput(spendingIndex.toInt().toLong())
        verify(output)
    }
*/

    /**
     * Verifies that this input can spend the given output. Note that this input must be a part of a transaction.
     * Also note that the consistency of the outpoint will be checked, even if this input has not been connected.
     *
     * @param output the output that this input is supposed to spend.
     * @throws ScriptException If the script doesn't verify.
     * @throws VerificationException If the outpoint doesn't match the given output.
     */
    /*
    @Throws(VerificationException::class)
    fun verify(output: TransactionOutput) {
        var inputValue = Coin.ZERO
        if (output.parent != null) {
            if (outpoint!!.hash != output.parentTransaction!!.hash)
                throw VerificationException("This input does not refer to the tx containing the output.")
            if (outpoint!!.index != output.index.toLong())
                throw VerificationException("This input refers to a different output on the given tx.")
            if (outpoint!!.getConnectedOutput() != null)
                inputValue = outpoint!!.getConnectedOutput().getValue()
        }
        val pubKey = output.getScriptPubKey()
        val myIndex = parentTransaction.getInputs().indexOf(this)
        getScriptSig().correctlySpends(parentTransaction, myIndex.toLong(), pubKey, inputValue, Script.ALL_VERIFY_FLAGS)
    }

    */
    /** Returns a copy of the input detached from its containing transaction, if need be.  */
//    fun duplicateDetached(): TransactionInput {
//        return TransactionInput(params!!, null, bitcoinSerialize(), 0)
//    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        return if (o != null && o is TransactionInput)
            sequenceNumber == o.sequenceNumber && outpoint == o.outpoint && Arrays.equals(scriptBytes, o.scriptBytes)
        else
            false
    }

    override fun hashCode(): Int {
        return Objects.hash(sequenceNumber, outpoint, Arrays.hashCode(scriptBytes))
    }

    /**
     * Returns a human readable debug string.
     */
    override fun toString(): String {
        val s = StringBuilder("TxIn")
        try {
            if (isCoinBase) {
                s.append(": COINBASE")
            } else {
                s.append(" for [").append(outpoint).append("]: ").append(scriptSig)
                if (hasSequence()) s.append(" (sequence: " + java.lang.Long.toHexString(sequenceNumber) + ")")
            }
            return s.toString()
        } catch (e: ScriptException) {
            throw RuntimeException(e)
        }

    }

}
