/*
 * Copyright 2011 Google Inc.
 * Copyright 2014 Andreas Schildbach
 * Copyright 2014-2016 the libsecp256k1 contributors
 * Copyright 2018 the bitcoinj-cash developers
 * Copyright 2018 nChain Ltd.
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
 *
 * This file has been modified for the bitcoinkt project.
 * The original file was from the bitcoinj-cash project (https://github.com/bitcoinj-cash/bitcoinj).
 */

package com.nchain.key

import com.nchain.address.CashAddress
import com.nchain.params.NetworkParameters
import com.nchain.keycrypter.KeyCrypterException
import com.nchain.shared.Randomizer
import com.nchain.shared.Sha256Hash
import com.nchain.tools.ByteUtils
import com.nchain.tools.ByteUtils.readUint32BE
import com.nchain.tools.hexStringToByteArray
import com.nchain.tools.toHex
import org.spongycastle.asn1.*
import org.spongycastle.asn1.x9.X9IntegerConverter
import org.spongycastle.crypto.ec.CustomNamedCurves
import org.spongycastle.crypto.generators.ECKeyPairGenerator
import org.spongycastle.crypto.params.ECDomainParameters
import org.spongycastle.crypto.params.ECKeyGenerationParameters
import org.spongycastle.crypto.params.ECPrivateKeyParameters
import org.spongycastle.crypto.params.ECPublicKeyParameters
import org.spongycastle.math.ec.ECPoint
import org.spongycastle.math.ec.FixedPointCombMultiplier
import org.spongycastle.math.ec.FixedPointUtil
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigInteger
import java.security.SecureRandom
import java.security.SignatureException
import java.util.*

// TODO: Move this class to tracking compression state itself.
// The Bouncy Castle developers are deprecating their own tracking of the compression state.

/**
 *
 * Represents an elliptic curve public and (optionally) private key, usable for digital signatures but not encryption.
 * Creating a new ECKey with the empty constructor will generate a new random keypair. Other static methods can be used
 * when you already have the public or private parts. If you create a key with only the public part, you can check
 * signatures but not create them.
 *
 *
 * ECKey also provides access to Bitcoin Core compatible text message signing, as accessible via the UI or JSON-RPC.
 * This is slightly different to signing raw bytes - if you want to sign your own data and it won't be exposed as
 * text to people, you don't want to use this. If in doubt, ask on the mailing list.
 *
 *
 * The ECDSA algorithm supports *key recovery* in which a signature plus a couple of discriminator bits can
 * be reversed to find the public key used to calculate it. This can be convenient when you have a message and a
 * signature and want to find out who signed it, rather than requiring the user to provide the expected identity.
 *
 *
 * This class supports a variety of serialization forms. The methods that accept/return byte arrays serialize
 * private keys as raw byte arrays and public keys using the SEC standard byte encoding for public keys. Signatures
 * are encoded using ASN.1/DER inside the Bitcoin protocol.
 *
 *
 * A key can be *compressed* or *uncompressed*. This refers to whether the public key is represented
 * when encoded into bytes as an (x, y) coordinate on the elliptic curve, or whether it's represented as just an X
 * co-ordinate and an extra byte that carries a sign bit. With the latter form the Y coordinate can be calculated
 * dynamically, however, **because the binary serialization is different the address of a key changes if its
 * compression status is changed**. If you deviate from the defaults it's important to understand this: money sent
 * to a compressed version of the key will have a different address to the same key in uncompressed form. Whether
 * a public key is compressed or not is recorded in the SEC binary serialisation format, and preserved in a flag in
 * this class so round-tripping preserves state. Unless you're working with old software or doing unusual things, you
 * can usually ignore the compressed/uncompressed distinction.
 */
class ECKey constructor(val priv: BigInteger?, val pub: LazyECPoint) {

    /**
     * Gets the raw public key value. This appears in transaction scriptSigs. Note that this is **not** the same
     * as the pubKeyHash/address.
     */
    val pubKey: ByteArray
        get() = pub.encoded

    /** Gets the public key in the form of an elliptic curve point object from Bouncy Castle.  */
    val pubKeyPoint: ECPoint
        get() = pub.get()

