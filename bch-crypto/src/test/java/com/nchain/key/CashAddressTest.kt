/*
 * Copyright 2018 the bitcoinj-cash developers
 * Copyright 2018 nChain Ltd.
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
 *
 * This file has been modified for the bitcoinkt project.
 * The original file was from the bitcoinj-cash project (https://github.com/bitcoinj-cash/bitcoinj).
 */
package com.nchain.key

import com.nchain.address.*
import com.nchain.bitcoinkt.params.MainNetParams
import com.nchain.bitcoinkt.params.TestNet3Params
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.io.IOException
import java.io.StringReader
import java.util.*

/**
 * Created by Hash Engineering Soltuions on 1/19/2018.
 */
class CashAddressTest {

    var cashAddressFactory: CashAddressFactory = CashAddressFactory
    val ADDRESSES_FILE_PATH_MAIN = "/bch_addresses_main.csv"
    val ADDRESSES_FILE_PATH_TEST = "/bch_addresses_test.csv"
    val CASH_ADDRESS_BY_LEGACY_FORMAT_MAIN = HashMap<String, String>()
    val CASH_ADDRESS_BY_LEGACY_FORMAT_TEST = HashMap<String, String>()

    @Before
    @Throws(IOException::class)
    fun loadAddressBatch() {
        val bchAddressesCsvFileText = CashAddressTest::class.java.getResource(ADDRESSES_FILE_PATH_MAIN).readText()
        StringReader(bchAddressesCsvFileText).forEachLine { line ->
            val components = line.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            CASH_ADDRESS_BY_LEGACY_FORMAT_MAIN[components[0]] = components[1]
        }

        val bchAddressesTestCsvFileText = CashAddressTest::class.java.getResource(ADDRESSES_FILE_PATH_TEST).readText()
        StringReader(bchAddressesTestCsvFileText).forEachLine { line ->
            val components = line.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            CASH_ADDRESS_BY_LEGACY_FORMAT_TEST[components[0]] = components[1]
        }
    }

    @Test
    fun testPrefixDoesNotMatchWithChecksum() {
        val params = MainNetParams
        val plainAddress = "bchtest:qpk4hk3wuxe2uqtqc97n8atzrrr6r5mleczf9sur4h"
        try {
            cashAddressFactory.fromFormattedAddress(params, plainAddress)
            fail("Exception expected but didn't happen")
        } catch (ignored: AddressFormatException) {
        }

    }

    @Test
    fun testPrefixDoesMatchesWithChecksum() {
        val params = MainNetParams
        val plainAddress = "bitcoincash:qpk4hk3wuxe2uqtqc97n8atzrrr6r5mleczf9sur4h"
        try {
            cashAddressFactory.fromFormattedAddress(params, plainAddress)
        } catch (ex: AddressFormatException) {
            fail("Unexpected exception: " + ex.message)
        }

    }

    @Test
    fun testNoPayload() {
        val params = MainNetParams
        val payload = byteArrayOf()
        val plainAddress = CashAddressHelper.encodeCashAddress(params.cashAddrPrefix, payload)
        try {
            cashAddressFactory.fromFormattedAddress(params, plainAddress)
            fail("Exception expected but didn't happen")
        } catch (ignore: AddressFormatException) {
        }

    }

    @Test
    fun testUnknownVersionByte() {
        val params = MainNetParams
        val payload = byteArrayOf(0x07, 0x01, 0x16, 0x15, 0x17, 0x16, 0x11, 0x0e, 0x1c, 0x06, 0x19, 0xa, 0x1c, 0x00, 0xb, 0x00, 0x18, 0x05, 0x1e, 0x13, 0x07, 0x1d, 0x0b, 0x02, 0x03, 0x03, 0x03, 0x1a, 0x3, 0x14, 0x1b, 0x1f, 0x19, 0x18)
        val plainAddress = CashAddressHelper.encodeCashAddress(params.cashAddrPrefix, payload)
        try {
            cashAddressFactory.fromFormattedAddress(params, plainAddress)
            fail("Exception expected but didn't happen")
        } catch (ignore: AddressFormatException) {
        }

    }

