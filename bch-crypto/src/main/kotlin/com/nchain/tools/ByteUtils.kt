/*
 * Copyright 2011 Google Inc.
 * Copyright 2014 Andreas Schildbach
 * Copyright 2018 nChain Ltd
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

package com.nchain.tools

import com.nchain.shared.Sha256Hash
import org.spongycastle.crypto.digests.RIPEMD160Digest
import java.io.*
import java.math.BigInteger
import java.nio.charset.Charset
import java.util.*

/**
 * A collection of various utility methods that are helpful for working with the Bitcoin protocol.
 * To enable debug logging from the library, run with -Dbitcoinj.logging=true on your command line.
 */
object ByteUtils {

    // zero length arrays are immutable so we can save some object allocation by reusing the same instance.
    @JvmStatic val EMPTY_BYTE_ARRAY = ByteArray(0)

    // 00000001, 00000010, 00000100, 00001000, ...
    private val bitMask = intArrayOf(0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80)

    /**
     * The regular [java.math.BigInteger.toByteArray] method isn't quite what we often need: it appends a
     * leading zero to indicate that the number is positive and may need padding.
     *
     * @param b the integer to format into a byte array
     * @param numBytes the desired size of the resulting byte array
     * @return numBytes byte long array.
     */
    @JvmStatic fun bigIntegerToBytes(b: BigInteger, numBytes: Int): ByteArray {
        val bytes = ByteArray(numBytes)
        val biBytes = b.toByteArray()
        val start = if (biBytes.size == numBytes + 1) 1 else 0
        val length = Math.min(biBytes.size, numBytes)
        System.arraycopy(biBytes, start, bytes, numBytes - length, length)
        return bytes
    }

    @JvmStatic fun uint32ToByteArrayBE(v: Long): ByteArray {
        val out = ByteArray(4)
        uint32ToByteArrayBE(v, out, 0)
        return out
    }

    @JvmStatic fun uint32ToByteArrayBE(v: Long, out: ByteArray, offset: Int) {
        out[offset] = (0xFF and (v shr 24).toInt()).toByte()
        out[offset + 1] = (0xFF and (v shr 16).toInt()).toByte()
        out[offset + 2] = (0xFF and (v shr 8).toInt()).toByte()
        out[offset + 3] = (0xFF and v.toInt()).toByte()
    }

    @JvmStatic fun uint32ToByteArrayLE(v: Long): ByteArray {
        val out = ByteArray(4)
        uint32ToByteArrayLE(v, out, 0)
        return out
    }

    @JvmStatic fun uint32ToByteArrayLE(v: Long, out: ByteArray, offset: Int) {
        out[offset] = (0xFF and v.toInt()).toByte()
        out[offset + 1] = (0xFF and (v shr 8).toInt()).toByte()
        out[offset + 2] = (0xFF and (v shr 16).toInt()).toByte()
        out[offset + 3] = (0xFF and (v shr 24).toInt()).toByte()
    }

    @JvmStatic fun uint64ToByteArrayLE(v: Long): ByteArray {
        val out = ByteArray(8)
        uint64ToByteArrayLE(v, out, 0)
        return out
    }

    @JvmStatic fun uint64ToByteArrayLE(v: Long, out: ByteArray, offset: Int) {
        out[offset] = (0xFF and v.toInt()).toByte()
        out[offset + 1] = (0xFF and (v shr 8).toInt()).toByte()
        out[offset + 2] = (0xFF and (v shr 16).toInt()).toByte()
        out[offset + 3] = (0xFF and (v shr 24).toInt()).toByte()
        out[offset + 4] = (0xFF and (v shr 32).toInt()).toByte()
        out[offset + 5] = (0xFF and (v shr 40).toInt()).toByte()
        out[offset + 6] = (0xFF and (v shr 48).toInt()).toByte()
        out[offset + 7] = (0xFF and (v shr 56).toInt()).toByte()
    }

    @Throws(IOException::class)
    @JvmStatic fun uint32ToByteStreamLE(v: Long, stream: OutputStream) {
        stream.write((0xFF and v.toInt()))
        stream.write((0xFF and (v shr 8).toInt()))
        stream.write((0xFF and (v shr 16).toInt()))
        stream.write((0xFF and (v shr 24).toInt()))
    }

