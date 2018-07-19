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

package com.nchain.key

import com.nchain.address.CashAddress
import com.nchain.params.MainNetParams
import com.nchain.params.TestNet3Params
import com.nchain.params.UnitTestParams
import com.nchain.keycrypter.ECKeyCrypted
import com.nchain.keycrypter.KeyCrypter
import com.nchain.keycrypter.KeyCrypterException
import com.nchain.keycrypter.KeyCrypterScrypt
import org.junit.Before
import org.junit.Test

import java.math.BigInteger

import com.nchain.tools.ByteUtils.reverseBytes
import com.nchain.tools.HEX
import com.nchain.shared.Sha256Hash
import com.nchain.tools.loggerFor
import com.nchain.tools.toHex
import org.junit.Assert.*
import java.security.SignatureException
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

class ECKeyTest {

    val keyCrypter: KeyCrypter = KeyCrypterScrypt.create()
    val PASSWORD1 = "my hovercraft has eels"
    val WRONG_PASSWORD = "it is a snowy day today"

    @Before
    @Throws(Exception::class)
    fun setUp() {
//        BriefLogFormatter.init()
    }


    @Test
    @Throws(Exception::class)
    fun sValue() {
        // Check that we never generate an S value that is larger than half the curve order. This avoids a malleability
        // issue that can allow someone to change a transaction [hash] without invalidating the signature.
        val ITERATIONS = 10
        val executor = Executors.newFixedThreadPool(ITERATIONS)
        val sigFutures = arrayListOf<Future<ECKey.ECDSASignature>>()
        val key = ECKey.create()
        for (i in 0 until ITERATIONS) {
            val hash = Sha256Hash.of(byteArrayOf(i.toByte()))
            sigFutures.add(executor.submit(Callable { key.sign(hash) }))
        }
        val sigs = sigFutures.map { it.get() }
        for (signature in sigs) {
            assertTrue(signature.isCanonical)
        }
        val first = sigs[0]
        val duplicate = ECKey.ECDSASignature(first.r, first.s)
        assertEquals(first, duplicate)
        assertEquals(first.hashCode().toLong(), duplicate.hashCode().toLong())

        val highS = ECKey.ECDSASignature(first.r, ECKey.CURVE.n.subtract(first.s))
        assertFalse(highS.isCanonical)
    }


    @Test
    @Throws(Exception::class)
    fun testSignatures() {
        // Test that we can construct an ECKey from a private key (deriving the public from the private), then signing
        // a message with it.
        val privkey = BigInteger(1, HEX.hexToBytes("180cb41c7c600be951b5d3d0a7334acc7506173875834f7a6c4c786a28fcbb19"))
        val key = ECKey.fromPrivate(privkey)
        val output = key.sign(Sha256Hash.ZERO_HASH).encodeToDER()
        assertTrue(key.verify(Sha256Hash.ZERO_HASH.bytes, output))

        // Test interop with a signature from elsewhere.
        val sig = HEX.hexToBytes(
                "3046022100dffbc26774fc841bbe1c1362fd643609c6e42dcb274763476d87af2c0597e89e022100c59e3c13b96b316cae9fa0ab0260612c7a133a6fe2b3445b6bf80b3123bf274d")
        assertTrue(key.verify(Sha256Hash.ZERO_HASH.bytes, sig))
    }

    @Test
    @Throws(Exception::class)
    fun testASN1Roundtrip() {
        val decodedKey = ECKey.fromASN1Hex("3082011302010104205c0b98e524ad188ddef35dc6abba13c34a351a05409e5d285403718b93336a4aa081a53081a2020101302c06072a8648ce3d0101022100fffffffffffffffffffffffffffffffffffffffffffffffffffffffefffffc2f300604010004010704410479be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798483ada7726a3c4655da4fbfc0e1108a8fd17b448a68554199c47d08ffb10d4b8022100fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141020101a144034200042af7a2aafe8dafd7dc7f9cfb58ce09bda7dce28653ab229b98d1d3d759660c672dd0db18c8c2d76aa470448e876fc2089ab1354c01a6e72cefc50915f4a963ee")

        // Now re-encode and decode the ASN.1 to see if it is equivalent (it does not produce the exact same byte
        // sequence, some integers are padded now).
        val roundtripKey = ECKey.fromASN1(decodedKey.toASN1())

        assertArrayEquals(decodedKey.privKeyBytes, roundtripKey.privKeyBytes)

        for (key in arrayOf(decodedKey, roundtripKey)) {
            val message = reverseBytes(HEX.hexToBytes(
                    "11da3761e86431e4a54c176789e41f1651b324d240d599a7067bee23d328ec2a"))
            var output = key.sign(Sha256Hash.wrap(message)).encodeToDER()
            assertTrue(key.verify(message, output))

            output = HEX.hexToBytes(
                    "304502206faa2ebc614bf4a0b31f0ce4ed9012eb193302ec2bcaccc7ae8bb40577f47549022100c73a1a1acc209f3f860bf9b9f5e13e9433db6f8b7bd527a088a0e0cd0a4c83e9")
            assertTrue(key.verify(message, output))
        }

        // Try to sign with one key and verify with the other.
        val message = reverseBytes(HEX.hexToBytes(
                "11da3761e86431e4a54c176789e41f1651b324d240d599a7067bee23d328ec2a"))
        assertTrue(roundtripKey.verify(message, decodedKey.sign(Sha256Hash.wrap(message)).encodeToDER()))
        assertTrue(decodedKey.verify(message, roundtripKey.sign(Sha256Hash.wrap(message)).encodeToDER()))
    }

