package com.nchain.key

import com.nchain.keycrypter.KeyCrypterException
import com.nchain.tools.ByteUtils
import com.nchain.shared.Sha256Hash
import com.nchain.shared.VarInt
import com.nchain.tools.DER
import com.nchain.tools.loggerFor
import org.spongycastle.crypto.digests.SHA256Digest
import org.spongycastle.crypto.params.ECPrivateKeyParameters
import org.spongycastle.crypto.params.ECPublicKeyParameters
import org.spongycastle.crypto.signers.ECDSASigner
import org.spongycastle.crypto.signers.HMacDSAKCalculator
import org.spongycastle.math.ec.ECAlgorithms
import org.spongycastle.math.ec.custom.sec.SecP256K1Curve
import org.spongycastle.util.encoders.Base64
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigInteger
import java.nio.charset.Charset
import java.security.SignatureException
import java.util.*

/*
 * @author Alberto Vilches
 * @date 18/07/2018
 */

object ECKeySigner {
        private val log = loggerFor(ECKeySigner::class.java)


        /** The string that prefixes all text messages signed using Bitcoin keys.  */
        const val BITCOIN_SIGNED_MESSAGE_HEADER = "Bitcoin Signed Message:\n"
        @JvmStatic val BITCOIN_SIGNED_MESSAGE_HEADER_BYTES = BITCOIN_SIGNED_MESSAGE_HEADER.toByteArray(Charsets.UTF_8)


        @Throws(KeyCrypterException::class)
        @JvmStatic fun sign(input: Sha256Hash, priv: BigInteger): ECKey.ECDSASignature {
            val signer = ECDSASigner(HMacDSAKCalculator(SHA256Digest()))
            val privKey = ECPrivateKeyParameters(priv, ECKey.CURVE)
            signer.init(true, privKey)
            val components = signer.generateSignature(input.bytes)
            return ECKey.ECDSASignature(components[0], components[1]).toCanonicalised()
        }

        /**
         *
         * Verifies the given ECDSA signature against the message bytes using the public key bytes.
         *
         *
         * When using native ECDSA verification, data must be 32 bytes, and no element may be
         * larger than 520 bytes.
         *
         * @param data      Hash of the data to verify.
         * @param signature ASN.1 encoded signature.
         * @param pub       The public key bytes to use.
         */
        @JvmStatic fun verify(data: ByteArray, signature: ECKey.ECDSASignature, pub: ByteArray): Boolean {
            val signer = ECDSASigner()
            try {
                val params = ECPublicKeyParameters(ECKey.CURVE.curve.decodePoint(pub), ECKey.CURVE)
                signer.init(false, params)
                return signer.verifySignature(data, signature.r, signature.s)
            } catch (e: NullPointerException) {
                // Bouncy Castle contains a bug that can cause NPEs given specially crafted signatures. Those signatures
                // are inherently invalid/attack sigs so we just fail them here rather than crash the thread.
                log.error("Caught NPE inside bouncy castle", e)
                return false
            } catch (e: IllegalArgumentException) {
                throw VerificationException.SignatureFormatError(e)
            } catch (e: ArrayIndexOutOfBoundsException) {
                throw VerificationException.SignatureFormatError(e)
            }
        }

        /**
         * Verifies the given ASN.1 encoded ECDSA signature against a hash using the public key.
         *
         * @param data      Hash of the data to verify.
         * @param signature ASN.1 encoded signature.
         * @param pub       The public key bytes to use.
         */
        @JvmStatic fun verify(data: ByteArray, signature: ByteArray, pub: ByteArray): Boolean {
            return verify(data, DER.decodeSignature(signature), pub)
        }

        /**
         *
         * Given a textual message, returns a byte buffer formatted as follows:
         *
         * <tt>
         *
         *[24] "Bitcoin Signed Message:\n" [message.length as a varint] message</tt>
         */
        @JvmStatic fun formatMessageForSigning(message: String): ByteArray {

            try {
                val bos = ByteArrayOutputStream()
                bos.write(BITCOIN_SIGNED_MESSAGE_HEADER_BYTES.size)
                bos.write(BITCOIN_SIGNED_MESSAGE_HEADER_BYTES)
                val messageBytes = message.toByteArray(Charsets.UTF_8)
                val size = VarInt(messageBytes.size.toLong())
                bos.write(size.encode())
                bos.write(messageBytes)
                return bos.toByteArray()
            } catch (e: IOException) {
                throw RuntimeException(e)  // Cannot happen.
            }

        }