    /** Gets the hash160 form of the public key (as seen in addresses).  */
    var _pubKeyHash: ByteArray? = null
    val pubKeyHash: ByteArray
        get() {
            if (_pubKeyHash == null) {
                _pubKeyHash = ByteUtils.sha256hash160(pub.encoded)
            }
            return _pubKeyHash as ByteArray
        }
    open val isPubKeyOnly: Boolean
        get() = priv == null

    /**
     * Gets the private key in the form of an integer field element. The public key is derived by performing EC
     * point addition this number of times (i.e. point multiplying).
     *
     * @throws java.lang.IllegalStateException if the private key bytes are not available.
     */
    val privKey: BigInteger
        get() = priv ?: throw MissingPrivateKeyException()

    /**
     * Returns whether this key is using the compressed form or not. Compressed pubkeys are only 33 bytes, not 64.
     */
    val isCompressed: Boolean
        get() = pub.isCompressed

    /**
     * Returns a 32 byte array containing the private key.
     * @throws org.bitcoinj.core.ECKey.MissingPrivateKeyException if the private key bytes are missing/encrypted.
     */
    val privKeyBytes: ByteArray
        get() = ByteUtils.bigIntegerToBytes(privKey, 32)

    val privKeyAsHex: String
        get() = privKeyBytes.toHex()

    val pubKeyAsHex: String
        get() = pub.encoded.toHex()

    /**
     * Returns a copy of this key, but with the public point represented in uncompressed form. Normally you would
     * never need this: it's for specialised scenarios or when backwards compatibility in encoded form is necessary.
     */
    fun decompress(): ECKey {
        return if (!pub.isCompressed)
            this
        else
            ECKey(priv, decompressPoint(pub.get()))
    }


    constructor(priv: BigInteger?, pub: ECPoint) : this(priv, LazyECPoint(pub)) {
        if (priv != null) {
            // Try and catch buggy callers or bad key imports, etc. Zero and one are special because these are often
            // used as sentinel values and because scripting languages have a habit of auto-casting true and false to
            // 1 and 0 or vice-versa. Type confusion bugs could therefore result in private keys with these values.
            check(priv != BigInteger.ZERO)
            check(priv != BigInteger.ONE)
        }
    }

    /**
     * Output this ECKey as an ASN.1 encoded private key, as understood by OpenSSL or used by Bitcoin Core
     * in its wallet storage format.
     * @throws org.bitcoinj.core.ECKey.MissingPrivateKeyException if the private key is missing or encrypted.
     */
    fun toASN1(): ByteArray {
        try {
            val privKeyBytes = privKeyBytes
            val baos = ByteArrayOutputStream(400)

            // ASN1_SEQUENCE(EC_PRIVATEKEY) = {
            //   ASN1_SIMPLE(EC_PRIVATEKEY, version, LONG),
            //   ASN1_SIMPLE(EC_PRIVATEKEY, privateKey, ASN1_OCTET_STRING),
            //   ASN1_EXP_OPT(EC_PRIVATEKEY, parameters, ECPKPARAMETERS, 0),
            //   ASN1_EXP_OPT(EC_PRIVATEKEY, publicKey, ASN1_BIT_STRING, 1)
            // } ASN1_SEQUENCE_END(EC_PRIVATEKEY)
            val seq = DERSequenceGenerator(baos)
            seq.addObject(ASN1Integer(1)) // version
            seq.addObject(DEROctetString(privKeyBytes))
            seq.addObject(DERTaggedObject(0, CURVE_PARAMS.toASN1Primitive()))
            seq.addObject(DERTaggedObject(1, DERBitString(pubKey)))
            seq.close()
            return baos.toByteArray()
        } catch (e: IOException) {
            throw RuntimeException(e)  // Cannot happen, writing to memory stream.
        }

    }

    /**
     * Returns the address that corresponds to the public part of this ECKey. Note that an address is derived from
     * the RIPEMD-160 hash of the public key and is not the public key itself (which is too large to be convenient).
     */
    fun toCashAddress(params: NetworkParameters): CashAddress {
        return CashAddress.fromP2PubKey(params, pubKeyHash)

    }

    /**
     * Groups the two components that make up a signature, and provides a way to encode to DER form, which is
     * how ECDSA signatures are represented when embedded in other data structures in the Bitcoin protocol. The raw
     * components can be useful for doing further EC maths on them.
     */
    data class ECDSASignature(val r: BigInteger, val s: BigInteger) {

