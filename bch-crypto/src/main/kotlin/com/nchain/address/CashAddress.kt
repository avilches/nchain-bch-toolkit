/*
 * Copyright 2018 Hash Engineering
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
package com.nchain.address

import com.nchain.bitcoinkt.params.NetworkParameters
import com.nchain.key.WrongNetworkException
import com.nchain.params.Networks

class CashAddress : VersionedChecksummedBytes {

    @Transient
    var parameters: NetworkParameters? = null
        private set

    /** The (big endian) 20 byte hash that is the core of a Bitcoin address.  */
    val hash160: ByteArray
        get() = bytes

    var addressType: CashAddressType? = null
        private set

    val isP2SHAddress: Boolean
        get() = addressType == CashAddressType.Script

    val isP2PKHAddress: Boolean
        get() = addressType == CashAddressType.PubKey


    enum class CashAddressType private constructor(private val value: Int) {
        PubKey(0),
        Script(1);

        internal fun getValue(): Byte {
            return value.toByte()
        }
    }

    internal constructor(params: NetworkParameters, addressType: CashAddressType, hash: ByteArray) : super(getLegacyVersion(params, addressType), hash) {
        this.parameters = params
        this.addressType = addressType
    }

    internal constructor(params: NetworkParameters, version: Int, hash160: ByteArray) : super(version, hash160) {
        this.parameters = params
        this.addressType = getType(params, version)
    }

    fun toCashAddress(): String {
        return CashAddressHelper.encodeCashAddress(parameters!!.cashAddrPrefix,
                CashAddressHelper.packAddressData(hash160, addressType!!.getValue()))
    }
    override fun toString(): String {
        return toCashAddress()
    }

//    @Throws(CloneNotSupportedException::class)
//    override fun clone(): CashAddress {
//        return super.clone()
//    }

    companion object {

        @Throws(AddressFormatException::class)
        fun fromHash160(params: NetworkParameters, hash160: ByteArray): CashAddress {
            return fromHash160(params, params.addressHeader, hash160)
        }

        @Throws(AddressFormatException::class)
        fun fromHash160(params: NetworkParameters, version: Int, hash160: ByteArray): CashAddress {
            return CashAddress(params, version, hash160)

        }
        @Throws(AddressFormatException::class)
        fun fromBase58(params: NetworkParameters?, base58: String): CashAddress {
            val parsed = VersionedChecksummedBytes(base58)
            var addressParams: NetworkParameters? = null
            if (params != null) {
                if (!isAcceptableVersion(params, parsed.version)) {
                    throw WrongNetworkException(parsed.version, params.acceptableAddressCodes)
                }
                addressParams = params
            } else {
                for (p in Networks.get()) {
                    if (isAcceptableVersion(p, parsed.version)) {
                        addressParams = p
                        break
                    }
                }
                if (addressParams == null) {
                    throw AddressFormatException("No network found for $base58")
                }
            }
            return CashAddress(addressParams, parsed.version, parsed.bytes)
        }

        fun fromP2PubKey(params: NetworkParameters, hash160: ByteArray): CashAddress {
            return CashAddress(params, CashAddress.CashAddressType.PubKey, hash160)
        }

        fun fromP2SHHash(params: NetworkParameters, hash160: ByteArray): CashAddress {
            return CashAddress(params, CashAddress.CashAddressType.Script, hash160)
        }

    //    fun fromP2SHScript(params: NetworkParameters, scriptPubKey: Script): CashAddress {
    //        checkArgument(scriptPubKey.isPayToScriptHash, "Not a P2SH script")
    //        return fromP2SHHash(params, scriptPubKey.pubKeyHash!!)
    //    }


        /**
         * Given an address, examines the version byte and attempts to find a matching NetworkParameters. If you aren't sure
         * which network the address is intended for (eg, it was provided by a user), you can use this to decide if it is
         * compatible with the current wallet.
         * @return a NetworkParameters of the address
         * @throws AddressFormatException if the string wasn't of a known version
         */
        @Throws(AddressFormatException::class)
        fun getParametersFromAddress(address: String): NetworkParameters? {
            try {
                return fromBase58(null, address).parameters
            } catch (e: WrongNetworkException) {
                throw RuntimeException(e)  // Cannot happen.
            }

        }


        /**
         * Check if a given address version is valid given the NetworkParameters.
         */
        internal fun isAcceptableVersion(params: NetworkParameters, version: Int): Boolean {
            for (v in params.acceptableAddressCodes!!) {
                if (version == v) {
                    return true
                }
            }
            return false
        }


        internal fun getLegacyVersion(params: NetworkParameters, type: CashAddressType): Int {
            when (type) {
                CashAddressType.PubKey -> return params.addressHeader
                CashAddressType.Script -> return params.p2SHHeader
            }
            throw AddressFormatException("Invalid Cash address type: " + type.getValue())
        }

        internal fun getType(params: NetworkParameters, version: Int): CashAddressType {
            if (version == params.addressHeader) {
                return CashAddressType.PubKey
            } else if (version == params.p2SHHeader) {
                return CashAddressType.Script
            }
            throw AddressFormatException("Invalid Cash address version: $version")
        }
    }
}
