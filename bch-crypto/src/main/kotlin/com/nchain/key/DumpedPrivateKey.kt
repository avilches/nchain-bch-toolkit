/*
 * Copyright 2011 Google Inc.
 * Copyright 2015 Andreas Schildbach
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

package com.nchain.key

import com.nchain.address.AddressFormatException
import com.nchain.address.VersionedChecksummedBytes
import com.nchain.params.NetworkParameters
import java.util.*

/**
 * Parses and generates private keys in the form used by the Bitcoin "dumpprivkey" command. This is the private key
 * bytes with a header byte and 4 checksum bytes at the end. If there are 33 private key bytes instead of 32, then
 * the last byte is a discriminator value for the compressed pubkey.
 */
class DumpedPrivateKey : VersionedChecksummedBytes {
    private var compressed: Boolean = false

    /**
     * Returns an ECKey created from this encoded private key.
     */
    val key: ECKey
        get() {
            val key = ECKey.fromPrivate(bytes)
            return if (compressed) key else key.decompress()
        }

    internal constructor(version: Int, bytes: ByteArray, compressed: Boolean) : super(version, bytes) {
        this.compressed = compressed
    }

    @Deprecated("Use {@link #fromBase58(NetworkParameters, String)} ")
    @Throws(AddressFormatException::class)
    constructor(params: NetworkParameters?, encoded: String) : super(encoded) {
        if (params != null && version != params.dumpedPrivateKeyHeader)
            throw WrongNetworkException(version, intArrayOf(params.dumpedPrivateKeyHeader))
        if (bytes.size == 33 && bytes[32].toInt() == 1) {
            compressed = true
            bytes = Arrays.copyOf(bytes, 32)  // Chop off the additional marker byte.
        } else if (bytes.size == 32) {
            compressed = false
        } else {
            throw AddressFormatException("Wrong number of bytes for a private key, not 32 or 33")
        }
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        return if (o != null && o is DumpedPrivateKey)
            super.equals(o) && compressed == o.compressed
        else
            false
    }

    override fun hashCode(): Int {
        return Objects.hash(super.hashCode(), compressed)
    }

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): DumpedPrivateKey {
        return DumpedPrivateKey(version, bytes, compressed)
    }

    companion object {

        /**
         * Construct a private key from its Base58 representation.
         * @param params
         * The expected NetworkParameters or null if you don't want validation.
         * @param base58
         * The textual form of the private key.
         * @throws AddressFormatException
         * if the given base58 doesn't parse or the checksum is invalid
         * @throws WrongNetworkException
         * if the given private key is valid but for a different chain (eg testnet vs mainnet)
         */
        @Throws(AddressFormatException::class)
        @JvmStatic @JvmOverloads fun fromBase58(params: NetworkParameters? = null, base58: String): DumpedPrivateKey {
            return DumpedPrivateKey(params, base58)
        }

        @JvmStatic fun createFromPrivKey(params: NetworkParameters, keyBytes: ByteArray, compressed: Boolean):DumpedPrivateKey {
            return DumpedPrivateKey(params.dumpedPrivateKeyHeader, checkSize(keyBytes, compressed), compressed)
        }

        private fun checkSize(keyBytes: ByteArray, compressed: Boolean): ByteArray {
            check(keyBytes.size == 32, {"Private keys must be 32 bytes"})
            if (!compressed) {
                return keyBytes
            } else {
                // Keys that have compressed public components have an extra 1 byte on the end in dumped form.
                val bytes = ByteArray(33)
                System.arraycopy(keyBytes, 0, bytes, 0, 32)
                bytes[32] = 1
                return bytes
            }
        }
    }
}