        /**
         * Returns true if the S component is "low", that means it is below [ECKey.HALF_CURVE_ORDER]. See [BIP62](https://github.com/bitcoinkt/bips/blob/master/bip-0062.mediawiki#Low_S_values_in_signatures).
         */
        val isCanonical: Boolean
            get() = s.compareTo(HALF_CURVE_ORDER) <= 0

        /**
         * Will automatically adjust the S component to be less than or equal to half the curve order, if necessary.
         * This is required because for every signature (r,s) the signature (r, -s (mod N)) is a valid signature of
         * the same message. However, we dislike the ability to modify the bits of a Bitcoin transaction after it's
         * been signed, as that violates various assumed invariants. Thus in future only one of those forms will be
         * considered legal and the other will be banned.
         */
        open fun toCanonicalised(): ECDSASignature {
            return if (!isCanonical) {
                // The order of the curve is the number of valid points that exist on that curve. If S is in the upper
                // half of the number of valid points, then bring it back to the lower half. Otherwise, imagine that
                //    N = 10
                //    s = 8, so (-8 % 10 == 2) thus both (r, 8) and (r, 2) are valid solutions.
                //    10 - 8 == 2, giving us always the latter solution, which is canonical.
                ECDSASignature(r, CURVE.n.subtract(s))
            } else {
                this
            }
        }

        /**
         * DER is an international standard for serializing data structures which is widely used in cryptography.
         * It's somewhat like protocol buffers but less convenient. This method returns a standard DER encoding
         * of the signature, as recognized by OpenSSL and other libraries.
         */
        fun encodeToDER(): ByteArray {
            try {
                return derByteStream().toByteArray()
            } catch (e: IOException) {
                throw RuntimeException(e)  // Cannot happen.
            }

        }

        @Throws(IOException::class)
        fun derByteStream(): ByteArrayOutputStream {
            // Usually 70-72 bytes.
            val bos = ByteArrayOutputStream(72)
            val seq = DERSequenceGenerator(bos)
            seq.addObject(ASN1Integer(r))
            seq.addObject(ASN1Integer(s))
            seq.close()
            return bos
        }

        companion object {

            @JvmStatic fun decodeFromDER(bytes: ByteArray): ECDSASignature {
                var decoder: ASN1InputStream? = null
                try {
                    decoder = ASN1InputStream(bytes)
                    val seq = decoder.readObject() as DLSequence
                            ?: throw VerificationException.SignatureFormatError("Reached past end of ASN.1 stream.")
                    val r: ASN1Integer
                    val s: ASN1Integer
                    try {
                        r = seq.getObjectAt(0) as ASN1Integer
                        s = seq.getObjectAt(1) as ASN1Integer
                    } catch (e: ClassCastException) {
                        throw IllegalArgumentException(e)
                    }

                    // OpenSSL deviates from the DER spec by interpreting these values as unsigned, though they should not be
                    // Thus, we always use the positive versions. See: http://r6.ca/blog/20111119T211504Z.html
                    return ECDSASignature(r.positiveValue, s.positiveValue)
                } catch (e: Exception) {
                    throw VerificationException.SignatureFormatError(e)
                } finally {
                    if (decoder != null)
                        try {
                            decoder.close()
                        } catch (x: IOException) {
                        }

                }
            }
        }
    }

    /**
     * Signs the given hash and returns the R and S components as BigIntegers. In the Bitcoin protocol, they are
     * usually encoded using ASN.1 format, so you want [org.bitcoinj.core.ECKey.ECDSASignature.toASN1]
     * instead. However sometimes the independent components can be useful, for instance, if you're going to do
     * further EC maths on them.
     * @throws KeyCrypterException if this ECKey doesn't have a private part.
     */
    @Throws(KeyCrypterException::class)
    fun sign(input: Sha256Hash): ECDSASignature {
        return ECKeySigner.sign(input, priv!!)
    }

    /**
     * Verifies the given ASN.1 encoded ECDSA signature against a hash using the public key.
     *
     * @param hash      Hash of the data to verify.
     * @param signature ASN.1 encoded signature.
     */
    fun verify(hash: ByteArray, signature: ByteArray): Boolean {
        return ECKeySigner.verify(hash, signature, pubKey)
    }

    /**
     * Verifies the given R/S pair (signature) against a hash using the public key.
     */
    fun verify(sigHash: Sha256Hash, signature: ECDSASignature): Boolean {
        return ECKeySigner.verify(sigHash.bytes, signature, pubKey)
    }