    @Test
    @Throws(Exception::class)
    fun testKeyPairRoundtrip() {
        val decodedKey = ECKey.fromASN1Hex("3082011302010104205c0b98e524ad188ddef35dc6abba13c34a351a05409e5d285403718b93336a4aa081a53081a2020101302c06072a8648ce3d0101022100fffffffffffffffffffffffffffffffffffffffffffffffffffffffefffffc2f300604010004010704410479be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798483ada7726a3c4655da4fbfc0e1108a8fd17b448a68554199c47d08ffb10d4b8022100fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141020101a144034200042af7a2aafe8dafd7dc7f9cfb58ce09bda7dce28653ab229b98d1d3d759660c672dd0db18c8c2d76aa470448e876fc2089ab1354c01a6e72cefc50915f4a963ee")

        // Now re-encode and decode the ASN.1 to see if it is equivalent (it does not produce the exact same byte
        // sequence, some integers are padded now).
        val roundtripKey = ECKey.fromPrivateAndPrecalculatedPublic(decodedKey.privKey!!, decodedKey.pubKeyPoint)

        for (key in arrayOf(decodedKey, roundtripKey)) {
            val message = reverseBytes(HEX.hexToBytes(
                    "11da3761e86431e4a54c176789e41f1651b324d240d599a7067bee23d328ec2a"))
            var output = key.sign(Sha256Hash.wrap(message)).encodeToDER()
            assertTrue(key.verify(message, output))

            output = HEX.hexToBytes(
                    "304502206faa2ebc614bf4a0b31f0ce4ed9012eb193302ec2bcaccc7ae8bb40577f47549022100c73a1a1acc209f3f860bf9b9f5e13e9433db6f8b7bd527a088a0e0cd0a4c83e9")
            assertTrue(key.verify(message, output))
        }

        // Try to sign with one key and verify with the other.
        val message = reverseBytes(HEX.hexToBytes(
                "11da3761e86431e4a54c176789e41f1651b324d240d599a7067bee23d328ec2a"))
        assertTrue(roundtripKey.verify(message, decodedKey.sign(Sha256Hash.wrap(message)).encodeToDER()))
        assertTrue(decodedKey.verify(message, roundtripKey.sign(Sha256Hash.wrap(message)).encodeToDER()))

        // Verify bytewise equivalence of public keys (i.e. compression state is preserved)
        val key = ECKey.create()
        val key2 = ECKey.fromASN1(key.toASN1())
        assertArrayEquals(key.pubKey, key2.pubKey)
    }


    @Test
    @Throws(Exception::class)
    fun base58Encoding() {
        val addr = "mqAJmaxMcG5pPHHc3H3NtyXzY7kGbJLuMF"
        val privkey = "92shANodC6Y4evT5kFzjNFQAdjqTtHAnDTLzqBBq4BbKUPyx6CD"
        val key = ECKey.fromPrivateDump(TestNet3Params, privkey)
        assertEquals(privkey, key.dumpPrivKey(TestNet3Params).toString())
        assertEquals(addr, key.toCashAddress(TestNet3Params).toBase58())
    }

    @Test
    @Throws(Exception::class)
    fun base58Encoding_leadingZero() {
        val privkey = "91axuYLa8xK796DnBXXsMbjuc8pDYxYgJyQMvFzrZ6UfXaGYuqL"
        val key = ECKey.fromPrivateDump(TestNet3Params, privkey)
        assertEquals(privkey, key.dumpPrivKey(TestNet3Params).toString())
        assertEquals(0, key.privKeyBytes[0].toLong())
    }

    @Test
    @Throws(Exception::class)
    fun base58Encoding_stress() {
        // Replace the loop bound with 1000 to get some keys with leading zero byte
        for (i in 0..19) {
            val key = ECKey.create()
//            val key1 = DumpedPrivateKey.fromBase58(TestNet3Params,
//                    key.dumpPrivKey(TestNet3Params).toString()).key

            val key1 = ECKey.fromPrivateDump(TestNet3Params, key.dumpPrivKey(TestNet3Params).toBase58())

            assertArrayEquals(key.privKeyBytes, key1.privKeyBytes)
            assertEquals(key.privKeyBytes.toHex(), key1.privKeyBytes.toHex())
        }
    }


