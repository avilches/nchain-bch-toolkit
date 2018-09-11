/*
 * Copyright 2011 Google Inc.
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

package com.nchain.shared

import com.nchain.tools.ByteUtils
import com.nchain.tools.hexStringToByteArray
import com.nchain.tools.toHex
import java.io.IOException
import java.io.Serializable
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import kotlin.experimental.and

/**
 * A Sha256Hash just wraps a byte[] so that equals and hashcode work correctly, allowing it to be used as keys in a
 * map. It also checks that the length is correct and provides a bit more type safety.
 */
class Sha256Hash : Serializable, Comparable<Sha256Hash> {

    /**
     * Returns the internal byte array, without defensively copying. Therefore do NOT modify the returned array.
     */
    val bytes: ByteArray

    /**
     * Returns a reversed copy of the internal byte array.
     */
    val reversedBytes: ByteArray
        get() = ByteUtils.reverseBytes(bytes)

    /**
     * Use [.wrap] instead.
     */
    private constructor(rawHashBytes: ByteArray) {
        check(rawHashBytes.size == LENGTH)
        this.bytes = rawHashBytes
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        return if (o != null && o is Sha256Hash)
            Arrays.equals(bytes, o.bytes)
        else
            false
    }

    override fun hashCode(): Int {
        // Use the last 4 bytes, not the first 4 which are often zeros in Bitcoin.
        return Arrays.hashCode(bytes)
    }

    override fun toString(): String {
        return bytes.toHex()
    }

    /**
     * Returns the bytes interpreted as a positive integer.
     */
    fun toBigInteger(): BigInteger {
        return BigInteger(1, bytes)
    }

    override fun compareTo(other: Sha256Hash): Int {
        for (i in LENGTH - 1 downTo 0) {
            val thisByte = this.bytes[i] and 0xff.toByte()
            val otherByte = other.bytes[i] and 0xff.toByte()
            if (thisByte > otherByte)
                return 1
            if (thisByte < otherByte)
                return -1
        }
        return 0
    }

    companion object {
        val LENGTH = 32 // bytes

        @JvmStatic
        val ZERO_HASH = wrap(ByteArray(LENGTH))

        /**
         * Creates a new instance that wraps the given hash value.
         *
         * @param rawHashBytes the raw hash bytes to wrap
         * @return a new instance
         * @throws IllegalArgumentException if the given array length is not exactly 32
         */
        // the constructor will be made private in the future
        @JvmStatic fun wrap(rawHashBytes: ByteArray): Sha256Hash {
            return Sha256Hash(rawHashBytes)
        }

        /**
         * Creates a new instance that wraps the given hash value (represented as a hex string).
         *
         * @param hexString a hash value represented as a hex string
         * @return a new instance
         * @throws IllegalArgumentException if the given string is not a valid
         * hex string, or if it does not represent exactly 32 bytes
         */
        @JvmStatic fun wrap(hexString: String): Sha256Hash {
            check(hexString.length == LENGTH * 2)
            return wrap(hexString.hexStringToByteArray())
        }



        /**
         * Creates a new instance that wraps the given hash value, but with byte order reversed.
         *
         * @param rawHashBytes the raw hash bytes to wrap
         * @return a new instance
         * @throws IllegalArgumentException if the given array length is not exactly 32
         */
        // the constructor will be made private in the future
        @JvmStatic fun wrapReversed(rawHashBytes: ByteArray): Sha256Hash {
            return wrap(ByteUtils.reverseBytes(rawHashBytes))
        }

        /**
         * Creates a new instance containing the calculated (one-time) hash of the given bytes.
         *
         * @param contents the bytes on which the hash value is calculated
         * @return a new instance containing the calculated (one-time) hash
         */
        @JvmStatic fun of(contents: ByteArray): Sha256Hash {
            return wrap(hash(contents))
        }

        /** Use [.twiceOf] instead: this old name is ambiguous.  */
//        @Deprecated("")
//        fun createDouble(contents: ByteArray): Sha256Hash {
//            return twiceOf(contents)
//        }

        /**
         * Creates a new instance containing the hash of the calculated hash of the given bytes.
         *
         * @param contents the bytes on which the hash value is calculated
         * @return a new instance containing the calculated (two-time) hash
         */
        @JvmStatic fun twiceOf(contents: ByteArray): Sha256Hash {
            return wrap(hashTwice(contents))
        }

        /**
         * Creates a new instance containing the calculated (one-time) hash of the given file's contents.
         *
         * The file contents are read fully into memory, so this method should only be used with small files.
         *
         * @param file the file on which the hash value is calculated
         * @return a new instance containing the calculated (one-time) hash
         * @throws IOException if an error occurs while reading the file
         */
/*
        @Throws(IOException::class)
        fun of(file: File): Sha256Hash {
            val `in` = FileInputStream(file)
            try {
                return of(ByteStreams.toByteArray(`in`))
            } finally {
                `in`.close()
            }
        }
*/

        /**
         * Returns a new SHA-256 MessageDigest instance.
         *
         * This is a convenience method which wraps the checked
         * exception that can never occur with a RuntimeException.
         *
         * @return a new SHA-256 MessageDigest instance
         */
        fun newDigest(): MessageDigest {
            try {
                return MessageDigest.getInstance("SHA-256")
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException(e)  // Can't happen.
            }

        }

        /**
         * Calculates the SHA-256 hash of the given byte range.
         *
         * @param input the array containing the bytes to hash
         * @param offset the offset within the array of the bytes to hash
         * @param length the number of bytes to hash
         * @return the hash (in big-endian order)
         */
        @JvmOverloads
        @JvmStatic fun hash(input: ByteArray, offset: Int = 0, length: Int = input.size): ByteArray {
            val digest = newDigest()
            digest.update(input, offset, length)
            return digest.digest()
        }

        /**
         * Calculates the SHA-256 hash of the given byte range,
         * and then hashes the resulting hash again.
         *
         * @param input the array containing the bytes to hash
         * @param offset the offset within the array of the bytes to hash
         * @param length the number of bytes to hash
         * @return the double-hash (in big-endian order)
         */
        @JvmOverloads
        @JvmStatic fun hashTwice(input: ByteArray?, offset: Int = 0, length: Int = input!!.size): ByteArray {
            val digest = newDigest()
            digest.update(input, offset, length)
            return digest.digest(digest.digest())
        }

        /**
         * Calculates the hash of hash on the given byte ranges. This is equivalent to
         * concatenating the two ranges and then passing the result to [.hashTwice].
         */
        @JvmStatic fun hashTwice(input1: ByteArray, offset1: Int, length1: Int,
                      input2: ByteArray, offset2: Int, length2: Int): ByteArray {
            val digest = newDigest()
            digest.update(input1, offset1, length1)
            digest.update(input2, offset2, length2)
            return digest.digest(digest.digest())
        }
    }
}
