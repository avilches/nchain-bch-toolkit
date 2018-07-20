/*
 * Copyright 2013 Matija Mazi.
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

package com.nchain.bip32

import com.nchain.params.MainNetParams
import com.nchain.params.NetworkParameters
import com.nchain.params.TestNet3Params
import com.nchain.params.UnitTestParams
import com.nchain.tools.HEX
import org.junit.*

import org.junit.Assert.*

/**
 * This test is adapted from Armory's BIP 32 tests.
 */
class ChildKeyDerivationTest {

    @Test
    @Throws(Exception::class)
    fun testChildKeyDerivation() {
        val ckdTestVectors = arrayOf(
                // test case 1:
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "04" + "6a04ab98d9e4774ad806e302dddeb63b" +
                "ea16b5cb5f223ee77478e861bb583eb3" +
                "36b6fbcb60b5b3d4f1551ac45e5ffc49" +
                "36466e7d98f6c7c0ec736539f74691a6", "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd",

                // test case 2:
                "be05d9ded0a73f81b814c93792f753b35c575fe446760005d44e0be13ba8935a", "02" + "b530da16bbff1428c33020e87fc9e699" +
                "cc9c753a63b8678ce647b7457397acef", "7012bc411228495f25d666d55fdce3f10a93908b5f9b9b7baa6e7573603a7bda")

        for (i in 0..0) {
            val priv = HEX.decode(ckdTestVectors[3 * i])
            val pub = HEX.decode(ckdTestVectors[3 * i + 1])
            val chain = HEX.decode(ckdTestVectors[3 * i + 2]) // chain code

            //////////////////////////////////////////////////////////////////////////
            // Start with an extended PRIVATE key
            val ekprv = HDKeyDerivation.createMasterPrivKeyFromBytes(priv, chain)

            // Create two accounts
            val ekprv_0 = HDKeyDerivation.deriveChildKey(ekprv, 0)
            val ekprv_1 = HDKeyDerivation.deriveChildKey(ekprv, 1)

            // Create internal and external chain on Account 0
            val ekprv_0_EX = HDKeyDerivation.deriveChildKey(ekprv_0, HDW_CHAIN_EXTERNAL)
            val ekprv_0_IN = HDKeyDerivation.deriveChildKey(ekprv_0, HDW_CHAIN_INTERNAL)

            // Create three addresses on external chain
            val ekprv_0_EX_0 = HDKeyDerivation.deriveChildKey(ekprv_0_EX, 0)
            val ekprv_0_EX_1 = HDKeyDerivation.deriveChildKey(ekprv_0_EX, 1)
            val ekprv_0_EX_2 = HDKeyDerivation.deriveChildKey(ekprv_0_EX, 2)

            // Create three addresses on internal chain
            val ekprv_0_IN_0 = HDKeyDerivation.deriveChildKey(ekprv_0_IN, 0)
            val ekprv_0_IN_1 = HDKeyDerivation.deriveChildKey(ekprv_0_IN, 1)
            val ekprv_0_IN_2 = HDKeyDerivation.deriveChildKey(ekprv_0_IN, 2)

            // Now add a few more addresses with very large indices
            val ekprv_1_IN = HDKeyDerivation.deriveChildKey(ekprv_1, HDW_CHAIN_INTERNAL)
            val ekprv_1_IN_4095 = HDKeyDerivation.deriveChildKey(ekprv_1_IN, 4095)
            //            ExtendedHierarchicKey ekprv_1_IN_4bil = HDKeyDerivation.deriveChildKey(ekprv_1_IN, 4294967295L);

            //////////////////////////////////////////////////////////////////////////
            // Repeat the above with PUBLIC key
            val ekpub = HDKeyDerivation.createMasterPubKeyFromBytes(HDUtils.toCompressed(pub), chain)

            // Create two accounts
            val ekpub_0 = HDKeyDerivation.deriveChildKey(ekpub, 0)
            val ekpub_1 = HDKeyDerivation.deriveChildKey(ekpub, 1)

            // Create internal and external chain on Account 0
            val ekpub_0_EX = HDKeyDerivation.deriveChildKey(ekpub_0, HDW_CHAIN_EXTERNAL)
            val ekpub_0_IN = HDKeyDerivation.deriveChildKey(ekpub_0, HDW_CHAIN_INTERNAL)

            // Create three addresses on external chain
            val ekpub_0_EX_0 = HDKeyDerivation.deriveChildKey(ekpub_0_EX, 0)
            val ekpub_0_EX_1 = HDKeyDerivation.deriveChildKey(ekpub_0_EX, 1)
            val ekpub_0_EX_2 = HDKeyDerivation.deriveChildKey(ekpub_0_EX, 2)

            // Create three addresses on internal chain
            val ekpub_0_IN_0 = HDKeyDerivation.deriveChildKey(ekpub_0_IN, 0)
            val ekpub_0_IN_1 = HDKeyDerivation.deriveChildKey(ekpub_0_IN, 1)
            val ekpub_0_IN_2 = HDKeyDerivation.deriveChildKey(ekpub_0_IN, 2)

            // Now add a few more addresses with very large indices
            val ekpub_1_IN = HDKeyDerivation.deriveChildKey(ekpub_1, HDW_CHAIN_INTERNAL)
            val ekpub_1_IN_4095 = HDKeyDerivation.deriveChildKey(ekpub_1_IN, 4095)
            //            ExtendedHierarchicKey ekpub_1_IN_4bil = HDKeyDerivation.deriveChildKey(ekpub_1_IN, 4294967295L);

            assertEquals(hexEncodePub(ekprv.dropPrivateBytes().dropParent()), hexEncodePub(ekpub))
            assertEquals(hexEncodePub(ekprv_0.dropPrivateBytes().dropParent()), hexEncodePub(ekpub_0))
            assertEquals(hexEncodePub(ekprv_1.dropPrivateBytes().dropParent()), hexEncodePub(ekpub_1))
            assertEquals(hexEncodePub(ekprv_0_IN.dropPrivateBytes().dropParent()), hexEncodePub(ekpub_0_IN))
            assertEquals(hexEncodePub(ekprv_0_IN_0.dropPrivateBytes().dropParent()), hexEncodePub(ekpub_0_IN_0))
            assertEquals(hexEncodePub(ekprv_0_IN_1.dropPrivateBytes().dropParent()), hexEncodePub(ekpub_0_IN_1))
            assertEquals(hexEncodePub(ekprv_0_IN_2.dropPrivateBytes().dropParent()), hexEncodePub(ekpub_0_IN_2))
            assertEquals(hexEncodePub(ekprv_0_EX_0.dropPrivateBytes().dropParent()), hexEncodePub(ekpub_0_EX_0))
            assertEquals(hexEncodePub(ekprv_0_EX_1.dropPrivateBytes().dropParent()), hexEncodePub(ekpub_0_EX_1))
            assertEquals(hexEncodePub(ekprv_0_EX_2.dropPrivateBytes().dropParent()), hexEncodePub(ekpub_0_EX_2))
            assertEquals(hexEncodePub(ekprv_1_IN.dropPrivateBytes().dropParent()), hexEncodePub(ekpub_1_IN))
            assertEquals(hexEncodePub(ekprv_1_IN_4095.dropPrivateBytes().dropParent()), hexEncodePub(ekpub_1_IN_4095))
            //assertEquals(hexEncodePub(ekprv_1_IN_4bil.dropPrivateBytes()), hexEncodePub(ekpub_1_IN_4bil));
        }
    }

