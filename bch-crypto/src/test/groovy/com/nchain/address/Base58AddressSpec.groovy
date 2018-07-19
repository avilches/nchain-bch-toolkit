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

package com.nchain.address

import com.nchain.params.MainNetParams
import com.nchain.params.TestNet3Params
import com.nchain.key.WrongNetworkException
import com.nchain.tools.ByteUtils
import com.nchain.tools.HEX
import org.junit.Assert
import spock.lang.Specification

class Base58AddressSpec extends Specification {

    void 'serialization'() {
        setup:
        CashAddress testAddress = CashAddress.fromBase58(network, address)

        when:
        def testAddressSerialized = (ByteUtils.serializeRound(testAddress) as VersionedChecksummedBytes)

        then:
        testAddressSerialized == testAddress
        Assert.assertNotSame(testAddress, testAddressSerialized)

        when:
        def os = new ByteArrayOutputStream()
        new ObjectOutputStream(os).writeObject(testAddress)
        def testAddressCopy = new ObjectInputStream(
                new ByteArrayInputStream(os.toByteArray())).readObject() as VersionedChecksummedBytes

        then: 'binary serializaction should create identical objects'
        testAddress == testAddressCopy
        Assert.assertNotSame(testAddress, testAddressCopy)

        where:
        network                 | address
        TestNet3Params.INSTANCE | "n4eA2nbYqErp7H6jebchxAN59DmNpksexv"
        MainNetParams.INSTANCE  | "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL"
    }

    void 'hexadecimal tests and networks'() {
        expect: 'roundtrip'
        CashAddress.fromBase58(network, base58).toBase58() == base58

        when: 'create base58 addresses from hex bytes'
        def a = CashAddress.fromHash160(network, HEX.hexToBytes(hex))

        then:
        a.toBase58() == base58
        !a.isP2SHAddress()

        and: 'decoding addresses from base58 to hex'
        a == CashAddress.fromBase58(network, base58)

        then:
        HEX.bytesToHex(a.hash160) == hex

        where:
        network                 | hex                                        | base58
        TestNet3Params.INSTANCE | "fda79a24e50ff70ff42f7d89585da5bd19d9e5cc" | "n4eA2nbYqErp7H6jebchxAN59DmNpksexv"
        MainNetParams.INSTANCE  | "4a22c3c4cbb31e4d03b15550636762bda0baf85a" | "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL"

    }

    void 'error paths'() {
        when: 'decode garbage'
        CashAddress.fromBase58(TestNet3Params.INSTANCE, addresses)

        then:
        thrown(AddressFormatException)

        where:
        addresses << ["INVALID ADDRESS", ""]
    }

    void 'network mismatch'() {
        when: 'main net address using the testnet parameters'
        CashAddress.fromBase58(TestNet3Params.INSTANCE, "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL")

        then:
        def ex = thrown(WrongNetworkException)
        ((WrongNetworkException)ex).verCode == MainNetParams.INSTANCE.addressHeader
        ((WrongNetworkException)ex).acceptableVersions == TestNet3Params.INSTANCE.acceptableAddressCodes
    }

    void 'networks'() {
        expect:
        CashAddress.getParametersFromAddress(base58) == network
        CashAddress.from(base58).parameters == network

        where:
        network                 | base58
        TestNet3Params.INSTANCE | "n4eA2nbYqErp7H6jebchxAN59DmNpksexv"
        MainNetParams.INSTANCE  | "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL"
    }

    def p2shAddress() {
        // Test that we can construct P2SH addresses

        when:
        def mainNetP2SHAddress = CashAddress.fromBase58(MainNetParams.INSTANCE, "35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU")

        then:
        mainNetP2SHAddress.version == MainNetParams.INSTANCE.p2SHHeader
        mainNetP2SHAddress.isP2SHAddress()

        when:
        def testNetP2SHAddress = CashAddress.fromBase58(TestNet3Params.INSTANCE, "2MuVSxtfivPKJe93EC1Tb9UhJtGhsoWEHCe")

        then:
        testNetP2SHAddress.version == TestNet3Params.INSTANCE.p2SHHeader

        when: "Test that we can determine what network a P2SH address belongs to"
        def mainNetParams = CashAddress.getParametersFromAddress("35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU")

        then:
        MainNetParams.INSTANCE == mainNetParams

        when:
        def testNetParams = CashAddress.getParametersFromAddress("2MuVSxtfivPKJe93EC1Tb9UhJtGhsoWEHCe")

        then:
        TestNet3Params.INSTANCE == testNetParams

        when: "Test that we can convert them from hashes"
        def hex = HEX.hexToBytes("2ac4b0b501117cc8119c5797b519538d4942e90e")
        def a = CashAddress.fromP2SHHash(MainNetParams.INSTANCE, hex)

        then:
        a.toBase58() == "35b9vsyH1KoFT5a5KtrKusaCcPLkiSo1tU"

        when:
        def b = CashAddress.fromP2SHHash(TestNet3Params.INSTANCE, HEX.hexToBytes("18a0e827269b5211eb51a4af1b2fa69333efa722"))

        then:
        b.toBase58() == "2MuVSxtfivPKJe93EC1Tb9UhJtGhsoWEHCe"

        // TODO: vilches
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

    def cloning() {
        when:
        def a = CashAddress.from(address)
        def b = a.clone()

        then:
        a == b
        a.parameters == b.parameters
        a.hashCode() == b.hashCode()
        Assert.assertNotSame(a, b)
        a.compareTo(b) == 0


        where:
        network                 | address
        TestNet3Params.INSTANCE | "n4eA2nbYqErp7H6jebchxAN59DmNpksexv"
        MainNetParams.INSTANCE  | "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL"
    }


    def comparisonLessThan() {
        when:
        def a = CashAddress.fromBase58(MainNetParams.INSTANCE, "1Dorian4RoXcnBv9hnQ4Y2C1an6NJ4UrjX")
        def b = CashAddress.fromBase58(MainNetParams.INSTANCE, "1EXoDusjGwvnjZUyKkxZ4UHEf77z6A5S4P")

        then:
        a.compareTo(b) < 0
        b.compareTo(a) > 0
    }

    def comparisonBytesVsString() {
        // TODO: To properly test this we need a much larger data set
        when:
        def a = CashAddress.fromBase58(MainNetParams.INSTANCE, "1Dorian4RoXcnBv9hnQ4Y2C1an6NJ4UrjX")
        def b = CashAddress.fromBase58(MainNetParams.INSTANCE, "1EXoDusjGwvnjZUyKkxZ4UHEf77z6A5S4P")

        def resultBytes = a.compareTo(b)
        def resultsString = a.toBase58().compareTo(b.toBase58())

        then:
        resultBytes < 0
        resultsString < 0
    }

}
