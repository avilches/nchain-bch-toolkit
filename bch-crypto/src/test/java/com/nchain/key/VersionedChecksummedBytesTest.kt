/*
 * Copyright 2014 bitcoinj project
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

import com.nchain.address.VersionedChecksummedBytes
import com.nchain.bitcoinkt.params.MainNetParams
import com.nchain.bitcoinkt.params.NetworkParameters
import com.nchain.bitcoinkt.params.TestNet3Params
import com.nchain.tools.HEX
import org.junit.Test

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue

/**
 *
 */
class VersionedChecksummedBytesTest {

    @Test
    @Throws(Exception::class)
    fun stringification() {
        // Test a testnet address.
        val a = VersionedChecksummedBytes(testParams.addressHeader, HEX.hexToBytes("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc"))
        assertEquals("n4eA2nbYqErp7H6jebchxAN59DmNpksexv", a.toString())

        val b = VersionedChecksummedBytes(mainParams.addressHeader, HEX.hexToBytes("4a22c3c4cbb31e4d03b15550636762bda0baf85a"))
        assertEquals("17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL", b.toString())
    }

    @Test
    @Throws(Exception::class)
    fun cloning() {
        val a = VersionedChecksummedBytes(testParams.addressHeader, HEX.hexToBytes("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc"))
        val b = a.clone()

        assertEquals(a, b)
        assertNotSame(a, b)
    }

    @Test
    @Throws(Exception::class)
    fun comparisonCloneEqualTo() {
        val a = VersionedChecksummedBytes(testParams.addressHeader, HEX.hexToBytes("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc"))
        val b = a.clone()

        assertTrue(a.compareTo(b) == 0)
    }

    companion object {
        internal val testParams: NetworkParameters = TestNet3Params
        internal val mainParams: NetworkParameters = MainNetParams
    }
}
