/*
 * Copyright 2013 Jim Burton.
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the MIT license (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://opensource.org/licenses/mit-license.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nchain.keycrypter


import com.lambdaworks.crypto.SCrypt
import com.nchain.tools.loggerFor
import com.nchain.shared.Randomizer
import com.nchain.tools.Stopwatch
import org.spongycastle.crypto.engines.AESFastEngine
import org.spongycastle.crypto.modes.CBCBlockCipher
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.spongycastle.crypto.params.KeyParameter
import org.spongycastle.crypto.params.ParametersWithIV
import java.util.*

/**
 *
 * This class encrypts and decrypts byte arrays and strings using scrypt as the
 * key derivation function and AES for the encryption.
 *
 *
 * You can use this class to:
 *
 *
 * 1) Using a user password, create an AES key that can encrypt and decrypt your private keys.
 * To convert the password to the AES key, scrypt is used. This is an algorithm resistant
 * to brute force attacks. You can use the ScryptParameters to tune how difficult you
 * want this to be generation to be.
 *
 *
 * 2) Using the AES Key generated above, you then can encrypt and decrypt any bytes using
 * the AES symmetric cipher. Eight bytes of salt is used to prevent dictionary attacks.
 */
data class ScryptConfig(
        val salt: ByteArray,
        val n: Int,         // CPU/ memory cost parameter
        val r: Int,         // Block size parameter
        val p: Int          // Parallelisation parameter
)


class KeyCrypterScrypt(val config: ScryptConfig) : KeyCrypter {

    /**
     * Generate AES key.
     *
     * This is a very slow operation compared to encrypt/ decrypt so it is normally worth caching the result.
     *
     * @param password    The password to use in key generation
     * @return            The KeyParameter containing the created AES key
     * @throws            KeyCrypterException
     */
    @Throws(KeyCrypterException::class)
    override fun deriveKey(password: CharSequence): KeyParameter {
        var passwordBytes: ByteArray? = null
        try {
            passwordBytes = convertToByteArray(password)

            val watch = Stopwatch().start()
            val keyBytes = SCrypt.scrypt(passwordBytes, config.salt, config.n, config.r, config.p, KEY_LENGTH)
            watch.stop()
            log.info("Deriving key took ${watch.elapsed}ms for ${config.n} scrypt iterations.")
            return KeyParameter(keyBytes)
        } catch (e: Exception) {
            throw KeyCrypterException("Could not generate key from password and salt.", e)
        } finally {
            // Zero the password bytes.
            if (passwordBytes != null) {
                java.util.Arrays.fill(passwordBytes, 0.toByte())
            }
        }
    }

    /**
     * Password based encryption using AES - CBC 256 bits.
     */
    @Throws(KeyCrypterException::class)
    override fun encrypt(plainBytes: ByteArray, aesKey: KeyParameter): EncryptedData {
        try {
            // Generate iv - each encryption call has a different iv.
            val iv = ByteArray(BLOCK_LENGTH)
            Randomizer.nextBytes(iv)

            val keyWithIv = ParametersWithIV(aesKey, iv)

            // Encrypt using AES.
            val cipher = PaddedBufferedBlockCipher(CBCBlockCipher(AESFastEngine()))
            cipher.init(true, keyWithIv)
            val encryptedBytes = ByteArray(cipher.getOutputSize(plainBytes.size))
            val length1 = cipher.processBytes(plainBytes, 0, plainBytes.size, encryptedBytes, 0)
            val length2 = cipher.doFinal(encryptedBytes, length1)

            return EncryptedData(iv, Arrays.copyOf(encryptedBytes, length1 + length2))
        } catch (e: Exception) {
            throw KeyCrypterException("Could not encrypt bytes.", e)
        }

    }

    /**
     * Decrypt bytes previously encrypted with this class.
     *
     * @param dataToDecrypt    The data to decrypt
     * @param aesKey           The AES key to use for decryption
     * @return                 The decrypted bytes
     * @throws                 KeyCrypterException if bytes could not be decrypted
     */
    @Throws(KeyCrypterException::class)
    override fun decrypt(dataToDecrypt: EncryptedData, aesKey: KeyParameter): ByteArray {

        try {
            val keyWithIv = ParametersWithIV(KeyParameter(aesKey.key), dataToDecrypt.initialisationVector)

            // Decrypt the message.
            val cipher = PaddedBufferedBlockCipher(CBCBlockCipher(AESFastEngine()))
            cipher.init(false, keyWithIv)

            val cipherBytes = dataToDecrypt.encryptedBytes
            val decryptedBytes = ByteArray(cipher.getOutputSize(cipherBytes.size))
            val length1 = cipher.processBytes(cipherBytes, 0, cipherBytes.size, decryptedBytes, 0)
            val length2 = cipher.doFinal(decryptedBytes, length1)

            return Arrays.copyOf(decryptedBytes, length1 + length2)
        } catch (e: Exception) {
            throw KeyCrypterException("Could not decrypt bytes", e)
        }
    }

    override fun toString(): String {
        return "AES-" + KEY_LENGTH * 8 + "-CBC, Scrypt (N: " + config.n + ")"
    }

    companion object {

        private val log = loggerFor(KeyCrypterScrypt::class.java)

        fun create(config: ScryptConfig): KeyCrypterScrypt {
            return KeyCrypterScrypt(config)
        }

        fun create(salt: ByteArray = randomSalt(),
                   n: Int = 16384,         // CPU/ memory cost parameter
                   r: Int = 8,             // Block size parameter
                   p: Int = 1): KeyCrypter {
            return create(ScryptConfig(salt, n, r, p))
        }


//        private val log = LoggerFactory.getLogger(KeyCrypterScrypt::class.java)

        /**
         * Key length in bytes.
         */
        val KEY_LENGTH = 32 // = 256 bits.

        /**
         * The size of an AES block in bytes.
         * This is also the length of the initialisation vector.
         */
        val BLOCK_LENGTH = 16  // = 128 bits.

        /**
         * The length of the salt used.
         */
        val SALT_LENGTH = 8

        /** Returns SALT_LENGTH (8) bytes of random data  */
        fun randomSalt(): ByteArray {
            val salt = ByteArray(SALT_LENGTH)
            Randomizer.nextBytes(salt)
            return salt
        }

        /**
         * Convert a CharSequence (which are UTF16) into a byte array.
         *
         * Note: a String.getBytes() is not used to avoid creating a String of the password in the JVM.
         */
        private fun convertToByteArray(charSequence: CharSequence): ByteArray {
            val byteArray = ByteArray(charSequence.length shl 1)
            for (i in 0 until charSequence.length) {
                val bytePosition = i shl 1
                byteArray[bytePosition] = (charSequence[i].toInt() and 0xFF00 shr 8).toByte()
                byteArray[bytePosition + 1] = (charSequence[i].toInt() and 0x00FF).toByte()
            }
            return byteArray
        }
    }
}
