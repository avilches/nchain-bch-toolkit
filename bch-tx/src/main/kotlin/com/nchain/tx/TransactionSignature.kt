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

package com.nchain.tx

import com.nchain.key.ECKey
import com.nchain.shared.VerificationException
import com.nchain.tools.DER

import java.io.IOException
import java.math.BigInteger

/**
 * A TransactionSignature wraps an [ECKey.ECDSASignature] and adds methods for handling
 * the additional SIGHASH mode byte that is used.
 */
class TransactionSignature {

    var signature: ECKey.ECDSASignature
    /**
     * A byte that controls which parts of a transaction are signed. This is exposed because signatures
     * parsed off the wire may have sighash flags that aren't "normal" serializations of the enum values.
     * Because Bitcoin Core works via bit testing, we must not lose the exact value when round-tripping
     * otherwise we'll fail to verify signature hashes.
     */
    val sighashFlags: Int

    /** Constructs a signature with the given components and raw sighash flag bytes (needed for rule compatibility).  */
    @JvmOverloads constructor(r: BigInteger, s: BigInteger, sighashFlags: Int = Transaction.SigHash.ALL.value) {
        this.signature = ECKey.ECDSASignature(r, s)
        this.sighashFlags = sighashFlags
    }

    /** Constructs a transaction signature based on the ECDSA signature.  */
    constructor(signature: ECKey.ECDSASignature, mode: Transaction.SigHash, anyoneCanPay: Boolean) {
        this.signature = ECKey.ECDSASignature(signature.r, signature.s)
        sighashFlags = calcSigHashValue(mode, anyoneCanPay)
    }

    constructor(signature: ECKey.ECDSASignature, mode: Transaction.SigHash, anyoneCanPay: Boolean, useForkId: Boolean) {
        this.signature = ECKey.ECDSASignature(signature.r, signature.s)
        sighashFlags = calcSigHashValue(mode, anyoneCanPay, useForkId)
    }

    fun anyoneCanPay(): Boolean {
        return sighashFlags and Transaction.SigHash.ANYONECANPAY.value != 0
    }

    fun useForkId(): Boolean {
        return sighashFlags and Transaction.SigHash.FORKID.value != 0
    }

    fun sigHashMode(): Transaction.SigHash {
        val mode = sighashFlags and 0x1f
        return if (mode == Transaction.SigHash.NONE.value)
            Transaction.SigHash.NONE
        else if (mode == Transaction.SigHash.SINGLE.value)
            Transaction.SigHash.SINGLE
        else
            Transaction.SigHash.ALL
    }

    /**
     * What we get back from the signer are the two components of a signature, r and s. To get a flat byte stream
     * of the type used by Bitcoin we have to encode them using DER encoding, which is just a way to pack the two
     * components into a structure, and then we append a byte to the end for the sighash flags.
     */
    fun bitcoinSerialize(): ByteArray {
        try {
            val bos = DER.createByteStream(signature)
            bos.write(sighashFlags)
            return bos.toByteArray()
        } catch (e: IOException) {
            throw RuntimeException(e)  // Cannot happen.
        }

    }

    fun toCanonicalised(): ECKey.ECDSASignature {
        return TransactionSignature(signature.toCanonicalised(), sigHashMode(), anyoneCanPay(), useForkId()).signature
    }

