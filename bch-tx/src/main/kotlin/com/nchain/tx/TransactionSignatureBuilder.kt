package com.nchain.bitcoinkt.core

import com.nchain.key.ECKey
import com.nchain.shared.Sha256Hash
import com.nchain.shared.VarInt
import com.nchain.tools.ByteUtils
import com.nchain.tools.UnsafeByteArrayOutputStream
import com.nchain.tx.Coin
import com.nchain.tx.Transaction
import com.nchain.tx.TransactionOutput
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptOpCodes
import org.bitcoinj.tx.TransactionSignature
import org.spongycastle.crypto.params.KeyParameter
import java.io.IOException
import java.math.BigInteger
import java.util.ArrayList
import kotlin.experimental.and

/*
 * @author Alberto Vilches
 * @date 27/06/2018
 */


class TransactionSignatureBuilder(val transaction: Transaction) {


    /**
     * Calculates a signature that is valid for being inserted into the input at the given position. This is simply
     * a wrapper around calling [Transaction.hashForSignature]
     * followed by [ECKey.sign] and then returning a new [TransactionSignature]. The key
     * must be usable for signing as-is: if the key is encrypted it must be decrypted first external to this method.
     *
     * @param inputIndex Which input to calculate the signature for, as an index.
     * @param key The private key used to calculate the signature.
     * @param redeemScript Byte-exact contents of the scriptPubKey that is being satisified, or the P2SH redeem script.
     * @param hashType Signing mode, see the enum for documentation.
     * @param anyoneCanPay Signing mode, see the SigHash enum for documentation.
     * @return A newly calculated signature object that wraps the r, s and sighash components.
     */
    fun calculateSignature(inputIndex: Int, key: ECKey,
                           redeemScript: ByteArray,
                           hashType: Transaction.SigHash, anyoneCanPay: Boolean): TransactionSignature {
        val hash = hashForSignature(inputIndex, redeemScript, hashType, anyoneCanPay)
        return TransactionSignature(key.sign(hash), hashType, anyoneCanPay)
    }

    fun calculateWitnessSignature(
            inputIndex: Int,
            key: ECKey,
            redeemScript: ByteArray,
            value: Coin,
            hashType: Transaction.SigHash,
            anyoneCanPay: Boolean): TransactionSignature {
        val hash = hashForSignatureWitness(inputIndex, redeemScript, value, hashType, anyoneCanPay)
        return TransactionSignature(key.sign(hash), hashType, anyoneCanPay, true)
    }

    /**
     * Calculates a signature that is valid for being inserted into the input at the given position. This is simply
     * a wrapper around calling [Transaction.hashForSignature]
     * followed by [ECKey.sign] and then returning a new [TransactionSignature].
     *
     * @param inputIndex Which input to calculate the signature for, as an index.
     * @param key The private key used to calculate the signature.
     * @param redeemScript The scriptPubKey that is being satisified, or the P2SH redeem script.
     * @param hashType Signing mode, see the enum for documentation.
     * @param anyoneCanPay Signing mode, see the SigHash enum for documentation.
     * @return A newly calculated signature object that wraps the r, s and sighash components.
     */
    fun calculateSignature(inputIndex: Int, key: ECKey,
                           redeemScript: Script,
                           hashType: Transaction.SigHash, anyoneCanPay: Boolean): TransactionSignature {
        val hash = hashForSignature(inputIndex, redeemScript.listProgram(), hashType, anyoneCanPay)
        return TransactionSignature(key.sign(hash), hashType, anyoneCanPay)
    }

    fun calculateWitnessSignature(
            inputIndex: Int,
            key: ECKey,
            redeemScript: Script,
            value: Coin,
            hashType: Transaction.SigHash,
            anyoneCanPay: Boolean): TransactionSignature {
        val hash = hashForSignatureWitness(inputIndex, redeemScript.listProgram(), value, hashType, anyoneCanPay)
        return TransactionSignature(key.sign(hash), hashType, anyoneCanPay, true)
    }

