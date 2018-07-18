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

class CashAddress : LegacyAddress {

    var addressType: CashAddressType? = null
        private set

    override val isP2SHAddress: Boolean
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

    internal constructor(params: NetworkParameters, addressType: CashAddressType, hash: ByteArray) : super(params, getLegacyVersion(params, addressType), hash) {
        this.addressType = addressType
    }

    internal constructor(params: NetworkParameters, version: Int, hash160: ByteArray) : super(params, version, hash160) {
        this.addressType = getType(params, version)
    }

    override fun toCashAddress(): String {
        return CashAddressHelper.encodeCashAddress(parameters!!.cashAddrPrefix,
                CashAddressHelper.packAddressData(hash160, addressType!!.getValue()))
    }
    override fun toString(): String {
        return toCashAddress()
    }

    @Throws(CloneNotSupportedException::class)
    override fun clone(): LegacyAddress {
        return super.clone()
    }

    companion object {

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
