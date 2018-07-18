/*
 * Copyright 2011 Thilo Planz
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

package com.nchain.key

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigInteger
import java.util.ArrayList

import com.nchain.tools.HEX
import com.nchain.tools.ByteUtils
import org.junit.Test

import org.junit.Assert.*

class UtilsTest {

    @Test
    fun bigIntegerToBytesTest() {
        assertArrayEquals(ByteArray(0), ByteUtils.bigIntegerToBytes(BigInteger("0"), 0))
        assertArrayEquals(ByteArray(0), ByteUtils.bigIntegerToBytes(BigInteger("10"), 0))
        assertArrayEquals(ByteArray(0), ByteUtils.bigIntegerToBytes(BigInteger("1212110"), 0))

        assertArrayEquals(ByteUtils.toByteArray(0, 1, -117, 111), ByteUtils.bigIntegerToBytes(BigInteger("101231"), 4))
        assertArrayEquals(ByteUtils.toByteArray(1, -117, 111), ByteUtils.bigIntegerToBytes(BigInteger("101231"), 3))
        assertArrayEquals(ByteUtils.toByteArray(-117, 111), ByteUtils.bigIntegerToBytes(BigInteger("101231"), 2))
        assertArrayEquals(ByteUtils.toByteArray(1), ByteUtils.bigIntegerToBytes(BigInteger("101231"), 1))
    }

    @Test
    fun uint32_64ToByteArrayBE_LeTest() {
        assertArrayEquals(ByteUtils.toByteArray(0, 0, 0, -1), ByteUtils.uint32ToByteArrayBE(255))
        assertArrayEquals(ByteUtils.toByteArray(-1, -1, -1, 1), ByteUtils.uint32ToByteArrayBE(-255))

        assertArrayEquals(ByteUtils.toByteArray(-1, 0, 0, 0), ByteUtils.uint32ToByteArrayLE(255))
        assertArrayEquals(ByteUtils.toByteArray(1, -1, -1, -1), ByteUtils.uint32ToByteArrayLE(-255))
        assertArrayEquals(ByteUtils.toByteArray(-1, 0, 0, 0, 0, 0, 0, 0), ByteUtils.uint64ToByteArrayLE(255))
        assertArrayEquals(ByteUtils.toByteArray(1, -1, -1, -1, -1, -1, -1, -1), ByteUtils.uint64ToByteArrayLE(-255))

        assertArrayEquals(ByteUtils.toByteArray(0, 3, -27, 23), ByteUtils.uint32ToByteArrayBE(255255))
        assertArrayEquals(ByteUtils.toByteArray(-1, -4, 26, -23), ByteUtils.uint32ToByteArrayBE(-255255))

        assertArrayEquals(ByteUtils.toByteArray(23, -27, 3, 0), ByteUtils.uint32ToByteArrayLE(255255))
        assertArrayEquals(ByteUtils.toByteArray(-23, 26, -4, -1), ByteUtils.uint32ToByteArrayLE(-255255))

        assertArrayEquals(ByteUtils.toByteArray(23, -27, 3, 0, 0, 0, 0, 0), ByteUtils.uint64ToByteArrayLE(255255))
        assertArrayEquals(ByteUtils.toByteArray(-23, 26, -4, -1, -1, -1, -1, -1), ByteUtils.uint64ToByteArrayLE(-255255))
        assertArrayEquals(ByteUtils.toByteArray(-1, -1, -1, -1, -1, -1, -1, 127), ByteUtils.uint64ToByteArrayLE(java.lang.Long.MAX_VALUE))
        assertArrayEquals(ByteUtils.toByteArray(0, 0, 0, 0, 0, 0, 0, -128), ByteUtils.uint64ToByteArrayLE(java.lang.Long.MIN_VALUE))
    }

    @Test
    @Throws(IOException::class)
    fun uint32_64ToByteArrayOutputStreamLeTest() {
        assertArrayEquals(ByteUtils.toByteArray(-1, 0, 0, 0), outputStreamUint32LEBytesToArray(255))
        assertArrayEquals(ByteUtils.toByteArray(-1, 0, 0, 0, 0, 0, 0, 0), outputStreamInt64LEBytesToArray(255))
        assertArrayEquals(ByteUtils.toByteArray(-1, 0, 0, 0, 0, 0, 0, 0), outputStreamUint64LEBytesToArray(BigInteger("255")))

        assertArrayEquals(ByteUtils.toByteArray(23, -27, 3, 0), outputStreamUint32LEBytesToArray(255255))
        assertArrayEquals(ByteUtils.toByteArray(23, -27, 3, 0, 0, 0, 0, 0), outputStreamInt64LEBytesToArray(255255))
        assertArrayEquals(ByteUtils.toByteArray(23, -27, 3, 0, 0, 0, 0, 0), outputStreamUint64LEBytesToArray(BigInteger("255255")))

        assertArrayEquals(ByteUtils.toByteArray(-1, -1, -1, -1, -1, -1, -1, 127), outputStreamInt64LEBytesToArray(java.lang.Long.MAX_VALUE))
        assertArrayEquals(ByteUtils.toByteArray(-1, -1, -1, -1, -1, -1, -1, 127), outputStreamUint64LEBytesToArray(BigInteger("" + java.lang.Long.MAX_VALUE)))
        assertArrayEquals(ByteUtils.toByteArray(0, 0, 0, 0, 0, 0, 0, -128), outputStreamInt64LEBytesToArray(java.lang.Long.MIN_VALUE))
        assertArrayEquals(ByteUtils.toByteArray(0, 0, 0, 0, 0, 0, 0, -128), outputStreamUint64LEBytesToArray(BigInteger("" + java.lang.Long.MIN_VALUE)))
    }

    @Throws(IOException::class)
    private fun outputStreamUint32LEBytesToArray(`val`: Long): ByteArray {
        val stream = ByteArrayOutputStream()
        ByteUtils.uint32ToByteStreamLE(`val`, stream)
        return stream.toByteArray()
    }

    @Throws(IOException::class)
    private fun outputStreamInt64LEBytesToArray(`val`: Long): ByteArray {
        val stream = ByteArrayOutputStream()
        ByteUtils.int64ToByteStreamLE(`val`, stream)
        return stream.toByteArray()
    }

    @Throws(IOException::class)
    private fun outputStreamUint64LEBytesToArray(`val`: BigInteger): ByteArray {
        val stream = ByteArrayOutputStream()
        ByteUtils.uint64ToByteStreamLE(`val`, stream)
        return stream.toByteArray()
    }

    @Test
    fun reverseBytesTest() {
        assertArrayEquals(byteArrayOf(), ByteUtils.reverseBytes(byteArrayOf()))
        assertArrayEquals(ByteUtils.toByteArray(1), ByteUtils.reverseBytes(ByteUtils.toByteArray(1)))
        assertArrayEquals(ByteUtils.toByteArray(200, 10), ByteUtils.reverseBytes(ByteUtils.toByteArray(10, 200)))
        assertArrayEquals(ByteUtils.toByteArray(10, 200), ByteUtils.reverseBytes(ByteUtils.toByteArray(200, 10)))
        assertArrayEquals(ByteUtils.toByteArray(0, 10, 200), ByteUtils.reverseBytes(ByteUtils.toByteArray(200, 10, 0)))
    }

    @Test
    fun reverseDwordBytesTest() {
        assertArrayEquals(ByteUtils.toByteArray(40, 30, 20, 10), ByteUtils.reverseDwordBytes(ByteUtils.toByteArray(10, 20, 30, 40), 4))
        assertArrayEquals(ByteUtils.toByteArray(40, 30, 20, 10), ByteUtils.reverseDwordBytes(ByteUtils.toByteArray(10, 20, 30, 40, 50, 60, 70, 80), 4))
        assertArrayEquals(ByteUtils.toByteArray(40, 30, 20, 10, 80, 70, 60, 50), ByteUtils.reverseDwordBytes(ByteUtils.toByteArray(10, 20, 30, 40, 50, 60, 70, 80), 8))
    }

    @Test
    fun MPITests() {
        val candidate = object : ArrayList<Long>() {
            init {
                add(0L)
                add(-0L)
                add(java.lang.Long.MAX_VALUE)
                add(java.lang.Long.MIN_VALUE)
                add(-9001000100000100001L)
                add(-9001000100000100001L)
                add(9001000100000100001L)
                add(-900100010000010000L)
                add(900100010000010000L)
                add(-900100010000010L)
                add(900100010000010L)
                add(-9001000100000L)
                add(9001000100000L)
                add(-9001000100L)
                add(9001000100L)
                add(-90010001L)
                add(90010001L)
                add(-900100L)
                add(900100L)
                add(-900L)
                add(900L)
                add(-9L)
                add(9L)
                add(-1L)
                add(1L)
            }
        }
        for (v in candidate) {
            val value = BigInteger.valueOf(v)
            assertEquals(value, ByteUtils.decodeMPI(ByteUtils.encodeMPI(value, false), false))
        }
    }

    @Test
    fun compactBitsTest() {
        val candidate = object : ArrayList<Long>() {
            init {
                add(8388608L) // Maximum! One more an it will fail
                add(4000000L)
                add(1000000L)
                add(900000L)
                add(90000L)
                add(9000L)
                add(900L)
                add(90L)
                add(9L)
                add(1L)
                add(0L)
                add(-0L)
            }
        }
        for (v in candidate) {
            val value = BigInteger.valueOf(v)
            println(value)
            assertEquals(value, ByteUtils.decodeCompactBits(ByteUtils.encodeCompactBits(value)))
        }
    }

    @Test
    fun testReverseDwordBytes() {
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8), ByteUtils.reverseDwordBytes(byteArrayOf(4, 3, 2, 1, 8, 7, 6, 5), -1))
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), ByteUtils.reverseDwordBytes(byteArrayOf(4, 3, 2, 1, 8, 7, 6, 5), 4))
        assertArrayEquals(ByteArray(0), ByteUtils.reverseDwordBytes(byteArrayOf(4, 3, 2, 1, 8, 7, 6, 5), 0))
        assertArrayEquals(ByteArray(0), ByteUtils.reverseDwordBytes(ByteArray(0), 0))
    }


    @Test
    @Throws(Exception::class)
    fun compactEncoding() {
        assertEquals(BigInteger("1234560000", 16), ByteUtils.decodeCompactBits(0x05123456L))
        assertEquals(BigInteger("c0de000000", 16), ByteUtils.decodeCompactBits(0x0600c0de))
        assertEquals(0x05123456L, ByteUtils.encodeCompactBits(BigInteger("1234560000", 16)))
        assertEquals(0x0600c0deL, ByteUtils.encodeCompactBits(BigInteger("c0de000000", 16)))
    }


    @Test
    fun testReadUint16BE() {
        assertEquals(ByteUtils.readUint16BE(HEX.hexToBytes("0000"), 0).toLong(), 0L)
        assertEquals(ByteUtils.readUint16BE(HEX.hexToBytes("00FF"), 0).toLong(), Math.pow(2.0, 8.0).toLong() - 1)
        assertEquals(ByteUtils.readUint16BE(HEX.hexToBytes("FFFF"), 0).toLong(), Math.pow(2.0, 16.0).toLong() - 1)
    }

    @Test
    fun testReadUint32BE() {
        assertEquals(ByteUtils.readUint32BE(HEX.hexToBytes("00000000"), 0), 0L)
        assertEquals(ByteUtils.readUint32BE(HEX.hexToBytes("000000FF"), 0), Math.pow(2.0, 8.0).toLong() - 1)
        assertEquals(ByteUtils.readUint32BE(HEX.hexToBytes("0000FFFF"), 0), Math.pow(2.0, 16.0).toLong() - 1)
        assertEquals(ByteUtils.readUint32BE(HEX.hexToBytes("00FFFFFF"), 0), Math.pow(2.0, 24.0).toLong() - 1)
        assertEquals(ByteUtils.readUint32BE(HEX.hexToBytes("FFFFFFFF"), 0), Math.pow(2.0, 32.0).toLong() - 1)
    }

    @Test
    fun testReadUint32() {
        assertEquals(ByteUtils.readUint32(HEX.hexToBytes("00000000"), 0), 0L)
        assertEquals(ByteUtils.readUint32(HEX.hexToBytes("FF000000"), 0), Math.pow(2.0, 8.0).toLong() - 1)
        assertEquals(ByteUtils.readUint32(HEX.hexToBytes("FFFF0000"), 0), Math.pow(2.0, 16.0).toLong() - 1)
        assertEquals(ByteUtils.readUint32(HEX.hexToBytes("FFFFFF00"), 0), Math.pow(2.0, 24.0).toLong() - 1)
        assertEquals(ByteUtils.readUint32(HEX.hexToBytes("FFFFFFFF"), 0), Math.pow(2.0, 32.0).toLong() - 1)
    }

    @Test
    fun testReadInt64() {
        assertEquals(ByteUtils.readInt64(HEX.hexToBytes("0000000000000000"), 0), 0L)
        assertEquals(ByteUtils.readInt64(HEX.hexToBytes("FF00000000000000"), 0), Math.pow(2.0, 8.0).toLong() - 1)
        assertEquals(ByteUtils.readInt64(HEX.hexToBytes("FFFF000000000000"), 0), Math.pow(2.0, 16.0).toLong() - 1)
        assertEquals(ByteUtils.readInt64(HEX.hexToBytes("FFFFFF0000000000"), 0), Math.pow(2.0, 24.0).toLong() - 1)
        assertEquals(ByteUtils.readInt64(HEX.hexToBytes("FFFFFFFF00000000"), 0), Math.pow(2.0, 32.0).toLong() - 1)
        assertEquals(ByteUtils.readInt64(HEX.hexToBytes("FFFFFFFFFF000000"), 0), Math.pow(2.0, 40.0).toLong() - 1)
        assertEquals(ByteUtils.readInt64(HEX.hexToBytes("FFFFFFFFFFFF0000"), 0), Math.pow(2.0, 48.0).toLong() - 1)
        assertEquals(ByteUtils.readInt64(HEX.hexToBytes("FFFFFFFFFFFFFF00"), 0), Math.pow(2.0, 56.0).toLong() - 1)
        assertEquals(ByteUtils.readInt64(HEX.hexToBytes("FFFFFFFFFFFFFFFF"), 0), -1L)
    }

}
