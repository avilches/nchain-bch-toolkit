package com.nchain.address

import com.nchain.params.MainNetParams
import com.nchain.params.TestNet3Params
import com.nchain.tools.HEX
import org.junit.Assert
import spock.lang.Specification


/*
 * @author Alberto Vilches
 * @date 18/07/2018
 */

class VersionedChecksummedBytesSpec extends Specification {

    void stringification() {
        when:
        def a = new VersionedChecksummedBytes(TestNet3Params.INSTANCE.addressHeader, HEX.hexToBytes("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc"))
        then:
        "n4eA2nbYqErp7H6jebchxAN59DmNpksexv" == a.toString()

        when:
        def b = new VersionedChecksummedBytes(MainNetParams.INSTANCE.addressHeader, HEX.hexToBytes("4a22c3c4cbb31e4d03b15550636762bda0baf85a"))
        then:
        "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL" == b.toString()
    }

    void cloning() {
        when:
        def a = new VersionedChecksummedBytes(TestNet3Params.INSTANCE.addressHeader, HEX.hexToBytes("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc"))
        def b = a.clone()

        then:
        a == b
        a.hashCode() == b.hashCode()
        a.compareTo(b) == 0
        Assert.assertNotSame(a, b)
    }

}