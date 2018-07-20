package com.nchain.address

import com.nchain.params.MainNetParams
import com.nchain.params.TestNet3Params
import spock.lang.Shared
import spock.lang.Specification


/*
 * @author Alberto Vilches
 * @date 18/07/2018
 */

class CashAddressSpec extends Specification {

    @Shared List<AddressTuple> CASH_ADDRESS_BY_LEGACY_FORMAT_MAIN
    @Shared List<AddressTuple> CASH_ADDRESS_BY_LEGACY_FORMAT_TEST

    void setupSpec() {
        CASH_ADDRESS_BY_LEGACY_FORMAT_MAIN = loadCashAddressResource("/bch_addresses_main.csv")
        CASH_ADDRESS_BY_LEGACY_FORMAT_TEST = loadCashAddressResource("/bch_addresses_test.csv")
    }


    void testPrefixDoesNotMatchWithChecksum() {
        def params = MainNetParams.INSTANCE
        def plainAddress = "bchtest:qpk4hk3wuxe2uqtqc97n8atzrrr6r5mleczf9sur4h"
        when:
        CashAddress.fromFormattedAddress(params, plainAddress)
        then:
        thrown(AddressFormatException)
    }

    void testPrefixDoesMatchesWithChecksum() {
        def params = MainNetParams.INSTANCE
        def plainAddress = "bitcoincash:qpk4hk3wuxe2uqtqc97n8atzrrr6r5mleczf9sur4h"
        when:
        CashAddress.fromFormattedAddress(params, plainAddress)
        then:
        notThrown(AddressFormatException)
    }

    void testNoPayload() {
        def params = MainNetParams.INSTANCE
        def payload = [] as byte[]
        when:
        def plainAddress = CashAddressHelper.encodeCashAddress(params.cashAddrPrefix, payload)
        CashAddress.fromFormattedAddress(params, plainAddress)
        then:
        thrown(AddressFormatException)
    }

    void testUnknownVersionByte() {
        def params = MainNetParams.INSTANCE
        def payload = byteArrayOf(0x07, 0x01, 0x16, 0x15, 0x17, 0x16, 0x11, 0x0e, 0x1c, 0x06, 0x19, 0xa, 0x1c, 0x00, 0xb, 0x00, 0x18, 0x05, 0x1e, 0x13, 0x07, 0x1d, 0x0b, 0x02, 0x03, 0x03, 0x03, 0x1a, 0x3, 0x14, 0x1b, 0x1f, 0x19, 0x18)
        def plainAddress = CashAddressHelper.encodeCashAddress(params.cashAddrPrefix, payload)
        when:
        CashAddress.fromFormattedAddress(params, plainAddress)
        then:
        thrown(AddressFormatException)

    }

    void testMoreThanAllowedPadding() {
        def params = MainNetParams.INSTANCE
        def payload = byteArrayOf(0x07, 0x01, 0x16, 0x15, 0x17, 0x16, 0x11, 0x0e, 0x1c, 0x06, 0x19, 0xa, 0x1c, 0x00, 0xb, 0x00, 0x18, 0x05, 0x1e, 0x13, 0x07, 0x1d, 0x0b, 0x02, 0x03)
        def plainAddress = CashAddressHelper.encodeCashAddress(params.cashAddrPrefix, payload)
        when:
        CashAddress.fromFormattedAddress(params, plainAddress)
        then:
        thrown(AddressFormatException)
    }

    void testNonZeroPadding() {
        def params = MainNetParams.INSTANCE
        def payload = byteArrayOf(0x07, 0x01, 0x16, 0x15, 0x17, 0x16, 0x11, 0x0e, 0x1c, 0x06, 0x19, 0xa, 0x1c, 0x00, 0xb, 0x00, 0x18, 0x05, 0x1e, 0x13, 0x0d)
        def plainAddress = CashAddressHelper.encodeCashAddress(params.cashAddrPrefix, payload)
        when:
        CashAddress.fromFormattedAddress(params, plainAddress)
        then:
        thrown(AddressFormatException)

    }

