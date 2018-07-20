/*
 * Copyright 2013 Matija Mazi.
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

package com.nchain.bip32

import org.spongycastle.math.ec.*

import java.math.*
import java.nio.*
import java.security.*
import java.util.*

import com.nchain.key.ECKey
import com.nchain.key.LazyECPoint

/**
 * Implementation of the [BIP 32](https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki)
 * deterministic wallet child key generation algorithm.
 */
object HDKeyDerivation {

    // Some arbitrary random number. Doesn't matter what it is.
    private val RAND_INT: BigInteger

    /**
     * Child derivation may fail (although with extremely low probability); in such case it is re-attempted.
     * This is the maximum number of re-attempts (to avoid an infinite loop in case of bugs etc.).
     */
    val MAX_CHILD_DERIVATION_ATTEMPTS = 100

    init {
        // Init proper random number generator, as some old Android installations have bugs that make it unsecure.
        RAND_INT = BigInteger(256, SecureRandom())
    }

    /**
     * Generates a new deterministic key from the given seed, which can be any arbitrary byte array. However resist
     * the temptation to use a string as the seed - any key derived from a password is likely to be weak and easily
     * broken by attackers (this is not theoretical, people have had money stolen that way). This method checks
     * that the given seed is at least 64 bits long.
     *
     * @throws HDDerivationException if generated master key is invalid (private key 0 or >= n).
     * @throws IllegalArgumentException if the seed is less than 8 bytes and could be brute forced.
     */
    @Throws(HDDerivationException::class)
    fun createMasterPrivateKey(seed: ByteArray): DeterministicKey {
        check(seed.size > 8, {"Seed is too short and could be brute forced"})
        // Calculate I = HMAC-SHA512(key="Bitcoin seed", msg=S)
        val i = HDUtils.hmacSha512(HDUtils.createHmacSha512Digest("Bitcoin seed".toByteArray()), seed)
        // Split I into two 32-byte sequences, Il and Ir.
        // Use Il as master secret key, and Ir as master chain code.
        check(i.size == 64, {"Wrong size: ${i.size}"})
        val il = Arrays.copyOfRange(i, 0, 32)
        val ir = Arrays.copyOfRange(i, 32, 64)
        Arrays.fill(i, 0.toByte())
        val masterPrivKey = createMasterPrivKeyFromBytes(il, ir)
        Arrays.fill(il, 0.toByte())
        Arrays.fill(ir, 0.toByte())
        // Child deterministic keys will chain up to their parents to find the keys.
//        masterPrivKey.updateCreationTimeSeconds(System.currentTimeMillis())
        return masterPrivKey
    }

    /**
     * @throws HDDerivationException if privKeyBytes is invalid (0 or >= n).
     */
    @Throws(HDDerivationException::class)
    fun createMasterPrivKeyFromBytes(privKeyBytes: ByteArray, chainCode: ByteArray): DeterministicKey {
        val priv = BigInteger(1, privKeyBytes)
        assertNonZero(priv, "Generated master key is invalid.")
        assertLessThanN(priv, "Generated master key is invalid.")
        return DeterministicKey(emptyList(), chainCode, priv,
                null)
    }

    fun createMasterPubKeyFromBytes(pubKeyBytes: ByteArray, chainCode: ByteArray): DeterministicKey {
        return DeterministicKey(emptyList(), chainCode, LazyECPoint(ECKey.CURVE.curve, pubKeyBytes), null, null)
    }

    /**
     * Derives a key given the "extended" child number, ie. the 0x80000000 bit of the value that you
     * pass for `childNumber` will determine whether to use hardened derivation or not.
     * Consider whether your code would benefit from the clarity of the equivalent, but explicit, form
     * of this method that takes a `ChildNumber` rather than an `int`, for example:
     * `deriveChildKey(parent, new ChildNumber(childNumber, true))`
     * where the value of the hardened bit of `childNumber` is zero.
     */
    fun deriveChildKey(parent: DeterministicKey, childNumber: Int): DeterministicKey {
        return deriveChildKey(parent, ChildNumber(childNumber))
    }

    /**
     * Derives a key of the "extended" child number, ie. with the 0x80000000 bit specifying whether to use
     * hardened derivation or not. If derivation fails, tries a next child.
     */
    fun deriveThisOrNextChildKey(parent: DeterministicKey, childNumber: Int): DeterministicKey {
        var nAttempts = 0
        var child = ChildNumber(childNumber)
        val isHardened = child.isHardened
        while (nAttempts < MAX_CHILD_DERIVATION_ATTEMPTS) {
            try {
                child = ChildNumber(child.num() + nAttempts, isHardened)
                return deriveChildKey(parent, child)
            } catch (ignore: HDDerivationException) {
            }

            nAttempts++
        }
        throw HDDerivationException("Maximum number of child derivation attempts reached, this is probably an indication of a bug.")

    }