    @Throws(IOException::class)
    @JvmStatic fun int64ToByteStreamLE(v: Long, stream: OutputStream) {
        stream.write((0xFF and v.toInt()))
        stream.write((0xFF and (v shr 8).toInt()))
        stream.write((0xFF and (v shr 16).toInt()))
        stream.write((0xFF and (v shr 24).toInt()))
        stream.write((0xFF and (v shr 32).toInt()))
        stream.write((0xFF and (v shr 40).toInt()))
        stream.write((0xFF and (v shr 48).toInt()))
        stream.write((0xFF and (v shr 56).toInt()))
    }

    @Throws(IOException::class)
    @JvmStatic fun uint64ToByteStreamLE(`val`: BigInteger, stream: OutputStream) {
        var bytes = `val`.toByteArray()
        if (bytes.size > 8) {
            throw RuntimeException("Input too large to encode into a uint64")
        }
        bytes = reverseBytes(bytes)
        stream.write(bytes)
        if (bytes.size < 8) {
            for (i in 0 until 8 - bytes.size)
                stream.write(0)
        }
    }

    /**
     * Work around lack of unsigned types in Java.
     */
//    @JvmStatic fun isLessThanUnsigned(n1: Long, n2: Long): Boolean {
//        return UnsignedLongs.compare(n1, n2) < 0
//    }

    /**
     * Work around lack of unsigned types in Java.
     */
//    @JvmStatic fun isLessThanOrEqualToUnsigned(n1: Long, n2: Long): Boolean {
//        return UnsignedLongs.compare(n1, n2) <= 0
//    }

    /**
     * Returns a copy of the given byte array in reverse order.
     */
    @JvmStatic fun reverseBytes(bytes: ByteArray): ByteArray {
        // We could use the XOR trick here but it's easier to understand if we don't. If we find this is really a
        // performance issue the matter can be revisited.
        val buf = ByteArray(bytes.size)
        for (i in bytes.indices)
            buf[i] = bytes[bytes.size - 1 - i]
        return buf
    }

    /**
     * Returns a copy of the given byte array with the bytes of each double-word (4 bytes) reversed.
     *
     * @param bytes length must be divisible by 4.
     * @param trimLength trim output to this length.  If positive, must be divisible by 4.
     */
    @JvmStatic fun reverseDwordBytes(bytes: ByteArray, trimLength: Int): ByteArray {
        check(bytes.size % 4 == 0)
        check(trimLength < 0 || trimLength % 4 == 0)

        val rev = ByteArray(if (trimLength >= 0 && bytes.size > trimLength) trimLength else bytes.size)

        var i = 0
        while (i < rev.size) {
            System.arraycopy(bytes, i, rev, i, 4)
            for (j in 0..3) {
                rev[i + j] = bytes[i + 3 - j]
            }
            i += 4
        }
        return rev
    }

    @JvmStatic fun readUint32(inputStream: InputStream): Long {
        val bytes = ByteArray(4)
        inputStream.read(bytes)
        return readUint32(bytes, 0)
    }

    /** Parse 4 bytes from the byte array (starting at the offset) as unsigned 32-bit integer in little endian format.  */
    @JvmStatic fun readUint32(bytes: ByteArray, offset: Int): Long {
        return  ((bytes[offset].toLong() and 0xffL)) or
                ((bytes[offset + 1].toLong() and 0xffL) shl 8) or
                ((bytes[offset + 2].toLong() and 0xffL) shl 16) or
                ((bytes[offset + 3].toLong() and 0xffL) shl 24)
    }

    @JvmStatic fun readInt64(inputStream: InputStream): Long {
        val bytes = ByteArray(8)
        inputStream.read(bytes)
        return readInt64(bytes, 0)
    }

    /** Parse 8 bytes from the byte array (starting at the offset) as signed 64-bit integer in little endian format.  */
    @JvmStatic fun readInt64(bytes: ByteArray, offset: Int): Long {
        return  ((bytes[offset].toLong() and 0xffL)) or
                ((bytes[offset + 1].toLong() and 0xffL) shl 8) or
                ((bytes[offset + 2].toLong() and 0xffL) shl 16) or
                ((bytes[offset + 3].toLong() and 0xffL) shl 24) or
                ((bytes[offset + 4].toLong() and 0xffL) shl 32) or
                ((bytes[offset + 5].toLong() and 0xffL) shl 40) or
                ((bytes[offset + 6].toLong() and 0xffL) shl 48) or
                ((bytes[offset + 7].toLong() and 0xffL) shl 56)
    }