    @Test
    @Throws(Exception::class)
    fun signTextMessage() {
        val key = ECKey.create()
        val message = "聡中本"
        val signatureBase64 = key.signMessage(message)
        log.info("Message signed with " + key.toCashAddress(MainNetParams).toBase58() + ": " + signatureBase64)
        // Should verify correctly.
        key.verifyMessage(message, signatureBase64)
        try {
            key.verifyMessage("Evil attacker says hello!", signatureBase64)
            fail()
        } catch (e: SignatureException) {
            // OK.
        }

    }

    @Test
    @Throws(Exception::class)
    fun verifyMessage() {
        // Test vector generated by Bitcoin-Qt.
        val message = "hello"
        val sigBase64 = "HxNZdo6ggZ41hd3mM3gfJRqOQPZYcO8z8qdX2BwmpbF11CaOQV+QiZGGQxaYOncKoNW61oRuSMMF8udfK54XqI8="
        val key = ECKeySigner.signedMessageToKey(message, sigBase64)

        val gotCashAddress = key.toCashAddress(MainNetParams)

        val base58Address = "14YPSNPi6NSXnUxtPAsyJSuw3pv7AU3Cag"
        assertEquals(base58Address, gotCashAddress.toBase58())

        val expectedCashAddress = CashAddress.fromBase58(MainNetParams, base58Address)
        assertEquals(expectedCashAddress, gotCashAddress)
    }


    @Test
    @Throws(Exception::class)
    fun keyRecovery() {
        var key = ECKey.create()
        val message = "Hello World!"
        val hash = Sha256Hash.of(message.toByteArray())
        val sig = key.sign(hash)
        key = ECKey.fromPublicOnly(key.pubKeyPoint)
        var found = false
        for (i in 0..3) {
            val key2 = ECKeySigner.recoverFromSignature(i, sig, hash, true)
            checkNotNull<ECKey>(key2)
            if (key == key2) {
                found = true
                break
            }
        }
        assertTrue(found)
    }



    @Test
    @Throws(Exception::class)
    fun testUnencryptedCreate() {
        var key = ECKey.create()
        assertNotNull(key.priv)
        assertNotNull(key.privKey)
        assertNotNull(key.privKeyBytes)
        assertNotNull(key.pubKey)

        val encryptedKey = ECKeyCrypted.create(key, keyCrypter, PASSWORD1)
        assertNull(encryptedKey.ecKey.priv)
        assertArrayEquals(key.pubKey, encryptedKey.ecKey.pubKey)

        val rebornUnencryptedKey = encryptedKey.decrypt(keyCrypter.deriveKey(PASSWORD1))
        assertArrayEquals(key.pubKey, rebornUnencryptedKey.pubKey)
        assertEquals(key.pub, rebornUnencryptedKey.pub)
        assertEquals(key.priv, rebornUnencryptedKey.priv)
        assertEquals(key.privKey, rebornUnencryptedKey.privKey)
        assertArrayEquals(key.privKeyBytes, rebornUnencryptedKey.privKeyBytes)

        try {
            encryptedKey.decrypt(WRONG_PASSWORD)
            fail("wrong password should fail on decrypt")
        } catch (e: KeyCrypterException) {
        }
    }



    @Test
    @Throws(Exception::class)
    fun testToString() {
        val key = ECKey.fromPrivate(BigInteger.TEN).decompress() // An example private key.
        val params = MainNetParams
        assertEquals("ECKey{pub HEX=04a0434d9e47f3c86235477c7b1ae6ae5d3442d49b1943c2b752a68e2a47e247c7893aba425419bc27a3b6c7e693a24c696f794c2ed877a1593cbee53b037368d7}", key.toString())
        assertEquals("ECKey{pub HEX=04a0434d9e47f3c86235477c7b1ae6ae5d3442d49b1943c2b752a68e2a47e247c7893aba425419bc27a3b6c7e693a24c696f794c2ed877a1593cbee53b037368d7, priv HEX=000000000000000000000000000000000000000000000000000000000000000a, priv WIF=5HpHagT65TZzG1PH3CSu63k8DbpvD8s5ip4nEB3kEsreBoNWTw6}", key.toStringWithPrivate(params))
    }

    @Test
    @Throws(Exception::class)
    fun testGetPrivateKeyAsHex() {
        val key = ECKey.fromPrivate(BigInteger.TEN).decompress() // An example private key.
        assertEquals("000000000000000000000000000000000000000000000000000000000000000a", key.privKeyAsHex)
    }

