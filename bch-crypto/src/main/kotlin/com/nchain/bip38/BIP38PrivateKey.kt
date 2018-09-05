/*
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

package com.nchain.bip38

import com.lambdaworks.crypto.SCrypt
import com.nchain.address.AddressFormatException
import com.nchain.address.VersionedChecksummedBytes
import com.nchain.key.ECKey
import com.nchain.params.NetworkParameters
import com.nchain.shared.Sha256Hash
import com.nchain.tools.ByteUtils
import com.nchain.tools.DRMWorkaround

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.math.BigInteger
import java.security.GeneralSecurityException
import java.text.Normalizer
import java.util.Arrays

import kotlin.experimental.and
import kotlin.experimental.xor

/**
 * Implementation of [BIP 38](https://github.com/bitcoin/bips/blob/master/bip-0038.mediawiki)
 * passphrase-protected private keys. Currently, only decryption is supported.
 */
class BIP38PrivateKey
@Deprecated("Use {@link #fromBase58(NetworkParameters, String)} ")
@Throws(AddressFormatException::class)
constructor(params: NetworkParameters, encoded: String) : VersionedChecksummedBytes(encoded) {

    @Transient var params: NetworkParameters
    val ecMultiply: Boolean
    val compressed: Boolean
    val hasLotAndSequence: Boolean
    val addressHash: ByteArray
    val content: ByteArray

    class BadPassphraseException : Exception()

    init {
        this.params = params
        if (version != 0x01)
            throw AddressFormatException("Mismatched version number: " + version)
        if (bytes.size != 38)
            throw AddressFormatException("Wrong number of bytes, excluding version byte: " + bytes.size)
        hasLotAndSequence = (bytes[1] and 0x04).toInt() != 0 // bit 2
        compressed = (bytes[1] and 0x20).toInt() != 0// bit 5
        if ((bytes[1] and 0x01).toInt() != 0) // bit 0
            throw AddressFormatException("Bit 0x01 reserved for future use.")
        if ((bytes[1] and 0x02).toInt() != 0) // bit 1
            throw AddressFormatException("Bit 0x02 reserved for future use.")
        if ((bytes[1] and 0x08).toInt() != 0) // bit 3
            throw AddressFormatException("Bit 0x08 reserved for future use.")
        if ((bytes[1] and 0x10).toInt() != 0) // bit 4
            throw AddressFormatException("Bit 0x10 reserved for future use.")
        if (bytes[0] == 0x42.toByte()) {
            // Non-EC-multiplied key
            if ((bytes[1].toInt() and 0xc0) != 0xc0) // bits 6+7
                throw AddressFormatException("Bits 0x40 and 0x80 must be set for non-EC-multiplied keys.")
            ecMultiply = false
            if (hasLotAndSequence)
                throw AddressFormatException("Non-EC-multiplied keys cannot have lot/sequence.")
        } else if (bytes[0] == 0x43.toByte()) {
            // EC-multiplied key
            if (bytes[1].toInt() and 0xc0 != 0)
            // bits 6+7
                throw AddressFormatException("Bits 0x40 and 0x80 must be cleared for EC-multiplied keys.")
            ecMultiply = true
        } else {
            throw AddressFormatException("Second byte must by 0x42 or 0x43.")
        }
        addressHash = Arrays.copyOfRange(bytes, 2, 6)
        content = Arrays.copyOfRange(bytes, 6, 38)
    }

    @Throws(BadPassphraseException::class)
    fun decrypt(passphrase: String): ECKey {
        val normalizedPassphrase = Normalizer.normalize(passphrase, Normalizer.Form.NFC)
        val key = if (ecMultiply) decryptEC(normalizedPassphrase) else decryptNoEC(normalizedPassphrase)
        val hash = Sha256Hash.twiceOf(key.toCashAddress(params).toBase58().toByteArray(Charsets.US_ASCII))
        val actualAddressHash = Arrays.copyOfRange(hash.bytes, 0, 4)
        if (!Arrays.equals(actualAddressHash, addressHash))
            throw BadPassphraseException()
        return key
    }

    private fun decryptNoEC(normalizedPassphrase: String): ECKey {
        try {
            val derived = SCrypt.scrypt(normalizedPassphrase.toByteArray(Charsets.UTF_8), addressHash, 16384, 8, 8, 64)
            val key = Arrays.copyOfRange(derived, 32, 64)
            val keyspec = SecretKeySpec(key, "AES")

            DRMWorkaround.maybeDisableExportControls()
            val cipher = Cipher.getInstance("AES/ECB/NoPadding")

            cipher.init(Cipher.DECRYPT_MODE, keyspec)
            val decrypted = cipher.doFinal(content, 0, 32)
            for (i in 0..31)
                decrypted[i] = decrypted[i] xor derived[i]
            return ECKey.fromPrivate(decrypted, compressed)
        } catch (x: GeneralSecurityException) {
            throw RuntimeException(x)
        }

    }

    private fun decryptEC(normalizedPassphrase: String): ECKey {
        try {
            val ownerEntropy = Arrays.copyOfRange(content, 0, 8)
            val ownerSalt = if (hasLotAndSequence) Arrays.copyOfRange(ownerEntropy, 0, 4) else ownerEntropy

            var passFactorBytes = SCrypt.scrypt(normalizedPassphrase.toByteArray(Charsets.UTF_8), ownerSalt, 16384, 8, 8, 32)
            if (hasLotAndSequence) {
                val hashBytes = ByteUtils.concat(passFactorBytes, ownerEntropy)
                check(hashBytes.size == 40)
                passFactorBytes = Sha256Hash.hashTwice(hashBytes)
            }
            val passFactor = BigInteger(1, passFactorBytes)
            val k = ECKey.fromPrivate(passFactor, true)

            val salt = ByteUtils.concat(addressHash, ownerEntropy)
            check(salt.size == 12)
            val derived = SCrypt.scrypt(k.pubKey, salt, 1024, 1, 1, 64)
            val aeskey = Arrays.copyOfRange(derived, 32, 64)

            val keyspec = SecretKeySpec(aeskey, "AES")
            val cipher = Cipher.getInstance("AES/ECB/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, keyspec)

            val encrypted2 = Arrays.copyOfRange(content, 16, 32)
            val decrypted2 = cipher.doFinal(encrypted2)
            check(decrypted2.size == 16)
            for (i in 0..15)
                decrypted2[i] = decrypted2[i] xor derived[i + 16]

            val encrypted1 = ByteUtils.concat(Arrays.copyOfRange(content, 8, 16), Arrays.copyOfRange(decrypted2, 0, 8))
            val decrypted1 = cipher.doFinal(encrypted1)
            check(decrypted1.size == 16)
            for (i in 0..15)
                decrypted1[i] = decrypted1[i] xor derived[i]

            val seed = ByteUtils.concat(decrypted1, Arrays.copyOfRange(decrypted2, 8, 16))
            check(seed.size == 24)
            val seedFactor = BigInteger(1, Sha256Hash.hashTwice(seed))
            check(passFactor.signum() >= 0)
            check(seedFactor.signum() >= 0)
            val priv = passFactor.multiply(seedFactor).mod(ECKey.CURVE.n)

            return ECKey.fromPrivate(priv, compressed)
        } catch (x: GeneralSecurityException) {
            throw RuntimeException(x)
        }

    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o != null && o is BIP38PrivateKey) {
            return super.equals(o) && params.equals(o.params)
        }
        return false
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(arrayOf(super.hashCode(), params))
    }

    override fun clone(): VersionedChecksummedBytes {
        return fromBase58(params, toBase58())
    }

    companion object {

        /**
         * Construct a password-protected private key from its Base58 representation.
         * @param params
         * *            The network parameters of the chain that the key is for.
         * *
         * @param base58
         * *            The textual form of the password-protected private key.
         * *
         * @throws AddressFormatException
         * *             if the given base58 doesn't parse or the checksum is invalid
         */
        @Throws(AddressFormatException::class)
        fun fromBase58(params: NetworkParameters, base58: String): BIP38PrivateKey {
            return BIP38PrivateKey(params, base58)
        }
    }
}