    /**
     * Calculates a signature that is valid for being inserted into the input at the given position. This is simply
     * a wrapper around calling [Transaction.hashForSignature]
     * followed by [ECKey.sign] and then returning a new [TransactionSignature]. The key
     * must be usable for signing as-is: if the key is encrypted it must be decrypted first external to this method.
     *
     * @param inputIndex Which input to calculate the signature for, as an index.
     * @param key The private key used to calculate the signature.
     * @param aesKey The AES key to use for decryption of the private key. If null then no decryption is required.
     * @param redeemScript Byte-exact contents of the scriptPubKey that is being satisified, or the P2SH redeem script.
     * @param hashType Signing mode, see the enum for documentation.
     * @param anyoneCanPay Signing mode, see the SigHash enum for documentation.
     * @return A newly calculated signature object that wraps the r, s and sighash components.
     */
/*
    fun calculateSignature(
            inputIndex: Int,
            key: ECKey,
            aesKey: KeyParameter?,
            redeemScript: ByteArray,
            hashType: Transaction.SigHash,
            anyoneCanPay: Boolean, forkId: Boolean): TransactionSignature {
        val hash = hashForSignature(inputIndex, redeemScript, hashType, anyoneCanPay)
        return TransactionSignature(key.sign(hash, aesKey), hashType, anyoneCanPay)
    }

    fun calculateWitnessSignature(
            inputIndex: Int,
            key: ECKey,
            aesKey: KeyParameter?,
            redeemScript: ByteArray,
            value: Coin,
            hashType: Transaction.SigHash,
            anyoneCanPay: Boolean): TransactionSignature {
        val hash = hashForSignatureWitness(inputIndex, redeemScript, value, hashType, anyoneCanPay)
        return TransactionSignature(key.sign(hash, aesKey), hashType, anyoneCanPay, true)
    }
*/

    /**
     * Calculates a signature that is valid for being inserted into the input at the given position. This is simply
     * a wrapper around calling [Transaction.hashForSignature]
     * followed by [ECKey.sign] and then returning a new [TransactionSignature].
     *
     * @param inputIndex Which input to calculate the signature for, as an index.
     * @param key The private key used to calculate the signature.
     * @param aesKey The AES key to use for decryption of the private key. If null then no decryption is required.
     * @param redeemScript The scriptPubKey that is being satisified, or the P2SH redeem script.
     * @param hashType Signing mode, see the enum for documentation.
     * @param anyoneCanPay Signing mode, see the SigHash enum for documentation.
     * @return A newly calculated signature object that wraps the r, s and sighash components.
     */
/*
    fun calculateSignature(
            inputIndex: Int,
            key: ECKey,
            aesKey: KeyParameter?,
            redeemScript: Script,
            hashType: Transaction.SigHash,
            anyoneCanPay: Boolean): TransactionSignature {
        val hash = hashForSignature(inputIndex, redeemScript.listProgram(), hashType, anyoneCanPay)
        return TransactionSignature(key.sign(hash, aesKey), hashType, anyoneCanPay, false)
    }

    fun calculateWitnessSignature(
            inputIndex: Int,
            key: ECKey,
            aesKey: KeyParameter?,
            redeemScript: Script,
            value: Coin,
            hashType: Transaction.SigHash,
            anyoneCanPay: Boolean): TransactionSignature {
        val hash = hashForSignatureWitness(inputIndex, redeemScript.listProgram(), value, hashType, anyoneCanPay)
        return TransactionSignature(key.sign(hash, aesKey), hashType, anyoneCanPay, true)
    }
*/

    /**
     *
     * Calculates a signature hash, that is, a hash of a simplified form of the transaction. How exactly the transaction
     * is simplified is specified by the type and anyoneCanPay parameters.
     *
     *
     * This is a low level API and when using the regular [Wallet] class you don't have to call this yourself.
     * When working with more complex transaction types and contracts, it can be necessary. When signing a P2SH output
     * the redeemScript should be the script encoded into the scriptSig field, for normal transactions, it's the
     * scriptPubKey of the output you're signing for.
     *
     * @param inputIndex input the signature is being calculated for. Tx signatures are always relative to an input.
     * @param redeemScript the bytes that should be in the given input during signing.
     * @param type Should be SigHash.ALL
     * @param anyoneCanPay should be false.
     */
    fun hashForSignature(inputIndex: Int, redeemScript: ByteArray,
                         type: Transaction.SigHash, anyoneCanPay: Boolean): Sha256Hash {
        val sigHashType = TransactionSignature.calcSigHashValue(type, anyoneCanPay).toByte()
        return hashForSignature(inputIndex, redeemScript, sigHashType)!!
    }

