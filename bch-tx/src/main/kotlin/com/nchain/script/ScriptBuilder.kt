/*
 * Copyright 2013 Google Inc.
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

package com.nchain.script

import com.nchain.address.CashAddress
import com.nchain.key.ECKey
import com.nchain.tools.ByteUtils
import com.nchain.tx.Transaction
import com.nchain.tx.TransactionSignature
import com.nchain.script.Script
import com.nchain.script.ScriptOpCodes.OP_0
import com.nchain.script.ScriptOpCodes.OP_CHECKLOCKTIMEVERIFY
import com.nchain.script.ScriptOpCodes.OP_CHECKMULTISIG
import com.nchain.script.ScriptOpCodes.OP_CHECKSIG
import com.nchain.script.ScriptOpCodes.OP_CHECKSIGVERIFY
import com.nchain.script.ScriptOpCodes.OP_DROP
import com.nchain.script.ScriptOpCodes.OP_DUP
import com.nchain.script.ScriptOpCodes.OP_ELSE
import com.nchain.script.ScriptOpCodes.OP_ENDIF
import com.nchain.script.ScriptOpCodes.OP_EQUAL
import com.nchain.script.ScriptOpCodes.OP_EQUALVERIFY
import com.nchain.script.ScriptOpCodes.OP_HASH160
import com.nchain.script.ScriptOpCodes.OP_IF
import com.nchain.script.ScriptOpCodes.OP_PUSHDATA1
import com.nchain.script.ScriptOpCodes.OP_PUSHDATA2
import com.nchain.script.ScriptOpCodes.OP_PUSHDATA4
import com.nchain.script.ScriptOpCodes.OP_RETURN

import java.math.BigInteger
import java.util.*

/**
 *
 * Tools for the construction of commonly used script types. You don't normally need this as it's hidden behind
 * convenience methods on [Transaction], but they are useful when working with the
 * protocol at a lower level.
 */
class ScriptBuilder {
    private var chunks: MutableList<ScriptChunk>? = null

    /** Creates a fresh ScriptBuilder with an empty program.  */
    constructor() {
        chunks = LinkedList()
    }

    /** Creates a fresh ScriptBuilder with the given program as the starting point.  */
    constructor(template: Script) {
        chunks = ArrayList(template.getChunks())
    }

    /** Adds the given chunk to the end of the program  */
    fun addChunk(chunk: ScriptChunk): ScriptBuilder {
        return addChunk(chunks!!.size, chunk)
    }

    /** Adds the given chunk at the given index in the program  */
    fun addChunk(index: Int, chunk: ScriptChunk): ScriptBuilder {
        chunks!!.add(index, chunk)
        return this
    }

    /** Adds the given opcode to the end of the program.  */
    fun op(opcode: Int): ScriptBuilder {
        return op(chunks!!.size, opcode)
    }

    /** Adds the given opcode to the given index in the program  */
    fun op(index: Int, opcode: Int): ScriptBuilder {
        check(opcode > OP_PUSHDATA4);
        return addChunk(index, ScriptChunk(opcode, null))
    }

    /** Adds a copy of the given byte array as a data element (i.e. PUSHDATA) at the end of the program.  */
    fun data(data: ByteArray): ScriptBuilder {
        return if (data.size == 0)
            smallNum(0)
        else
            data(chunks!!.size, data)
    }

    /** Adds a copy of the given byte array as a data element (i.e. PUSHDATA) at the given index in the program.  */
    fun data(index: Int, data: ByteArray): ScriptBuilder {
        // implements BIP62
        val copy = Arrays.copyOf(data, data.size)
        val opcode: Int
        if (data.size == 0) {
            opcode = OP_0
        } else if (data.size == 1) {
            val b = data[0]
            if (b >= 1 && b <= 16)
                opcode = Script.encodeToOpN(b.toInt())
            else
                opcode = 1
        } else if (data.size < OP_PUSHDATA1) {
            opcode = data.size
        } else if (data.size < 256) {
            opcode = OP_PUSHDATA1
        } else if (data.size < 65536) {
            opcode = OP_PUSHDATA2
        } else {
            throw RuntimeException("Unimplemented")
        }
        return addChunk(index, ScriptChunk(opcode, copy))
    }

