package com.nchain.shared

import com.nchain.tools.ByteUtils
import com.nchain.tools.HEX
import spock.lang.Specification


/*
 * @author Alberto Vilches
 * @date 19/07/2018
 */

class Sha256HashSpec extends Specification {

    void test() {

        expect:
        Sha256Hash.of(i).bytes == HEX.hexToBytes(o)
        Sha256Hash.of(i).reversedBytes == ByteUtils.reverseBytes(Sha256Hash.of(i).bytes)
        Sha256Hash.of(i).toString() == o
        Sha256Hash.of(i) == Sha256Hash.of(i)
        Sha256Hash.of(i).equals(Sha256Hash.of(i))
        Sha256Hash.of(i).hashCode() == Sha256Hash.of(i).hashCode()

        Sha256Hash.twiceOf(i).bytes == Sha256Hash.hash(Sha256Hash.hash(i))
        Sha256Hash.hashTwice(i) == Sha256Hash.hash(Sha256Hash.hash(i))

        where:
        i                          | o
        "BITCOIN".bytes            | "885a386bc23f3974c5336e4194d23b5b3317c63c642fdafaeb82ecb7286edab6"
        "abc".bytes                | "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        [] as byte[]               | "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        ByteUtils.EMPTY_BYTE_ARRAY | "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

    }

}