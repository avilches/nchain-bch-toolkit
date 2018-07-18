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


/**
 * Created by Hash Engineering on 1/19/2018.
 */
object CashAddressHelper {

    /**
     * The cashaddr character set for encoding.
     */
    internal val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"

    /**
     * The cashaddr character set for decoding.
     */
    internal val CHARSET_REV = byteArrayOf(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 15, -1, 10, 17, 21, 20, 26, 30, 7, 5, -1, -1, -1, -1, -1, -1, -1, 29, -1, 24, 13, 25, 9, 8, 23, -1, 18, 22, 31, 27, 19, -1, 1, 0, 3, 16, 11, 28, 12, 14, 6, 4, 2, -1, -1, -1, -1, -1, -1, 29, -1, 24, 13, 25, 9, 8, 23, -1, 18, 22, 31, 27, 19, -1, 1, 0, 3, 16, 11, 28, 12, 14, 6, 4, 2, -1, -1, -1, -1, -1)

    /**
     * Concatenate two byte arrays.
     */
    internal fun concatenateByteArrays(x: ByteArray, y: ByteArray): ByteArray {
        val z = ByteArray(x.size + y.size)
        System.arraycopy(x, 0, z, 0, x.size)
        System.arraycopy(y, 0, z, x.size, y.size)
        return z
    }

    /**
     * This function will compute what 8 5-bit values to XOR into the last 8 input
     * values, in order to make the checksum 0. These 8 values are packed together
     * in a single 40-bit integer. The higher bits correspond to earlier values.
     */

    internal fun computePolyMod(v: ByteArray): Long {
        /**
         * The input is interpreted as a list of coefficients of a polynomial over F
         * = GF(32), with an implicit 1 in front. If the input is [v0,v1,v2,v3,v4],
         * that polynomial is v(x) = 1*x^5 + v0*x^4 + v1*x^3 + v2*x^2 + v3*x + v4.
         * The implicit 1 guarantees that [v0,v1,v2,...] has a distinct checksum
         * from [0,v0,v1,v2,...].
         *
         * The output is a 40-bit integer whose 5-bit groups are the coefficients of
         * the remainder of v(x) mod g(x), where g(x) is the cashaddr generator, x^8
         * + {19}*x^7 + {3}*x^6 + {25}*x^5 + {11}*x^4 + {25}*x^3 + {3}*x^2 + {19}*x
         * + {1}. g(x) is chosen in such a way that the resulting code is a BCH
         * code, guaranteeing detection of up to 4 errors within a window of 1025
         * characters. Among the various possible BCH codes, one was selected to in
         * fact guarantee detection of up to 5 errors within a window of 160
         * characters and 6 erros within a window of 126 characters. In addition,
         * the code guarantee the detection of a burst of up to 8 errors.
         *
         * Note that the coefficients are elements of GF(32), here represented as
         * decimal numbers between {}. In this finite field, addition is just XOR of
         * the corresponding numbers. For example, {27} + {13} = {27 ^ 13} = {22}.
         * Multiplication is more complicated, and requires treating the bits of
         * values themselves as coefficients of a polynomial over a smaller field,
         * GF(2), and multiplying those polynomials mod a^5 + a^3 + 1. For example,
         * {5} * {26} = (a^2 + 1) * (a^4 + a^3 + a) = (a^4 + a^3 + a) * a^2 + (a^4 +
         * a^3 + a) = a^6 + a^5 + a^4 + a = a^3 + 1 (mod a^5 + a^3 + 1) = {9}.
         *
         * During the course of the loop below, `c` contains the bitpacked
         * coefficients of the polynomial constructed from just the values of v that
         * were processed so far, mod g(x). In the above example, `c` initially
         * corresponds to 1 mod (x), and after processing 2 inputs of v, it
         * corresponds to x^2 + v0*x + v1 mod g(x). As 1 mod g(x) = 1, that is the
         * starting value for `c`.
         */
        var c: Long = 1
        for (d in v) {
            /**
             * We want to update `c` to correspond to a polynomial with one extra
             * term. If the initial value of `c` consists of the coefficients of
             * c(x) = f(x) mod g(x), we modify it to correspond to
             * c'(x) = (f(x) * x + d) mod g(x), where d is the next input to
             * process.
             *
             * Simplifying:
             * c'(x) = (f(x) * x + d) mod g(x)
             * ((f(x) mod g(x)) * x + d) mod g(x)
             * (c(x) * x + d) mod g(x)
             * If c(x) = c0*x^5 + c1*x^4 + c2*x^3 + c3*x^2 + c4*x + c5, we want to
             * compute
             * c'(x) = (c0*x^5 + c1*x^4 + c2*x^3 + c3*x^2 + c4*x + c5) * x + d
             * mod g(x)
             * = c0*x^6 + c1*x^5 + c2*x^4 + c3*x^3 + c4*x^2 + c5*x + d
             * mod g(x)
             * = c0*(x^6 mod g(x)) + c1*x^5 + c2*x^4 + c3*x^3 + c4*x^2 +
             * c5*x + d
             * If we call (x^6 mod g(x)) = k(x), this can be written as
             * c'(x) = (c1*x^5 + c2*x^4 + c3*x^3 + c4*x^2 + c5*x + d) + c0*k(x)
             */

            // First, determine the value of c0:
            val c0:Long = c shr 35

            // Then compute c1*x^5 + c2*x^4 + c3*x^3 + c4*x^2 + c5*x + d:
            c = c and 0x07ffffffffL shl 5 xor d.toLong()

            // Finally, for each set bit n in c0, conditionally add {2^n}k(x):
            if (c0 and 0x01L != 0L) {
                // k(x) = {19}*x^7 + {3}*x^6 + {25}*x^5 + {11}*x^4 + {25}*x^3 +
                //        {3}*x^2 + {19}*x + {1}
                c = c xor 0x98f2bc8e61L
            }

            if (c0 and 0x02L != 0L) {
                // {2}k(x) = {15}*x^7 + {6}*x^6 + {27}*x^5 + {22}*x^4 + {27}*x^3 +
                //           {6}*x^2 + {15}*x + {2}
                c = c xor 0x79b76d99e2L
            }

            if (c0 and 0x04L != 0L) {
                // {4}k(x) = {30}*x^7 + {12}*x^6 + {31}*x^5 + {5}*x^4 + {31}*x^3 +
                //           {12}*x^2 + {30}*x + {4}
                c = c xor 0xf33e5fb3c4L
            }

            if (c0 and 0x08L != 0L) {
                // {8}k(x) = {21}*x^7 + {24}*x^6 + {23}*x^5 + {10}*x^4 + {23}*x^3 +
                //           {24}*x^2 + {21}*x + {8}
                c = c xor 0xae2eabe2a8L
            }

            if (c0 and 0x10L != 0L) {
                // {16}k(x) = {3}*x^7 + {25}*x^6 + {7}*x^5 + {20}*x^4 + {7}*x^3 +
                //            {25}*x^2 + {3}*x + {16}
                c = c xor 0x1e4f43e470L
            }
        }

        /**
         * computePolyMod computes what value to xor into the final values to make the
         * checksum 0. However, if we required that the checksum was 0, it would be
         * the case that appending a 0 to a valid list of values would result in a
         * new valid list. For that reason, cashaddr requires the resulting checksum
         * to be 1 instead.
         */
        return c xor 1
    }

