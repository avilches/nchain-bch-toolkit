/*
 * Copyright 2011 Google Inc.
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

import com.nchain.address.AddressFormatException
import com.nchain.address.Base58
import junit.framework.TestCase
import org.junit.Test

import java.math.BigInteger
import java.util.Arrays

class Base58Test : TestCase() {
    @Test
    @Throws(Exception::class)
    fun testEncode() {
        val testbytes = "Hello World".toByteArray()
        TestCase.assertEquals("JxF12TrwUP45BMd", Base58.encode(testbytes))

        val bi = BigInteger.valueOf(3471844090L)
        TestCase.assertEquals("16Ho7Hs", Base58.encode(bi.toByteArray()))

        val zeroBytes1 = ByteArray(1)
        TestCase.assertEquals("1", Base58.encode(zeroBytes1))

        val zeroBytes7 = ByteArray(7)
        TestCase.assertEquals("1111111", Base58.encode(zeroBytes7))

        // test empty encode
        TestCase.assertEquals("", Base58.encode(ByteArray(0)))
    }

    @Test
    @Throws(Exception::class)
    fun testDecode() {
        val testbytes = "Hello World".toByteArray()
        val actualbytes = Base58.decode("JxF12TrwUP45BMd")
        TestCase.assertTrue(String(actualbytes), Arrays.equals(testbytes, actualbytes))

        TestCase.assertTrue("1", Arrays.equals(Base58.decode("1"), ByteArray(1)))
        TestCase.assertTrue("1111", Arrays.equals(Base58.decode("1111"), ByteArray(4)))

        try {
            Base58.decode("This isn't valid base58")
            TestCase.fail()
        } catch (e: AddressFormatException) {
            // expected
        }

        Base58.decodeChecked("4stwEBjT6FYyVV")

        // Checksum should fail.
        try {
            Base58.decodeChecked("4stwEBjT6FYyVW")
            TestCase.fail()
        } catch (e: AddressFormatException) {
            // expected
        }

        // Input is too short.
        try {
            Base58.decodeChecked("4s")
            TestCase.fail()
        } catch (e: AddressFormatException) {
            // expected
        }

        // Test decode of empty String.
        TestCase.assertEquals(0, Base58.decode("").size)

        // Now check we can correctly decode the case where the high bit of the first byte is not zero, so BigInteger
        // sign extends. Fix for a bug that stopped us parsing keys exported using sipas patch.
        Base58.decodeChecked("93VYUMzRG9DdbRP72uQXjaWibbQwygnvaCu9DumcqDjGybD864T")
    }

    @Test
    fun testDecodeToBigInteger() {
        val input = Base58.decode("129")
        TestCase.assertEquals(BigInteger(1, input), Base58.decodeToBigInteger("129"))
    }
}