        /**
         *
         * Given the components of a signature and a selector value, recover and return the public key
         * that generated the signature according to the algorithm in SEC1v2 section 4.1.6.
         *
         *
         * The recId is an index from 0 to 3 which indicates which of the 4 possible keys is the correct one. Because
         * the key recovery operation yields multiple potential keys, the correct key must either be stored alongside the
         * signature, or you must be willing to try each recId in turn until you find one that outputs the key you are
         * expecting.
         *
         *
         * If this method returns null it means recovery was not possible and recId should be iterated.
         *
         *
         * Given the above two points, a correct usage of this method is inside a for loop from 0 to 3, and if the
         * output is null OR a key that is not the one you expect, you try again with the next recId.
         *
         * @param recId Which possible key to recover.
         * @param sig the R and S components of the signature, wrapped.
         * @param message Hash of the data that was signed.
         * @param compressed Whether or not the original pubkey was compressed.
         * @return An ECKey containing only the public part, or null if recovery wasn't possible.
         */
        @JvmStatic fun recoverFromSignature(recId: Int, sig: ECKey.ECDSASignature, message: Sha256Hash, compressed: Boolean): ECKey? {
            check(recId >= 0) {"recId must be positive"}
            check(sig.r.signum() >= 0) {"r must be positive"}
            check(sig.s.signum() >= 0) {"s must be positive"}
            // 1.0 For j from 0 to h   (h == recId here and the loop is outside this function)
            //   1.1 Let x = r + jn
            val n = ECKey.CURVE.n  // Curve order.
            val i = BigInteger.valueOf(recId.toLong() / 2)
            val x = sig.r.add(i.multiply(n))
            //   1.2. Convert the integer x to an octet string X of length mlen using the conversion routine
            //        specified in Section 2.3.7, where mlen = ⌈(log2 p)/8⌉ or mlen = ⌈m/8⌉.
            //   1.3. Convert the octet string (16 set binary digits)||X to an elliptic curve point R using the
            //        conversion routine specified in Section 2.3.4. If this conversion routine outputs “invalid”, then
            //        do another iteration of Step 1.
            //
            // More concisely, what these points mean is to use X as a compressed public key.
            val prime = SecP256K1Curve.q
            if (x.compareTo(prime) >= 0) {
                // Cannot have point co-ordinates larger than this as everything takes place modulo Q.
                return null
            }
            // Compressed keys require you to know an extra bit of data about the y-coord as there are two possibilities.
            // So it's encoded in the recId.
            val R = ECKey.decompressKey(x, recId and 1 == 1)
            //   1.4. If nR != point at infinity, then do another iteration of Step 1 (callers responsibility).
            if (!R.multiply(n).isInfinity)
                return null
            //   1.5. Compute e from M using Steps 2 and 3 of ECDSA signature verification.
            val e = message.toBigInteger()
            //   1.6. For k from 1 to 2 do the following.   (loop is outside this function via iterating recId)
            //   1.6.1. Compute a candidate public key as:
            //               Q = mi(r) * (sR - eG)
            //
            // Where mi(x) is the modular multiplicative inverse. We transform this into the following:
            //               Q = (mi(r) * s ** R) + (mi(r) * -e ** G)
            // Where -e is the modular additive inverse of e, that is z such that z + e = 0 (mod n). In the above equation
            // ** is point multiplication and + is point addition (the EC group operator).
            //
            // We can find the additive inverse by subtracting e from zero then taking the mod. For example the additive
            // inverse of 3 modulo 11 is 8 because 3 + 8 mod 11 = 0, and -3 mod 11 = 8.
            val eInv = BigInteger.ZERO.subtract(e).mod(n)
            val rInv = sig.r.modInverse(n)
            val srInv = rInv.multiply(sig.s).mod(n)
            val eInvrInv = rInv.multiply(eInv).mod(n)
            val q = ECAlgorithms.sumOfTwoMultiplies(ECKey.CURVE.g, eInvrInv, R, srInv)
            return ECKey.fromPublicOnly(q.getEncoded(compressed))
        }

