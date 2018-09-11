/*
 * Copyright 2013 Google Inc.
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

package com.nchain.script

import com.nchain.tools.ByteUtils
import com.nchain.tools.HEX

import java.io.IOException
import java.io.OutputStream
import java.util.Arrays
import java.util.Objects

import com.nchain.tools.Preconditions.checkState
import com.nchain.script.Script
import com.nchain.script.ScriptOpCodes.*

/**
 * A script element that is either a data push (signature, pubkey, etc) or a non-push (logic, numeric, etc) operation.
 */
class ScriptChunk
    @JvmOverloads
    constructor(
        /** Operation to be executed. Opcodes are defined in [ScriptOpCodes].  */
        val opcode: Int,
        /**
         * For push operations, this is the vector to be pushed on the stack. For [ScriptOpCodes.OP_0], the vector is
         * empty. Null for non-push operations.
         */
        val data: ByteArray?, private val startLocationInProgram: Int = -1) {

    /**
     * If this chunk is a single byte of non-pushdata content (could be OP_RESERVED or some invalid Opcode)
     */
    val isOpCode: Boolean
        get() = opcode > OP_PUSHDATA4

    /**
     * Returns true if this chunk is pushdata content, including the single-byte pushdatas.
     */
    val isPushData: Boolean
        get() = opcode <= OP_16

    /**
     * Called on a pushdata chunk, returns true if it uses the smallest possible way (according to BIP62) to push the data.
     */
    // OP_N
    // can never be used, but implemented for completeness
    val isShortestPossiblePushData: Boolean
        get() {
            checkState(isPushData)
            if (data == null)
                return true
            if (data.size == 0)
                return opcode == OP_0
            if (data.size == 1) {
                val b = data[0]
                if (b >= 0x01 && b <= 0x10)
                    return opcode == OP_1 + b - 1
                if (b.toInt() and 0xFF == 0x81)
                    return opcode == OP_1NEGATE
            }
            if (data.size < OP_PUSHDATA1)
                return opcode == data.size
            if (data.size < 256)
                return opcode == OP_PUSHDATA1
            return if (data.size < 65536) opcode == OP_PUSHDATA2 else opcode == OP_PUSHDATA4
        }

    fun equalsOpCode(opcode: Int): Boolean {
        return opcode == this.opcode
    }

    fun getStartLocationInProgram(): Int {
        checkState(startLocationInProgram >= 0)
        return startLocationInProgram
    }

    /** If this chunk is an OP_N opcode returns the equivalent integer value.  */
    fun decodeOpN(): Int {
        checkState(isOpCode)
        return Script.decodeFromOpN(opcode)
    }

    @Throws(IOException::class)
    fun write(stream: OutputStream) {
        if (isOpCode) {
            checkState(data == null)
            stream.write(opcode)
        } else if (data != null) {
            if (opcode < OP_PUSHDATA1) {
                checkState(data.size == opcode)
                stream.write(opcode)
            } else if (opcode == OP_PUSHDATA1) {
                checkState(data.size <= 0xFF)
                stream.write(OP_PUSHDATA1)
                stream.write(data.size)
            } else if (opcode == OP_PUSHDATA2) {
                checkState(data.size <= 0xFFFF)
                stream.write(OP_PUSHDATA2)
                stream.write(0xFF and data.size)
                stream.write(0xFF and (data.size shr 8))
            } else if (opcode == OP_PUSHDATA4) {
                checkState(data.size <= Script.MAX_SCRIPT_ELEMENT_SIZE)
                stream.write(OP_PUSHDATA4)
                ByteUtils.uint32ToByteStreamLE(data.size.toLong(), stream)
            } else {
                throw RuntimeException("Unimplemented")
            }
            stream.write(data)
        } else {
            stream.write(opcode) // smallNum
        }
    }

    override fun toString(): String {
        val buf = StringBuilder()
        if (isOpCode) {
            buf.append(getOpCodeName(opcode))
        } else if (data != null) {
            // Data chunk
            buf.append(getPushDataName(opcode)).append("[").append(HEX.encode(data)).append("]")
        } else {
            // Small num
            buf.append(Script.decodeFromOpN(opcode))
        }
        return buf.toString()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val other = o as ScriptChunk?
        return (opcode == other!!.opcode && startLocationInProgram == other.startLocationInProgram
                && Arrays.equals(data, other.data))
    }

    override fun hashCode(): Int {
        return Objects.hash(opcode, startLocationInProgram, Arrays.hashCode(data))
    }
}
