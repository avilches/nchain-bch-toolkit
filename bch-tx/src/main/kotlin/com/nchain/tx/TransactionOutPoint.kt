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

package com.nchain.tx

import com.nchain.shared.Sha256Hash
import com.nchain.tools.ByteUtils
import com.nchain.tools.MessageReader
import com.nchain.script.ProtocolException
import java.io.IOException
import java.io.OutputStream
import java.util.*

/**
 *
 * Reference or pointer to an output of a different transaction.
 *
 * Instances of this class are thread safe.
 */
class TransactionOutPoint
    @JvmOverloads
    constructor(
            val index: Long,
            val hash: Sha256Hash,
            val connectedOutput:TransactionOutput? = null) {

    val length = MESSAGE_LENGTH

    companion object {
        const val MESSAGE_LENGTH = 36
        // Magic outpoint index that indicates the input is in fact unconnected.
        const val UNCONNECTED_INDEX = 0xFFFFFFFFL

        @JvmStatic
        val UNCONNECTED = TransactionOutPoint(UNCONNECTED_INDEX, Sha256Hash.ZERO_HASH)

        @JvmStatic fun create(index:Long, fromTx: Transaction): TransactionOutPoint {
            return TransactionOutPoint(index, fromTx.hash, fromTx.getOutput(index))
        }

        @Throws(ProtocolException::class)
        @JvmStatic fun parse(payload: ByteArray, offset: Int = 0): TransactionOutPoint {
            return parse(MessageReader(payload, offset))
        }

        @Throws(ProtocolException::class)
        @JvmStatic fun parse(reader: MessageReader): TransactionOutPoint {
            val offset = reader.cursor
            val hash = reader.readHash()
            val index = reader.readUint32()
            check(MESSAGE_LENGTH == reader.cursor - offset)
            return TransactionOutPoint(index, hash, null)
        }
    }

    val isUnconnected:Boolean = index == UNCONNECTED_INDEX

    /**
     * Returns the pubkey script from the connected output.
     * @throws java.lang.NullPointerException if there is no connected output.
     */
    val connectedPubKeyScript: ByteArray
        get() {
            val result = connectedOutput!!.scriptBytes
            check(result!!.size > 0)
            return result

        }

    @Throws(IOException::class)
    fun bitcoinSerializeToStream(stream: OutputStream) {
        stream.write(hash.reversedBytes)
        ByteUtils.uint32ToByteStreamLE(index, stream)
    }


    /**
     * Returns the ECKey identified in the connected output, for either pay-to-address scripts or pay-to-key scripts.
     * For P2SH scripts you can use [.getConnectedRedeemData] and then get the
     * key from RedeemData.
     * If the script form cannot be understood, throws ScriptException.
     *
     * @return an ECKey or null if the connected key cannot be found in the wallet.
     */
/*
    @Throws(ScriptException::class)
    fun getConnectedKey(keyBag: KeyBag): ECKey? {
        val connectedOutput = getConnectedOutput()
        checkNotNull<TransactionOutput>(connectedOutput, "Input is not connected so cannot retrieve key")
        val connectedScript = connectedOutput!!.getScriptPubKey()
        if (connectedScript.isSentToAddress) {
            val addressBytes = connectedScript.pubKeyHash
            return keyBag.findKeyFromPubHash(addressBytes!!)
        } else if (connectedScript.isSentToRawPubKey) {
            val pubkeyBytes = connectedScript.pubKey
            return keyBag.findKeyFromPubKey(pubkeyBytes)
        } else {
            throw ScriptException("Could not understand form of connected output script: " + connectedScript)
        }
    }
*/

    /**
     * Returns the RedeemData identified in the connected output, for either pay-to-address scripts, pay-to-key
     * or P2SH scripts.
     * If the script forms cannot be understood, throws ScriptException.
     *
     * @return a RedeemData or null if the connected data cannot be found in the wallet.
     */
/*
    @Throws(ScriptException::class)
    fun getConnectedRedeemData(keyBag: KeyBag): RedeemData? {
        val connectedOutput = getConnectedOutput()
        checkNotNull<TransactionOutput>(connectedOutput, "Input is not connected so cannot retrieve key")
        val connectedScript = connectedOutput!!.getScriptPubKey()
        if (connectedScript.isSentToAddress) {
            val addressBytes = connectedScript.pubKeyHash
            return RedeemData.of(keyBag.findKeyFromPubHash(addressBytes!!), connectedScript)
        } else if (connectedScript.isSentToRawPubKey) {
            val pubkeyBytes = connectedScript.pubKey
            return RedeemData.of(keyBag.findKeyFromPubKey(pubkeyBytes), connectedScript)
        } else if (connectedScript.isPayToScriptHash) {
            val scriptHash = connectedScript.pubKeyHash
            return keyBag.findRedeemDataFromScriptHash(scriptHash!!)
        } else {
            throw ScriptException("Could not understand form of connected output script: " + connectedScript)
        }
    }
*/

    override fun toString(): String {
        return hash.toString() + ":" + index
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        return if (o != null && o is TransactionOutPoint)
            index == o.index && hash == o.hash
        else
            false
    }

    override fun hashCode(): Int {
        return Objects.hash(index, hash)
    }

}