        /**
         * Given an arbitrary piece of text and a Bitcoin-format message signature encoded in base64, returns an ECKey
         * containing the public key that was used to sign it. This can then be compared to the expected public key to
         * determine if the signature was correct. These sorts of signatures are compatible with the Bitcoin-Qt/bitcoind
         * format generated by signmessage/verifymessage RPCs and GUI menu options. They are intended for humans to verify
         * their communications with each other, hence the base64 format and the fact that the input is text.
         *
         * @param message Some piece of human readable text.
         * @param signatureBase64 The Bitcoin-format message signature in base64
         * @throws SignatureException If the public key could not be recovered or if there was a signature format error.
         */

        @Throws(SignatureException::class)
        @JvmStatic fun signedMessageToKey(message: String, signatureBase64: String): ECKey {
            val signatureEncoded: ByteArray
            try {
                signatureEncoded = Base64.decode(signatureBase64)
            } catch (e: RuntimeException) {
                // This is what you get back from Bouncy Castle if base64 doesn't decode :(
                throw SignatureException("Could not decode base64", e)
            }

            // Parse the signature bytes into r/s and the selector value.
            if (signatureEncoded.size < 65)
                throw SignatureException("Signature truncated, expected 65 bytes and got " + signatureEncoded.size)
            var header = signatureEncoded[0].toInt() and 0xFF
            // The header byte: 0x1B = first key with even y, 0x1C = first key with odd y,
            //                  0x1D = second key with even y, 0x1E = second key with odd y
            if (header < 27 || header > 34)
                throw SignatureException("Header byte out of range: " + header)
            val r = BigInteger(1, Arrays.copyOfRange(signatureEncoded, 1, 33))
            val s = BigInteger(1, Arrays.copyOfRange(signatureEncoded, 33, 65))
            val sig = ECKey.ECDSASignature(r, s)
            val messageBytes = formatMessageForSigning(message)
            // Note that the C++ code doesn't actually seem to specify any character encoding. Presumably it's whatever
            // JSON-SPIRIT hands back. Assume UTF-8 for now.
            val messageHash = Sha256Hash.twiceOf(messageBytes)
            var compressed = false
            if (header >= 31) {
                compressed = true
                header = (header - 4)
            }
            val recId = header - 27
            return recoverFromSignature(recId, sig, messageHash, compressed) ?: throw SignatureException("Could not recover public key from signature")
        }

        /**
         * Signs a text message using the standard Bitcoin messaging signing format and returns the signature as a base64
         * encoded string.
         *
         * @throws IllegalStateException if this ECKey does not have the private part.
         * @throws KeyCrypterException if this ECKey is encrypted and no AESKey is provided or it does not decrypt the ECKey.
         */

        @Throws(KeyCrypterException::class)
        @JvmStatic fun signMessage(ecKey: ECKey, message: String): String {
            val data = formatMessageForSigning(message)
            val hash = Sha256Hash.twiceOf(data)
            val sig = sign(hash, ecKey.priv!!)
            // Now we have to work backwards to figure out the recId needed to recover the signature.
            var recId = -1
            for (i in 0..3) {
                val k = recoverFromSignature(i, sig, hash, ecKey.isCompressed)
                if (k != null && k.pub == ecKey.pub) {
                    recId = i
                    break
                }
            }
            if (recId == -1)
                throw RuntimeException("Could not construct a recoverable key. This should never happen.")
            val headerByte = recId + 27 + if (ecKey.isCompressed) 4 else 0
            val sigData = ByteArray(65)  // 1 header + 32 bytes for R + 32 bytes for S
            sigData[0] = headerByte.toByte()
            System.arraycopy(ByteUtils.bigIntegerToBytes(sig.r, 32), 0, sigData, 1, 32)
            System.arraycopy(ByteUtils.bigIntegerToBytes(sig.s, 32), 0, sigData, 33, 32)
            return String(Base64.encode(sigData), Charset.forName("UTF-8"))
        }


         /* Convenience wrapper around [ECKey.signedMessageToKey]. If the key derived from the
         * signature is not the same as this one, throws a SignatureException.
         */

        @Throws(SignatureException::class)
        @JvmStatic fun verifyMessage(ecKey: ECKey, message: String, signatureBase64: String) {
            val key = signedMessageToKey(message, signatureBase64)
            if (key.pub != ecKey.pub)
                throw SignatureException("Signature did not match for message")
        }

}