    @Test
    @Throws(Exception::class)
    fun inverseEqualsNormal() {
        val key1 = HDKeyDerivation.createMasterPrivateKey("Wired / Aug 13th 2014 / Snowden: I Left the NSA Clues, But They Couldn't Find Them".toByteArray())
        val key2 = HDKeyDerivation.deriveChildKeyBytesFromPublic(key1.dropPrivateBytes().dropParent(), ChildNumber.ZERO, HDKeyDerivation.PublicDeriveMode.NORMAL)
        val key3 = HDKeyDerivation.deriveChildKeyBytesFromPublic(key1.dropPrivateBytes().dropParent(), ChildNumber.ZERO, HDKeyDerivation.PublicDeriveMode.WITH_INVERSION)
        assertArrayEquals(key2.keyBytes, key3.keyBytes)
        assertArrayEquals(key2.chainCode, key3.chainCode)
    }
/*

    @Test
    @Throws(Exception::class)
    fun encryptedDerivation() {
        // Check that encrypting a parent key in the heirarchy and then deriving from it yields a DeterministicKey
        // with no private key component, and that the private key bytes are derived on demand.
        val scrypter = KeyCrypterScrypt()
        val aesKey = scrypter.deriveKey("we never went to the moon")

        val key1 = HDKeyDerivation.createMasterPrivateKey("it was all a hoax".toByteArray())
        val encryptedKey1 = key1.encrypt(scrypter, aesKey, null)
        val decryptedKey1 = encryptedKey1.decrypt(aesKey)
        assertEquals(key1, decryptedKey1)

        val key2 = HDKeyDerivation.deriveChildKey(key1, ChildNumber.ZERO)
        val derivedKey2 = HDKeyDerivation.deriveChildKey(encryptedKey1, ChildNumber.ZERO)
        assertTrue(derivedKey2.isEncrypted)   // parent is encrypted.
        val decryptedKey2 = derivedKey2.decrypt(aesKey)
        assertFalse(decryptedKey2.isEncrypted)
        assertEquals(key2, decryptedKey2)

        val hash = Sha256Hash.of("the mainstream media won't cover it. why is that?".toByteArray())
        try {
            derivedKey2.sign(hash)
            fail()
        } catch (e: ECKey.KeyIsEncryptedException) {
            // Ignored.
        }

        val signature = derivedKey2.sign(hash, aesKey)
        assertTrue(derivedKey2.verify(hash, signature))
    }

*/
    @Test
    @Throws(Exception::class)
    fun pubOnlyDerivation() {
        val key1 = HDKeyDerivation.createMasterPrivateKey("satoshi lives!".toByteArray())
        assertFalse(key1.isPubKeyOnly)
        var key2 = HDKeyDerivation.deriveChildKey(key1, ChildNumber.ZERO_HARDENED)
        assertFalse(key2.isPubKeyOnly)
        val key3 = HDKeyDerivation.deriveChildKey(key2, ChildNumber.ZERO)
        assertFalse(key3.isPubKeyOnly)

        key2 = key2.dropPrivateBytes()
        assertFalse(key2.isPubKeyOnly)   // still got private key bytes from the parents!

        // pubkey2 got its cached private key bytes (if any) dropped, and now it'll lose its parent too, so now it
        // becomes a true pubkey-only object.
        val pubkey2 = key2.dropParent()

        val pubkey3 = HDKeyDerivation.deriveChildKey(pubkey2, ChildNumber.ZERO)
        assertTrue(pubkey3.isPubKeyOnly)
        assertEquals(key3.key.pubKeyPoint, pubkey3.key.pubKeyPoint)
    }

