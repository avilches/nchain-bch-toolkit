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
interface MessageReader {

    @Throws(ProtocolException::class)
    fun readUint32(): Long

    @Throws(ProtocolException::class)
    fun readInt64(): Long

    @Throws(ProtocolException::class)
    fun readUint64(): BigInteger

    @Throws(ProtocolException::class)
    fun readVarInt(): Long

    @Throws(ProtocolException::class)
    fun readBytes(length: Int): ByteArray

    @Throws(ProtocolException::class)
    fun readByteArray(): ByteArray

    @Throws(ProtocolException::class)
    fun readStr(): String

    @Throws(ProtocolException::class)
    fun readHash(): Sha256Hash

    fun hasMoreBytes(): Boolean
}