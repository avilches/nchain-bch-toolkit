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
import com.nchain.address.AddressFormatException
import com.nchain.address.VersionedChecksummedBytes
import com.nchain.bitcoinkt.params.MainNetParams
import com.nchain.bitcoinkt.params.NetworkParameters
import com.nchain.bitcoinkt.params.TestNet3Params
import com.nchain.tools.HEX
import org.junit.Test

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.Arrays

import org.junit.Assert.*

class Base58AddressTest {

    @Test
    @Throws(Exception::class)
    fun testJavaSerialization() {
        val testAddress = CashAddress.fromBase58(testParams, "n4eA2nbYqErp7H6jebchxAN59DmNpksexv")
        var os = ByteArrayOutputStream()
        ObjectOutputStream(os).writeObject(testAddress)
        val testAddressCopy = ObjectInputStream(
                ByteArrayInputStream(os.toByteArray())).readObject() as VersionedChecksummedBytes
        assertEquals(testAddress, testAddressCopy)

        val mainAddress = CashAddress.fromBase58(mainParams, "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL")
        os = ByteArrayOutputStream()
        ObjectOutputStream(os).writeObject(mainAddress)
        val mainAddressCopy = ObjectInputStream(
                ByteArrayInputStream(os.toByteArray())).readObject() as VersionedChecksummedBytes
        assertEquals(mainAddress, mainAddressCopy)
    }

    @Test
    @Throws(Exception::class)
    fun stringification() {
        // Test a testnet address.
        val a = CashAddress.fromHash160(testParams, HEX.hexToBytes("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc"))
        assertEquals("n4eA2nbYqErp7H6jebchxAN59DmNpksexv", a.toBase58())
        assertFalse(a.isP2SHAddress)

        val b = CashAddress.fromHash160(mainParams, HEX.hexToBytes("4a22c3c4cbb31e4d03b15550636762bda0baf85a"))
        assertEquals("17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL", b.toBase58())
        assertFalse(b.isP2SHAddress)
    }

    @Test
    @Throws(Exception::class)
    fun decoding() {
        val a = CashAddress.fromBase58(testParams, "n4eA2nbYqErp7H6jebchxAN59DmNpksexv")
        assertEquals("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc", HEX.bytesToHex(a.hash160))

        val b = CashAddress.fromBase58(mainParams, "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL")
        assertEquals("4a22c3c4cbb31e4d03b15550636762bda0baf85a", HEX.bytesToHex(b.hash160))
    }

    @Test
    fun errorPaths() {
        // Check what happens if we try and decode garbage.
        try {
            CashAddress.fromBase58(testParams, "this is not a valid address!")
            fail()
        } catch (e: WrongNetworkException) {
            fail()
        } catch (e: AddressFormatException) {
            // Success.
        }

        // Check the empty case.
        try {
            CashAddress.fromBase58(testParams, "")
            fail()
        } catch (e: WrongNetworkException) {
            fail()
        } catch (e: AddressFormatException) {
            // Success.
        }

        // Check the case of a mismatched network.
        try {
            CashAddress.fromBase58(testParams, "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL")
            fail()
        } catch (e: WrongNetworkException) {
            // Success.
            assertEquals(e.verCode.toLong(), MainNetParams.addressHeader.toLong())
            assertTrue(Arrays.equals(e.acceptableVersions, TestNet3Params.acceptableAddressCodes))
        } catch (e: AddressFormatException) {
            fail()
        }

    }

    @Test
    @Throws(Exception::class)
    fun getNetwork() {
        var params = CashAddress.getParametersFromAddress("17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL")
        assertEquals(MainNetParams.id, params!!.id)
        params = CashAddress.getParametersFromAddress("n4eA2nbYqErp7H6jebchxAN59DmNpksexv")
        assertEquals(TestNet3Params.id, params!!.id)
    }

