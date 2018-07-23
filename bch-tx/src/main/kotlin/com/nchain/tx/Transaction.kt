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

import com.google.common.collect.ImmutableMap
import com.google.common.primitives.Longs
import com.nchain.address.CashAddress
import com.nchain.bitcoinkt.core.TransactionSignatureBuilder
import com.nchain.key.ECKey
import org.slf4j.LoggerFactory
import java.util.*

import com.nchain.key.VerificationException
import com.nchain.params.NetworkParameters
import com.nchain.shared.Sha256Hash
import com.nchain.shared.VarInt
import com.nchain.tools.ByteUtils
import org.bitcoinj.script.ProtocolException
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.script.ScriptException
import org.bitcoinj.tx.TransactionSignature
import java.io.IOException
import java.io.OutputStream
import java.math.BigInteger

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
class Transaction(val params:NetworkParameters) {

    // These are bitcoinkt serialized.
    var version: Long = 0
        private set
    private var inputs: MutableList<TransactionInput> = mutableListOf()
    private var outputs: MutableList<TransactionOutput> = mutableListOf()

    private var lockTime: Long = 0

    // This is either the time the transaction was broadcast as measured from the local clock, or the time from the
    // block in which it was included. Note that this can be changed by re-orgs so the wallet may update this field.
    // Old serialized transactions don't have this field, thus null is valid. It is used for returning an ordered
    // list of transactions from a wallet, which is helpful for presenting to users.
//    private var updatedAt: Date? = null

    // This is an in memory helper only.
    /**
     * Returns the transaction hash as you see them in the block explorer.
     */
    /**
     * Used by BitcoinSerializer.  The serializer has to calculate a hash for checksumming so to
     * avoid wasting the considerable effort a set method is provided so the serializer can set it.
     *
     * No verification is performed on this hash.
     */
     var hash: Sha256Hash? = null

    // Data about how confirmed this tx is. Serialized, may be null.
//    private var confidence: TransactionConfidence? = null

    // Records a map of which blocks the transaction has appeared in (keys) to an index within that block (values).
    // The "index" is not a real index, instead the values are only meaningful relative to each other. For example,
    // consider two transactions that appear in the same block, t1 and t2, where t2 spends an output of t1. Both
    // will have the same block hash as a key in their appearsInHashes, but the counter would be 1 and 2 respectively
    // regardless of where they actually appeared in the block.
    //
    // If this transaction is not stored in the wallet, appearsInHashes is null.
//    private var appearsInHashes: MutableMap<Sha256Hash, Int>? = null

    // Transactions can be encoded in a way that will use more bytes than is optimal
    // (due to VarInts having multiple encodings)
    // MAX_BLOCK_SIZE must be compared to the optimal encoding, not the actual encoding, so when parsing, we keep track
    // of the size of the ideal encoding in addition to the actual message size (which Message needs) so that Blocks
    // can properly keep track of optimal encoded size
//    private var optimalEncodingMessageSize: Int = 0

    /**
     * Returns the purpose for which this transaction was created. See the javadoc for [Purpose] for more
     * information on the point of this field and what it can be.
     */
    /**
     * Marks the transaction as being created for the given purpose. See the javadoc for [Purpose] for more
     * information on the point of this field and what it can be.
     */
//    var purpose: Purpose? = Purpose.UNKNOWN

    /**
     * This field can be used by applications to record the exchange rate that was valid when the transaction happened.
     * It's optional.
     */
    /**
     * Getter for [.exchangeRate].
     */
    /**
     * Setter for [.exchangeRate].
     */
//    var exchangeRate: ExchangeRate? = null

    /**
     * This field can be used to record the memo of the payment request that initiated the transaction. It's optional.
     */
    /**
     * Returns the transaction [.memo].
     */
    /**
     * Set the transaction [.memo]. It can be used to record the memo of the payment request that initiated the
     * transaction.
     */
//    var memo: String? = null

    val hashAsString: String
        get() = hash.toString()

    /**
     * Gets the sum of the inputs, regardless of who owns them.
     */
    val inputSum: Coin
        get() {
            var inputTotal = Coin.ZERO
            for (input in inputs) {
                inputTotal = inputTotal.add(input.value?:Coin.ZERO)
            }

            return inputTotal
        }

    /**
     * Convenience wrapper around getConfidence().getConfidenceType()
     * @return true if this transaction hasn't been seen in any block yet.
     */
//    val isPending: Boolean
//        get() = getConfidence().getConfidenceType() == TransactionConfidence.ConfidenceType.PENDING

    /**
     * Gets the sum of the outputs of the transaction. If the outputs are less than the inputs, it does not count the fee.
     * @return the sum of the outputs regardless of who owns them.
     */
    val outputSum: Coin
        get() {
            var totalOut = Coin.ZERO
             for (output in outputs) {
                totalOut = totalOut.add(output.getValue())
             }

            return totalOut
        }

