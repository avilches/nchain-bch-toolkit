/*
 * Copyright 2015 Ross Nicoll.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nchain.bitcoinkt.utils

import java.util.Stack
import com.nchain.bitcoinkt.core.StoredBlock
import com.nchain.bitcoinkt.store.BlockStore
import com.nchain.bitcoinkt.store.BlockStoreException
import com.nchain.params.NetworkParameters

/**
 * Caching counter for the block versions within a moving window. This class
 * is NOT thread safe (as if two threads are trying to use it concurrently,
 * there's risk of getting versions out of sequence).
 *
 * @see com.nchain.bitcoinkt.core.NetworkParameters.getMajorityWindow
 * @see com.nchain.bitcoinkt.core.NetworkParameters.getMajorityEnforceBlockUpgrade
 * @see com.nchain.bitcoinkt.core.NetworkParameters.getMajorityRejectBlockOutdated
 */
class VersionTally(params: NetworkParameters) {
    /**
     * Cache of version numbers.
     */
    private val versionWindow: LongArray

    /**
     * Offset within the version window at which the next version will be
     * written.
     */
    private var versionWriteHead = 0

    /**
     * Number of versions written into the tally. Until this matches the length
     * of the version window, we do not have sufficient data to return values.
     */
    private var versionsStored = 0

    init {
        versionWindow = LongArray(params.majorityWindow)
    }

    /**
     * Add a new block version to the tally, and return the count for that version
     * within the window.
     *
     * @param version the block version to add.
     */
    fun add(version: Long) {
        versionWindow[versionWriteHead++] = version
        if (versionWriteHead == versionWindow.size) {
            versionWriteHead = 0
        }
        versionsStored++
    }

    /**
     * Get the count of blocks at or above the given version, within the window.
     *
     * @param version the block version to query.
     * @return the count for the block version, or null if the window is not yet
     * full.
     */
    fun getCountAtOrAbove(version: Long): Int? {
        if (versionsStored < versionWindow.size) {
            return null
        }
        var count = 0
        for (versionIdx in versionWindow.indices) {
            if (versionWindow[versionIdx] >= version) {
                count++
            }
        }

        return count
    }

    /**
     * Initialize the version tally from the block store. Note this does not
     * search backwards past the start of the block store, so if starting from
     * a checkpoint this may not fill the window.
     *
     * @param blockStore block store to load blocks from.
     * @param chainHead current chain tip.
     */
    @Throws(BlockStoreException::class)
    fun initialize(blockStore: BlockStore, chainHead: StoredBlock?) {
        var versionBlock: StoredBlock? = chainHead
        val versions = Stack<Long>()

        // We don't know how many blocks back we can go, so load what we can first
        versions.push(versionBlock!!.header.version)
        for (headOffset in versionWindow.indices) {
            versionBlock = versionBlock!!.getPrev(blockStore)
            if (null == versionBlock) {
                break
            }
            versions.push(versionBlock.header.version)
        }

        // Replay the versions into the tally
        while (!versions.isEmpty()) {
            add(versions.pop())
        }
    }

    /**
     * Get the size of the version window.
     */
    fun size(): Int {
        return versionWindow.size
    }
}