    @Test
    fun testSerializationMainAndTestNetworks() {
        val key1 = HDKeyDerivation.createMasterPrivateKey("satoshi lives!".toByteArray())
        var params: NetworkParameters = MainNetParams
        var pub58 = key1.serializePubB58(params)
        var priv58 = key1.serializePrivB58(params)
        assertEquals("xpub661MyMwAqRbcF7mq7Aejj5xZNzFfgi3ABamE9FedDHVmViSzSxYTgAQGcATDo2J821q7Y9EAagjg5EP3L7uBZk11PxZU3hikL59dexfLkz3", pub58)
        assertEquals("xprv9s21ZrQH143K2dhN197jMx1ppxRBHFKJpMqdLsF1ewxncv7quRED8N5nksxphju3W7naj1arF56L5PUEWfuSk8h73Sb2uh7bSwyXNrjzhAZ", priv58)
        params = TestNet3Params
        pub58 = key1.serializePubB58(params)
        priv58 = key1.serializePrivB58(params)
        assertEquals("tpubD6NzVbkrYhZ4WuxgZMdpw1Hvi7MKg6YDjDMXVohmZCFfF17hXBPYpc56rCY1KXFMovN29ik37nZimQseiykRTBTJTZJmjENyv2k3R12BJ1M", pub58)
        assertEquals("tprv8ZgxMBicQKsPdSvtfhyEXbdp95qPWmMK9ukkDHfU8vTGQWrvtnZxe7TEg48Ui7HMsZKMj7CcQRg8YF1ydtFPZBxha5oLa3qeN3iwpYhHPVZ", priv58)
    }