    @Test
    @Throws(Exception::class)
    fun testGetPublicKeyAsHex() {
        val key = ECKey.fromPrivate(BigInteger.TEN).decompress() // An example private key.
        assertEquals("04a0434d9e47f3c86235477c7b1ae6ae5d3442d49b1943c2b752a68e2a47e247c7893aba425419bc27a3b6c7e693a24c696f794c2ed877a1593cbee53b037368d7", key.pubKeyAsHex)
    }


    @Test
    @Throws(Exception::class)
    fun keyRecoveryWithEncryptedKey() {
        var unencryptedKey = ECKey.create()
        val encryptedKey = ECKeyCrypted.create(unencryptedKey, keyCrypter, PASSWORD1)

        val message = "Goodbye Jupiter!"
        val hash = Sha256Hash.of(message.toByteArray())
        val sig = encryptedKey.decrypt(PASSWORD1).sign(hash)
        unencryptedKey = ECKey.fromPublicOnly(unencryptedKey.pubKeyPoint)
        var found = false
        for (i in 0..3) {
            val key2 = ECKeySigner.recoverFromSignature(i, sig, hash, true)
            checkNotNull<ECKey>(key2)
            if (unencryptedKey == key2) {
                found = true
                break
            }
        }
        assertTrue(found)
    }



    @Test
    @Throws(Exception::class)
    fun roundTripDumpedPrivKey() {
        val key = ECKey.create()
        assertTrue(key.isCompressed)
        val params = UnitTestParams
        val base58 = key.dumpPrivKey(params).toString()
        val key2 = DumpedPrivateKey.fromBase58(params, base58).key
        assertTrue(key2.isCompressed)
        assertArrayEquals(key.privKeyBytes, key2.privKeyBytes)
        assertArrayEquals(key.pubKey, key2.pubKey)
    }



    /*
    @Test
    @Throws(Exception::class)
    fun testCanonicalSigs() {
        // Tests the canonical sigs from Bitcoin Core unit tests
        val sigStream = javaClass.getResourceAsStream("sig_canonical.json")

        // Poor man's JSON parser (because pulling in a lib for this is overkill)
        while (sigStream.available() > 0) {
            while (sigStream.available() > 0 && sigStream.read() != '"'.toInt());
            if (sigStream.available() < 1)
                break

            val sig = StringBuilder()
            var c: Int
            var done = false
            while (sigStream.available() > 0 && !done) {
                c = sigStream.read()
                if (c != '"'.toInt())
                    sig.append(c.toChar())
                else
                    done = true
            }

            assertTrue(TransactionSignature.isEncodingCanonical(HEX.hexToBytes(sig.toString())))
        }
        sigStream.close()
    }

    @Test
    @Throws(Exception::class)
    fun testNonCanonicalSigs() {
        // Tests the noncanonical sigs from Bitcoin Core unit tests
        val sigStream = javaClass.getResourceAsStream("sig_noncanonical.json")

        // Poor man's JSON parser (because pulling in a lib for this is overkill)
        while (sigStream.available() > 0) {
            while (sigStream.available() > 0 && sigStream.read() != '"'.toInt());
            if (sigStream.available() < 1)
                break

            val sig = StringBuilder()
            var c: Int
            var done = false
            while (sigStream.available() > 0 && !done) {
                c = sigStream.read()
                if (c != '"'.toInt())
                    sig.append(c.toChar())
                else
                    done = true
            }

            try {
                val sigStr = sig.toString()
                assertFalse(TransactionSignature.isEncodingCanonical(HEX.hexToBytes(sigStr)))
            } catch (e: IllegalArgumentException) {
                // Expected for non-hex strings in the JSON that we should ignore
            }

        }
        sigStream.close()
    }
    */
/*
    @Test
    @Throws(Exception::class)
    fun testCreatedSigAndPubkeyAreCanonical() {
        // Tests that we will not generate non-canonical pubkeys or signatures
        // We dump failed data to error log because this test is not expected to be deterministic
        val key = ECKey.create()
        if (!ECKey.isPubKeyCanonical(key.pubKey)) {
//            log.error(HEX.encode(key.pubKey))
            fail()
        }

        val hash = ByteArray(32)
        Randomizer.nextBytes(hash)
        val sigBytes = key.sign(Sha256Hash.wrap(hash)).encodeToDER()
        val encodedSig = Arrays.copyOf(sigBytes, sigBytes.size + 1)
        encodedSig[sigBytes.size] = Transaction.SigHash.ALL.byteValue()
        if (!TransactionSignature.isEncodingCanonical(encodedSig)) {
            log.error(HEX.encode(sigBytes))
            fail()
        }
    }
*/

    companion object {
        private val log = loggerFor(ECKeyTest::class.java)


        private fun checkSomeBytesAreNonZero(bytes: ByteArray?): Boolean {
            if (bytes == null) return false
            for (b in bytes) if (b.toInt() != 0) return true
            return false
        }
    }


}