    @JvmStatic fun readUint32BE(inputStream: InputStream): Long {
        val bytes = ByteArray(4)
        inputStream.read(bytes)
        return readUint32BE(bytes, 0)
    }

    /** Parse 4 bytes from the byte array (starting at the offset) as unsigned 32-bit integer in big endian format.  */
    @JvmStatic fun readUint32BE(bytes: ByteArray, offset: Int): Long {
        return  ((bytes[offset].toLong() and 0xffL) shl 24) or
                ((bytes[offset + 1].toLong() and 0xffL) shl 16) or
                ((bytes[offset + 2].toLong() and 0xffL) shl 8)  or
                ((bytes[offset + 3].toLong() and 0xffL))
    }

    @JvmStatic fun readUint16BE(inputStream: InputStream): Int {
        val bytes = ByteArray(2)
        inputStream.read(bytes)
        return readUint16BE(bytes, 0)
    }

    /** Parse 2 bytes from the byte array (starting at the offset) as unsigned 16-bit integer in big endian format.  */
    @JvmStatic fun readUint16BE(bytes: ByteArray, offset: Int): Int {
        return bytes[offset].toInt() and 0xff shl 8 or (bytes[offset + 1].toInt() and 0xff)
    }


    @JvmStatic fun sha256hash160(inputStream: InputStream, length: Int): ByteArray {
        val bytes = ByteArray(length)
        inputStream.read(bytes)
        return sha256hash160(bytes)
    }

    /**
     * Calculates RIPEMD160(SHA256(input)). This is used in Address calculations.
     */
    @JvmStatic fun sha256hash160(input: ByteArray): ByteArray {
        val sha256 = Sha256Hash.hash(input)
        val digest = RIPEMD160Digest()
        digest.update(sha256, 0, sha256.size)
        val out = ByteArray(20)
        digest.doFinal(out, 0)
        return out
    }

    /**
     * MPI encoded numbers are produced by the OpenSSL BN_bn2mpi function. They consist of
     * a 4 byte big endian length field, followed by the stated number of bytes representing
     * the number in big endian format (with a sign bit).
     * @param hasLength can be set to false if the given array is missing the 4 byte length field
     */
    @JvmStatic fun decodeMPI(mpi: ByteArray, hasLength: Boolean): BigInteger {
        val buf: ByteArray
        if (hasLength) {
            val length = readUint32BE(mpi, 0).toInt()
            buf = ByteArray(length)
            System.arraycopy(mpi, 4, buf, 0, length)
        } else
            buf = mpi
        if (buf.size == 0)
            return BigInteger.ZERO
        val isNegative = buf[0].toInt() and 0x80 == 0x80
        if (isNegative)
            buf[0] = (buf[0].toInt() and 0x7f).toByte()
        val result = BigInteger(buf)
        return if (isNegative) result.negate() else result
    }

    /**
     * MPI encoded numbers are produced by the OpenSSL BN_bn2mpi function. They consist of
     * a 4 byte big endian length field, followed by the stated number of bytes representing
     * the number in big endian format (with a sign bit).
     * @param includeLength indicates whether the 4 byte length field should be included
     */
    @JvmStatic fun encodeMPI(value: BigInteger, includeLength: Boolean): ByteArray {
        var value = value
        if (value == BigInteger.ZERO) {
            return if (!includeLength)
                byteArrayOf()
            else
                byteArrayOf(0x00, 0x00, 0x00, 0x00)
        }
        val isNegative = value.signum() < 0
        if (isNegative)
            value = value.negate()
        val array = value.toByteArray()
        var length = array.size
        if (array[0].toInt() and 0x80 == 0x80)
            length++
        if (includeLength) {
            val result = ByteArray(length + 4)
            System.arraycopy(array, 0, result, length - array.size + 3, array.size)
            uint32ToByteArrayBE(length.toLong(), result, 0)
            if (isNegative)
                result[4] = (result[4].toInt() or 0x80).toByte()
            return result
        } else {
            val result: ByteArray
            if (length != array.size) {
                result = ByteArray(length)
                System.arraycopy(array, 0, result, 1, array.size)
            } else
                result = array
            if (isNegative)
                result[0] = (result[0].toInt() or 0x80).toByte()
            return result
        }
    }