    /*
    fun getOutputSum: Coin{
        var totalOut = Coin.ZERO
        for (output in outputs){
            totalOut = totalOut.add(output.getValue())
        }
        return totalOut
    }*/
//    private var cachedValue: Coin? = null
//    private var cachedForBag: TransactionBag? = null

    /**
     * The transaction fee is the difference of the value of all inputs and the value of all outputs. Currently, the fee
     * can only be determined for transactions created by us.
     *
     * @return fee, or null if it cannot be determined
     */
    val fee: Coin?
        get() {
            var fee = Coin.ZERO
            for (input in inputs) {
                if (input.value == null)
                    return null
                fee = fee.add(input.value!!)
            }
            fee = fee.subtract(outputSum)
            return fee
        }

    /**
     * Returns true if any of the outputs is marked as spent.
     */
/*
    val isAnyOutputSpent: Boolean
        get() {
            for (output in outputs) {
                if (!output.isAvailableForSpending)
                    return true
            }
            return false
        }
*/

    /**
     * Returns the earliest time at which the transaction was seen (broadcast or included into the chain),
     * or the epoch if that information isn't available.
     */
    // Older wallets did not store this field. Set to the epoch.
/*
    var updateTime: Date?
        get() {
            if (updatedAt == null) {
                updatedAt = Date(0)
            }
            return updatedAt as Date
        }
        set(updatedAt) {
            this.updatedAt = updatedAt ?: Date(0)       // support setting null for very old tx
        }
*/

    /**
     * The priority (coin age) calculation doesn't use the regular message size, but rather one adjusted downwards
     * for the number of inputs. The goal is to incentivise cleaning up the UTXO set with free transactions, if one
     * can do so.
     */
    // 41: min size of an input
    // 110: enough to cover a compressed pubkey p2sh redemption (somewhat arbitrary).
/*
    val messageSizeForPriorityCalc: Int
        get() {
            var size = messageSize
            for (input in inputs) {
                val benefit = 41 + Math.min(110, input.getScriptSig().listProgram().size)
                if (size > benefit)
                    size -= benefit
            }
            return size
        }
*/

    /**
     * A coinbase transaction is one that creates a new coin. They are the first transaction in each block and their
     * value is determined by a formula that all implementations of Bitcoin share. In 2011 the value of a coinbase
     * transaction is 50 coins, but in future it will be less. A coinbase transaction is defined not only by its
     * position in a block but by the data in the inputs.
     */
    val isCoinBase: Boolean
        get() = inputs.size == 1 && inputs[0].isCoinBase

    /**
     * A transaction is mature if it is either a building coinbase tx that is as deep or deeper than the required coinbase depth, or a non-coinbase tx.
     */
/*
    val isMature: Boolean
        get() {
            if (!isCoinBase)
                return true

            return if (getConfidence().getConfidenceType() != ConfidenceType.BUILDING) false else getConfidence().depthInBlocks >= params!!.spendableCoinbaseDepth

        }
*/

    /**
     * Gets the count of regular SigOps in this transactions
     */
    val sigOpCount: Int
        @Throws(ScriptException::class)
        get() {
            var sigOps = 0
            for (input in inputs)
                sigOps += Script.getSigOpCount(input.getScriptBytes()!!)
            for (output in outputs)
                sigOps += Script.getSigOpCount(output.scriptBytes!!)
            return sigOps
        }

    /**
     *
     * A transaction is time locked if at least one of its inputs is non-final and it has a lock time
     *
     *
     * To check if this transaction is final at a given height and time, see [Transaction.isFinal]
     *
     */
/*
    val isTimeLocked: Boolean
        get() {
            if (getLockTime() == 0L)
                return false
            for (input in getInputs())
                if (input.hasSequence())
                    return true
            return false
        }
*/

/*
    val isOpReturn: Boolean
        get() = opReturnData != null

    val opReturnData: ByteArray?
        get() {
            // Only one OP_RETURN output per transaction is allowed as "standard" transaction
            // So just return the first OP_RETURN data found
            for (output in outputs) {
                if (output.isOpReturn) {
                    return output.opReturnData
                }
            }
            return null
        }

*/

    /**
     * This enum describes the underlying reason the transaction was created. It's useful for rendering wallet GUIs
     * more appropriately.
     */
    enum class Purpose {
        /** Used when the purpose of a transaction is genuinely unknown.  */
        UNKNOWN,
        /** Transaction created to satisfy a user payment request.  */
        USER_PAYMENT,
        /** Transaction automatically created and broadcast in order to reallocate money from old to new keys.  */
        KEY_ROTATION,
        /** Transaction that uses up pledges to an assurance contract  */
        ASSURANCE_CONTRACT_CLAIM,
        /** Transaction that makes a pledge to an assurance contract.  */
        ASSURANCE_CONTRACT_PLEDGE,
        /** Send-to-self transaction that exists just to create an output of the right size we can pledge.  */
        ASSURANCE_CONTRACT_STUB,
        /** Raise fee, e.g. child-pays-for-parent.  */
        RAISE_FEE
        // In future: de/refragmentation, privacy boosting/mixing, etc.
        // When adding a value, it also needs to be added to wallet.proto, WalletProtobufSerialize.makeTxProto()
        // and WalletProtobufSerializer.readTransaction()!
    }

/*
    constructor(params: NetworkParameters) : super(params) {
        version = 1
        inputs = ArrayList()
        outputs = ArrayList()
        // We don't initialize appearsIn deliberately as it's only useful for transactions stored in the wallet.
//        length = 8 // 8 for std fields
    }
*/

