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

package com.nchain.key

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

import org.junit.Test
import com.nchain.params.MainNetParams
import com.nchain.params.TestNet3Params

class DumpedPrivateKeyTest {

    @Test
    @Throws(Exception::class)
    fun checkNetwork() {
        DumpedPrivateKey.fromBase58(MAINNET, "5HtUCLMFWNueqN9unpgX2DzjMg6SDNZyKRb8s3LJgpFg5ubuMrk")
    }

    @Test(expected = WrongNetworkException::class)
    @Throws(Exception::class)
    fun checkNetworkWrong() {
        DumpedPrivateKey.fromBase58(TESTNET, "5HtUCLMFWNueqN9unpgX2DzjMg6SDNZyKRb8s3LJgpFg5ubuMrk")
    }

    @Test
    @Throws(Exception::class)
    fun testJavaSerialization() {

        val key = DumpedPrivateKey(MAINNET, ECKey.create().privKeyBytes, true)
        val os = ByteArrayOutputStream()
        ObjectOutputStream(os).writeObject(key)
        val keyCopy = ObjectInputStream(ByteArrayInputStream(os.toByteArray()))
                .readObject() as DumpedPrivateKey
        assertEquals(key, keyCopy)
    }

    @Test
    @Throws(Exception::class)
    fun cloning() {
        val a = DumpedPrivateKey(MAINNET, ECKey.create().privKeyBytes, true)
        // TODO: Consider overriding clone() in DumpedPrivateKey to narrow the type
        val b = a.clone() as DumpedPrivateKey

        assertEquals(a, b)
        assertNotSame(a, b)
    }

    @Test
    @Throws(Exception::class)
    fun roundtripBase58() {
        val base58 = "5HtUCLMFWNueqN9unpgX2DzjMg6SDNZyKRb8s3LJgpFg5ubuMrk"
        assertEquals(base58, DumpedPrivateKey.fromBase58(null, base58).toBase58())
    }

    companion object {

        private val MAINNET = MainNetParams
        private val TESTNET = TestNet3Params
    }
}