    /**
     * Verifies the given ASN.1 encoded ECDSA signature against a hash using the public key, and throws an exception
     * if the signature doesn't match
     * @throws java.security.SignatureException if the signature does not match.
     */
    @Throws(SignatureException::class)
    fun verifyOrThrow(hash: ByteArray, signature: ByteArray) {
        if (!verify(hash, signature))
            throw SignatureException()
    }

    /**
     * Verifies the given R/S pair (signature) against a hash using the public key, and throws an exception
     * if the signature doesn't match
     * @throws java.security.SignatureException if the signature does not match.
     */
    @Throws(SignatureException::class)
    fun verifyOrThrow(sigHash: Sha256Hash, signature: ECDSASignature) {
        if (!ECKeySigner.verify(sigHash.bytes, signature, pubKey))
            throw SignatureException()
    }

    /**
     * Signs a text message using the standard Bitcoin messaging signing format and returns the signature as a base64
     * encoded string.
     *
     * @throws IllegalStateException if this ECKey does not have the private part.
     * @throws KeyCrypterException if this ECKey is encrypted and no AESKey is provided or it does not decrypt the ECKey.
     */

    @Throws(KeyCrypterException::class)
    fun signMessage(message: String): String {
        return ECKeySigner.signMessage(this, message)
    }

    @Throws(SignatureException::class)
    fun verifyMessage(message: String, signatureBase64: String) {
        ECKeySigner.verifyMessage(this, message, signatureBase64)
    }

    /**
     * Exports the private key in the form used by Bitcoin Core's "dumpprivkey" and "importprivkey" commands. Use
     * the [org.bitcoinj.core.DumpedPrivateKey.toString] method to get the string.
     *
     * @param params The network this key is intended for use on.
     * @return Private key bytes as a [DumpedPrivateKey].
     * @throws IllegalStateException if the private key is not available.
     */
    fun dumpPrivKey(params: NetworkParameters): DumpedPrivateKey {
        return DumpedPrivateKey.createFromPrivKey(params, privKeyBytes!!, isCompressed)
    }