    /**
     * Creates a transaction from the given serialized bytes, eg, from a block or a tx network message.
     */
//    @Throws(ProtocolException::class)
//    constructor(params: NetworkParameters, payloadBytes: ByteArray) : super(params, payloadBytes, 0) {
//    }

    /**
     * Creates a transaction by reading payload starting from offset bytes in. Length of a transaction is fixed.
     */
//    @Throws(ProtocolException::class)
//    constructor(params: NetworkParameters, payload: ByteArray, offset: Int) : super(params, payload, offset) {
//         inputs/outputs will be created in parse()
//    }

    /**
     * Creates a transaction by reading payload starting from offset bytes in. Length of a transaction is fixed.
     * @param params NetworkParameters object.
     * @param payload Bitcoin protocol formatted byte array containing message content.
     * @param offset The location of the first payload byte within the array.
     * @param parseRetain Whether to retain the backing byte array for quick reserialization.
     * If true and the backing byte array is invalidated due to modification of a field then
     * the cached bytes may be repopulated and retained if the message is serialized again in the future.
     * @param length The length of message if known.  Usually this is provided when deserializing of the wire
     * as the length will be provided as part of the header.  If unknown then set to Message.UNKNOWN_LENGTH
     * @throws ProtocolException
     */
//    @Throws(ProtocolException::class)
//    constructor(params: NetworkParameters?, payload: ByteArray?, offset: Int, parent: Message?, setSerializer: MessageSerializer, length: Int) : super(params, payload, offset, parent, setSerializer, length) {
//    }

    /**
     * Creates a transaction by reading payload. Length of a transaction is fixed.
     */
//    @Throws(ProtocolException::class)
//    constructor(params: NetworkParameters, payload: ByteArray, parent: Message?, setSerializer: MessageSerializer, length: Int) : super(params, payload, 0, parent, setSerializer, length) {
//    }

    /**
     * Calculates the sum of the outputs that are sending coins to a key in the wallet.
     */
/*
    fun getValueSentToMe(transactionBag: TransactionBag): Coin {
        // This is tested in WalletTest.
        var v = Coin.ZERO
        for (o in outputs) {
            if (!o.isMineOrWatched(transactionBag)) continue
            v = v.add(o.getValue())
        }
        return v
    }
*/

    /**
     * Returns a map of block [hashes] which contain the transaction mapped to relativity counters, or null if this
     * transaction doesn't have that data because it's not stored in the wallet or because it has never appeared in a
     * block.
     */
//    fun getAppearsInHashes(): Map<Sha256Hash, Int>? {
//        return if (appearsInHashes != null) ImmutableMap.copyOf(appearsInHashes!!) else null
//    }

    /**
     *
     * Puts the given block in the internal set of blocks in which this transaction appears. This is
     * used by the wallet to ensure transactions that appear on side chains are recorded properly even though the
     * block stores do not save the transaction data at all.
     *
     *
     * If there is a re-org this will be called once for each block that was previously seen, to update which block
     * is the best chain. The best chain block is guaranteed to be called last. So this must be idempotent.
     *
     *
     * Sets updatedAt to be the earliest valid block time where this tx was seen.
     *
     * @param block     The [StoredBlock] in which the transaction has appeared.
     * @param bestChain whether to set the updatedAt timestamp from the block header (only if not already set)
     * @param relativityOffset A number that disambiguates the order of transactions within a block.
     */
/*
    fun setBlockAppearance(block: StoredBlock, bestChain: Boolean, relativityOffset: Int) {
        val blockTime = block.header.timeSeconds * 1000
        if (bestChain && (updatedAt == null || updatedAt!!.time == 0L || updatedAt!!.time > blockTime)) {
            updatedAt = Date(blockTime)
        }

        addBlockAppearance(block.header.hash!!, relativityOffset)

        if (bestChain) {
            val transactionConfidence = getConfidence()
            // This sets type to BUILDING and depth to one.
            transactionConfidence.setAppearedAtChainHeight( block.height )
        }
    }

    fun addBlockAppearance(blockHash: Sha256Hash, relativityOffset: Int) {
        if (appearsInHashes == null) {
            // TODO: This could be a lot more memory efficient as we'll typically only store one element.
            appearsInHashes = TreeMap()
        }
        appearsInHashes!!.put(blockHash, relativityOffset)
    }
*/

