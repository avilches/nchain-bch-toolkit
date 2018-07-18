/*
 * Copyright 2011 Google Inc.
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

import com.nchain.shared.VarInt
import junit.framework.TestCase

import org.junit.Test

class VarIntTest {

    @Test
    @Throws(Exception::class)
    fun testBytes() {
        val a = VarInt(10) // with widening conversion
        TestCase.assertEquals(1, a.sizeInBytes)
        TestCase.assertEquals(1, a.encode().size)
        TestCase.assertEquals(10, VarInt(a.encode(), 0).value)
    }

    @Test
    @Throws(Exception::class)
    fun testShorts() {
        val a = VarInt(64000) // with widening conversion
        TestCase.assertEquals(3, a.sizeInBytes)
        TestCase.assertEquals(3, a.encode().size)
        TestCase.assertEquals(64000, VarInt(a.encode(), 0).value)
    }

    @Test
    @Throws(Exception::class)
    fun testShortFFFF() {
        val a = VarInt(0xFFFFL)
        TestCase.assertEquals(3, a.sizeInBytes)
        TestCase.assertEquals(3, a.encode().size)
        TestCase.assertEquals(0xFFFFL, VarInt(a.encode(), 0).value)
    }

    @Test
    @Throws(Exception::class)
    fun testInts() {
        val a = VarInt(0xAABBCCDDL)
        TestCase.assertEquals(5, a.sizeInBytes)
        TestCase.assertEquals(5, a.encode().size)
        val bytes = a.encode()
        TestCase.assertEquals(0xAABBCCDDL, 0xFFFFFFFFL and VarInt(bytes, 0).value)
    }

    @Test
    @Throws(Exception::class)
    fun testIntFFFFFFFF() {
        val a = VarInt(0xFFFFFFFFL)
        TestCase.assertEquals(5, a.sizeInBytes)
        TestCase.assertEquals(5, a.encode().size)
        val bytes = a.encode()
        TestCase.assertEquals(0xFFFFFFFFL, 0xFFFFFFFFL and VarInt(bytes, 0).value)
    }

    @Test
    @Throws(Exception::class)
    fun testLong() {
        val a = VarInt(-0x3501454121524111L)
        TestCase.assertEquals(9, a.sizeInBytes)
        TestCase.assertEquals(9, a.encode().size)
        val bytes = a.encode()
        TestCase.assertEquals(-0x3501454121524111L, VarInt(bytes, 0).value)
    }

    @Test
    @Throws(Exception::class)
    fun testSizeOfNegativeInt() {
        // shouldn't normally be passed, but at least stay consistent (bug regression test)
        TestCase.assertEquals(VarInt.sizeOf(-1), VarInt(-1).encode().size)
    }
}