    /**
     *
     * Calculates a signature hash, that is, a hash of a simplified form of the transaction. How exactly the transaction
     * is simplified is specified by the type and anyoneCanPay parameters.
     *
     *
     * This is a low level API and when using the regular [Wallet] class you don't have to call this yourself.
     * When working with more complex transaction types and contracts, it can be necessary. When signing a P2SH output
     * the redeemScript should be the script encoded into the scriptSig field, for normal transactions, it's the
     * scriptPubKey of the output you're signing for.
     *
     * @param inputIndex input the signature is being calculated for. Tx signatures are always relative to an input.
     * @param redeemScript the script that should be in the given input during signing.
     * @param type Should be SigHash.ALL
     * @param anyoneCanPay should be false.
     */
    fun hashForSignature(inputIndex: Int, redeemScript: Script,
                         type: Transaction.SigHash, anyoneCanPay: Boolean): Sha256Hash {
        val sigHash = TransactionSignature.calcSigHashValue(type, anyoneCanPay)
        return hashForSignature(inputIndex, redeemScript.listProgram(), sigHash.toByte())!!
    }

    /**
     * This is required for signatures which use a sigHashType which cannot be represented using SigHash and anyoneCanPay
     * See transaction c99c49da4c38af669dea436d3e73780dfdb6c1ecf9958baa52960e8baee30e73, which has sigHashType 0
     */
    fun hashForSignature(inputIndex: Int, connectedScript: ByteArray, sigHashType: Byte): Sha256Hash? {
        var connectedScript = connectedScript
        // The SIGHASH flags are used in the design of contracts, please see this page for a further understanding of
        // the purposes of the code in this method:
        //
        //   https://en.bitcoin.it/wiki/Contracts

        try {
            // Create a copy of this transaction to operate upon because we need make changes to the inputs and outputs.
            // It would not be thread-safe to change the attributes of the transaction object itself.
            val transaction = transaction // .params!!.defaultSerializer!!.makeTransaction(transaction.bitcoinSerialize())

            // Clear input scripts in preparation for signing. If we're signing a fresh
            // transaction that step isn't very helpful, but it doesn't add much cost relative to the actual
            // EC math so we'll do it anyway.
            val inputs = transaction.getInputs()
            for (i in inputs.indices) {
                inputs[i].clearScriptBytes()
            }

            // This step has no purpose beyond being synchronized with Bitcoin Core's bugs. OP_CODESEPARATOR
            // is a legacy holdover from a previous, broken design of executing scripts that shipped in Bitcoin 0.1.
            // It was seriously flawed and would have let anyone take anyone elses money. Later versions switched to
            // the design we use today where scripts are executed independently but share a stack. This left the
            // OP_CODESEPARATOR instruction having no purpose as it was only meant to be used internally, not actually
            // ever put into scripts. Deleting OP_CODESEPARATOR is a step that should never be required but if we don't
            // do it, we could split off the main chain.
            connectedScript = Script.removeAllInstancesOfOp(connectedScript, ScriptOpCodes.OP_CODESEPARATOR)

            // Set the input to the script of its output. Bitcoin Core does this but the step has no obvious purpose as
            // the signature covers the hash of the prevout transaction which obviously includes the output script
            // already. Perhaps it felt safer to him in some way, or is another leftover from how the code was written.
            val input = inputs[inputIndex]
            input.setScriptBytes(connectedScript)

            if (sigHashType and 0x1f == Transaction.SigHash.NONE.value.toByte()) {
                // SIGHASH_NONE means no outputs are signed at all - the signature is effectively for a "blank cheque".
//                transaction.clearOutputs()
                // The signature isn't broken by new versions of the transaction issued by other parties.
                for (i in inputs.indices)
                    if (i != inputIndex)
                        inputs[i].sequenceNumber = 0
            } else if (sigHashType and 0x1f == Transaction.SigHash.SINGLE.value.toByte()) {
                // SIGHASH_SINGLE means only sign the output at the same index as the input (ie, my output).
                if (inputIndex >= transaction.getOutputs().size) {
                    // The input index is beyond the number of outputs, it's a buggy signature made by a broken
                    // Bitcoin implementation. Bitcoin Core also contains a bug in handling this case:
                    // any transaction output that is signed in this case will result in both the signed output
                    // and any future outputs to this public key being steal-able by anyone who has
                    // the resulting signature and the public key (both of which are part of the signed tx input).

                    // Bitcoin Core's bug is that SignatureHash was supposed to return a hash and on this codepath it
                    // actually returns the constant "1" to indicate an error, which is never checked for. Oops.
//                    return Sha256Hash.wrap("0100000000000000000000000000000000000000000000000000000000000000")
                }
                // In SIGHASH_SINGLE the outputs after the matching input index are deleted, and the outputs before
                // that position are "nulled out". Unintuitively, the value in a "null" transaction is set to -1.
                val outputs = ArrayList(transaction.getOutputs().subList(0, inputIndex + 1))
                for (i in 0 until inputIndex)
                    outputs!![i] = TransactionOutput(transaction.params!!, transaction, Coin.NEGATIVE_SATOSHI, byteArrayOf())
                // The signature isn't broken by new versions of the transaction issued by other parties.
                for (i in inputs.indices)
                    if (i != inputIndex)
                        inputs[i].sequenceNumber = 0
                transaction.clearOutputs()
                outputs.forEach { transaction.addOutput(it) }
            }

            if (sigHashType and Transaction.SigHash.ANYONECANPAY.value.toByte() == Transaction.SigHash.ANYONECANPAY.value.toByte()) {
                // SIGHASH_ANYONECANPAY means the signature in the input is not broken by changes/additions/removals
                // of other inputs. For example, this is useful for building assurance contracts.
                transaction.clearInputs()
                transaction.addInput(input)
            }

//            val bos = UnsafeByteArrayOutputStream(if (transaction.length == Message.UNKNOWN_LENGTH) 256 else transaction.length + 4)
            val bos = UnsafeByteArrayOutputStream()
            transaction.bitcoinSerializeToStream(bos)
            // We also have to write a hash type (sigHashType is actually an unsigned char)
            ByteUtils.uint32ToByteStreamLE((0x000000ff and sigHashType.toInt()).toLong(), bos)
            // Note that this is NOT reversed to ensure it will be signed correctly. If it were to be printed out
            // however then we would expect that it is IS reversed.
            val hash = Sha256Hash.twiceOf(bos.toByteArray())
            bos.close()

            return hash
        } catch (e: IOException) {
            throw RuntimeException(e)  // Cannot happen.
        }

    }