    @Test
    fun serializeToTextAndBytes() {
        val key1 = HDKeyDerivation.createMasterPrivateKey("satoshi lives!".toByteArray())
        val key2 = HDKeyDerivation.deriveChildKey(key1, ChildNumber.ZERO_HARDENED)

        // Creation time can't survive the xpub serialization format unfortunately.
//        key1.updateCreationTimeSeconds(0)
        val params = MainNetParams

        run {
            val pub58 = key1.serializePubB58(params)
            val priv58 = key1.serializePrivB58(params)
            val pub = key1.serializePublic(params)
            val priv = key1.serializePrivate(params)
            assertEquals("xpub661MyMwAqRbcF7mq7Aejj5xZNzFfgi3ABamE9FedDHVmViSzSxYTgAQGcATDo2J821q7Y9EAagjg5EP3L7uBZk11PxZU3hikL59dexfLkz3", pub58)
            assertEquals("xprv9s21ZrQH143K2dhN197jMx1ppxRBHFKJpMqdLsF1ewxncv7quRED8N5nksxphju3W7naj1arF56L5PUEWfuSk8h73Sb2uh7bSwyXNrjzhAZ", priv58)
            assertArrayEquals(byteArrayOf(4, -120, -78, 30, 0, 0, 0, 0, 0, 0, 0, 0, 0, 57, -68, 93, -104, -97, 31, -105, -18, 109, 112, 104, 45, -77, -77, 18, 85, -29, -120, 86, -113, 26, 48, -18, -79, -110, -6, -27, 87, 86, 24, 124, 99, 3, 96, -33, -14, 67, -19, -47, 16, 76, -49, -11, -30, -123, 7, 56, 101, 91, 74, 125, -127, 61, 42, -103, 90, -93, 66, -36, 2, -126, -107, 30, 24, -111), pub)
            assertArrayEquals(byteArrayOf(4, -120, -83, -28, 0, 0, 0, 0, 0, 0, 0, 0, 0, 57, -68, 93, -104, -97, 31, -105, -18, 109, 112, 104, 45, -77, -77, 18, 85, -29, -120, 86, -113, 26, 48, -18, -79, -110, -6, -27, 87, 86, 24, 124, 99, 0, -96, -75, 47, 90, -49, 92, -74, 92, -128, -125, 23, 38, -10, 97, -66, -19, 50, -112, 30, -111, -57, -124, 118, -86, 126, -35, -4, -51, 19, 109, 67, 116), priv)
            assertEquals(DeterministicKey.deserializeB58(null, priv58, params), key1)
            assertEquals(DeterministicKey.deserializeB58(priv58, params), key1)
            assertEquals(DeterministicKey.deserializeB58(null, pub58, params).key.pubKeyPoint, key1.key.pubKeyPoint)
            assertEquals(DeterministicKey.deserializeB58(pub58, params).key.pubKeyPoint, key1.key.pubKeyPoint)
            assertEquals(DeterministicKey.deserialize(params, priv, null), key1)
            assertEquals(DeterministicKey.deserialize(params, priv), key1)
            assertEquals(DeterministicKey.deserialize(params, pub, null).key.pubKeyPoint, key1.key.pubKeyPoint)
            assertEquals(DeterministicKey.deserialize(params, pub).key.pubKeyPoint, key1.key.pubKeyPoint)
        }
        run {
            val pub58 = key2.serializePubB58(params)
            val priv58 = key2.serializePrivB58(params)
            val pub = key2.serializePublic(params)
            val priv = key2.serializePrivate(params)
            assertEquals(DeterministicKey.deserializeB58(key1, priv58, params), key2)
            assertEquals(DeterministicKey.deserializeB58(key1, pub58, params).key.pubKeyPoint, key2.key.pubKeyPoint)
            assertEquals(DeterministicKey.deserialize(params, priv, key1), key2)
            assertEquals(DeterministicKey.deserialize(params, pub, key1).key.pubKeyPoint, key2.key.pubKeyPoint)
        }
    }

