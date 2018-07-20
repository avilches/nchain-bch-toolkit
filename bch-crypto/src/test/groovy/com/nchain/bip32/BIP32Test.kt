/*
 * Copyright 2013 Matija Mazi.
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

package com.nchain.bip32

import com.nchain.address.Base58
import com.nchain.params.MainNetParams
import com.nchain.tools.HEX
import com.nchain.tools.loggerFor
import org.junit.Test

import java.util.Arrays
import java.util.Locale

import org.junit.Assert.assertEquals

/**
 * A test with test vectors as per BIP 32 spec: https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki#Test_Vectors
 */
class BIP32Test {

    internal var tvs = arrayOf(HDWTestVector(
            "000102030405060708090a0b0c0d0e0f",
            "xprv9s21ZrQH143K3QTDL4LXw2F7HEK3wJUD2nW2nRk4stbPy6cq3jPPqjiChkVvvNKmPGJxWUtg6LnF5kejMRNNU3TGtRBeJgk33yuGBxrMPHi",
            "xpub661MyMwAqRbcFtXgS5sYJABqqG9YLmC4Q1Rdap9gSE8NqtwybGhePY2gZ29ESFjqJoCu1Rupje8YtGqsefD265TMg7usUDFdp6W1EGMcet8",
            Arrays.asList<HDWTestVector.DerivedTestCase>(
                    HDWTestVector.DerivedTestCase(
                            "Test1 m/0H",
                            arrayOf(ChildNumber(0, true)),
                            "xprv9uHRZZhk6KAJC1avXpDAp4MDc3sQKNxDiPvvkX8Br5ngLNv1TxvUxt4cV1rGL5hj6KCesnDYUhd7oWgT11eZG7XnxHrnYeSvkzY7d2bhkJ7",
                            "xpub68Gmy5EdvgibQVfPdqkBBCHxA5htiqg55crXYuXoQRKfDBFA1WEjWgP6LHhwBZeNK1VTsfTFUHCdrfp1bgwQ9xv5ski8PX9rL2dZXvgGDnw"
                    ),
                    HDWTestVector.DerivedTestCase(
                            "Test1 m/0H/1",
                            arrayOf(ChildNumber(0, true), ChildNumber(1, false)),
                            "xprv9wTYmMFdV23N2TdNG573QoEsfRrWKQgWeibmLntzniatZvR9BmLnvSxqu53Kw1UmYPxLgboyZQaXwTCg8MSY3H2EU4pWcQDnRnrVA1xe8fs",
                            "xpub6ASuArnXKPbfEwhqN6e3mwBcDTgzisQN1wXN9BJcM47sSikHjJf3UFHKkNAWbWMiGj7Wf5uMash7SyYq527Hqck2AxYysAA7xmALppuCkwQ"
                    ),
                    HDWTestVector.DerivedTestCase(
                            "Test1 m/0H/1/2H",
                            arrayOf(ChildNumber(0, true), ChildNumber(1, false), ChildNumber(2, true)),
                            "xprv9z4pot5VBttmtdRTWfWQmoH1taj2axGVzFqSb8C9xaxKymcFzXBDptWmT7FwuEzG3ryjH4ktypQSAewRiNMjANTtpgP4mLTj34bhnZX7UiM",
                            "xpub6D4BDPcP2GT577Vvch3R8wDkScZWzQzMMUm3PWbmWvVJrZwQY4VUNgqFJPMM3No2dFDFGTsxxpG5uJh7n7epu4trkrX7x7DogT5Uv6fcLW5"
                    ),
                    HDWTestVector.DerivedTestCase(
                            "Test1 m/0H/1/2H/2",
                            arrayOf(ChildNumber(0, true), ChildNumber(1, false), ChildNumber(2, true), ChildNumber(2, false)),
                            "xprvA2JDeKCSNNZky6uBCviVfJSKyQ1mDYahRjijr5idH2WwLsEd4Hsb2Tyh8RfQMuPh7f7RtyzTtdrbdqqsunu5Mm3wDvUAKRHSC34sJ7in334",
                            "xpub6FHa3pjLCk84BayeJxFW2SP4XRrFd1JYnxeLeU8EqN3vDfZmbqBqaGJAyiLjTAwm6ZLRQUMv1ZACTj37sR62cfN7fe5JnJ7dh8zL4fiyLHV"
                    ),
                    HDWTestVector.DerivedTestCase(
                            "Test1 m/0H/1/2H/2/1000000000",
                            arrayOf(ChildNumber(0, true), ChildNumber(1, false), ChildNumber(2, true), ChildNumber(2, false), ChildNumber(1000000000, false)),
                            "xprvA41z7zogVVwxVSgdKUHDy1SKmdb533PjDz7J6N6mV6uS3ze1ai8FHa8kmHScGpWmj4WggLyQjgPie1rFSruoUihUZREPSL39UNdE3BBDu76",
                            "xpub6H1LXWLaKsWFhvm6RVpEL9P4KfRZSW7abD2ttkWP3SSQvnyA8FSVqNTEcYFgJS2UaFcxupHiYkro49S8yGasTvXEYBVPamhGW6cFJodrTHy"
                    )
            )
    ), HDWTestVector(
            "fffcf9f6f3f0edeae7e4e1dedbd8d5d2cfccc9c6c3c0bdbab7b4b1aeaba8a5a29f9c999693908d8a8784817e7b7875726f6c696663605d5a5754514e4b484542",
            "xprv9s21ZrQH143K31xYSDQpPDxsXRTUcvj2iNHm5NUtrGiGG5e2DtALGdso3pGz6ssrdK4PFmM8NSpSBHNqPqm55Qn3LqFtT2emdEXVYsCzC2U",
            "xpub661MyMwAqRbcFW31YEwpkMuc5THy2PSt5bDMsktWQcFF8syAmRUapSCGu8ED9W6oDMSgv6Zz8idoc4a6mr8BDzTJY47LJhkJ8UB7WEGuduB",
            Arrays.asList<HDWTestVector.DerivedTestCase>(
                    HDWTestVector.DerivedTestCase(
                            "Test2 m/0",
                            arrayOf(ChildNumber(0, false)),
                            "xprv9vHkqa6EV4sPZHYqZznhT2NPtPCjKuDKGY38FBWLvgaDx45zo9WQRUT3dKYnjwih2yJD9mkrocEZXo1ex8G81dwSM1fwqWpWkeS3v86pgKt",
                            "xpub69H7F5d8KSRgmmdJg2KhpAK8SR3DjMwAdkxj3ZuxV27CprR9LgpeyGmXUbC6wb7ERfvrnKZjXoUmmDznezpbZb7ap6r1D3tgFxHmwMkQTPH"
                    ),
                    HDWTestVector.DerivedTestCase(
                            "Test2 m/0/2147483647H",
                            arrayOf(ChildNumber(0, false), ChildNumber(2147483647, true)),
                            "xprv9wSp6B7kry3Vj9m1zSnLvN3xH8RdsPP1Mh7fAaR7aRLcQMKTR2vidYEeEg2mUCTAwCd6vnxVrcjfy2kRgVsFawNzmjuHc2YmYRmagcEPdU9",
                            "xpub6ASAVgeehLbnwdqV6UKMHVzgqAG8Gr6riv3Fxxpj8ksbH9ebxaEyBLZ85ySDhKiLDBrQSARLq1uNRts8RuJiHjaDMBU4Zn9h8LZNnBC5y4a"
                    ),
                    HDWTestVector.DerivedTestCase(
                            "Test2 m/0/2147483647H/1",
                            arrayOf(ChildNumber(0, false), ChildNumber(2147483647, true), ChildNumber(1, false)),
                            "xprv9zFnWC6h2cLgpmSA46vutJzBcfJ8yaJGg8cX1e5StJh45BBciYTRXSd25UEPVuesF9yog62tGAQtHjXajPPdbRCHuWS6T8XA2ECKADdw4Ef",
                            "xpub6DF8uhdarytz3FWdA8TvFSvvAh8dP3283MY7p2V4SeE2wyWmG5mg5EwVvmdMVCQcoNJxGoWaU9DCWh89LojfZ537wTfunKau47EL2dhHKon"
                    ),
                    HDWTestVector.DerivedTestCase(
                            "Test2 m/0/2147483647H/1/2147483646H",
                            arrayOf(ChildNumber(0, false), ChildNumber(2147483647, true), ChildNumber(1, false), ChildNumber(2147483646, true)),
                            "xprvA1RpRA33e1JQ7ifknakTFpgNXPmW2YvmhqLQYMmrj4xJXXWYpDPS3xz7iAxn8L39njGVyuoseXzU6rcxFLJ8HFsTjSyQbLYnMpCqE2VbFWc",
                            "xpub6ERApfZwUNrhLCkDtcHTcxd75RbzS1ed54G1LkBUHQVHQKqhMkhgbmJbZRkrgZw4koxb5JaHWkY4ALHY2grBGRjaDMzQLcgJvLJuZZvRcEL"
                    ),
                    HDWTestVector.DerivedTestCase(
                            "Test2 m/0/2147483647H/1/2147483646H/2",
                            arrayOf(ChildNumber(0, false), ChildNumber(2147483647, true), ChildNumber(1, false), ChildNumber(2147483646, true), ChildNumber(2, false)),
                            "xprvA2nrNbFZABcdryreWet9Ea4LvTJcGsqrMzxHx98MMrotbir7yrKCEXw7nadnHM8Dq38EGfSh6dqA9QWTyefMLEcBYJUuekgW4BYPJcr9E7j",
                            "xpub6FnCn6nSzZAw5Tw7cgR9bi15UV96gLZhjDstkXXxvCLsUXBGXPdSnLFbdpq8p9HmGsApME5hQTZ3emM2rnY5agb9rXpVGyy3bdW6EEgAtqt"
                    )
            )
    ), HDWTestVector(
            "4b381541583be4423346c643850da4b320e46a87ae3d2a4e6da11eba819cd4acba45d239319ac14f863b8d5ab5a0d0c64d2e8a1e7d1457df2e5a3c51c73235be",
            "xprv9s21ZrQH143K25QhxbucbDDuQ4naNntJRi4KUfWT7xo4EKsHt2QJDu7KXp1A3u7Bi1j8ph3EGsZ9Xvz9dGuVrtHHs7pXeTzjuxBrCmmhgC6",
            "xpub661MyMwAqRbcEZVB4dScxMAdx6d4nFc9nvyvH3v4gJL378CSRZiYmhRoP7mBy6gSPSCYk6SzXPTf3ND1cZAceL7SfJ1Z3GC8vBgp2epUt13",
            Arrays.asList<HDWTestVector.DerivedTestCase>(
                    HDWTestVector.DerivedTestCase(
                            "Test3 m/0H",
                            arrayOf(ChildNumber(0, true)),
                            "xprv9uPDJpEQgRQfDcW7BkF7eTya6RPxXeJCqCJGHuCJ4GiRVLzkTXBAJMu2qaMWPrS7AANYqdq6vcBcBUdJCVVFceUvJFjaPdGZ2y9WACViL4L",
                            "xpub68NZiKmJWnxxS6aaHmn81bvJeTESw724CRDs6HbuccFQN9Ku14VQrADWgqbhhTHBaohPX4CjNLf9fq9MYo6oDaPPLPxSb7gwQN3ih19Zm4Y"
                    )
            )
    ))