    @Test
    fun testMoreThanAllowedPadding() {
        val params = MainNetParams
        val payload = byteArrayOf(0x07, 0x01, 0x16, 0x15, 0x17, 0x16, 0x11, 0x0e, 0x1c, 0x06, 0x19, 0xa, 0x1c, 0x00, 0xb, 0x00, 0x18, 0x05, 0x1e, 0x13, 0x07, 0x1d, 0x0b, 0x02, 0x03)
        val plainAddress = CashAddressHelper.encodeCashAddress(params.cashAddrPrefix, payload)
        try {
            cashAddressFactory.fromFormattedAddress(params, plainAddress)
            fail("Exception expected but didn't happen")
        } catch (ignore: AddressFormatException) {
        }

    }

    @Test
    fun testNonZeroPadding() {
        val params = MainNetParams
        val payload = byteArrayOf(0x07, 0x01, 0x16, 0x15, 0x17, 0x16, 0x11, 0x0e, 0x1c, 0x06, 0x19, 0xa, 0x1c, 0x00, 0xb, 0x00, 0x18, 0x05, 0x1e, 0x13, 0x0d)
        val plainAddress = CashAddressHelper.encodeCashAddress(params.cashAddrPrefix, payload)
        try {
            cashAddressFactory.fromFormattedAddress(params, plainAddress)
            fail("Exception expected but didn't happen")
        } catch (ignore: AddressFormatException) {
        }

    }

    @Test
    fun testFirstBitOfByteVersionNonZero() {
        val params = MainNetParams
        val payload = byteArrayOf(0x1f, 0x01, 0x16, 0x15, 0x17, 0x16, 0x11, 0x0e, 0x1c, 0x06, 0x19, 0xa, 0x1c, 0x00, 0xb, 0x00, 0x18, 0x05, 0x1e, 0x13, 0x07, 0x1d, 0x0b, 0x02, 0x03, 0x03, 0x03, 0x1a, 0x3, 0x14, 0x1b, 0x1f, 0x19, 0x18)
        val plainAddress = CashAddressHelper.encodeCashAddress(params.cashAddrPrefix, payload)
        try {
            cashAddressFactory.fromFormattedAddress(params, plainAddress)
            fail("Exception expected but didn't happen")
        } catch (ignore: AddressFormatException) {
        }

    }

    @Test
    fun testHashSizeDoesNotMatch() {
        val params = MainNetParams
        val payload = byteArrayOf(0x00, 0x06, 0x16, 0x15, 0x17, 0x16, 0x11, 0x0e, 0x1c, 0x06, 0x19, 0xa, 0x1c, 0x00, 0xb, 0x00, 0x18, 0x05, 0x1e, 0x13, 0x07, 0x1d, 0x0b, 0x02, 0x03, 0x03, 0x03, 0x1a, 0x3, 0x14, 0x1b, 0x1f, 0x19, 0x18)
        val plainAddress = CashAddressHelper.encodeCashAddress(params.cashAddrPrefix, payload)
        try {
            cashAddressFactory.fromFormattedAddress(params, plainAddress)
            fail("Exception expected but didn't happen")
        } catch (ignore: AddressFormatException) {
        }

    }

    @Test
    fun testInvalidChecksum() {
        val params = MainNetParams
        val plainAddress = "bitcoincash:ppk4hk3wuxe2uqtqc97n8atzrrr6r5mleczf9sur4h"
        try {
            cashAddressFactory.fromFormattedAddress(params, plainAddress)
            fail("Exception expected but didn't happen")
        } catch (ignored: AddressFormatException) {
        }

    }

    @Test
    fun testAllUpperCaseAddress() {
        val params = MainNetParams
        val plainAddress = "BITCOINCASH:QPK4HK3WUXE2UQTQC97N8ATZRRR6R5MLECZF9SUR4H"
        try {
            cashAddressFactory.fromFormattedAddress(params, plainAddress)
        } catch (ex: AddressFormatException) {
            fail("Unexpected exception: " + ex.message)
        }

    }

    @Test
    fun testAllLowerCaseAddress() {
        val params = MainNetParams
        val plainAddress = "bitcoincash:qpk4hk3wuxe2uqtqc97n8atzrrr6r5mleczf9sur4h"
        try {
            cashAddressFactory.fromFormattedAddress(params, plainAddress)
        } catch (ex: AddressFormatException) {
            fail("Unexpected exception: " + ex.message)
        }

    }

    @Test
    fun testMixingCaseAddress() {
        val params = MainNetParams
        val plainAddress = "bitcoincash:qPk4hk3wuxe2UQtqc97n8atzrRR6r5mlECzf9sur4H"
        try {
            cashAddressFactory.fromFormattedAddress(params, plainAddress)
            fail("Exception expected but didn't happen")
        } catch (ignore: AddressFormatException) {
        }
    }

