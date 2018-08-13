package com.nchain.tools

import com.nchain.shared.ProtocolException
import com.nchain.shared.Sha256Hash
import com.nchain.shared.VarInt
import org.slf4j.LoggerFactory
import java.math.BigInteger

/*
 * @author Alberto Vilches
 * @date 10/08/2018
 */
class MessageReader(val payload: ByteArray, var cursor:Int = 0) {

    fun addOffset(offset:Int) {
        cursor += offset
    }
    
    companion object {
        private val log = LoggerFactory.getLogger(MessageReader::class.java!!)

        val MAX_SIZE = 0x02000000 // 32MB

        val UNKNOWN_LENGTH = Integer.MIN_VALUE

        // Useful to ensure serialize/deserialize are consistent with each other.
        private val SELF_CHECK = false
    }
    

    @Throws(ProtocolException::class)
    fun readUint32(): Long {
        try {
            val u = ByteUtils.readUint32(payload, cursor)
            cursor += 4
            return u
        } catch (e: ArrayIndexOutOfBoundsException) {
            throw ProtocolException(e)
        }

    }

    @Throws(ProtocolException::class)
    fun readInt64(): Long {
        try {
            val u = ByteUtils.readInt64(payload, cursor)
            cursor += 8
            return u
        } catch (e: ArrayIndexOutOfBoundsException) {
            throw ProtocolException(e)
        }

    }

    @Throws(ProtocolException::class)
    fun readUint64(): BigInteger {
        // Java does not have an unsigned 64 bit type. So scrape it off the wire then flip.
        return BigInteger(ByteUtils.reverseBytes(readBytes(8)))
    }

    @Throws(ProtocolException::class)
    fun readVarInt(): Long {
        try {
            val varint = VarInt(payload, cursor)
            cursor += varint.originalSizeInBytes
            return varint.value
        } catch (e: ArrayIndexOutOfBoundsException) {
            throw ProtocolException(e)
        }

    }

    @Throws(ProtocolException::class)
    fun readBytes(length: Int): ByteArray {
        if (length > MAX_SIZE) {
            throw ProtocolException("Claimed value length too large: " + length)
        }
        try {
            val b = ByteArray(length)
            System.arraycopy(payload, cursor, b, 0, length)
            cursor += length
            return b
        } catch (e: IndexOutOfBoundsException) {
            throw ProtocolException(e)
        }

    }

    @Throws(ProtocolException::class)
    fun readByteArray(): ByteArray {
        val len = readVarInt()
        return readBytes(len.toInt())
    }

    @Throws(ProtocolException::class)
    fun readStr(): String {
        val length = readVarInt()
        return if (length == 0L) "" else ByteUtils.toString(readBytes(length.toInt()), "UTF-8") // optimization for empty strings
    }

    @Throws(ProtocolException::class)
    fun readHash(): Sha256Hash {
        // TODO
        // We have to flip it around, as it's been read off the wire in little endian.
        // Not the most efficient way to do this but the clearest.
        return Sha256Hash.wrapReversed(readBytes(32))
    }

    fun hasMoreBytes(): Boolean {
        return cursor < payload.size
    }

    
    

}