    @Test
    @Throws(Exception::class)
    fun testVector1() {
        testVector(0)
    }

    @Test
    @Throws(Exception::class)
    fun testVector2() {
        testVector(1)
    }

    @Test
    @Throws(Exception::class)
    fun testVector3() {
        testVector(2)
    }

    private fun testVector(testCase: Int) {
        log.info("=======  Test vector $testCase")
        val tv = tvs[testCase]
        val params = MainNetParams
        val masterPrivateKey = HDKeyDerivation.createMasterPrivateKey(HEX.decode(tv.seed))
        assertEquals(testEncode(tv.priv), testEncode(masterPrivateKey.serializePrivB58(params)))
        assertEquals(testEncode(tv.pub), testEncode(masterPrivateKey.serializePubB58(params)))
        val dh = DeterministicHierarchy(masterPrivateKey)
        for (i in tv.derived.indices) {
            val tc = tv.derived[i]
            log.info(tc.name)
            assertEquals(tc.name, String.format(Locale.US, "Test%d %s", testCase + 1, tc.pathDescription))
            val depth = tc.path.size - 1
            val ehkey = dh.deriveChild(Arrays.asList(*tc.path).subList(0, depth), false, true, tc.path[depth])
            assertEquals(testEncode(tc.priv), testEncode(ehkey.serializePrivB58(params)))
            assertEquals(testEncode(tc.pub), testEncode(ehkey.serializePubB58(params)))
        }
    }

    private fun testEncode(what: String): String {
        return HEX.encode(Base58.decodeChecked(what))
    }

    internal class HDWTestVector(val seed: String, val priv: String, val pub: String, val derived: List<DerivedTestCase>) {

        internal class DerivedTestCase(val name: String, val path: Array<ChildNumber>, val priv: String, val pub: String) {

            val pathDescription: String
                get() = "m/" + path.map {it.toString() }.joinToString("/")
        }
    }

    companion object {
        private val log = loggerFor(BIP32Test::class.java)
    }
}