    /**
     * @throws HDDerivationException if private derivation is attempted for a public-only parent key, or
     * if the resulting derived key is invalid (eg. private key == 0).
     */
    @Throws(HDDerivationException::class)
    fun deriveChildKey(parent: DeterministicKey, childNumber: ChildNumber): DeterministicKey {
        if (!parent.hasPrivKey()) {
            val rawKey = deriveChildKeyBytesFromPublic(parent, childNumber, PublicDeriveMode.NORMAL)
            return DeterministicKey(
                    HDUtils.append(parent.path, childNumber),
                    rawKey.chainCode,
                    LazyECPoint(ECKey.CURVE.curve, rawKey.keyBytes), null,
                    parent)
        } else {
            val rawKey = deriveChildKeyBytesFromPrivate(parent, childNumber)
            return DeterministicKey(
                    HDUtils.append(parent.path, childNumber),
                    rawKey.chainCode,
                    BigInteger(1, rawKey.keyBytes),
                    parent)
        }
    }

    @Throws(HDDerivationException::class)
    fun deriveChildKeyBytesFromPrivate(parent: DeterministicKey,
                                       childNumber: ChildNumber): RawKeyBytes {
        check(parent.hasPrivKey(), {"Parent key must have private key bytes for this method."})
        val parentPublicKey = parent.key!!.pubKeyPoint.getEncoded(true)
        check(parentPublicKey.size == 33, {"Parent pubkey must be 33 bytes, but is " + parentPublicKey.size})
        val data = ByteBuffer.allocate(37)
        if (childNumber.isHardened) {
            data.put(parent.privKeyBytes33)
        } else {
            data.put(parentPublicKey)
        }
        data.putInt(childNumber.i())
        val i = HDUtils.hmacSha512(parent.chainCode, data.array())
        check(i.size == 64)
        val il = Arrays.copyOfRange(i, 0, 32)
        val chainCode = Arrays.copyOfRange(i, 32, 64)
        val ilInt = BigInteger(1, il)
        assertLessThanN(ilInt, "Illegal derived key: I_L >= n")
        val priv = parent.privKey
        val ki = priv!!.add(ilInt).mod(ECKey.CURVE.n)
        assertNonZero(ki, "Illegal derived key: derived private key equals 0.")
        return RawKeyBytes(ki.toByteArray(), chainCode)
    }

    enum class PublicDeriveMode {
        NORMAL,
        WITH_INVERSION
    }

    @Throws(HDDerivationException::class)
    fun deriveChildKeyBytesFromPublic(parent: DeterministicKey, childNumber: ChildNumber, mode: PublicDeriveMode): RawKeyBytes {
        check(!childNumber.isHardened, {"Can't use private derivation with public keys only."})
        val parentPublicKey = parent.key!!.pubKeyPoint.getEncoded(true)
        check(parentPublicKey.size == 33, {"Parent pubkey must be 33 bytes, but is " + parentPublicKey.size})
        val data = ByteBuffer.allocate(37)
        data.put(parentPublicKey)
        data.putInt(childNumber.i())
        val i = HDUtils.hmacSha512(parent.chainCode, data.array())
        check(i.size == 64)
        val il = Arrays.copyOfRange(i, 0, 32)
        val chainCode = Arrays.copyOfRange(i, 32, 64)
        val ilInt = BigInteger(1, il)
        assertLessThanN(ilInt, "Illegal derived key: I_L >= n")

        val N = ECKey.CURVE.n
        var Ki: ECPoint
        when (mode) {
            HDKeyDerivation.PublicDeriveMode.NORMAL -> Ki = ECKey.publicPointFromPrivate(ilInt).add(parent.key!!.pubKeyPoint)
            HDKeyDerivation.PublicDeriveMode.WITH_INVERSION -> {
                // This trick comes from Gregory Maxwell. Check the homomorphic properties of our curve hold. The
                // below calculations should be redundant and give the same result as NORMAL but if the precalculated
                // tables have taken a bit flip will yield a different answer. This mode is used when vending a key
                // to perform a last-ditch sanity check trying to catch bad RAM.
                Ki = ECKey.publicPointFromPrivate(ilInt.add(RAND_INT).mod(N))
                val additiveInverse = RAND_INT.negate().mod(N)
                Ki = Ki.add(ECKey.publicPointFromPrivate(additiveInverse))
                Ki = Ki.add(parent.key!!.pubKeyPoint)
            }
        }

        assertNonInfinity(Ki, "Illegal derived key: derived public key equals infinity.")
        return RawKeyBytes(Ki.getEncoded(true), chainCode)
    }

    private fun assertNonZero(integer: BigInteger, errorMessage: String) {
        if (integer == BigInteger.ZERO)
            throw HDDerivationException(errorMessage)
    }

    private fun assertNonInfinity(point: ECPoint, errorMessage: String) {
        if (point.equals(ECKey.CURVE.curve.infinity))
            throw HDDerivationException(errorMessage)
    }

    private fun assertLessThanN(integer: BigInteger, errorMessage: String) {
        if (integer.compareTo(ECKey.CURVE.n) > 0)
            throw HDDerivationException(errorMessage)
    }

    class RawKeyBytes(val keyBytes: ByteArray, val chainCode: ByteArray)
}