    /**
     * Returns a minimally encoded encoded version of the data. That is, a version will pass the check
     * in checkMinimallyEncodedLE(byte[] bytesLE).
     *
     * If the data is already minimally encoded the original byte array will be returned.
     *
     * inspired by: https://reviews.bitcoinabc.org/D1219
     *
     * @param dataLE
     * @return
     */
    @JvmStatic fun minimallyEncodeLE(dataLE: ByteArray): ByteArray {
        var dataLE = dataLE

        if (dataLE.size == 0) {
            return dataLE
        }

        // If the last byte is not 0x00 or 0x80, we are minimally encoded.
        val last = dataLE[dataLE.size - 1].toInt()
        if (last and 0x7f != 0) {
            return dataLE
        }

        // If the script is one byte long, then we have a zero, which encodes as an
        // empty array.
        if (dataLE.size == 1) {
            return EMPTY_BYTE_ARRAY
        }

        // If the next byte has it sign bit set, then we are minimaly encoded.
        if ((dataLE[dataLE.size - 2].toInt() and 0x80) != 0) {
            return dataLE
        }

        //we might modify the array so clone it
        dataLE = dataLE.clone()

        // We are not minimally encoded, we need to figure out how much to trim.
        // we are using i - 1 indexes here as we want to ignore the last byte (first byte in BE)
        var i = dataLE.size - 1
        while (i > 0) {
            // We found a non zero byte, time to encode.
            if (dataLE[i - 1].toInt() != 0) {
                if ((dataLE[i - 1].toInt() and 0x80) != 0) {
                    // We found a byte with it's sign bit set so we need one more
                    // byte.
                    dataLE[i++] = last.toByte()
                } else {
                    // the sign bit is clear, we can use it.
                    // add the sign bit from the last byte
                    dataLE[i - 1] = (dataLE[i - 1].toInt() or last).toByte()
                }

                return Arrays.copyOf(dataLE, i)
            }
            i--
        }

        // If we the whole thing is zeros, then we have a zero.
        return EMPTY_BYTE_ARRAY
    }

    /**
     * checks that LE encoded number is minimally represented.  That is that there are no leading zero bytes except in
     * the case: if there's more than one byte and the most significant bit of the second-most-significant-byte is set it
     * would conflict with the sign bit.
     * @param bytesLE
     * @return
     */
    @JvmStatic fun checkMinimallyEncodedLE(bytesLE: ByteArray, maxNumSize: Int): Boolean {

        if (bytesLE.size > maxNumSize) {
            return false
        }

        if (bytesLE.size > 0) {
            // Check that the number is encoded with the minimum possible number
            // of bytes.
            //
            // If the most-significant-byte - excluding the sign bit - is zero
            // then we're not minimal. Note how this test also rejects the
            // negative-zero encoding, 0x80.
            if ((bytesLE[bytesLE.size - 1].toInt() and 0x7f) == 0) {
                // One exception: if there's more than one byte and the most
                // significant bit of the second-most-significant-byte is set it
                // would conflict with the sign bit. An example of this case is
                // +-255, which encode to 0xff00 and 0xff80 respectively.
                // (big-endian).
                if (bytesLE.size <= 1 || (bytesLE[bytesLE.size - 2].toInt() and 0x80) == 0) {
                    return false
                }
            }
        }

        return true
    }

    /**
     *
     * The "compact" format is a representation of a whole number N using an unsigned 32 bit number similar to a
     * floating point format. The most significant 8 bits are the unsigned exponent of base 256. This exponent can
     * be thought of as "number of bytes of N". The lower 23 bits are the mantissa. Bit number 24 (0x800000) represents
     * the sign of N. Therefore, N = (-1^sign) * mantissa * 256^(exponent-3).
     *
     *
     * Satoshi's original implementation used BN_bn2mpi() and BN_mpi2bn(). MPI uses the most significant bit of the
     * first byte as sign. Thus 0x1234560000 is compact 0x05123456 and 0xc0de000000 is compact 0x0600c0de. Compact
     * 0x05c0de00 would be -0x40de000000.
     *
     *
     * Bitcoin only uses this "compact" format for encoding difficulty targets, which are unsigned 256bit quantities.
     * Thus, all the complexities of the sign bit and using base 256 are probably an implementation accident.
     */
    @JvmStatic fun decodeCompactBits(compact: Long): BigInteger {
        val size = (compact shr 24).toInt() and 0xFF
        val bytes = ByteArray(4 + size)
        bytes[3] = size.toByte()
        if (size >= 1) bytes[4] = (compact shr 16 and 0xFF).toByte()
        if (size >= 2) bytes[5] = (compact shr 8 and 0xFF).toByte()
        if (size >= 3) bytes[6] = (compact and 0xFF).toByte()
        return decodeMPI(bytes, true)
    }