    /**
     * Calculates the sum of the inputs that are spending coins with keys in the wallet. This requires the
     * transactions sending coins to those keys to be in the wallet. This method will not attempt to download the
     * blocks containing the input transactions if the key is in the wallet but the transactions are not.
     *
     * @return sum of the inputs that are spending coins with keys in the wallet
     */
/*
    @Throws(ScriptException::class)
    fun getValueSentFromMe(wallet: TransactionBag): Coin {
        // This is tested in WalletTest.
        var v = Coin.ZERO
        for (input in inputs) {
            // This input is taking value from a transaction in our wallet. To discover the value,
            // we must find the connected transaction.
            var connected = input.getConnectedOutput(wallet.getTransactionPool(Pool.UNSPENT))
            if (connected == null)
                connected = input.getConnectedOutput(wallet.getTransactionPool(Pool.SPENT))
            if (connected == null)
                connected = input.getConnectedOutput(wallet.getTransactionPool(Pool.PENDING))
            if (connected == null)
                continue
            // The connected output may be the change to the sender of a previous input sent to this wallet. In this
            // case we ignore it.
            if (!connected.isMineOrWatched(wallet))
                continue
            v = v.add(connected.getValue())
        }
        return v
    }
*/

    /**
     * Returns the difference of [Transaction.getValueSentToMe] and [Transaction.getValueSentFromMe].
     */
/*
    @Throws(ScriptException::class)
    fun getValue(wallet: TransactionBag): Coin {
        // FIXME: TEMP PERF HACK FOR ANDROID - this crap can go away once we have a real payments API.
        val isAndroid = Utils.isAndroidRuntime
        if (isAndroid && cachedValue != null && cachedForBag === wallet)
            return cachedValue as Coin
        val result = getValueSentToMe(wallet).subtract(getValueSentFromMe(wallet))
        if (isAndroid) {
            cachedValue = result
            cachedForBag = wallet
        }
        return result
    }
*/

    /**
     * Returns false if this transaction has at least one output that is owned by the given wallet and unspent, true
     * otherwise.
     */
/*
    fun isEveryOwnedOutputSpent(transactionBag: TransactionBag): Boolean {
        for (output in outputs) {
            if (output.isAvailableForSpending && output.isMineOrWatched(transactionBag))
                return false
        }
        return true
    }
*/

    /**
     * These constants are a part of a scriptSig signature on the inputs. They define the details of how a
     * transaction can be redeemed, specifically, they control how the hash of the transaction is calculated.
     */
    enum class SigHash
    /**
     * @param value
     */
    private constructor(// Caution: Using this type in isolation is non-standard. Treated similar to ALL.

            val value: Int) {
        ALL(1),
        NONE(2),
        SINGLE(3),
        FORKID(0x40),
        ANYONECANPAY(0x80), // Caution: Using this type in isolation is non-standard. Treated similar to ANYONECANPAY_ALL.
        ANYONECANPAY_ALL(0x81),
        ANYONECANPAY_NONE(0x82),
        ANYONECANPAY_SINGLE(0x83),
        UNSET(0);

        /**
         * @return the value as a byte
         */
        fun byteValue(): Byte {
            return this.value.toByte()
        }
    }

/*
    override fun unCache() {
        super.unCache()
        this.hash = null
    }
*/
/*

    @Throws(ProtocolException::class)
    override fun parse() {
        cursor = offset

        version = readUint32()
        optimalEncodingMessageSize = 4

        // First come the inputs.
        val numInputs = readVarInt()
        optimalEncodingMessageSize += VarInt.sizeOf(numInputs)
        inputs = ArrayList(numInputs.toInt())
        for (i in 0 until numInputs) {
            val input = TransactionInput(params!!, this, payload!!, cursor, serializer!!)
            inputs.add(input)
            val scriptLen = readVarInt(TransactionOutPoint.MESSAGE_LENGTH)
            optimalEncodingMessageSize += (TransactionOutPoint.MESSAGE_LENGTH.toLong() + VarInt.sizeOf(scriptLen).toLong() + scriptLen + 4).toInt()
            cursor += (scriptLen + 4).toInt()
        }
        // Now the outputs
        val numOutputs = readVarInt()
        optimalEncodingMessageSize += VarInt.sizeOf(numOutputs)
        outputs = ArrayList(numOutputs.toInt())
        for (i in 0 until numOutputs) {
            val output = TransactionOutput(params!!, this, payload!!, cursor, serializer!!)
            outputs.add(output)
            val scriptLen = readVarInt(8)
            optimalEncodingMessageSize += (8 + VarInt.sizeOf(scriptLen).toLong() + scriptLen).toInt()
            cursor += scriptLen.toInt()
        }
        lockTime = readUint32()
        optimalEncodingMessageSize += 4
        length = cursor - offset
    }
*/

/*
    fun getOptimalEncodingMessageSize(): Int {
        if (optimalEncodingMessageSize != 0)
            return optimalEncodingMessageSize
        optimalEncodingMessageSize = messageSize
        return optimalEncodingMessageSize
    }
*/

//    override fun toString(): String {
//        return toString(null)
//    }