    /**
     * Adds the given number to the end of the program. Automatically uses
     * shortest encoding possible.
     */
    fun number(num: Long): ScriptBuilder {
        return if (num >= 0 && num <= 16) {
            smallNum(num.toInt())
        } else {
            bigNum(num)
        }
    }

    /**
     * Adds the given number to the given index in the program. Automatically
     * uses shortest encoding possible.
     */
    fun number(index: Int, num: Long): ScriptBuilder {
        return if (num >= 0 && num <= 16) {
            addChunk(index, ScriptChunk(Script.encodeToOpN(num.toInt()), null))
        } else {
            bigNum(index, num)
        }
    }

    /**
     * Adds the given number as a OP_N opcode to the end of the program.
     * Only handles values 0-16 inclusive.
     *
     * @see .number
     */
    fun smallNum(num: Int): ScriptBuilder {
        return smallNum(chunks!!.size, num)
    }

    /** Adds the given number as a push data chunk.
     * This is intended to use for negative numbers or values > 16, and although
     * it will accept numbers in the range 0-16 inclusive, the encoding would be
     * considered non-standard.
     *
     * @see .number
     */
    protected fun bigNum(num: Long): ScriptBuilder {
        return bigNum(chunks!!.size, num)
    }

    /**
     * Adds the given number as a OP_N opcode to the given index in the program.
     * Only handles values 0-16 inclusive.
     *
     * @see .number
     */
    fun smallNum(index: Int, num: Int): ScriptBuilder {
        check(num >= 0, {"Cannot encode negative numbers with smallNum"});
        check(num <= 16, {"Cannot encode numbers larger than 16 with smallNum"});
        return addChunk(index, ScriptChunk(Script.encodeToOpN(num), null))
    }

    /**
     * Adds the given number as a push data chunk to the given index in the program.
     * This is intended to use for negative numbers or values > 16, and although
     * it will accept numbers in the range 0-16 inclusive, the encoding would be
     * considered non-standard.
     *
     * @see .number
     */
    protected fun bigNum(index: Int, num: Long): ScriptBuilder {
        val data: ByteArray

        if (num == 0L) {
            data = ByteArray(0)
        } else {
            val result = Stack<Byte>()
            val neg = num < 0
            var absvalue = Math.abs(num)

            while (absvalue != 0L) {
                result.push((absvalue and 0xff).toByte())
                absvalue = absvalue shr 8
            }

            if (result.peek().toInt() and 0x80 != 0) {
                // The most significant byte is >= 0x80, so push an extra byte that
                // contains just the sign of the value.
                result.push((if (neg) 0x80 else 0).toByte())
            } else if (neg) {
                // The most significant byte is < 0x80 and the value is negative,
                // set the sign bit so it is subtracted and interpreted as a
                // negative when converting back to an integral.
                result.push((result.pop().toInt() or 0x80).toByte())
            }

            data = ByteArray(result.size)
            for (byteIdx in data.indices) {
                data[byteIdx] = result[byteIdx]
            }
        }

        // At most the encoded value could take up to 8 bytes, so we don't need
        // to use OP_PUSHDATA opcodes
        return addChunk(index, ScriptChunk(data.size, data))
    }

    /** Creates a new immutable Script based on the state of the builder.  */
    fun build(): Script {
        return Script(chunks)
    }