    /**
     * @see ByteUtils.decodeCompactBits
     */
    @JvmStatic fun encodeCompactBits(value: BigInteger): Long {
        var result: Long
        var size = value.toByteArray().size
        if (size <= 3)
            result = value.toLong() shl 8 * (3 - size)
        else
            result = value.shiftRight(8 * (size - 3)).toLong()
        // The 0x00800000 bit denotes the sign.
        // Thus, if it is already set, divide the mantissa by 256 and increase the exponent.
        if (result and 0x00800000L != 0L) {
            result = result shr 8
            size++
        }
        result = result or (size shl 24).toLong()
        result = result or (if (value.signum() == -1) 0x00800000 else 0).toLong()
        return result
    }


    @JvmStatic fun copyOf(`in`: ByteArray, length: Int): ByteArray {
        val out = ByteArray(length)
        System.arraycopy(`in`, 0, out, 0, Math.min(length, `in`.size))
        return out
    }

    /**
     * Creates a copy of bytes and appends b to the end of it
     */
    @JvmStatic fun appendByte(bytes: ByteArray, b: Byte): ByteArray {
        val result = Arrays.copyOf(bytes, bytes.size + 1)
        result[result.size - 1] = b
        return result
    }

    fun concat(vararg arrays: ByteArray): ByteArray {
        var length = 0
        for (array in arrays) {
            length += array.size
        }
        val result = ByteArray(length)
        var pos = 0
        for (array in arrays) {
            System.arraycopy(array, 0, result, pos, array.size)
            pos += array.size
        }
        return result
    }


    /**
     * Constructs a new String by decoding the given bytes using the specified charset.
     *
     *
     * This is a convenience method which wraps the checked exception with a RuntimeException.
     * The exception can never occur given the charsets
     * US-ASCII, ISO-8859-1, UTF-8, UTF-16, UTF-16LE or UTF-16BE.
     *
     * @param bytes the bytes to be decoded into characters
     * @param charsetName the name of a supported [charset][java.nio.charset.Charset]
     * @return the decoded String
     */
    @JvmStatic fun toString(bytes: ByteArray, charsetName: String): String {
        try {
            return String(bytes, Charset.forName(charsetName))
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }
    }

    /**
     * Encodes the given string into a sequence of bytes using the named charset.
     *
     *
     * This is a convenience method which wraps the checked exception with a RuntimeException.
     * The exception can never occur given the charsets
     * US-ASCII, ISO-8859-1, UTF-8, UTF-16, UTF-16LE or UTF-16BE.
     *
     * @param str the string to encode into bytes
     * @param charsetName the name of a supported [charset][java.nio.charset.Charset]
     * @return the encoded bytes
     */
    @JvmStatic fun toBytes(str: CharSequence, charsetName: String): ByteArray {
        try {
            return str.toString().toByteArray(charset(charsetName))
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }
    }

    /** Checks if the given bit is set in data, using little endian (not the same as Java native big endian)  */
    @JvmStatic fun checkBitLE(data: ByteArray, index: Int): Boolean {
        return data[index ushr 3].toInt() and bitMask[7 and index] != 0
    }

    /** Sets the given bit in data to one, using little endian (not the same as Java native big endian)  */
    @JvmStatic fun setBitLE(data: ByteArray, index: Int) {
        data[index ushr 3 ] = (data[index ushr 3].toInt() or bitMask[7 and index]).toByte()
    }

    @JvmStatic fun toByteArray(vararg values: Int): ByteArray {
        val conv = ByteArray(values.size)
        for (i in values.indices) {
            conv[i] = (values[i] and 0xFF).toByte()
        }
        return conv
    }

    @JvmStatic fun deserialize(o:ByteArray):Any {
        return ObjectInputStream(ByteArrayInputStream(o)).readObject()
    }

    @JvmStatic fun serialize(o:Any):ByteArray {
        val os = ByteArrayOutputStream()
        ObjectOutputStream(os).writeObject(o)
        return os.toByteArray()
    }

    @JvmStatic fun serializeRound(o:Any):Any {
        return deserialize(serialize(o))
    }

}