    open class MissingPrivateKeyException : RuntimeException()

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || o !is ECKey) return false
        return Objects.equals(this.priv, o.priv) && Objects.equals(this.pub, o.pub)
    }

    override fun hashCode(): Int {
        // Public keys are random already so we can just use a part of them as the hashcode. Read from the start to
        // avoid picking up the type code (compressed vs uncompressed) which is tacked on the end.
        return readUint32BE(byteArrayOf(pubKey[0], pubKey[1], pubKey[2], pubKey[3]), 0).toInt()
    }


    override fun toString(): String {
        return toString(false, null)
    }

    /**
     * Produce a string rendering of the ECKey INCLUDING the private key.
     * Unless you absolutely need the private key it is better for security reasons to just use [.toString].
     */
    fun toStringWithPrivate(params: NetworkParameters): String {
        return toString(true, params)
    }

    class StringHelper {
        val buffer = StringBuffer()
        fun add(o: Any, v: Any?) {
            if (buffer.length > 0) {
                buffer.append(", ")
            }
            buffer.append("$o=$v")
        }

        override fun toString(): String {
            buffer.append("}")
            return buffer.toString()
        }
    }

    private fun toString(includePrivate: Boolean, params: NetworkParameters?): String {
        val helper = StringHelper() // MoreObjects.toStringHelper(this).omitNullValues()
        helper.add("ECKey{pub HEX", pubKeyAsHex)
        if (includePrivate) {
            try {
                helper.add("priv HEX", privKeyAsHex)
                if (params != null) {
                    helper.add("priv WIF", dumpPrivKey(params))
                }
            } catch (e: IllegalStateException) {
                // TODO: Make hasPrivKey() work for deterministic keys and fix this.
            } catch (e: Exception) {
                val message = e.message
                helper.add("priv EXCEPTION", e.javaClass.getName() + if (message != null) ": " + message else "")
            }

        }
        return helper.toString()
    }

    /*
    open fun formatKeyWithAddress(includePrivateKeys: Boolean, builder: StringBuilder, params: NetworkParameters) {
        val address = toAddress(params)
        builder.append("  addr:")
        builder.append(address.toString())
        builder.append("  hash160:")
        builder.append(getPubKeyHash().toHex())
        if (creationTimeSeconds > 0)
            builder.append("  creationTimeSeconds:").append(creationTimeSeconds)
        builder.append("\n")
        if (includePrivateKeys) {
            builder.append("  ")
            builder.append(toStringWithPrivate(params))
            builder.append("\n")
        }
    }
    */

    companion object {
//        private val log = LoggerFactory.getLogger(ECKey::class.java!!)

        // The parameters of the secp256k1 curve that Bitcoin uses.
        private val CURVE_PARAMS = CustomNamedCurves.getByName("secp256k1")

        /** The parameters of the secp256k1 curve that Bitcoin uses.  */
        @JvmStatic val CURVE: ECDomainParameters

        /**
         * Equal to CURVE.getN().shiftRight(1), used for canonicalising the S value of a signature. If you aren't
         * sure what this is about, you can ignore it.
         */
        @JvmStatic val HALF_CURVE_ORDER: BigInteger

        init {
            // Init proper random number generator, as some old Android installations have bugs that make it unsecure.
//            if (Utils.isAndroidRuntime)
//                LinuxSecureRandom()

            // Tell Bouncy Castle to precompute data that's needed during secp256k1 calculations. Increasing the width
            // number makes calculations faster, but at a cost of extra memory usage and with decreasing returns. 12 was
            // picked after consulting with the BC team.
            FixedPointUtil.precompute(CURVE_PARAMS.g, 12)
            CURVE = ECDomainParameters(CURVE_PARAMS.curve, CURVE_PARAMS.g, CURVE_PARAMS.n,
                    CURVE_PARAMS.h)
            HALF_CURVE_ORDER = CURVE_PARAMS.n.shiftRight(1)
        }


        /**
         * Generates an entirely new keypair with the given [SecureRandom] object. Point compression is used so the
         * resulting public key will be 33 bytes (32 for the co-ordinate and 1 byte to represent the y bit).
         */
        @JvmStatic @JvmOverloads fun create(secureRandom: SecureRandom = Randomizer.random): ECKey {
            val generator = ECKeyPairGenerator()
            val keygenParams = ECKeyGenerationParameters(CURVE, secureRandom)
            generator.init(keygenParams)
            val keypair = generator.generateKeyPair()
            val privParams = keypair.private as ECPrivateKeyParameters
            val pubParams = keypair.public as ECPublicKeyParameters
            val ecKey = ECKey(privParams.d, LazyECPoint(CURVE.curve, pubParams.q.getEncoded(true)))
            return ecKey
        }

        /**
         * Creates an ECKey given either the private key only, the public key only, or both. If only the private key
         * is supplied, the public key will be calculated from it (this is slow). If both are supplied, it's assumed
         * the public key already correctly matches the private key. If only the public key is supplied, this ECKey cannot
         * be used for signing.
         * @param compressed If set to true and pubKey is null, the derived public key will be in compressed form.
         */
        /*
        @Deprecated("")
        fun create(privKey: BigInteger?, pubKey: ByteArray?, compressed: Boolean): ECKey {
            if (privKey == null && pubKey == null)
                throw IllegalArgumentException("ECKey requires at least private or public key")
            if (pubKey == null) {
                // Derive public from private.
                var point = publicPointFromPrivate(privKey!!)
                point = getPointWithCompression(point, compressed)
                return ECKey(privKey, LazyECPoint(point))
            } else {
                // We expect the pubkey to be in regular encoded form, just as a BigInteger. Therefore the first byte is
                // a special marker byte.
                // TODO: This is probably not a useful API and may be confusing.
                return ECKey(privKey, LazyECPoint(CURVE.curve, pubKey))
            }
        }
        */

        /**
         * Creates an ECKey given either the private key only, the public key only, or both. If only the private key
         * is supplied, the public key will be calculated from it (this is slow). If both are supplied, it's assumed
         * the public key already correctly matches the public key. If only the public key is supplied, this ECKey cannot
         * be used for signing.
         */
//        @Deprecated("")
//        private fun create(privKey: BigInteger?, pubKey: ByteArray?): ECKey {
//            return create(privKey, pubKey, false)
//        }

        /**
         * Creates an ECKey given only the private key bytes. This is the same as using the BigInteger constructor, but
         * is more convenient if you are importing a key from elsewhere. The public key will be automatically derived
         * from the private key.
         */
//        @Deprecated("")
//        fun create(privKeyBytes: ByteArray?, pubKey: ByteArray?): ECKey {
//            return create(if (privKeyBytes == null) null else BigInteger(1, privKeyBytes), pubKey)
//        }

        /**
         * Utility for compressing an elliptic curve point. Returns the same point if it's already compressed.
         * See the ECKey class docs for a discussion of point compression.
         */
        @JvmStatic fun compressPoint(point: ECPoint): ECPoint {
            return getPointWithCompression(point, true)
        }

        @JvmStatic fun compressPoint(point: LazyECPoint): LazyECPoint {
            return if (point.isCompressed) point else LazyECPoint(compressPoint(point.get()))
        }

        @JvmStatic fun decompressPoint(point: ECPoint): ECPoint {
            return getPointWithCompression(point, false)
        }

        private fun getPointWithCompression(point: ECPoint, compressed: Boolean): ECPoint {
            var point = point
            if (point.isCompressed == compressed)
                return point
            point = point.normalize()
            val x = point.affineXCoord.toBigInteger()
            val y = point.affineYCoord.toBigInteger()
            return CURVE.curve.createPoint(x, y, compressed)
        }

        /**
         * Construct an ECKey from an ASN.1 encoded private key. These are produced by OpenSSL and stored by Bitcoin
         * Core in its wallet. Note that this is slow because it requires an EC point multiply.
         */
        @JvmStatic fun fromASN1Hex(asn1privkey: String): ECKey {
            return fromASN1(asn1privkey.hexStringToByteArray())
        }

        @JvmStatic fun fromASN1(asn1privkey: ByteArray): ECKey {
            return extractKeyFromASN1(asn1privkey)
        }

        /**
         * Creates an ECKey given the private key only. The public key is calculated from it (this is slow), either
         * compressed or not.
//         */
        @JvmStatic fun fromPrivateDump(dumpedPrivateKey: DumpedPrivateKey): ECKey {
            return DumpedPrivateKey.fromBase58(null, dumpedPrivateKey.toBase58()).key
        }

        @JvmStatic fun fromPrivateDump(params: NetworkParameters? = null, base58Wif:String): ECKey {
            return DumpedPrivateKey.fromBase58(params, base58Wif).key
        }

        @JvmStatic fun fromPrivate(privKey: BigInteger, compressed: Boolean = true): ECKey {
            val point = publicPointFromPrivate(privKey)
            return ECKey(privKey, getPointWithCompression(point, compressed))
        }

        /**
         * Creates an ECKey given the private key only. The public key is calculated from it (this is slow), either
         * compressed or not.
         */
        @JvmStatic fun fromPrivate(privKeyBytes: ByteArray, compressed: Boolean = true): ECKey {
            return fromPrivate(BigInteger(1, privKeyBytes), compressed)
        }

        /**
         * Creates an ECKey that simply trusts the caller to ensure that point is really the result of multiplying the
         * generator point by the private key. This is used to speed things up when you know you have the right values
         * already. The compression state of pub will be preserved.
         */
        @JvmStatic fun fromPrivateAndPrecalculatedPublic(priv: BigInteger, pub: ECPoint): ECKey {
            return ECKey(priv, pub)
        }

        /**
         * Creates an ECKey that simply trusts the caller to ensure that point is really the result of multiplying the
         * generator point by the private key. This is used to speed things up when you know you have the right values
         * already. The compression state of the point will be preserved.
         */
//        fun fromPrivateAndPrecalculatedPublic(priv: ByteArray, pub: ByteArray): ECKey {
//            return ECKey(BigInteger(1, priv), CURVE.curve.decodePoint(pub))
//        }

        /**
         * Creates an ECKey that cannot be used for signing, only verifying signatures, from the given point. The
         * compression state of pub will be preserved.
         */
        @JvmStatic fun fromPublicOnly(pub: ECPoint): ECKey {
            return ECKey(null, pub)
        }

        /**
         * Creates an ECKey that cannot be used for signing, only verifying signatures, from the given encoded point.
         * The compression state of pub will be preserved.
         */
        @JvmStatic fun fromPublicOnly(pub: ByteArray): ECKey {
            return ECKey(null, CURVE.curve.decodePoint(pub))
        }

        /**
         * Returns public key bytes from the given private key. To convert a byte array into a BigInteger, use <tt>
         * new BigInteger(1, bytes);</tt>
         */
//        fun publicKeyFromPrivate(privKey: BigInteger, compressed: Boolean): ByteArray {
//            val point = publicPointFromPrivate(privKey)
//            return point.getEncoded(compressed)
//        }

        /**
         * Returns public key point from the given private key. To convert a byte array into a BigInteger, use <tt>
         * new BigInteger(1, bytes);</tt>
         */
        @JvmStatic fun publicPointFromPrivate(privKey: BigInteger): ECPoint {
            var privKey = privKey
            /*
             * TODO: FixedPointCombMultiplier currently doesn't support scalars longer than the group order,
             * but that could change in future versions.
             */
            if (privKey.bitLength() > CURVE.n.bitLength()) {
                privKey = privKey.mod(CURVE.n)
            }
            return FixedPointCombMultiplier().multiply(CURVE.g, privKey)
        }


        /**
         * Returns true if the given pubkey is canonical, i.e. the correct length taking into account compression.
         */
/*
        fun isPubKeyCanonical(pubkey: ByteArray): Boolean {
            if (pubkey.size < 33)
                return false
            if (pubkey[0].toInt() == 0x04) {
                // Uncompressed pubkey
                if (pubkey.size != 65)
                    return false
            } else if (pubkey[0].toInt() == 0x02 || pubkey[0].toInt() == 0x03) {
                // Compressed pubkey
                if (pubkey.size != 33)
                    return false
            } else
                return false
            return true
        }
*/

        /* To understand this code, see the definition of the ASN.1 format for EC private keys in the OpenSSL source
         code in ec_asn1.c:

         ASN1_SEQUENCE(EC_PRIVATEKEY) = {
           ASN1_SIMPLE(EC_PRIVATEKEY, version, LONG),
           ASN1_SIMPLE(EC_PRIVATEKEY, privateKey, ASN1_OCTET_STRING),
           ASN1_EXP_OPT(EC_PRIVATEKEY, parameters, ECPKPARAMETERS, 0),
           ASN1_EXP_OPT(EC_PRIVATEKEY, publicKey, ASN1_BIT_STRING, 1)
         } ASN1_SEQUENCE_END(EC_PRIVATEKEY)
        */
        private fun extractKeyFromASN1(asn1privkey: ByteArray): ECKey {
            try {
                val decoder = ASN1InputStream(asn1privkey)
                val seq = decoder.readObject() as DLSequence
                check(decoder.readObject() == null, { "Input contains extra bytes" })
                decoder.close()

                check(seq.size() == 4, { "Input does not appear to be an ASN.1 OpenSSL EC private key" })

                check((seq.getObjectAt(0) as ASN1Integer).value == BigInteger.ONE,
                        { "Input is of wrong version" })

                val privbits = (seq.getObjectAt(1) as ASN1OctetString).octets
                val privkey = BigInteger(1, privbits)

                val pubkey = seq.getObjectAt(3) as ASN1TaggedObject
                check(pubkey.tagNo == 1, { "Input has 'publicKey' with bad tag number" })
                val pubbits = (pubkey.`object` as DERBitString).bytes
                check(pubbits.size == 33 || pubbits.size == 65, { "Input has 'publicKey' with invalid length" })
                val encoding = pubbits[0].toInt() and 0xFF
                // Only allow compressed(2,3) and uncompressed(4), not infinity(0) or hybrid(6,7)
                check(encoding >= 2 && encoding <= 4, { "Input has 'publicKey' with invalid encoding" })

                // Now sanity check to ensure the pubkey bytes match the privkey.
                val compressed = pubbits.size == 33
                val key = ECKey.fromPrivate(privkey, compressed)
                if (!Arrays.equals(key.pubKey, pubbits))
                    throw IllegalArgumentException("Public key in ASN.1 structure does not match private key.")
                return key
            } catch (e: IOException) {
                throw RuntimeException(e)  // Cannot happen, reading from memory stream.
            }

        }


        /*
        Decompress a compressed public key (x co-ord and low-bit of y-coord).
        */
        @JvmStatic fun decompressKey(xBN: BigInteger, yBit: Boolean): ECPoint {
            val x9 = X9IntegerConverter()
            val compEnc = x9.integerToBytes(xBN, 1 + x9.getByteLength(CURVE.curve))
            compEnc[0] = (if (yBit) 0x03 else 0x02).toByte()
            return CURVE.curve.decodePoint(compEnc)
        }

    }
}