package com.nchain.address

import spock.lang.Specification

/*
 * @author Alberto Vilches
 * @date 18/07/2018
 */

class Base58Spec extends Specification {

    def 'encode and decode'() {
        expect:
        Base58.encode(bytes) == base58
        Base58.encode(Base58.decode(base58)) == base58
        and:
        bytes == Base58.decode(base58)
        bytes == Base58.decode(Base58.encode(bytes))
        where:
        base58            | bytes
        ""                | [] as byte[]
        "1"               | [0] as byte[]
        "1111111"         | [0, 0, 0, 0, 0, 0, 0] as byte[]
        "16Ho7Hs"         | BigInteger.valueOf(3471844090L).toByteArray()
        "JxF12TrwUP45BMd" | "Hello World".getBytes()
    }

    def 'decode checked ok'() {
        when:
        Base58.decodeChecked(base58)
        then:
        notThrown(AddressFormatException)
        where:
        base58 << ["4stwEBjT6FYyVV", "93VYUMzRG9DdbRP72uQXjaWibbQwygnvaCu9DumcqDjGybD864T"]
    }

    def 'decode checked error by wron checksum'() {
        when:
        Base58.decodeChecked(base58)
        then:
        thrown(AddressFormatException)
        where:
        base58 << ["4s", "4stwEBjT6FYyVW"]
    }

    def 'errors'() {
        when:
        Base58.decode("This isn't valid base58")
        then:
        thrown(AddressFormatException)
    }
}