    void testFirstBitOfByteVersionNonZero() {
        def params = MainNetParams.INSTANCE
        def payload = byteArrayOf(0x1f, 0x01, 0x16, 0x15, 0x17, 0x16, 0x11, 0x0e, 0x1c, 0x06, 0x19, 0xa, 0x1c, 0x00, 0xb, 0x00, 0x18, 0x05, 0x1e, 0x13, 0x07, 0x1d, 0x0b, 0x02, 0x03, 0x03, 0x03, 0x1a, 0x3, 0x14, 0x1b, 0x1f, 0x19, 0x18)
        def plainAddress = CashAddressHelper.encodeCashAddress(params.cashAddrPrefix, payload)
        when:
        CashAddress.fromFormattedAddress(params, plainAddress)
        then:
        thrown(AddressFormatException)

    }

    void testHashSizeDoesNotMatch() {
        def params = MainNetParams.INSTANCE
        def payload = byteArrayOf(0x00, 0x06, 0x16, 0x15, 0x17, 0x16, 0x11, 0x0e, 0x1c, 0x06, 0x19, 0xa, 0x1c, 0x00, 0xb, 0x00, 0x18, 0x05, 0x1e, 0x13, 0x07, 0x1d, 0x0b, 0x02, 0x03, 0x03, 0x03, 0x1a, 0x3, 0x14, 0x1b, 0x1f, 0x19, 0x18)
        def plainAddress = CashAddressHelper.encodeCashAddress(params.cashAddrPrefix, payload)
        when:
        CashAddress.fromFormattedAddress(params, plainAddress)
        then:
        thrown(AddressFormatException)

    }

    void testInvalidChecksum() {
        def params = MainNetParams.INSTANCE
        def plainAddress = "bitcoincash:ppk4hk3wuxe2uqtqc97n8atzrrr6r5mleczf9sur4h"
        when:
        CashAddress.fromFormattedAddress(params, plainAddress)
        then:
        thrown(AddressFormatException)

    }

    void testAllUpperCaseAddress() {
        def params = MainNetParams.INSTANCE
        def plainAddress = "BITCOINCASH:QPK4HK3WUXE2UQTQC97N8ATZRRR6R5MLECZF9SUR4H"
        when:
        CashAddress.fromFormattedAddress(params, plainAddress)
        then:
        notThrown(AddressFormatException)

    }

    void testAllLowerCaseAddress() {
        def params = MainNetParams.INSTANCE
        def plainAddress = "bitcoincash:qpk4hk3wuxe2uqtqc97n8atzrrr6r5mleczf9sur4h"
        when:
        CashAddress.fromFormattedAddress(params, plainAddress)
        then:
        notThrown(AddressFormatException)

    }

    void testMixingCaseAddress() {
        def params = MainNetParams.INSTANCE
        def plainAddress = "bitcoincash:qPk4hk3wuxe2UQtqc97n8atzrRR6r5mlECzf9sur4H"
        when:
        CashAddress.fromFormattedAddress(params, plainAddress)
        then:
        thrown(AddressFormatException)
    }

    void isP2PKHAddress_bitboxTest() {
        when: "mainnet w cashaddr prefix"
        def address = CashAddress.from("bitcoincash:qqfx3wcg8ts09mt5l3zey06wenapyfqq2qrcyj5x0s")
        then:
        address.parameters == MainNetParams.INSTANCE
        address.isMainNet()
        !address.isTestNet()
        address.isP2PKHAddress()
        !address.isP2SHAddress()

        when: "mainnet w/ no cashaddr prefix"
        address = CashAddress.from("qqfx3wcg8ts09mt5l3zey06wenapyfqq2qrcyj5x0s")
        then:
        address.parameters == MainNetParams.INSTANCE
        address.isMainNet()
        !address.isTestNet()
        address.isP2PKHAddress()
        !address.isP2SHAddress()

        when: "legacy main net"
        address = CashAddress.from("14krEkSaKoTkbFT9iUCfUYARo4EXA8co6M")
        then:
        address.parameters == MainNetParams.INSTANCE
        address.isMainNet()
        !address.isTestNet()
        address.isP2PKHAddress()
        !address.isP2SHAddress()

        when: "testnet w cashaddr prefix"
        address = CashAddress.from("bchtest:qph2v4mkxjgdqgmlyjx6njmey0ftrxlnggt9t0a6zy")
        then:
        address.parameters == TestNet3Params.INSTANCE
        !address.isMainNet()
        address.isTestNet()
        address.isP2PKHAddress()
        !address.isP2SHAddress()

        when: "testnet w/ no cashaddr prefix"
        address = CashAddress.from("qph2v4mkxjgdqgmlyjx6njmey0ftrxlnggt9t0a6zy")
        then:
        address.parameters == TestNet3Params.INSTANCE
        !address.isMainNet()
        address.isTestNet()
        address.isP2PKHAddress()
        !address.isP2SHAddress()

        when: "legacy testnet"
        address = CashAddress.from("mqc1tmwY2368LLGktnePzEyPAsgADxbksi")
        then:
        address.parameters == TestNet3Params.INSTANCE
        !address.isMainNet()
        address.isTestNet()
        address.isP2PKHAddress()
        !address.isP2SHAddress()
    }

