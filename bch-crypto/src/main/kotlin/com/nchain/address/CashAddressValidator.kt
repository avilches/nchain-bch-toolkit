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

import com.nchain.params.NetworkParameters

class CashAddressValidator {

    @Throws(AddressFormatException::class)
    fun checkValidPrefix(params: NetworkParameters, prefix: String) {
        if (prefix != params.cashAddrPrefix) {
            throw AddressFormatException("Invalid prefix for network: " +
                    prefix + " != " + params.cashAddrPrefix + " (expected)")
        }
    }

    @Throws(AddressFormatException::class)
    fun checkNonEmptyPayload(payload: ByteArray) {
        if (payload.size == 0) {
            throw AddressFormatException("No payload")
        }
    }

    @Throws(AddressFormatException::class)
    fun checkAllowedPadding(extraBits: Byte) {
        if (extraBits >= 5) {
            throw AddressFormatException("More than allowed padding")
        }
    }

    fun checkNonZeroPadding(last: Byte, mask: Byte) {
        if (last.toInt() and mask.toInt() != 0) {
            throw AddressFormatException("Nonzero bytes ")
        }
    }

    fun checkFirstBitIsZero(versionByte: Byte) {
        if (versionByte.toInt() and 0x80 != 0) {
            throw AddressFormatException("First bit is reserved")
        }
    }

    fun checkDataLength(data: ByteArray, hashSize: Int) {
        if (data.size != hashSize + 1) {
            throw AddressFormatException("Data length " + data.size + " != hash size " + hashSize)
        }
    }

    companion object {

        fun create(): CashAddressValidator {
            return CashAddressValidator()
        }
    }

}