    @Test
    fun parentlessDeserialization() {
        val params = UnitTestParams
        val key1 = HDKeyDerivation.createMasterPrivateKey("satoshi lives!".toByteArray())
        val key2 = HDKeyDerivation.deriveChildKey(key1, ChildNumber.ZERO_HARDENED)
        val key3 = HDKeyDerivation.deriveChildKey(key2, ChildNumber.ZERO_HARDENED)
        val key4 = HDKeyDerivation.deriveChildKey(key3, ChildNumber.ZERO_HARDENED)
        assertEquals(key4.path.size.toLong(), 3)
        assertEquals(DeterministicKey.deserialize(params, key4.serializePrivate(params), key3).path.size.toLong(), 3)
        assertEquals(DeterministicKey.deserialize(params, key4.serializePrivate(params), null).path.size.toLong(), 1)
        assertEquals(DeterministicKey.deserialize(params, key4.serializePrivate(params)).path.size.toLong(), 1)
    }

    /** Reserializing a deserialized key should yield the original input  */
    @Test
    fun reserialization() {
        // This is the public encoding of the key with path m/0H/1/2H from BIP32 published test vector 1:
        // https://en.bitcoin.it/wiki/BIP_0032_TestVectors
        var encoded = "xpub6D4BDPcP2GT577Vvch3R8wDkScZWzQzMMUm3PWbmWvVJrZwQY4VUNgqFJPMM3No2dFDFGTsxxpG5uJh7n7epu4trkrX7x7DogT5Uv6fcLW5"
        var key = DeterministicKey.deserializeB58(encoded, MainNetParams)
        assertEquals("Reserialized parentless private HD key is wrong", key.serializePubB58(MainNetParams), encoded)
        assertEquals("Depth of deserialized parentless public HD key is wrong", key.depth.toLong(), 3)
        assertEquals("Path size of deserialized parentless public HD key is wrong", key.path.size.toLong(), 1)
        assertEquals("Parent fingerprint of deserialized parentless public HD key is wrong",
                key.parentFingerprint.toLong(), -0x410a5d07)

        // This encoding is the same key but including its private data:
        encoded = "xprv9z4pot5VBttmtdRTWfWQmoH1taj2axGVzFqSb8C9xaxKymcFzXBDptWmT7FwuEzG3ryjH4ktypQSAewRiNMjANTtpgP4mLTj34bhnZX7UiM"
        key = DeterministicKey.deserializeB58(encoded, MainNetParams)
        assertEquals("Reserialized parentless private HD key is wrong", key.serializePrivB58(MainNetParams), encoded)
        assertEquals("Depth of deserialized parentless private HD key is wrong", key.depth.toLong(), 3)
        assertEquals("Path size of deserialized parentless private HD key is wrong", key.path.size.toLong(), 1)
        assertEquals("Parent fingerprint of deserialized parentless private HD key is wrong",
                key.parentFingerprint.toLong(), -0x410a5d07)

        // These encodings are of the the root key of that hierarchy
        assertEquals("Parent fingerprint of root node public HD key should be zero",
                DeterministicKey.deserializeB58("xpub661MyMwAqRbcFW31YEwpkMuc5THy2PSt5bDMsktWQcFF8syAmRUapSCGu8ED9W6oDMSgv6Zz8idoc4a6mr8BDzTJY47LJhkJ8UB7WEGuduB", MainNetParams).parentFingerprint.toLong(),
                0)
        assertEquals("Parent fingerprint of root node private HD key should be zero",
                DeterministicKey.deserializeB58("xprv9s21ZrQH143K31xYSDQpPDxsXRTUcvj2iNHm5NUtrGiGG5e2DtALGdso3pGz6ssrdK4PFmM8NSpSBHNqPqm55Qn3LqFtT2emdEXVYsCzC2U", MainNetParams).parentFingerprint.toLong(),
                0)

    }

    companion object {
        private val HDW_CHAIN_EXTERNAL = 0
        private val HDW_CHAIN_INTERNAL = 1

        private fun hexEncodePub(pubKey: DeterministicKey): String {
            return HEX.encode(pubKey.key.pubKey)
        }
    }
}