    companion object {

        /** Creates a scriptPubKey that encodes payment to the given address.  */
        @JvmStatic
        fun createOutputScript(to: CashAddress): Script {
            return if (to.isP2SHAddress) {
                // OP_HASH160 <scriptHash> OP_EQUAL
                ScriptBuilder()
                        .op(OP_HASH160)
                        .data(to.hash160)
                        .op(OP_EQUAL)
                        .build()
            } else {
                // OP_DUP OP_HASH160 <pubKeyHash> OP_EQUALVERIFY OP_CHECKSIG
                ScriptBuilder()
                        .op(OP_DUP)
                        .op(OP_HASH160)
                        .data(to.hash160)
                        .op(OP_EQUALVERIFY)
                        .op(OP_CHECKSIG)
                        .build()
            }
        }

        /** Creates a scriptPubKey that encodes payment to the given raw public key.  */
        @JvmStatic
        fun createOutputScript(key: ECKey): Script {
            return ScriptBuilder().data(key.pubKey).op(OP_CHECKSIG).build()
        }

        /**
         * Creates a scriptSig that can redeem a pay-to-address output.
         * If given signature is null, incomplete scriptSig will be created with OP_0 instead of signature
         */

        @JvmStatic
        fun createInputScript(signature: TransactionSignature?, pubKey: ECKey): Script {
            val pubkeyBytes = pubKey.pubKey
            val sigBytes = if (signature != null) signature.bitcoinSerialize() else byteArrayOf()
            return ScriptBuilder().data(sigBytes).data(pubkeyBytes).build()
        }


        /**
         * Creates a scriptSig that can redeem a pay-to-pubkey output.
         * If given signature is null, incomplete scriptSig will be created with OP_0 instead of signature
         */
        @JvmStatic
        fun createInputScript(signature: TransactionSignature?): Script {
            val sigBytes = if (signature != null) signature.bitcoinSerialize() else byteArrayOf()
            return ScriptBuilder().data(sigBytes).build()
        }

        /** Creates a program that requires at least N of the given keys to sign, using OP_CHECKMULTISIG.  */
        @JvmStatic
        fun createMultiSigOutputScript(threshold: Int, pubkeys: List<ECKey>): Script {
            check(threshold > 0);
            check(threshold <= pubkeys.size);
            check(pubkeys.size <= 16);  // That's the max we can represent with a single opcode.
            val builder = ScriptBuilder()
            builder.smallNum(threshold)
            for (key in pubkeys) {
                builder.data(key.pubKey)
            }
            builder.smallNum(pubkeys.size)
            builder.op(OP_CHECKMULTISIG)
            return builder.build()
        }

        /** Create a program that satisfies an OP_CHECKMULTISIG program.  */
        @JvmStatic
        fun createMultiSigInputScript(signatures: List<TransactionSignature>): Script {
            val sigs = ArrayList<ByteArray>(signatures.size)
            for (signature in signatures) {
                sigs.add(signature.bitcoinSerialize())
            }

            return createMultiSigInputScriptBytes(sigs, null)
        }


        /** Create a program that satisfies an OP_CHECKMULTISIG program.  */
        @JvmStatic
        fun createMultiSigInputScript(vararg signatures: TransactionSignature): Script {
            return createMultiSigInputScript(Arrays.asList(*signatures))
        }

        /**
         * Create a program that satisfies a pay-to-script hashed OP_CHECKMULTISIG program.
         * If given signature list is null, incomplete scriptSig will be created with OP_0 instead of signatures
         */
        @JvmStatic
        fun createP2SHMultiSigInputScript(signatures: List<TransactionSignature>?,
                                          multisigProgram: Script): Script {
            val sigs = ArrayList<ByteArray>()
            if (signatures == null) {
                // create correct number of empty signatures
                val numSigs = multisigProgram.numberOfSignaturesRequiredToSpend
                for (i in 0 until numSigs)
                    sigs.add(byteArrayOf())
            } else {
                for (signature in signatures) {
                    sigs.add(signature.bitcoinSerialize())
                }
            }
            return createMultiSigInputScriptBytes(sigs, multisigProgram.getProgram())
        }


        /**
         * Create a program that satisfies an OP_CHECKMULTISIG program, using pre-encoded signatures.
         * Optionally, appends the script program bytes if spending a P2SH output.
         */
        @JvmStatic
        @JvmOverloads
        fun createMultiSigInputScriptBytes(signatures: List<ByteArray>, multisigProgramBytes: ByteArray? = null): Script {
            check(signatures.size <= 16);
            val builder = ScriptBuilder()
            builder.smallNum(0)  // Work around a bug in CHECKMULTISIG that is now a required part of the protocol.
            for (signature in signatures)
                builder.data(signature)
            if (multisigProgramBytes != null)
                builder.data(multisigProgramBytes)
            return builder.build()
        }

        /**
         * Returns a copy of the given scriptSig with the signature inserted in the given position.
         *
         * This function assumes that any missing sigs have OP_0 placeholders. If given scriptSig already has all the signatures
         * in place, IllegalArgumentException will be thrown.
         *
         * @param targetIndex where to insert the signature
         * @param sigsPrefixCount how many items to copy verbatim (e.g. initial OP_0 for multisig)
         * @param sigsSuffixCount how many items to copy verbatim at end (e.g. redeemScript for P2SH)
         */
        @JvmStatic
        fun updateScriptWithSignature(scriptSig: Script, signature: ByteArray, targetIndex: Int,
                                      sigsPrefixCount: Int, sigsSuffixCount: Int): Script {
            val builder = ScriptBuilder()
            val inputChunks = scriptSig.getChunks()
            val totalChunks = inputChunks.size

            // Check if we have a place to insert, otherwise just return given scriptSig unchanged.
            // We assume here that OP_0 placeholders always go after the sigs, so
            // to find if we have sigs missing, we can just check the chunk in latest sig position
            val hasMissingSigs = inputChunks[totalChunks - sigsSuffixCount - 1].equalsOpCode(OP_0)
            check(hasMissingSigs, {"ScriptSig is already filled with signatures"});

            // copy the prefix
            for (chunk in inputChunks.subList(0, sigsPrefixCount))
                builder.addChunk(chunk)

            // copy the sigs
            var pos = 0
            var inserted = false
            for (chunk in inputChunks.subList(sigsPrefixCount, totalChunks - sigsSuffixCount)) {
                if (pos == targetIndex) {
                    inserted = true
                    builder.data(signature)
                    pos++
                }
                if (!chunk.equalsOpCode(OP_0)) {
                    builder.addChunk(chunk)
                    pos++
                }
            }

            // add OP_0's if needed, since we skipped them in the previous loop
            while (pos < totalChunks - sigsPrefixCount - sigsSuffixCount) {
                if (pos == targetIndex) {
                    inserted = true
                    builder.data(signature)
                } else {
                    builder.addChunk(ScriptChunk(OP_0, null))
                }
                pos++
            }

            // copy the suffix
            for (chunk in inputChunks.subList(totalChunks - sigsSuffixCount, totalChunks))
                builder.addChunk(chunk)

            check(inserted);
            return builder.build()
        }

        /**
         * Creates a scriptPubKey that sends to the given script hash. Read
         * [BIP 16](https://github.com/bitcoin/bips/blob/master/bip-0016.mediawiki) to learn more about this
         * kind of script.
         */
        @JvmStatic
        fun createP2SHOutputScript(hash: ByteArray): Script {
            check(hash.size == 20);
            return ScriptBuilder().op(OP_HASH160).data(hash).op(OP_EQUAL).build()
        }

        /**
         * Creates a scriptPubKey for the given redeem script.
         */
        @JvmStatic
        fun createP2SHOutputScript(redeemScript: Script): Script {
            val hash = ByteUtils.sha256hash160(redeemScript.getProgram())
            return createP2SHOutputScript(hash)
        }

        /**
         * Creates a P2SH output script with given public keys and threshold. Given public keys will be placed in
         * redeem script in the lexicographical sorting order.
         */
        @JvmStatic
        fun createP2SHOutputScript(threshold: Int, pubkeys: List<ECKey>): Script {
            val redeemScript = createRedeemScript(threshold, pubkeys)
            return createP2SHOutputScript(redeemScript)
        }

        /**
         * Creates redeem script with given public keys and threshold. Given public keys will be placed in
         * redeem script in the lexicographical sorting order.
         */
        @JvmStatic
        fun createRedeemScript(threshold: Int, pubkeys: List<ECKey>): Script {
            var pubkeys = pubkeys
            pubkeys = ArrayList(pubkeys)
            //        Collections.sort(pubkeys, ECKey.PUBKEY_COMPARATOR);
            return createMultiSigOutputScript(threshold, pubkeys)
        }

        /**
         * Creates a script of the form OP_RETURN [data]. This feature allows you to attach a small piece of data (like
         * a hash of something stored elsewhere) to a zero valued output which can never be spent and thus does not pollute
         * the ledger.
         */
        @JvmStatic
        fun createOpReturnScript(data: ByteArray): Script {
            check(data.size <= 80);
            return ScriptBuilder().op(OP_RETURN).data(data).build()
        }

        @JvmStatic
        fun createCLTVPaymentChannelOutput(time: BigInteger, from: ECKey, to: ECKey): Script {
            val timeBytes = ByteUtils.reverseBytes(ByteUtils.encodeMPI(time, false))
            if (timeBytes.size > 5) {
                throw RuntimeException("Time too large to encode as 5-byte int")
            }
            return ScriptBuilder().op(OP_IF)
                    .data(to.pubKey).op(OP_CHECKSIGVERIFY)
                    .op(OP_ELSE)
                    .data(timeBytes).op(OP_CHECKLOCKTIMEVERIFY).op(OP_DROP)
                    .op(OP_ENDIF)
                    .data(from.pubKey).op(OP_CHECKSIG).build()
        }

        @JvmStatic
        fun createCLTVPaymentChannelRefund(signature: TransactionSignature): Script {
            val builder = ScriptBuilder()
            builder.data(signature.bitcoinSerialize())
            builder.data(byteArrayOf(0)) // Use the CHECKLOCKTIMEVERIFY if branch
            return builder.build()
        }

        @JvmStatic
        fun createCLTVPaymentChannelP2SHRefund(signature: TransactionSignature, redeemScript: Script): Script {
            val builder = ScriptBuilder()
            builder.data(signature.bitcoinSerialize())
            builder.data(byteArrayOf(0)) // Use the CHECKLOCKTIMEVERIFY if branch
            builder.data(redeemScript.getProgram())
            return builder.build()
        }

        @JvmStatic
        fun createCLTVPaymentChannelP2SHInput(from: ByteArray, to: ByteArray, redeemScript: Script): Script {
            val builder = ScriptBuilder()
            builder.data(from)
            builder.data(to)
            builder.smallNum(1) // Use the CHECKLOCKTIMEVERIFY if branch
            builder.data(redeemScript.getProgram())
            return builder.build()
        }

        @JvmStatic
        fun createCLTVPaymentChannelInput(from: TransactionSignature, to: TransactionSignature): Script {
            return createCLTVPaymentChannelInput(from.bitcoinSerialize(), to.bitcoinSerialize())
        }

        @JvmStatic
        fun createCLTVPaymentChannelInput(from: ByteArray, to: ByteArray): Script {
            val builder = ScriptBuilder()
            builder.data(from)
            builder.data(to)
            builder.smallNum(1) // Use the CHECKLOCKTIMEVERIFY if branch
            return builder.build()
        }
    }
}
/** Create a program that satisfies an OP_CHECKMULTISIG program, using pre-encoded signatures.  */
