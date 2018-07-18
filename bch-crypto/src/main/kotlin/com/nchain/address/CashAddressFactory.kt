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
package com.nchain.address


import com.nchain.address.CashAddressHelper.ConvertBits
import com.nchain.bitcoinkt.params.NetworkParameters
import com.nchain.key.WrongNetworkException

import com.nchain.params.Networks

object CashAddressFactory {

    @Throws(AddressFormatException::class)
    fun fromFormattedAddress(params: NetworkParameters, addr: String): CashAddress {
        val cashAddressValidator = CashAddressValidator.create()

        val (prefix, payload) = CashAddressHelper.decodeCashAddress(addr, params.cashAddrPrefix)

        cashAddressValidator.checkValidPrefix(params, prefix)
        cashAddressValidator.checkNonEmptyPayload(payload)

        val extraBits = (payload.size * 5 % 8).toByte()
        cashAddressValidator.checkAllowedPadding(extraBits)

        val last = payload[payload.size - 1]
        val mask = ((1 shl extraBits.toInt()) - 1).toByte()
        cashAddressValidator.checkNonZeroPadding(last, mask)

        val data = ByteArray(payload.size * 5 / 8)
        ConvertBits(data, payload, 5, 8, false)

        val versionByte = data[0]
        cashAddressValidator.checkFirstBitIsZero(versionByte)

        val hashSize = calculateHashSizeFromVersionByte(versionByte)
        cashAddressValidator.checkDataLength(data, hashSize)

        val result = ByteArray(data.size - 1)
        System.arraycopy(data, 1, result, 0, data.size - 1)
        val type = getAddressTypeFromVersionByte(versionByte)

        return CashAddress(params, type, result)
    }

    @Throws(AddressFormatException::class)
    private fun getAddressTypeFromVersionByte(versionByte: Byte): CashAddress.CashAddressType {
        when (versionByte.toInt() shr 3 and 0x1f) {
            0 -> return CashAddress.CashAddressType.PubKey
            1 -> return CashAddress.CashAddressType.Script
            else -> throw AddressFormatException("Unknown Type")
        }
    }

    private fun calculateHashSizeFromVersionByte(versionByte: Byte): Int {
        var hash_size = 20 + 4 * (versionByte.toInt() and 0x03)
        if (versionByte.toInt() and 0x04 != 0) {
            hash_size *= 2
        }
        return hash_size
    }


}