    internal fun toLowerCase(c: Char): Char {
        // ASCII black magic.
        return (c.toInt() or 0x20).toChar()
    }

    /**
     * Expand the address prefix for the checksum computation.
     */
    internal fun expandPrefix(prefix: String): ByteArray {
        val ret = ByteArray(prefix.length + 1)

        val prefixBytes = prefix.toByteArray()

        for (i in 0 until prefix.length) {
            ret[i] = (prefixBytes[i].toInt() and 0x1f).toByte()
        }

        ret[prefix.length] = 0
        return ret
    }

    internal fun verifyChecksum(prefix: String, payload: ByteArray): Boolean {
        return computePolyMod(concatenateByteArrays(expandPrefix(prefix), payload)) == 0L
    }

    internal fun createChecksum(prefix: String, payload: ByteArray): ByteArray {
        val enc = concatenateByteArrays(expandPrefix(prefix), payload)
        // Append 8 zeroes.
        val enc2 = ByteArray(enc.size + 8)
        System.arraycopy(enc, 0, enc2, 0, enc.size)
        // Determine what to XOR into those 8 zeroes.
        val mod = computePolyMod(enc2)
        val ret = ByteArray(8)
        for (i in 0..7) {
            // Convert the 5-bit groups in mod to checksum values.
            ret[i] = (mod shr 5 * (7 - i) and 0x1f).toByte()
        }

        return ret
    }

    @JvmStatic fun encodeCashAddress(prefix: String, payload: ByteArray): String {
        val checksum = createChecksum(prefix, payload)
        val combined = concatenateByteArrays(payload, checksum)
        val ret = StringBuilder("$prefix:")

        //ret.setLength(ret.length() + combined.length);
        for (c in combined) {
            ret.append(CHARSET.get(c.toInt()))
        }

        return ret.toString()
    }

