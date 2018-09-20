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

import com.nchain.params.NetworkParameters
import com.nchain.shared.Sha256Hash
import com.nchain.shared.VarInt
import com.nchain.shared.VerificationException
import com.nchain.tools.ByteUtils
import com.nchain.tools.HEX
import com.nchain.tools.LongMath
import com.nchain.tools.UnsafeByteArrayOutputStream
import com.nchain.script.Script
import com.nchain.script.ScriptException
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.OutputStream
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*

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
class Transaction(val version: Long = 0L,
                  val lockTime: Long = 1L,
                  inputs:List<TransactionInput>? = null,
                  outputs: List<TransactionOutput>? = null) {

    private var _hash: Sha256Hash? = null
    private var _inputSum: Coin? = null
    private var _outputSum: Coin? = null
    private var _fee: Coin? = null
    private var _length: Int? = null

    val inputs: List<TransactionInput>
    val outputs: List<TransactionOutput>

    init {
        this.inputs = if (inputs != null) Collections.unmodifiableList(inputs) else Collections.EMPTY_LIST as List<TransactionInput>
        this.outputs = if (outputs != null) Collections.unmodifiableList(outputs) else Collections.EMPTY_LIST as List<TransactionOutput>
    }

    val length: Int
        get() {
            if (_length == null) {
                val bytes = bitcoinSerialize()
                _length = bytes.size
                _hash = Sha256Hash.wrapReversed(Sha256Hash.hashTwice(bytes))
            }
            return _length!!
        }

    val hash: Sha256Hash
        get() {
            if (_hash == null) {
                val bytes = bitcoinSerialize()
                _length = bytes.size
                _hash = Sha256Hash.wrapReversed(Sha256Hash.hashTwice(bytes))
            }
            return _hash!!
        }

    val hashAsString: String
        get() = hash.toString()

    /**
     * Gets the sum of the inputs, regardless of who owns them.
     */
    val inputSum: Coin
        get() {
            if (_inputSum == null) {
                var inputSum = 0L
                for (input in inputs) {
                    if (input.connectedOutput?.value != null) {
                        inputSum = LongMath.checkedAdd(inputSum, input.connectedOutput!!.value.value)
                    }
                }
                _inputSum = Coin.valueOf(inputSum)
            }
            return _inputSum!!
        }

    val outputSum: Coin
        get() {
            if (_outputSum == null) {
                var outputSum = 0L
                for (output in outputs) {
                    outputSum = LongMath.checkedAdd(outputSum, output.value.value)
                }
                _outputSum = Coin.valueOf(outputSum)
            }
            return _outputSum!!
        }

    val fee: Coin
        get() {
            if (_fee == null) {
                _fee = inputSum.subtract(outputSum)
            }
            return _fee!!
        }


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


    /**
     * Convenience wrapper around getConfidence().getConfidenceType()
     * @return true if this transaction hasn't been seen in any block yet.
     */
//    val isPending: Boolean
//        get() = getConfidence().getConfidenceType() == TransactionConfidence.ConfidenceType.PENDING

//    private var cachedValue: Coin? = null
//    private var cachedForBag: TransactionBag? = null

    /**
     * The transaction fee is the difference of the value of all inputs and the value of all outputs. Currently, the fee
     * can only be determined for transactions created by us.
     *
     * @return fee, or null if it cannot be determined
     */

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
                val benefit = 41 + Math.min(110, input.scriptSig.listProgram().size)
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

            return if (getConfidence().getConfidenceType() != ConfidenceType.BUILDING) false else getConfidence().depthInBlocks >= params.spendableCoinbaseDepth

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
                sigOps += Script.getSigOpCount(input.scriptBytes)
            for (output in outputs)
                sigOps += Script.getSigOpCount(output.scriptBytes)
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
    val isTimeLocked: Boolean
        get() {
            if (lockTime == 0L)
                return false
            for (input in inputs)
                if (input.hasSequence())
                    return true
            return false
        }

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
    fun getOptimalEncodingMessageSize(): Int {
        if (optimalEncodingMessageSize != 0)
            return optimalEncodingMessageSize
        optimalEncodingMessageSize = messageSize
        return optimalEncodingMessageSize
    }
*/

    /**
     * A human readable version of the transaction useful for debugging. The format is not guaranteed to be stable.
     * @param chain If provided, will be used to estimate lock times (if set). Can be null.
     */
    override fun toString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")

        val s = StringBuilder()
        s.append("  ").append(hashAsString).append('\n')
//        if (updatedAt != null)
//            s.append("  updated: ").append(Utils.dateTimeFormat(updatedAt as Date)).append('\n')
        if (version != 1L)
            s.append("  version ").append(version).append('\n')

        if (isTimeLocked) {
            s.append("  time locked until ")
            if (lockTime < LOCKTIME_THRESHOLD) {
                s.append("block ").append(lockTime)
//                if (chain != null) {
//                    s.append(" (estimated to be reached at ")
//                            .append(Utils.dateTimeFormat(chain.estimateBlockTime(lockTime.toInt()))).append(')')
//                }
            } else {
                s.append(sdf.format(lockTime * 1000))
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
                script = inputs[0].scriptSig.toString()
                script2 = outputs[0].scriptPubKey.toString()
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
                val scriptSig = `in`.scriptSig
                s.append(scriptSig)
                if (`in`.connectedOutput?.value != null)
                    s.append(" ").append(`in`.connectedOutput!!.value.toFriendlyString())
                s.append("\n          ")
                s.append("outpoint:")
                val outpoint = `in`.outpoint
                s.append(outpoint.toString())
                val connectedOutput = outpoint.connectedOutput
                if (connectedOutput != null) {
                    val scriptPubKey = connectedOutput.scriptPubKey
                    if (scriptPubKey.isSentToAddress || scriptPubKey.isPayToScriptHash) {
                        s.append(" hash160:")
                        s.append(HEX.encode(scriptPubKey.pubKeyHash))
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
                val scriptPubKey = out.scriptPubKey
                s.append(scriptPubKey)
                s.append(" ")
                s.append(out.value.toFriendlyString())
//                if (!out.isAvailableForSpending) {
//                    s.append(" Spent")
//                }
//                if (out.spentBy != null) {
//                    s.append(" by ")
//                    s.append(out.spentBy!!.parentTransaction.hashAsString)
//                }
            } catch (e: Exception) {
                s.append("[exception: ").append(e.message).append("]")
            }

            s.append('\n')
        }
        val fee = fee
        if (fee != null) {
            s.append("     fee  ").append(fee.multiply(1000).divide(length.toLong()).toFriendlyString()).append("/kB, ")
                    .append(fee.toFriendlyString()).append(" for ").append(length).append(" bytes\n")
        }
//        if (purpose != null)
//            s.append("     prps ").append(purpose).append('\n')
        return s.toString()
    }


    fun bitcoinSerialize():ByteArray {
        val stream = UnsafeByteArrayOutputStream()
        try {
            bitcoinSerializeToStream(stream)
            stream.close()
        } catch (e: IOException) {
            // It will never happen
        }
        return stream.toByteArray()
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
        return if (o != null && o is Transaction)
            hash == o.hash
        else
            false
    }

    override fun hashCode(): Int {
        return hash.hashCode()
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

        if (length > NetworkParameters.MAX_BLOCK_SIZE)
            throw VerificationException.LargerThanMaxBlockSize()

        val outpoints = HashSet<TransactionOutPoint>()
        for (input in inputs) {
            if (outpoints.contains(input.outpoint))
                throw VerificationException.DuplicatedOutPoint()
            outpoints.add(input.outpoint)
        }
        try {
            var valueOut = Coin.ZERO
            for (output in outputs) {
                if (output.value.signum < 0)
                    throw VerificationException.NegativeValueOutput()
                valueOut = valueOut.add(output.value)
                if (valueOut.compareTo(MAX_MONEY) > 0)
                    throw IllegalArgumentException()
            }
        } catch (e: IllegalStateException) {
            throw VerificationException.ExcessiveValue()
        } catch (e: IllegalArgumentException) {
            throw VerificationException.ExcessiveValue()
        }

        if (isCoinBase) {
            if (inputs[0].scriptBytes.size < 2 || inputs[0].scriptBytes.size > 100)
                throw VerificationException.CoinbaseScriptSizeOutOfRange()
        } else {
            for (input in inputs)
                if (input.isCoinBase)
                    throw VerificationException.UnexpectedCoinbaseInput()
        }

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
            // TODO: move to a method
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

        @JvmStatic
        val MAX_MONEY = Coin.COIN.multiply(NetworkParameters.MAX_COINS)

        @JvmStatic
        @JvmOverloads
        fun parse(bytes:ByteArray, offset: Int = 0): Transaction {
            return TransactionBuilder.parse(bytes, offset).build()
        }

        @JvmStatic
        fun parse(rawHex:String) : Transaction {
            return TransactionBuilder.parse(rawHex).build()
        }

        private val log = LoggerFactory.getLogger(Transaction::class.java)

        /** Threshold for lockTime: below this value it is interpreted as block number, otherwise as timestamp.  */
        const val LOCKTIME_THRESHOLD = 500000000 // Tue Nov  5 00:53:20 1985 UTC

        /** Same but as a BigInteger for CHECKLOCKTIMEVERIFY  */
        @JvmStatic val LOCKTIME_THRESHOLD_BIG = BigInteger.valueOf(LOCKTIME_THRESHOLD.toLong())

        /** How many bytes a transaction can be before it won't be relayed anymore. Currently 100kb.  */
//        const val MAX_STANDARD_TX_SIZE = 100000

        /**
         * If feePerKb is lower than this, Bitcoin Core will treat it as if there were no fee.
         */
//        @JvmStatic val REFERENCE_DEFAULT_MIN_TX_FEE = Coin.valueOf(1000) // 0.01 mBTC

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

//        const val CURRENT_VERSION = 2


    }
}
