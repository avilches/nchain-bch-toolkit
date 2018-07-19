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

import com.nchain.params.MainNetParams
import com.nchain.params.TestNet3Params
import com.nchain.tools.DRMWorkaround
import org.junit.Test

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Before

class BIP38PrivateKeyTest {

    @Before
    fun config() {
        DRMWorkaround.maybeDisableExportControls()
    }

    @Test
    @Throws(Exception::class)
    fun bip38testvector_noCompression_noEcMultiply_test1() {
        val encryptedKey = BIP38PrivateKey.fromBase58(MainNetParams,
                "6PRVWUbkzzsbcVac2qwfssoUJAN1Xhrg6bNk8J7Nzm5H7kxEbn2Nh2ZoGg")
        val key = encryptedKey.decrypt("TestingOneTwoThree")
        assertEquals("5KN7MzqK5wt2TP1fQCYyHBtDrXdJuXbUzm4A9rKAteGu3Qi5CVR", key.dumpPrivKey(MainNetParams)
                .toString())
    }

    @Test
    @Throws(Exception::class)
    fun bip38testvector_noCompression_noEcMultiply_test2() {
        val encryptedKey = BIP38PrivateKey.fromBase58(MainNetParams,
                "6PRNFFkZc2NZ6dJqFfhRoFNMR9Lnyj7dYGrzdgXXVMXcxoKTePPX1dWByq")
        val key = encryptedKey.decrypt("Satoshi")
        assertEquals("5HtasZ6ofTHP6HCwTqTkLDuLQisYPah7aUnSKfC7h4hMUVw2gi5", key.dumpPrivKey(MainNetParams)
                .toString())
    }

    @Test
    @Throws(Exception::class)
    fun bip38testvector_noCompression_noEcMultiply_test3() {
        val encryptedKey = BIP38PrivateKey.fromBase58(MainNetParams,
                "6PRW5o9FLp4gJDDVqJQKJFTpMvdsSGJxMYHtHaQBF3ooa8mwD69bapcDQn")
        val passphrase = StringBuilder()
        passphrase.appendCodePoint(0x03d2) // GREEK UPSILON WITH HOOK
        passphrase.appendCodePoint(0x0301) // COMBINING ACUTE ACCENT
        passphrase.appendCodePoint(0x0000) // NULL
        passphrase.appendCodePoint(0x010400) // DESERET CAPITAL LETTER LONG I
        passphrase.appendCodePoint(0x01f4a9) // PILE OF POO
        val key = encryptedKey.decrypt(passphrase.toString())
        assertEquals("5Jajm8eQ22H3pGWLEVCXyvND8dQZhiQhoLJNKjYXk9roUFTMSZ4", key.dumpPrivKey(MainNetParams)
                .toString())
    }

    @Test
    @Throws(Exception::class)
    fun bip38testvector_compression_noEcMultiply_test1() {
        val encryptedKey = BIP38PrivateKey.fromBase58(MainNetParams,
                "6PYNKZ1EAgYgmQfmNVamxyXVWHzK5s6DGhwP4J5o44cvXdoY7sRzhtpUeo")
        val key = encryptedKey.decrypt("TestingOneTwoThree")
        assertEquals("L44B5gGEpqEDRS9vVPz7QT35jcBG2r3CZwSwQ4fCewXAhAhqGVpP", key.dumpPrivKey(MainNetParams)
                .toString())
    }

    @Test
    @Throws(Exception::class)
    fun bip38testvector_compression_noEcMultiply_test2() {
        val encryptedKey = BIP38PrivateKey.fromBase58(MainNetParams,
                "6PYLtMnXvfG3oJde97zRyLYFZCYizPU5T3LwgdYJz1fRhh16bU7u6PPmY7")
        val key = encryptedKey.decrypt("Satoshi")
        assertEquals("KwYgW8gcxj1JWJXhPSu4Fqwzfhp5Yfi42mdYmMa4XqK7NJxXUSK7", key.dumpPrivKey(MainNetParams)
                .toString())
    }

    @Test
    @Throws(Exception::class)
    fun bip38testvector_ecMultiply_noCompression_noLotAndSequence_test1() {
        val encryptedKey = BIP38PrivateKey.fromBase58(MainNetParams,
                "6PfQu77ygVyJLZjfvMLyhLMQbYnu5uguoJJ4kMCLqWwPEdfpwANVS76gTX")
        val key = encryptedKey.decrypt("TestingOneTwoThree")
        assertEquals("5K4caxezwjGCGfnoPTZ8tMcJBLB7Jvyjv4xxeacadhq8nLisLR2", key.dumpPrivKey(MainNetParams)
                .toString())
    }

    @Test
    @Throws(Exception::class)
    fun bip38testvector_ecMultiply_noCompression_noLotAndSequence_test2() {
        val encryptedKey = BIP38PrivateKey.fromBase58(MainNetParams,
                "6PfLGnQs6VZnrNpmVKfjotbnQuaJK4KZoPFrAjx1JMJUa1Ft8gnf5WxfKd")
        val key = encryptedKey.decrypt("Satoshi")
        assertEquals("5KJ51SgxWaAYR13zd9ReMhJpwrcX47xTJh2D3fGPG9CM8vkv5sH", key.dumpPrivKey(MainNetParams)
                .toString())
    }

