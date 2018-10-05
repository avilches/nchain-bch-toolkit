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

package com.nchain.bitcoinkt.store

import com.nchain.bitcoinkt.core.*
import com.nchain.bitcoinkt.pow.rule.GenesisBlock
import com.nchain.params.NetworkParameters
import com.nchain.shared.Sha256Hash
import com.nchain.shared.VerificationException

import java.util.LinkedHashMap

/**
 * Keeps [com.nchain.bitcoinkt.core.StoredBlock]s in memory. Used primarily for unit testing.
 */
class MemoryBlockStore(override var params: NetworkParameters) : BlockStore {
    private var blockMap: LinkedHashMap<Sha256Hash, StoredBlock>? = object : LinkedHashMap<Sha256Hash, StoredBlock>() {
        override fun removeEldestEntry(eldest: Map.Entry<Sha256Hash, StoredBlock>?): Boolean {
            return this.size > 5000
        }
    }
    override var chainHead: StoredBlock? = null
        get() {
            if (blockMap == null) throw BlockStoreException("MemoryBlockStore is closed")
            return field
        }
        set(value) {
            if (blockMap == null) throw BlockStoreException("MemoryBlockStore is closed")
            field = value
        }


    init {
        // Insert the genesis block.
        try {
            val genesisHeader = GenesisBlock.of(params).cloneAsHeader()
            val storedGenesis = StoredBlock(genesisHeader, genesisHeader.work, 0)
            put(storedGenesis)
            chainHead = storedGenesis
            this.params = params
        } catch (e: BlockStoreException) {
            throw RuntimeException(e)  // Cannot happen.
        } catch (e: VerificationException) {
            throw RuntimeException(e)  // Cannot happen.
        }

    }

    @Synchronized
    @Throws(BlockStoreException::class)
    override fun put(block: StoredBlock) {
        if (blockMap == null) throw BlockStoreException("MemoryBlockStore is closed")
        val hash:Sha256Hash = block.header.hash!!
        blockMap!!.put(hash, block)
    }

    @Synchronized
    @Throws(BlockStoreException::class)
    override fun get(hash: Sha256Hash): StoredBlock? {
        if (blockMap == null) throw BlockStoreException("MemoryBlockStore is closed")
        return blockMap!!.get(hash)
    }

    override fun close() {
        blockMap = null
    }
}
