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

import com.nchain.tools.HEX
import com.nchain.tools.loggerFor
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*

class KeyCrypterScryptTest {

    val keyCrypter = KeyCrypterScrypt.create()

    @Before
    @Throws(Exception::class)
    fun setUp() {
//        BriefLogFormatter.init()
    }

    @Test
    @Throws(KeyCrypterException::class)
    fun testKeyCrypterGood1() {


        // Encrypt.
        val data = keyCrypter.encrypt(TEST_BYTES1, keyCrypter.deriveKey(PASSWORD1))
        assertNotNull(data)

        // Decrypt.
        val reborn = keyCrypter.decrypt(data, keyCrypter.deriveKey(PASSWORD1))
        log.debug("Original: " + HEX.bytesToHex(TEST_BYTES1))
        log.debug("Reborn  : " + HEX.bytesToHex(reborn))
        assertEquals(HEX.bytesToHex(TEST_BYTES1), HEX.bytesToHex(reborn))
    }

    /**
     * Test with random plain text strings and random passwords.
     * UUIDs are used and hence will only cover hex characters (and the separator hyphen).
     * @throws KeyCrypterException
     */
    @Test
    fun testKeyCrypterGood2() {


        // Trying random UUIDs for plainText and passwords.
        val numberOfTests = 16
        for (i in 0 until numberOfTests) {
            // Create a UUID as the plaintext and use another for the password.
            val plainText = UUID.randomUUID().toString()
            val password = UUID.randomUUID().toString()

            val data = keyCrypter.encrypt(plainText.toByteArray(), keyCrypter.deriveKey(password))

            assertNotNull(data)

            val reconstructedPlainBytes = keyCrypter.decrypt(data, keyCrypter.deriveKey(password))
            assertEquals(HEX.bytesToHex(plainText.toByteArray()), HEX.bytesToHex(reconstructedPlainBytes))
        }
    }

    @Test
    @Throws(KeyCrypterException::class)
    fun testKeyCrypterWrongPassword() {


        // create a longer encryption string
        val builder = StringBuilder()
        for (i in 0..99) {
            builder.append(i).append(" The quick brown fox")
        }

        val data = keyCrypter.encrypt(builder.toString().toByteArray(), keyCrypter.deriveKey(PASSWORD2))
        assertNotNull(data)

        try {
            keyCrypter.decrypt(data, keyCrypter.deriveKey(WRONG_PASSWORD))
            // TODO: This test sometimes fails due to relying on padding.
            fail("Decrypt with wrong password did not throw exception")
        } catch (ede: KeyCrypterException) {
            assertTrue(ede.message!!.contains("Could not decrypt"))
        }

    }

    @Test
    @Throws(KeyCrypterException::class)
    fun testEncryptDecryptBytes1() {


        // Encrypt bytes.
        val data = keyCrypter.encrypt(TEST_BYTES1, keyCrypter.deriveKey(PASSWORD1))
        assertNotNull(data)
        log.debug("\nEncrypterDecrypterTest: cipherBytes = \nlength = " + data.encryptedBytes.size + "\n---------------\n" + HEX.bytesToHex(data.encryptedBytes) + "\n---------------\n")

        val rebornPlainBytes = keyCrypter.decrypt(data, keyCrypter.deriveKey(PASSWORD1))

        log.debug("Original: " + HEX.bytesToHex(TEST_BYTES1))
        log.debug("Reborn1 : " + HEX.bytesToHex(rebornPlainBytes))
        assertEquals(HEX.bytesToHex(TEST_BYTES1), HEX.bytesToHex(rebornPlainBytes))
    }

    @Test
    @Throws(KeyCrypterException::class)
    fun testEncryptDecryptBytes2() {


        // Encrypt random bytes of various lengths up to length 50.
        val random = Random()

        for (i in 0..49) {
            val plainBytes = ByteArray(i)
            random.nextBytes(plainBytes)

            val data = keyCrypter.encrypt(plainBytes, keyCrypter.deriveKey(PASSWORD1))
            assertNotNull(data)
            //log.debug("\nEncrypterDecrypterTest: cipherBytes = \nlength = " + cipherBytes.length + "\n---------------\n" + Utils.INSTANCE.getHEX().encode(cipherBytes) + "\n---------------\n");

            val rebornPlainBytes = keyCrypter.decrypt(data, keyCrypter.deriveKey(PASSWORD1))

            log.debug("Original: (" + i + ") " + HEX.bytesToHex(plainBytes))
            log.debug("Reborn1 : (" + i + ") " + HEX.bytesToHex(rebornPlainBytes))
            assertEquals(HEX.bytesToHex(plainBytes), HEX.bytesToHex(rebornPlainBytes))
        }
    }

    companion object {

        private val log = loggerFor(KeyCrypterScryptTest::class.java)

        // Nonsense bytes for encryption test.
        private val TEST_BYTES1 = byteArrayOf(0, -101, 2, 103, -4, 105, 6, 107, 8, -109, 10, 111, -12, 113, 14, -115, 16, 117, -18, 119, 20, 121, 22, 123, -24, 125, 26, 127, -28, 29, -30, 31)

        private val PASSWORD1 = "aTestPassword"
        private val PASSWORD2 = "0123456789"

        private val WRONG_PASSWORD = "thisIsTheWrongPassword"
    }
}