    @Test
    @Throws(Exception::class)
    fun p2shAddress() {
        // Test that we can construct P2SH addresses
        val mainNetP2SHAddress = CashAddress.fromBase58(MainNetParams, "35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU")
        assertEquals(mainNetP2SHAddress.version.toLong(), MainNetParams.p2SHHeader.toLong())
        assertTrue(mainNetP2SHAddress.isP2SHAddress)
        val testNetP2SHAddress = CashAddress.fromBase58(TestNet3Params, "2MuVSxtfivPKJe93EC1Tb9UhJtGhsoWEHCe")
        assertEquals(testNetP2SHAddress.version.toLong(), TestNet3Params.p2SHHeader.toLong())
        assertTrue(testNetP2SHAddress.isP2SHAddress)

        // Test that we can determine what network a P2SH address belongs to
        val mainNetParams = CashAddress.getParametersFromAddress("35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU")
        assertEquals(MainNetParams.id, mainNetParams!!.id)
        val testNetParams = CashAddress.getParametersFromAddress("2MuVSxtfivPKJe93EC1Tb9UhJtGhsoWEHCe")
        assertEquals(TestNet3Params.id, testNetParams!!.id)

        // Test that we can convert them from hashes
        val hex = HEX.hexToBytes("2ac4b0b501117cc8119c5797b519538d4942e90e")
        val a = CashAddress.fromP2SHHash(mainParams, hex)
        assertEquals("35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU", a.toBase58())
        val b = CashAddress.fromP2SHHash(testParams, HEX.hexToBytes("18a0e827269b5211eb51a4af1b2fa69333efa722"))
        assertEquals("2MuVSxtfivPKJe93EC1Tb9UhJtGhsoWEHCe", b.toBase58())
//        val c = Address.fromP2SHScript(mainParams, ScriptBuilder.createP2SHOutputScript(hex))
//        assertEquals("35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU", c.toString())
    }
    /*
    @Test
    @Throws(Exception::class)
    fun p2shAddressCreationFromKeys() {
        // import some keys from this example: https://gist.github.com/gavinandresen/3966071
        var key1 = DumpedPrivateKey.fromBase58(mainParams, "5JaTXbAUmfPYZFRwrYaALK48fN6sFJp4rHqq2QSXs8ucfpE4yQU").key
        key1 = ECKey.fromPrivate(key1.privKeyBytes)
        var key2 = DumpedPrivateKey.fromBase58(mainParams, "5Jb7fCeh1Wtm4yBBg3q3XbT6B525i17kVhy3vMC9AqfR6FH2qGk").key
        key2 = ECKey.fromPrivate(key2.privKeyBytes)
        var key3 = DumpedPrivateKey.fromBase58(mainParams, "5JFjmGo5Fww9p8gvx48qBYDJNAzR9pmH5S389axMtDyPT8ddqmw").key
        key3 = ECKey.fromPrivate(key3.privKeyBytes)

        val keys = Arrays.asList(key1, key2, key3)
        val p2shScript = ScriptBuilder.createP2SHOutputScript(2, keys)
        val address = Address.fromP2SHScript(mainParams, p2shScript)
        assertEquals("3N25saC4dT24RphDAwLtD8LUN4E2gZPJke", address.toString())
    }
      */
    @Test
    @Throws(Exception::class)
    fun cloning() {
        val a = CashAddress.fromHash160(testParams, HEX.hexToBytes("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc"))
        val b = a.clone()

        assertEquals(a, b)
        assertNotSame(a, b)
    }

    @Test
    @Throws(Exception::class)
    fun roundtripBase58() {
        val base58 = "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL"
        assertEquals(base58, CashAddress.fromBase58(null, base58).toBase58())
    }

    @Test
    @Throws(Exception::class)
    fun comparisonCloneEqualTo() {
        val a = CashAddress.fromBase58(mainParams, "1Dorian4RoXcnBv9hnQ4Y2C1an6NJ4UrjX")
        val b = a.clone()

        val result = a.compareTo(b)
        assertEquals(0, result.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun comparisonEqualTo() {
        val a = CashAddress.fromBase58(mainParams, "1Dorian4RoXcnBv9hnQ4Y2C1an6NJ4UrjX")
        val b = a.clone()

        val result = a.compareTo(b)
        assertEquals(0, result.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun comparisonLessThan() {
        val a = CashAddress.fromBase58(mainParams, "1Dorian4RoXcnBv9hnQ4Y2C1an6NJ4UrjX")
        val b = CashAddress.fromBase58(mainParams, "1EXoDusjGwvnjZUyKkxZ4UHEf77z6A5S4P")

        val result = a.compareTo(b)
        assertTrue(result < 0)
    }

    @Test
    @Throws(Exception::class)
    fun comparisonGreaterThan() {
        val a = CashAddress.fromBase58(mainParams, "1EXoDusjGwvnjZUyKkxZ4UHEf77z6A5S4P")
        val b = CashAddress.fromBase58(mainParams, "1Dorian4RoXcnBv9hnQ4Y2C1an6NJ4UrjX")

        val result = a.compareTo(b)
        assertTrue(result > 0)
    }

    @Test
    @Throws(Exception::class)
    fun comparisonBytesVsString() {
        // TODO: To properly test this we need a much larger data set
        val a = CashAddress.fromBase58(mainParams, "1Dorian4RoXcnBv9hnQ4Y2C1an6NJ4UrjX")
        val b = CashAddress.fromBase58(mainParams, "1EXoDusjGwvnjZUyKkxZ4UHEf77z6A5S4P")

        val resultBytes = a.compareTo(b)
        val resultsString = a.toBase58().compareTo(b.toBase58())
        assertTrue(resultBytes < 0)
        assertTrue(resultsString < 0)
    }

    companion object {
        internal val testParams: NetworkParameters = TestNet3Params
        internal val mainParams: NetworkParameters = MainNetParams
    }
}