    companion object {

        /**
         * Returns a dummy invalid signature whose R/S values are set such that they will take up the same number of
         * encoded bytes as a real signature. This can be useful when you want to fill out a transaction to be of the
         * right size (e.g. for fee calculations) but don't have the requisite signing key yet and will fill out the
         * real signature later.
         */
        @JvmStatic
        fun dummy(): TransactionSignature {
            val `val` = ECKey.HALF_CURVE_ORDER
            return TransactionSignature(`val`, `val`)
        }

        /** Calculates the byte used in the protocol to represent the combination of mode and anyoneCanPay.  */
        @JvmStatic
        fun calcSigHashValue(mode: Transaction.SigHash, anyoneCanPay: Boolean): Int {
            check(Transaction.SigHash.ALL == mode || Transaction.SigHash.NONE == mode || Transaction.SigHash.SINGLE == mode); // enforce compatibility since this code was made before the SigHash enum was updated
            var sighashFlags = mode.value
            if (anyoneCanPay)
                sighashFlags = sighashFlags or Transaction.SigHash.ANYONECANPAY.value
            return sighashFlags
        }

        @JvmStatic
        fun calcSigHashValue(mode: Transaction.SigHash, anyoneCanPay: Boolean, useForkId: Boolean): Int {
            check(Transaction.SigHash.ALL == mode || Transaction.SigHash.NONE == mode || Transaction.SigHash.SINGLE == mode); // enforce compatibility since this code was made before the SigHash enum was updated
            var sighashFlags = mode.value
            if (anyoneCanPay)
                sighashFlags = sighashFlags or Transaction.SigHash.ANYONECANPAY.value
            if (useForkId)
                sighashFlags = sighashFlags or Transaction.SigHash.FORKID.value
            return sighashFlags
        }

        /**
         * Checkes if the Hashtype is properly set in the signature.
         * @param signature Signature
         * @return          True (correct Hashtype)/ False
         */
        @JvmStatic
        fun isValidHashType(signature: ByteArray): Boolean {
            var result = true
            val hashType = signature[signature.size - 1].toInt() and 0xff and (Transaction.SigHash.ANYONECANPAY.value or Transaction.SigHash.FORKID.value).inv() // mask the byte to prevent sign-extension hurting us
            if (hashType < Transaction.SigHash.ALL.value || hashType > Transaction.SigHash.SINGLE.value)
                result = false
            return result
        }

        /**
         * Returns true if the given signature is has canonical encoding, and will thus be accepted as standard by
         * Bitcoin Core. DER and the SIGHASH encoding allow for quite some flexibility in how the same structures
         * are encoded, and this can open up novel attacks in which a man in the middle takes a transaction and then
         * changes its signature such that the transaction hash is different but it's still valid. This can confuse wallets
         * and generally violates people's mental model of how Bitcoin should work, thus, non-canonical signatures are now
         * not relayed by default.
         */
        @JvmStatic
        fun isEncodingCanonical(signature: ByteArray): Boolean {
            // See Bitcoin Core's IsCanonicalSignature, https://bitcointalk.org/index.php?topic=8392.msg127623#msg127623
            // A canonical signature exists of: <30> <total len> <02> <len R> <R> <02> <len S> <S> <hashtype>
            // Where R and S are not negative (their first byte has its highest bit not set), and not
            // excessively padded (do not start with a 0 byte, unless an otherwise negative number follows,
            // in which case a single 0 byte is necessary and even required).

            if (signature.size < 9 || signature.size > 73)
                return false

            //                   "wrong type"                  "wrong length marker"
            if (signature[0].toInt() and 0xff != 0x30 || signature[1].toInt() and 0xff != signature.size - 3)
                return false

            val lenR = signature[3].toInt() and 0xff
            if (5 + lenR >= signature.size || lenR == 0)
                return false
            val lenS = signature[5 + lenR].toInt() and 0xff
            if (lenR + lenS + 7 != signature.size || lenS == 0)
                return false

            //    R value type mismatch          R value negative
            if (signature[4 - 2].toInt() != 0x02 || signature[4].toInt() and 0x80 == 0x80)
                return false
            if (lenR > 1 && signature[4].toInt() == 0x00 && signature[4 + 1].toInt() and 0x80 != 0x80)
                return false // R value excessively padded

            //       S value type mismatch                    S value negative
            if (signature[6 + lenR - 2].toInt() != 0x02 || signature[6 + lenR].toInt() and 0x80 == 0x80)
                return false
            return if (lenS > 1 && signature[6 + lenR].toInt() == 0x00 && signature[6 + lenR + 1].toInt() and 0x80 != 0x80) false else true // S value excessively padded

        }

        @JvmStatic
        fun hasForkId(signature: ByteArray): Boolean {
            val forkId = signature[signature.size - 1].toInt() and 0xff and Transaction.SigHash.FORKID.value // mask the byte to prevent sign-extension hurting us

            return forkId == Transaction.SigHash.FORKID.value
        }

        /**
         * Returns a decoded signature.
         *
         * @param requireCanonicalEncoding if the encoding of the signature must
         * be canonical.
         * @throws RuntimeException if the signature is invalid or unparseable in some way.
         */
        @Deprecated("use {@link #decodeFromBitcoin(byte[], boolean, boolean)} instead.")
        @JvmStatic
        @Throws(VerificationException::class)
        fun decodeFromBitcoin(bytes: ByteArray,
                              requireCanonicalEncoding: Boolean): TransactionSignature {
            return decodeFromBitcoin(bytes, requireCanonicalEncoding, false)
        }

        /**
         * Returns a decoded signature.
         *
         * @param requireCanonicalEncoding if the encoding of the signature must
         * be canonical.
         * @param requireCanonicalSValue if the S-value must be canonical (below half
         * the order of the curve).
         * @throws RuntimeException if the signature is invalid or unparseable in some way.
         */
        @Throws(VerificationException::class)
        @JvmStatic
        fun decodeFromBitcoin(bytes: ByteArray,
                              requireCanonicalEncoding: Boolean,
                              requireCanonicalSValue: Boolean): TransactionSignature {
            // Bitcoin encoding is DER signature + sighash byte.
            if (requireCanonicalEncoding && !isEncodingCanonical(bytes))
                throw VerificationException("Signature encoding is not canonical.")
            val sig: ECKey.ECDSASignature
            try {
                sig = DER.decodeSignature(bytes)
            } catch (e: IllegalArgumentException) {
                throw VerificationException("Could not decode DER: " + e.message)
            }

            if (requireCanonicalSValue && !sig.isCanonical)
                throw VerificationException("S-value is not canonical.")

            // In Bitcoin, any value of the final byte is valid, but not necessarily canonical. See javadocs for
            // isEncodingCanonical to learn more about this. So we must store the exact byte found.
            return TransactionSignature(sig.r, sig.s, bytes[bytes.size - 1].toInt())
        }
    }
}
/** Constructs a signature with the given components and SIGHASH_ALL.  */