    /**
     * A human readable version of the transaction useful for debugging. The format is not guaranteed to be stable.
     * @param chain If provided, will be used to estimate lock times (if set). Can be null.
     */
/*
    fun toString(chain: AbstractBlockChain?): String {
        val s = StringBuilder()
        s.append("  ").append(hashAsString).append('\n')
        if (updatedAt != null)
            s.append("  updated: ").append(Utils.dateTimeFormat(updatedAt as Date)).append('\n')
        if (version != 1L)
            s.append("  version ").append(version).append('\n')
        if (isTimeLocked) {
            s.append("  time locked until ")
            if (lockTime < LOCKTIME_THRESHOLD) {
                s.append("block ").append(lockTime)
                if (chain != null) {
                    s.append(" (estimated to be reached at ")
                            .append(Utils.dateTimeFormat(chain.estimateBlockTime(lockTime.toInt()))).append(')')
                }
            } else {
                s.append(Utils.dateTimeFormat(lockTime * 1000))
            }
            s.append('\n')
        }
        if (inputs.size == 0) {
            s.append("  INCOMPLETE: No inputs!\n")
            return s.toString()
        }
        if (isCoinBase) {
            var script: String
            var script2: String
            try {
                script = inputs[0].getScriptSig().toString()
                script2 = outputs[0].getScriptPubKey().toString()
            } catch (e: ScriptException) {
                script = "???"
                script2 = "???"
            }

            s.append("     == COINBASE TXN (scriptSig ").append(script)
                    .append(")  (scriptPubKey ").append(script2).append(")\n")
            return s.toString()
        }
        for (`in` in inputs) {
            s.append("     ")
            s.append("in   ")

            try {
                val scriptSig = `in`.getScriptSig()
                s.append(scriptSig)
                if (`in`.value != null)
                    s.append(" ").append(`in`.value!!.toFriendlyString())
                s.append("\n          ")
                s.append("outpoint:")
                val outpoint = `in`.outpoint
                s.append(outpoint!!.toString())
                val connectedOutput = outpoint.getConnectedOutput()
                if (connectedOutput != null) {
                    val scriptPubKey = connectedOutput.getScriptPubKey()
                    if (scriptPubKey.isSentToAddress || scriptPubKey.isPayToScriptHash) {
                        s.append(" hash160:")
                        s.append(Utils.HEX.encode(scriptPubKey.pubKeyHash))
                    }
                }
                if (`in`.hasSequence()) s.append("\n          sequence:").append(java.lang.Long.toHexString(`in`.sequenceNumber))
            } catch (e: Exception) {
                s.append("[exception: ").append(e.message).append("]")
            }

            s.append('\n')
        }
        for (out in outputs) {
            s.append("     ")
            s.append("out  ")
            try {
                val scriptPubKey = out.getScriptPubKey()
                s.append(scriptPubKey)
                s.append(" ")
                s.append(out.getValue().toFriendlyString())
                if (!out.isAvailableForSpending) {
                    s.append(" Spent")
                }
                if (out.spentBy != null) {
                    s.append(" by ")
                    s.append(out.spentBy!!.parentTransaction.hashAsString)
                }
            } catch (e: Exception) {
                s.append("[exception: ").append(e.message).append("]")
            }

            s.append('\n')
        }
        val fee = fee
        if (fee != null) {
            val size = unsafeBitcoinSerialize().size
            s.append("     fee  ").append(fee.multiply(1000).divide(size.toLong()).toFriendlyString()).append("/kB, ")
                    .append(fee.toFriendlyString()).append(" for ").append(size).append(" bytes\n")
        }
        if (purpose != null)
            s.append("     prps ").append(purpose).append('\n')
        return s.toString()
    }
*/

    /**
     * Removes all the inputs from this transaction.
     * Note that this also invalidates the length attribute
     */
    fun clearInputs() {
//        unCache()
//        for (input in inputs) {
//            input.parent = null
//        }
        inputs.clear()
        // You wanted to reserialize, right?
//        this.length = this.unsafeBitcoinSerialize().size
    }

    /**
     * Adds an input to this transaction that imports value from the given output. Note that this input is *not*
     * complete and after every input is added with [.addInput] and every output is added with
     * [.addOutput], a [TransactionSigner] must be used to finalize the transaction and finish the inputs
     * off. Otherwise it won't be accepted by the network.
     * @return the newly created input.
     */
//    fun addInput(from: TransactionOutput): TransactionInput {
//        return addInput(TransactionInput(params!!, this, from))
//    }

    /**
     * Adds an input directly, with no checking that it's valid.
     * @return the new input.
     */
    fun addInput(input: TransactionInput): TransactionInput {
//        unCache()
//        input.parent = this
        inputs.add(input)
//        adjustLength(inputs.size, input.length)
        return input
    }

    /**
     * Creates and adds an input to this transaction, with no checking that it's valid.
     * @return the newly created input.
     */
    fun addInput(spendTxHash: Sha256Hash, outputIndex: Long, script: Script): TransactionInput {
        return addInput(TransactionInput(params!!, this, script.listProgram(), TransactionOutPoint(params!!, outputIndex, spendTxHash)))
    }