    @JvmStatic fun decodeCashAddress(str: String, defaultPrefix: String): Pair {
        // Go over the string and do some sanity checks.
        var lower = false
        var upper = false
        var hasNumber = false
        var prefixSize = 0
        for (i in 0 until str.length) {
            val c = str[i]
            if (c >= 'a' && c <= 'z') {
                lower = true
                continue
            }

            if (c >= 'A' && c <= 'Z') {
                upper = true
                continue
            }

            if (c >= '0' && c <= '9') {
                // We cannot have numbers in the prefix.
                hasNumber = true
                continue
            }

            if (c == ':') {
                // The separator cannot be the first character, cannot have number
                // and there must not be 2 separators.
                if (hasNumber || i == 0 || prefixSize != 0) {
                    throw AddressFormatException("cashaddr:  $str: The separator cannot be the first character, cannot have number and there must not be 2 separators")
                }

                prefixSize = i
                continue
            }

            // We have an unexpected character.
            throw AddressFormatException("cashaddr:  $str: Unexpected character at pos $i")
        }

        // We can't have both upper case and lowercase.
        if (upper && lower) {
            throw AddressFormatException("cashaddr:  $str: Cannot contain both upper and lower case letters")
        }

        // Get the prefix.
        val prefix: StringBuilder
        if (prefixSize == 0) {
            prefix = StringBuilder(defaultPrefix)
        } else {
            prefix = StringBuilder(str.substring(0, prefixSize).toLowerCase())

            // Now add the ':' in the size.
            prefixSize++
        }

        // Decode values.
        val valuesSize = str.length - prefixSize
        val values = ByteArray(valuesSize)
        for (i in 0 until valuesSize) {
            val c = str[i + prefixSize]
            // We have an invalid char in there.
            if (c.toInt() > 127 || CHARSET_REV[c.toInt()].toInt() == -1) {
                throw AddressFormatException("cashaddr:  $str: Unexpected character at pos $i")
            }

            values[i] = CHARSET_REV[c.toInt()]
        }

        // Verify the checksum.
        if (!verifyChecksum(prefix.toString(), values)) {
            throw AddressFormatException("cashaddr:  $str: Invalid Checksum ")
        }

        val result = ByteArray(values.size - 8)
        System.arraycopy(values, 0, result, 0, values.size - 8)
        return Pair(prefix.toString(), result)
    }

    @JvmStatic fun packAddressData(payload: ByteArray, type: Byte): ByteArray {
        var version_byte = (type.toInt() shl 3).toByte()
        val size = payload.size
        var encoded_size: Byte = 0
        when (size * 8) {
            160 -> encoded_size = 0
            192 -> encoded_size = 1
            224 -> encoded_size = 2
            256 -> encoded_size = 3
            320 -> encoded_size = 4
            384 -> encoded_size = 5
            448 -> encoded_size = 6
            512 -> encoded_size = 7
            else -> throw AddressFormatException("Error packing cashaddr: invalid address length")
        }
        version_byte = (version_byte.toInt() or encoded_size.toInt()).toByte()
        val data = ByteArray(1 + payload.size)
        data[0] = version_byte
        System.arraycopy(payload, 0, data, 1, payload.size)

        // Reserve the number of bytes required for a 5-bit packed version of a
        // hash, with version byte.  Add half a byte(4) so integer math provides
        // the next multiple-of-5 that would fit all the data.

        val converted = ByteArray(((size + 1) * 8 + 4) / 5)
        ConvertBits(converted, data, 8, 5, true)

        return converted
    }

    /**
     * Convert from one power-of-2 number base to another.
     *
     *
     * If padding is enabled, this always return true. If not, then it returns true
     * of all the bits of the input are encoded in the output.
     */
    internal fun ConvertBits(out: ByteArray, it: ByteArray, frombits: Int, tobits: Int, pad: Boolean): Boolean {
        var acc = 0
        var bits = 0
        val maxv = (1 shl tobits) - 1
        val max_acc = (1 shl frombits + tobits - 1) - 1
        var x = 0
        for (i in it.indices) {
            acc = acc shl frombits or (it[i].toInt() and 0xff) and max_acc
            bits += frombits
            while (bits >= tobits) {
                bits -= tobits
                out[x] = (acc shr bits and maxv).toByte()
                ++x
            }
        }

        // We have remaining bits to encode but do not pad.
        if (!pad && bits != 0) {
            return false
        }

        // We have remaining bits to encode so we do pad.
        if (pad && bits != 0) {
            out[x] = (acc shl tobits - bits and maxv).toByte()
            ++x
        }

        return true
    }
}

data class Pair(val key:String, val value:ByteArray)