    /**
     *
     * Calculates a signature hash, that is, a hash of a simplified form of the transaction. How exactly the transaction
     * is simplified is specified by the type and anyoneCanPay parameters.
     *
     *
     * This is a low level API and when using the regular [Wallet] class you don't have to call this yourself.
     * When working with more complex transaction types and contracts, it can be necessary. When signing a Witness output
     * the scriptCode should be the script encoded into the scriptSig field, for normal transactions, it's the
     * scriptPubKey of the output you're signing for. (See BIP143: https://github.com/bitcoinkt/bips/blob/master/bip-0143.mediawiki)
     *
     * @param inputIndex input the signature is being calculated for. Tx signatures are always relative to an input.
     * @param scriptCode the script that should be in the given input during signing.
     * @param prevValue the value of the coin being spent
     * @param type Should be SigHash.ALL
     * @param anyoneCanPay should be false.
     */
    @Synchronized
    fun hashForSignatureWitness(
            inputIndex: Int,
            scriptCode: Script,
            prevValue: Coin,
            type: Transaction.SigHash,
            anyoneCanPay: Boolean): Sha256Hash {
        val connectedScript = scriptCode.listProgram()
        return hashForSignatureWitness(inputIndex, connectedScript, prevValue, type, anyoneCanPay)
    }