    void testFromLegacyToCashAddressMain() {
        when:
        def cashAddress = CashAddress.fromBase58(params, tuple.legacy)
        def autoLegacyCashAddress = CashAddress.from(tuple.legacy)
        then:
        cashAddress.toBase58() == tuple.legacy
        cashAddress.toCashAddress() == tuple.cashAddress
        cashAddress.parameters == params
        cashAddress == autoLegacyCashAddress

        where:
        tuple << CASH_ADDRESS_BY_LEGACY_FORMAT_MAIN
        params = MainNetParams.INSTANCE
    }

    void testFromLegacyToCashAddressTest() {
        when:
        def cashAddress = CashAddress.fromBase58(params, tuple.legacy)
        def autoLegacyCashAddress = CashAddress.from(tuple.legacy)
        then:
        cashAddress.toBase58() == tuple.legacy
        cashAddress.toCashAddress() == tuple.cashAddress
        cashAddress.parameters == params
        cashAddress == autoLegacyCashAddress

        where:
        tuple << CASH_ADDRESS_BY_LEGACY_FORMAT_TEST
        params = TestNet3Params.INSTANCE
    }

    void testFromCashToLegacyAddressMain() {
        when:
        def cashAddress = CashAddress.fromFormattedAddress(params, tuple.cashAddress)
        def autoLegacyCashAddress = CashAddress.from(tuple.legacy)
        then:
        cashAddress.toBase58() == tuple.legacy
        cashAddress.toCashAddress() == tuple.cashAddress
        cashAddress.parameters == params
        cashAddress == autoLegacyCashAddress

        where:
        tuple << CASH_ADDRESS_BY_LEGACY_FORMAT_MAIN
        params = MainNetParams.INSTANCE
    }

    void testFromCashToLegacyAddressTest() {
        when:
        def cashAddress = CashAddress.fromFormattedAddress(params, tuple.cashAddress)
        def autoLegacyCashAddress = CashAddress.from(tuple.legacy)
        then:
        cashAddress.toBase58() == tuple.legacy
        cashAddress.toCashAddress() == tuple.cashAddress
        cashAddress.parameters == params
        cashAddress == autoLegacyCashAddress

        where:
        tuple << CASH_ADDRESS_BY_LEGACY_FORMAT_TEST
        params = TestNet3Params.INSTANCE
    }

    void getNetwork() {
        setup:
        def mainNetAddress = "qqfx3wcg8ts09mt5l3zey06wenapyfqq2qrcyj5x0s"
        def testNetAddress = "qph2v4mkxjgdqgmlyjx6njmey0ftrxlnggt9t0a6zy"

        expect:
        CashAddress.getParametersFromAddress(mainNetAddress) == MainNetParams.INSTANCE
        CashAddress.getParametersFromAddress("bitcoincash:$mainNetAddress") == MainNetParams.INSTANCE
        CashAddress.getParametersFromAddress(testNetAddress) == TestNet3Params.INSTANCE
        CashAddress.getParametersFromAddress("bchtest:$testNetAddress") == TestNet3Params.INSTANCE

        and: "wrong prefix"
        CashAddress.getParametersFromAddress("bchtest:$mainNetAddress") == null
        CashAddress.getParametersFromAddress("bitcoincash:$testNetAddress") == null

    }



    private List<AddressTuple> loadCashAddressResource(resource) {
        String text = CashAddressSpec.class.getResource(resource).getText()
        List list = []
        text.eachLine { line ->
            String[] components = line.split(",");
            list << new AddressTuple(legacy:components[0], cashAddress: components[1])
        }
        list
    }

    private byte[] byteArrayOf(int... args) {
        args.toList() as byte[]
    }

    static class AddressTuple {
        String legacy
        String cashAddress
    }


}