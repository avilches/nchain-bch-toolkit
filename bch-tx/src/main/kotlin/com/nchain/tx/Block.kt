package com.nchain.tx

import com.nchain.shared.ProtocolException
import com.nchain.shared.Sha256Hash
import com.nchain.shared.VarInt
import com.nchain.tools.*
import com.nchain.tx.Transaction
import com.nchain.tx.TransactionBuilder
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

/*
 * @author Alberto Vilches
 * @date 11/10/2018
 */


class Block(val version: Long,
            val prevBlockHash: Sha256Hash,
            val merkleRoot: Sha256Hash,
            val timeSeconds: Long,
            val difficultyTarget: Long,
            val nonce: Long,
            transactions: List<Transaction>? = null
) {

    val transactions: List<Transaction>
    val hash: Sha256Hash

    init {
        this.transactions = if (transactions != null) Collections.unmodifiableList(transactions) else Collections.EMPTY_LIST as List<Transaction>
    }

    init {
        hash = Sha256Hash.wrapReversed(Sha256Hash.hashTwice(bitcoinSerialize(true)))
    }


    @JvmOverloads
    fun bitcoinSerialize(headerOnly:Boolean = false):ByteArray {
        val stream = UnsafeByteArrayOutputStream()
        try {
            bitcoinSerializeToStream(stream, headerOnly)
            stream.close()
        } catch (e: IOException) {
            // It will never happen
        }
        return stream.toByteArray()
    }

    @Throws(IOException::class)
    fun bitcoinSerializeToStream(stream: OutputStream, headerOnly:Boolean = false) {
        writeHeader(stream)
        if (!headerOnly && transactions.isNotEmpty()) {
            stream.write(VarInt(transactions.size.toLong()).encode())

            transactions.forEach {
                it.bitcoinSerializeToStream(stream)
            }
        }
    }

    @Throws(IOException::class)
    fun writeHeader(stream: OutputStream) {
        // try for cached write first
        // fall back to manual write
        ByteUtils.uint32ToByteStreamLE(version, stream)
        stream.write(prevBlockHash.reversedBytes)
        stream.write(merkleRoot.reversedBytes)
        ByteUtils.uint32ToByteStreamLE(timeSeconds, stream)
        ByteUtils.uint32ToByteStreamLE(difficultyTarget, stream)
        ByteUtils.uint32ToByteStreamLE(nonce, stream)
    }


    companion object {
        private val log = LoggerFactory.getLogger(TransactionBuilder::class.java)

        val HEADER_SIZE = 80

        @JvmStatic
        fun parse(rawHex: String): Block {
            return parse(rawHex.hexStringToByteArray())
        }

        @JvmStatic
        fun parse(inputStream: InputStream): Block {
            return parse(InputStreamMessageReader(inputStream))
        }

        @JvmOverloads
        @JvmStatic
        fun parse(payload: ByteArray, offset: Int = 0): Block {
            return parse(ByteArrayMessageReader(payload, offset))
        }

        @JvmStatic
        @Throws(ProtocolException::class)
        fun parse(reader: MessageReader): Block {
            val version = reader.readUint32()
            val prevBlockHash = reader.readHash()
            val merkleRoot = reader.readHash()
            val timeSeconds = reader.readUint32()
            val difficultyTarget = reader.readUint32()
            val nonce = reader.readUint32()

            // transactions
            if (!reader.hasMoreBytes()) {
                return Block(version, prevBlockHash, merkleRoot, timeSeconds, difficultyTarget, nonce)
            }

            val transactionCount = reader.readVarInt()
            val transactions = ArrayList<Transaction>(transactionCount.toInt())
            for (i in 0 until transactionCount) {
                val txBuilder = TransactionBuilder.parse(reader)
                transactions.add(txBuilder.build())
            }
            return Block(version, prevBlockHash, merkleRoot, timeSeconds, difficultyTarget, nonce, transactions)
        }
    }

}