    /**
     * Adds a new and fully signed input for the given parameters. Note that this method is **not** thread safe
     * and requires external synchronization. Please refer to general documentation on Bitcoin scripting and contracts
     * to understand the values of sigHash and anyoneCanPay: otherwise you can use the other form of this method
     * that sets them to typical defaults.
     *
     * @throws ScriptException if the scriptPubKey is not a pay to address or pay to pubkey script.
     */
    @Throws(ScriptException::class)
    @JvmOverloads
    fun addSignedInput(prevOut: TransactionOutPoint, scriptPubKey: Script, sigKey: ECKey,
                       sigHash: SigHash = SigHash.ALL, anyoneCanPay: Boolean = false): TransactionInput {
        // Verify the API user didn't try to do operations out of order.
        check(!outputs.isEmpty(), {"Attempting to sign tx without outputs."})
        val input = TransactionInput(params!!, this, byteArrayOf(), prevOut)
        addInput(input)
        val hash = TransactionSignatureBuilder(this).hashForSignature(inputs.size - 1, scriptPubKey, sigHash, anyoneCanPay)
        val ecSig = sigKey.sign(hash)
        val txSig = TransactionSignature(ecSig, sigHash, anyoneCanPay, false)
        if (scriptPubKey.isSentToRawPubKey)
            input.setScriptSig( ScriptBuilder.createInputScript(txSig) )
        else if (scriptPubKey.isSentToAddress)
            input.setScriptSig( ScriptBuilder.createInputScript(txSig, sigKey) )
        else
            throw ScriptException("Don't know how to sign for this kind of scriptPubKey: " + scriptPubKey)
        return input
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
    fun addSignedInput(prevOut: TransactionOutPoint, scriptPubKey: Script, sigKey: ECKey,
                       sigHash: SigHash, anyoneCanPay: Boolean, forkId: Boolean): TransactionInput {
        // Verify the API user didn't try to do operations out of order.
        checkState(!outputs.isEmpty(), "Attempting to sign tx without outputs.")
        val input = TransactionInput(params!!, this, byteArrayOf(), prevOut)
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
    fun clearOutputs() {
//        unCache()
//        for (output in outputs) {
//            output.parent =(null)
//        }
        outputs.clear()
        // You wanted to reserialize, right?
//        this.length = this.unsafeBitcoinSerialize().size
    }

    /**
     * Adds the given output to this transaction. The output must be completely initialized. Returns the given output.
     */
    fun addOutput(to: TransactionOutput): TransactionOutput {
//        unCache()
//        to.parent = (this)
        outputs.add(to)
//        adjustLength(outputs.size, to.length)
        return to
    }

    /**
     * Creates an output based on the given address and value, adds it to this transaction, and returns the new output.
     */
    fun addOutput(value: Coin, address: CashAddress): TransactionOutput {
        return addOutput(TransactionOutput(params!!, this, value, address))
    }

    /**
     * Creates an output that pays to the given pubkey directly (no address) with the given value, adds it to this
     * transaction, and returns the new output.
     */
    fun addOutput(value: Coin, pubkey: ECKey): TransactionOutput {
        return addOutput(TransactionOutput(params!!, this, value, pubkey))
    }

    /**
     * Creates an output that pays to the given script. The address and key forms are specialisations of this method,
     * you won't normally need to use it unless you're doing unusual things.
     */
    fun addOutput(value: Coin, script: Script): TransactionOutput {
        return addOutput(TransactionOutput(params!!, this, value, script.listProgram()))
    }

    fun addData(data:ByteArray): TransactionOutput {
        val script = ScriptBuilder.createOpReturnScript(data)
        return addOutput(TransactionOutput(params!!, this, Coin.ZERO, script.listProgram()))
    }


    @Throws(IOException::class)
    fun bitcoinSerializeToStream(stream: OutputStream) {
        ByteUtils.uint32ToByteStreamLE(version, stream)
        stream.write(VarInt(inputs.size.toLong()).encode())
        for (`in` in inputs)
            `in`.bitcoinSerializeToStream(stream)
        stream.write(VarInt(outputs.size.toLong()).encode())
        for (out in outputs)
            out.bitcoinSerializeToStream(stream)
        ByteUtils.uint32ToByteStreamLE(lockTime, stream)
    }


    /**
     * Transactions can have an associated lock time, specified either as a block height or in seconds since the
     * UNIX epoch. A transaction is not allowed to be confirmed by miners until the lock time is reached, and
     * since Bitcoin 0.8+ a transaction that did not end its lock period (non final) is considered to be non
     * standard and won't be relayed or included in the memory pool either.
     */
    fun getLockTime(): Long {
        return lockTime
    }

    /**
     * Transactions can have an associated lock time, specified either as a block height or in seconds since the
     * UNIX epoch. A transaction is not allowed to be confirmed by miners until the lock time is reached, and
     * since Bitcoin 0.8+ a transaction that did not end its lock period (non final) is considered to be non
     * standard and won't be relayed or included in the memory pool either.
     */
    fun setLockTime(lockTime: Long) {
//        unCache()
        var seqNumSet = false
        for (input in inputs) {
            if (input.sequenceNumber != TransactionInput.NO_SEQUENCE) {
                seqNumSet = true
                break
            }
        }
        if (lockTime != 0L && (!seqNumSet || inputs.isEmpty())) {
            // At least one input must have a non-default sequence number for lock times to have any effect.
            // For instance one of them can be set to zero to make this feature work.
            log.warn("You are setting the lock time on a transaction but none of the inputs have non-default sequence numbers. This will not do what you expect!")
        }
        this.lockTime = lockTime
    }

    fun setVersion(version: Int) {
        this.version = version.toLong()
//        unCache()
    }

    /** Returns an unmodifiable view of all inputs.  */
    fun getInputs(): List<TransactionInput> {
        return Collections.unmodifiableList(inputs)
    }

    /** Returns an unmodifiable view of all outputs.  */
    fun getOutputs(): List<TransactionOutput> {
        return Collections.unmodifiableList(outputs)
    }

    /**
     *
     * Returns the list of transacion outputs, whether spent or unspent, that match a wallet by address or that are
     * watched by a wallet, i.e., transaction outputs whose script's address is controlled by the wallet and transaction
     * outputs whose script is watched by the wallet.
     *
     * @param transactionBag The wallet that controls addresses and watches scripts.
     * @return linked list of outputs relevant to the wallet in this transaction
     */
/*
    fun getWalletOutputs(transactionBag: TransactionBag): List<TransactionOutput> {
        val walletOutputs = LinkedList<TransactionOutput>()
        for (o in outputs) {
            if (!o.isMineOrWatched(transactionBag)) continue
            walletOutputs.add(o)
        }

        return walletOutputs
    }
*/

    /** Randomly re-orders the transaction outputs: good for privacy  */
//    fun shuffleOutputs() {
//        Collections.shuffle(outputs)
//    }

    /** Same as getInputs().get(index).  */
    fun getInput(index: Long): TransactionInput {
        return inputs[index.toInt()]
    }

    /** Same as getOutputs().get(index)  */
    fun getOutput(index: Long): TransactionOutput {
        return outputs[index.toInt()]
    }

    /**
     * Returns the confidence object for this transaction from the [org.bitcoinj.core.TxConfidenceTable]
     * referenced by the given [Context].
     */
/*
    @JvmOverloads
    fun getConfidence(context: Context? = Context.get()): TransactionConfidence {
        return getConfidence(context!!.confidenceTable)
    }
*/

    /**
     * Returns the confidence object for this transaction from the [org.bitcoinj.core.TxConfidenceTable]
     */
/*
    fun getConfidence(table: TxConfidenceTable): TransactionConfidence {
        if (confidence == null)
            confidence = table.getOrCreate(hash!!)
        return confidence as TransactionConfidence
    }
*/

    /** Check if the transaction has a known confidence  */
/*
    fun hasConfidence(): Boolean {
        return getConfidence().getConfidenceType() != TransactionConfidence.ConfidenceType.UNKNOWN
    }
*/

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        return if (o == null || javaClass != o.javaClass) false else hash == (o as Transaction).hash
    }

    override fun hashCode(): Int {
        return hash!!.hashCode()
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
     * Checks the transaction contents for sanity, in ways that can be done in a standalone manner.
     * Does **not** perform all checks on a transaction such as whether the inputs are already spent.
     * Specifically this method verifies:
     *
     *
     *  * That there is at least one input and output.
     *  * That the serialized size is not larger than the max block size.
     *  * That no outputs have negative value.
     *  * That the outputs do not sum to larger than the max allowed quantity of coin in the system.
     *  * If the tx is a coinbase tx, the coinbase scriptSig size is within range. Otherwise that there are no
     * coinbase inputs in the tx.
     *
     *
     * @throws VerificationException
     */
    @Throws(VerificationException::class)
    fun verify() {
        if (inputs.size == 0 || outputs.size == 0)
            throw VerificationException.EmptyInputsOrOutputs()

        // TODO:
//        if (this.messageSize > Block.MAX_BLOCK_SIZE)
//            throw VerificationException.LargerThanMaxBlockSize()

        var valueOut = Coin.ZERO
        val outpoints = HashSet<TransactionOutPoint>()
        for (input in inputs) {
            if (outpoints.contains(input.outpoint))
                throw VerificationException.DuplicatedOutPoint()
            outpoints.add(input.outpoint!!)
        }
        try {
            for (output in outputs) {
                if (output.getValue().signum < 0)
                // getValue() can throw IllegalStateException
                    throw VerificationException.NegativeValueOutput()
                valueOut = valueOut.add(output.getValue())
                if (valueOut.compareTo(MAX_MONEY) > 0)
                    throw IllegalArgumentException()
            }
        } catch (e: IllegalStateException) {
            throw VerificationException.ExcessiveValue()
        } catch (e: IllegalArgumentException) {
            throw VerificationException.ExcessiveValue()
        }

        if (isCoinBase) {
            if (inputs[0].getScriptBytes()!!.size < 2 || inputs[0].getScriptBytes()!!.size > 100)
                throw VerificationException.CoinbaseScriptSizeOutOfRange()
        } else {
            for (input in inputs)
                if (input.isCoinBase)
                    throw VerificationException.UnexpectedCoinbaseInput()
        }
    }

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

        @JvmStatic val MAX_MONEY = Coin.COIN.multiply(NetworkParameters.MAX_COINS)

        /**
         * A comparator that can be used to sort transactions by their updateTime field. The ordering goes from most recent
         * into the past.
         */
/*
        val SORT_TX_BY_UPDATE_TIME: Comparator<Transaction> = Comparator { tx1, tx2 ->
            val time1 = tx1.updateTime!!.time
            val time2 = tx2.updateTime!!.time
            val updateTimeComparison = -Longs.compare(time1, time2)
            //If time1==time2, compare by tx hash to make comparator consistent with equals
            if (updateTimeComparison != 0) updateTimeComparison else tx1.hash!!.compareTo(tx2.hash!!)
        }
*/
        /** A comparator that can be used to sort transactions by their chain height.  */
/*
        val SORT_TX_BY_HEIGHT: Comparator<Transaction> = Comparator { tx1, tx2 ->
            val confidence1 = tx1.getConfidence()
            val height1 = if (confidence1.getConfidenceType() == ConfidenceType.BUILDING)
                confidence1.getAppearedAtChainHeight()
            else
                Block.BLOCK_HEIGHT_UNKNOWN
            val confidence2 = tx2.getConfidence()
            val height2 = if (confidence2.getConfidenceType() == ConfidenceType.BUILDING)
                confidence2.getAppearedAtChainHeight()
            else
                Block.BLOCK_HEIGHT_UNKNOWN
            val heightComparison = -Ints.compare(height1, height2)
            //If height1==height2, compare by tx hash to make comparator consistent with equals
            if (heightComparison != 0) heightComparison else tx1.hash!!.compareTo(tx2.hash!!)
        }
*/
        private val log = LoggerFactory.getLogger(Transaction::class.java!!)

        /** Threshold for lockTime: below this value it is interpreted as block number, otherwise as timestamp.  */
        const val LOCKTIME_THRESHOLD = 500000000 // Tue Nov  5 00:53:20 1985 UTC
        /** Same but as a BigInteger for CHECKLOCKTIMEVERIFY  */
        @JvmStatic val LOCKTIME_THRESHOLD_BIG = BigInteger.valueOf(LOCKTIME_THRESHOLD.toLong())

        /** How many bytes a transaction can be before it won't be relayed anymore. Currently 100kb.  */
        const val MAX_STANDARD_TX_SIZE = 100000

        /**
         * If feePerKb is lower than this, Bitcoin Core will treat it as if there were no fee.
         */
        @JvmStatic val REFERENCE_DEFAULT_MIN_TX_FEE = Coin.valueOf(1000) // 0.01 mBTC

        /**
         * If using this feePerKb, transactions will get confirmed within the next couple of blocks.
         * This should be adjusted from time to time. Last adjustment: March 2016.
         */
        @JvmStatic val DEFAULT_TX_FEE = Coin.valueOf(5000) // 0.5 mBTC

        /**
         * Any standard (ie pay-to-address) output smaller than this value (in satoshis) will most likely be rejected by the network.
         * This is calculated by assuming a standard output will be 34 bytes, and then using the formula used in
         * [TransactionOutput.getMinNonDustValue].
         */
        @JvmStatic val MIN_NONDUST_OUTPUT = Coin.valueOf(546) // satoshis

        const val CURRENT_VERSION = 2


        protected fun calcLength(buf: ByteArray, offset: Int): Int {
            var varint: VarInt
            // jump past version (uint32)
            var cursor = offset + 4

            var i: Int
            var scriptLen: Long

            varint = VarInt(buf, cursor)
            val txInCount = varint.value
            cursor += varint.originalSizeInBytes

            i = 0
            while (i < txInCount) {
                // 36 = length of previous_outpoint
                cursor += 36
                varint = VarInt(buf, cursor)
                scriptLen = varint.value
                // 4 = length of sequence field (unint32)
                cursor += (scriptLen + 4 + varint.originalSizeInBytes.toLong()).toInt()
                i++
            }

            varint = VarInt(buf, cursor)
            val txOutCount = varint.value
            cursor += varint.originalSizeInBytes

            i = 0
            while (i < txOutCount) {
                // 8 = length of tx value field (uint64)
                cursor += 8
                varint = VarInt(buf, cursor)
                scriptLen = varint.value
                cursor += (scriptLen + varint.originalSizeInBytes).toInt()
                i++
            }
            // 4 = length of lock_time field (uint32)
            return cursor - offset + 4
        }
    }
}
/**
 * Same as [.addSignedInput]
 * but defaults to [SigHash.ALL] and "false" for the anyoneCanPay flag. This is normally what you want.
 */
/**
 * Returns the confidence object for this transaction from the [org.bitcoinj.core.TxConfidenceTable]
 * referenced by the implicit [Context].
 */