    @Synchronized
    fun hashForSignatureWitness(
            inputIndex: Int,
            connectedScript: ByteArray,
            prevValue: Coin,
            type: Transaction.SigHash,
            anyoneCanPay: Boolean): Sha256Hash {
        var anyoneCanPay = anyoneCanPay
        val sigHashType = TransactionSignature.calcSigHashValue(type, anyoneCanPay, true).toByte()
        val bos = UnsafeByteArrayOutputStream() // if (transaction.length == Message.UNKNOWN_LENGTH) 256 else transaction.length + 4)
        try {
            var hashPrevouts = ByteArray(32)
            var hashSequence = ByteArray(32)
            var hashOutputs = ByteArray(32)
            anyoneCanPay = sigHashType and Transaction.SigHash.ANYONECANPAY.value.toByte() == Transaction.SigHash.ANYONECANPAY.value.toByte() // *_*

            if (!anyoneCanPay) {
                val bosHashPrevouts = UnsafeByteArrayOutputStream(256)
                for (i in transaction.getInputs().indices) {
                    bosHashPrevouts.write(transaction.getInputs()[i].outpoint!!.hash!!.reversedBytes)
                    ByteUtils.uint32ToByteStreamLE(transaction.getInputs()[i].outpoint!!.index, bosHashPrevouts)
                }
                hashPrevouts = Sha256Hash.hashTwice(bosHashPrevouts.toByteArray())
            }

            if (!anyoneCanPay && type != Transaction.SigHash.SINGLE && type != Transaction.SigHash.NONE) {
                val bosSequence = UnsafeByteArrayOutputStream(256)
                for (i in transaction.getInputs().indices) {
                    ByteUtils.uint32ToByteStreamLE(transaction.getInputs()[i].sequenceNumber, bosSequence)
                }
                hashSequence = Sha256Hash.hashTwice(bosSequence.toByteArray())
            }

            if (type != Transaction.SigHash.SINGLE && type != Transaction.SigHash.NONE) {
                val bosHashOutputs = UnsafeByteArrayOutputStream(256)
                for (i in transaction.getOutputs().indices) {
                    ByteUtils.uint64ToByteStreamLE(
                            BigInteger.valueOf(transaction.getOutputs()[i].getValue().value),
                            bosHashOutputs
                    )
                    bosHashOutputs.write(VarInt(transaction.getOutputs()[i].scriptBytes!!.size.toLong()).encode())
                    bosHashOutputs.write(transaction.getOutputs()[i].scriptBytes!!)
                }
                hashOutputs = Sha256Hash.hashTwice(bosHashOutputs.toByteArray())
            } else if (type == Transaction.SigHash.SINGLE && inputIndex < transaction.getOutputs().size) {
                val bosHashOutputs = UnsafeByteArrayOutputStream(256)
                ByteUtils.uint64ToByteStreamLE(
                        BigInteger.valueOf(transaction.getOutputs()[inputIndex].getValue().value),
                        bosHashOutputs
                )
                bosHashOutputs.write(VarInt(transaction.getOutputs()[inputIndex].scriptBytes!!.size.toLong()).encode())
                bosHashOutputs.write(transaction.getOutputs()[inputIndex].scriptBytes!!)
                hashOutputs = Sha256Hash.hashTwice(bosHashOutputs.toByteArray())
            }
            ByteUtils.uint32ToByteStreamLE(transaction.version, bos)
            bos.write(hashPrevouts)
            bos.write(hashSequence)
            bos.write(transaction.getInputs()[inputIndex].outpoint!!.hash!!.reversedBytes)
            ByteUtils.uint32ToByteStreamLE(transaction.getInputs()[inputIndex].outpoint!!.index, bos)
            bos.write(VarInt(connectedScript.size.toLong()).encode())
            bos.write(connectedScript)
            ByteUtils.uint64ToByteStreamLE(BigInteger.valueOf(prevValue.value), bos)
            ByteUtils.uint32ToByteStreamLE(transaction.getInputs()[inputIndex].sequenceNumber, bos)
            bos.write(hashOutputs)
            ByteUtils.uint32ToByteStreamLE(transaction.getLockTime(), bos)
            ByteUtils.uint32ToByteStreamLE((0x000000ff and sigHashType.toInt()).toLong(), bos)
        } catch (e: IOException) {
            throw RuntimeException(e)  // Cannot happen.
        }

        return Sha256Hash.twiceOf(bos.toByteArray())
    }

}