    @Test
    @Throws(Exception::class)
    fun bip38testvector_ecMultiply_noCompression_lotAndSequence_test1() {
        val encryptedKey = BIP38PrivateKey.fromBase58(MainNetParams,
                "6PgNBNNzDkKdhkT6uJntUXwwzQV8Rr2tZcbkDcuC9DZRsS6AtHts4Ypo1j")
        val key = encryptedKey.decrypt("MOLON LABE")
        assertEquals("5JLdxTtcTHcfYcmJsNVy1v2PMDx432JPoYcBTVVRHpPaxUrdtf8", key.dumpPrivKey(MainNetParams)
                .toString())
    }

    @Test
    @Throws(Exception::class)
    fun bip38testvector_ecMultiply_noCompression_lotAndSequence_test2() {
        val encryptedKey = BIP38PrivateKey.fromBase58(MainNetParams,
                "6PgGWtx25kUg8QWvwuJAgorN6k9FbE25rv5dMRwu5SKMnfpfVe5mar2ngH")
        val key = encryptedKey.decrypt("ΜΟΛΩΝ ΛΑΒΕ")
        assertEquals("5KMKKuUmAkiNbA3DazMQiLfDq47qs8MAEThm4yL8R2PhV1ov33D", key.dumpPrivKey(MainNetParams)
                .toString())
    }

    @Test
    @Throws(Exception::class)
    fun bitcoinpaperwallet_testnet() {
        // values taken from bitcoinpaperwallet.com
        val encryptedKey = BIP38PrivateKey.fromBase58(TestNet3Params,
                "6PRPhQhmtw6dQu6jD8E1KS4VphwJxBS9Eh9C8FQELcrwN3vPvskv9NKvuL")
        val key = encryptedKey.decrypt("password")
        assertEquals("93MLfjbY6ugAsLeQfFY6zodDa8izgm1XAwA9cpMbUTwLkDitopg", key.dumpPrivKey(TestNet3Params)
                .toString())
    }

    @Test
    @Throws(Exception::class)
    fun bitaddress_testnet() {
        // values taken from bitaddress.org
        val encryptedKey = BIP38PrivateKey.fromBase58(TestNet3Params,
                "6PfMmVHn153N3x83Yiy4Nf76dHUkXufe2Adr9Fw5bewrunGNeaw2QCpifb")
        val key = encryptedKey.decrypt("password")
        assertEquals("91tCpdaGr4Khv7UAuUxa6aMqeN5GcPVJxzLtNsnZHTCndxkRcz2", key.dumpPrivKey(TestNet3Params)
                .toString())
    }

    @Test(expected = BIP38PrivateKey.BadPassphraseException::class)
    @Throws(Exception::class)
    fun badPassphrase() {
        val encryptedKey = BIP38PrivateKey.fromBase58(MainNetParams,
                "6PRVWUbkzzsbcVac2qwfssoUJAN1Xhrg6bNk8J7Nzm5H7kxEbn2Nh2ZoGg")
        encryptedKey.decrypt("BAD")
    }

    @Test
    @Throws(Exception::class)
    fun testJavaSerialization() {
        val testKey = BIP38PrivateKey.fromBase58(TestNet3Params,
                "6PfMmVHn153N3x83Yiy4Nf76dHUkXufe2Adr9Fw5bewrunGNeaw2QCpifb")
        var os = ByteArrayOutputStream()
        ObjectOutputStream(os).writeObject(testKey)
        val testKeyCopy = ObjectInputStream(
                ByteArrayInputStream(os.toByteArray())).readObject() as BIP38PrivateKey
        assertEquals(testKey, testKeyCopy)

        val mainKey = BIP38PrivateKey.fromBase58(MainNetParams,
                "6PfMmVHn153N3x83Yiy4Nf76dHUkXufe2Adr9Fw5bewrunGNeaw2QCpifb")
        os = ByteArrayOutputStream()
        ObjectOutputStream(os).writeObject(mainKey)
        val mainKeyCopy = ObjectInputStream(
                ByteArrayInputStream(os.toByteArray())).readObject() as BIP38PrivateKey
        assertEquals(mainKey, mainKeyCopy)
    }

    @Test
    @Throws(Exception::class)
    fun cloning() {
        val a = BIP38PrivateKey.fromBase58(TestNet3Params, "6PfMmVHn153N3x83Yiy4Nf76dHUkXufe2Adr9Fw5bewrunGNeaw2QCpifb")
        // TODO: Consider overriding clone() in BIP38PrivateKey to narrow the type
        val b = a.clone() as BIP38PrivateKey

        assertEquals(a, b)
        assertNotSame(a, b)
    }

    @Test
    @Throws(Exception::class)
    fun roundtripBase58() {
        val base58 = "6PfMmVHn153N3x83Yiy4Nf76dHUkXufe2Adr9Fw5bewrunGNeaw2QCpifb"
        assertEquals(base58, BIP38PrivateKey.fromBase58(MainNetParams, base58).toBase58())
    }

}
