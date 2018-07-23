/*
 * Copyright 2013 Matija Mazi.
 * Copyright 2014 Giannis Dzegoutanis.
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

package com.nchain.bip32

import com.nchain.key.ECKey
import org.spongycastle.crypto.digests.SHA512Digest
import org.spongycastle.crypto.macs.HMac
import org.spongycastle.crypto.params.KeyParameter
import java.nio.ByteBuffer
import java.util.ArrayList
import java.util.Arrays

/**
 * Static utilities used in BIP 32 Hierarchical Deterministic Wallets (HDW).
 */
object HDUtils {
    internal fun createHmacSha512Digest(key: ByteArray): HMac {
        val digest = SHA512Digest()
        val hMac = HMac(digest)
        hMac.init(KeyParameter(key))
        return hMac
    }

    internal fun hmacSha512(hmacSha512: HMac, input: ByteArray): ByteArray {
        hmacSha512.reset()
        hmacSha512.update(input, 0, input.size)
        val out = ByteArray(64)
        hmacSha512.doFinal(out, 0)
        return out
    }

    fun hmacSha512(key: ByteArray, data: ByteArray): ByteArray {
        return hmacSha512(createHmacSha512Digest(key), data)
    }

    internal fun toCompressed(uncompressedPoint: ByteArray): ByteArray {
        return ECKey.CURVE.curve.decodePoint(uncompressedPoint).getEncoded(true)
    }

    internal fun longTo4ByteArray(n: Long): ByteArray {
        val bytes = Arrays.copyOfRange(ByteBuffer.allocate(8).putLong(n).array(), 4, 8)
        assert(bytes.size == 4) { bytes.size }
        return bytes
    }

    /** Append a derivation level to an existing path  */
    fun append(path: List<ChildNumber>, childNumber: ChildNumber): List<ChildNumber> {
        val list = arrayListOf<ChildNumber>()
        list.addAll(path)
        list.add(childNumber)
        return list.toList()
    }

    /** Concatenate two derivation paths  */
    fun concat(path: List<ChildNumber>, path2: List<ChildNumber>): List<ChildNumber> {
        val list = arrayListOf<ChildNumber>()
        list.addAll(path)
        list.addAll(path2)
        return list.toList()
    }

    /** Convert to a string path, starting with "M/"  */
    fun formatPath(path: List<ChildNumber>): String {
        return "M/"+path.joinToString("/")
    }

    /**
     * The path is a human-friendly representation of the deterministic path. For example:
     *
     * "44H / 0H / 0H / 1 / 1"
     *
     * Where a letter "H" means hardened key. Spaces are ignored.
     */
    fun parsePath(path: String): List<ChildNumber> {
        val pathClean = if (path.startsWith("m") || path.startsWith("M")) {
            path.substring(1)
        } else { path }
        val parsedNodes = pathClean.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val nodes = ArrayList<ChildNumber>()

        for (node in parsedNodes) {
            var n = node.replace(" ".toRegex(), "")
            if (n.length == 0) continue
            val isHard = n.endsWith("H")
            if (isHard) n = n.substring(0, n.length - 1)
            val nodeNumber = Integer.parseInt(n)
            nodes.add(ChildNumber(nodeNumber, isHard))
        }

        return nodes
    }
}