    @Test
    fun isP2PKHAddress_bitboxTest() {
        val params = MainNetParams

        // cashaddr
        var address = cashAddressFactory.fromFormattedAddress(params, "bitcoincash:qqfx3wcg8ts09mt5l3zey06wenapyfqq2qrcyj5x0s")
        assert(address.isP2PKHAddress)
        assert(!address.isP2SHAddress)

        // w/ no cashaddr prefix
        address = cashAddressFactory.fromFormattedAddress(params, "qqfx3wcg8ts09mt5l3zey06wenapyfqq2qrcyj5x0s")
        assert(address.isP2PKHAddress)
        assert(!address.isP2SHAddress)

        // legacy main net
        address = cashAddressFactory.fromBase58(null, "14krEkSaKoTkbFT9iUCfUYARo4EXA8co6M")
        assertEquals(address.parameters, MainNetParams)
        assert(address.isP2PKHAddress)
        assert(!address.isP2SHAddress)
        // true

        // legacy testnet
        address = cashAddressFactory.fromBase58(null, "mqc1tmwY2368LLGktnePzEyPAsgADxbksi")
        assertEquals(address.parameters, TestNet3Params)
        assert(address.isP2PKHAddress)
        assert(!address.isP2SHAddress)

        // testnet w/ no cashaddr prefix
        address = cashAddressFactory.fromFormattedAddress(TestNet3Params, "qph2v4mkxjgdqgmlyjx6njmey0ftrxlnggt9t0a6zy")
        assert(address.isP2PKHAddress)
        assert(!address.isP2SHAddress)


    }

    @Test
    fun testFromLegacyToCashAddressMain() {
        val params = MainNetParams
        for (legacy in CASH_ADDRESS_BY_LEGACY_FORMAT_MAIN.keys) {
            val plainCashAddress = CASH_ADDRESS_BY_LEGACY_FORMAT_MAIN[legacy]

            val legacyAddress: LegacyAddress = LegacyAddress.fromBase58(params, legacy)
            assertEquals(legacyAddress.toString(), legacy)


            val legacyCashAddress: CashAddress = cashAddressFactory.fromBase58(params, legacy)
            assertEquals(legacyCashAddress.toString(), plainCashAddress)

            val legacyCashAddress2: CashAddress = cashAddressFactory.fromBase58(legacyAddress)
            assertEquals(legacyCashAddress2.toString(), plainCashAddress)

        }
    }

    @Test
    fun testFromLegacyToCashAddressTest() {
        val params = TestNet3Params
        for (legacy in CASH_ADDRESS_BY_LEGACY_FORMAT_TEST.keys) {
            val plainCashAddress = CASH_ADDRESS_BY_LEGACY_FORMAT_TEST[legacy]

            val legacyAddress: LegacyAddress = LegacyAddress.fromBase58(params, legacy)
            assertEquals(legacyAddress.toString(), legacy)
            assertEquals(legacyAddress.toCashAddress(), plainCashAddress)


            val legacyCashAddress: CashAddress = cashAddressFactory.fromBase58(params, legacy)
            assertEquals(legacyCashAddress.toCashAddress(), plainCashAddress)

            val legacyCashAddress2: CashAddress = cashAddressFactory.fromBase58(legacyAddress)
            assertEquals(legacyCashAddress2.toCashAddress(), plainCashAddress)

        }
    }

    @Test
    fun testFromCashToLegacyAddressMain() {
        val params = MainNetParams
        for (legacy in CASH_ADDRESS_BY_LEGACY_FORMAT_MAIN.keys) {
            val cashAddress = cashAddressFactory.fromFormattedAddress(params, CASH_ADDRESS_BY_LEGACY_FORMAT_MAIN[legacy]!!)

            assertEquals(cashAddress.toBase58(), legacy)
        }
    }

    @Test
    fun testFromCashToLegacyAddressTest() {
        val params = TestNet3Params
        for (legacy in CASH_ADDRESS_BY_LEGACY_FORMAT_TEST.keys) {
            val cashAddress = cashAddressFactory.fromFormattedAddress(params, CASH_ADDRESS_BY_LEGACY_FORMAT_TEST[legacy]!!)

            assertEquals(cashAddress.toBase58(), legacy)
        }
    }


}
