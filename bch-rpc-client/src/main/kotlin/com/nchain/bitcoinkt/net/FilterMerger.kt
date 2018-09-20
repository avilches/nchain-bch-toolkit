/*
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

package com.nchain.bitcoinkt.net

import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import com.nchain.bitcoinkt.core.BloomFilter
import com.nchain.bitcoinkt.core.PeerFilterProvider

// This code is unit tested by the PeerGroup tests.

/**
 *
 * A reusable object that will calculate, given a list of [com.nchain.bitcoinkt.core.PeerFilterProvider]s, a merged
 * [com.nchain.bitcoinkt.core.BloomFilter] and earliest key time for all of them.
 * Used by the [com.nchain.bitcoinkt.core.PeerGroup] class internally.
 *
 *
 * Thread safety: threading here can be complicated. Each filter provider is given a begin event, which may acquire
 * a lock (and is guaranteed to receive an end event). This class is mostly thread unsafe and is meant to be used from a
 * single thread only, PeerGroup ensures this by only accessing it from the dedicated PeerGroup thread. PeerGroup does
 * not hold any locks whilst this object is used, relying on the single thread to prevent multiple filters being
 * calculated in parallel, thus a filter provider can do things like make blocking calls into PeerGroup from a separate
 * thread. However the bloomFilterFPRate property IS thread safe, for convenience.
 */
class FilterMerger(@field:Volatile var bloomFilterFPRate: Double) {
    // We use a constant tweak to avoid giving up privacy when we regenerate our filter with new keys
    private val bloomFilterTweak = (Math.random() * java.lang.Long.MAX_VALUE).toLong()
    private var lastBloomFilterElementCount: Int = 0
    var lastFilter: BloomFilter? = null
        private set

    class Result {
        var filter: BloomFilter? = null
        var earliestKeyTimeSecs: Long = 0
        var changed: Boolean = false
    }

    fun calculate(providers: ImmutableList<PeerFilterProvider>): Result {
        val begunProviders = Lists.newLinkedList<PeerFilterProvider>()
        try {
            // All providers must be in a consistent, unchanging state because the filter is a merged one that's
            // large enough for all providers elements: if a provider were to get more elements in the middle of the
            // calculation, we might assert or calculate the filter wrongly. Most providers use a lock here but
            // snapshotting required state is also a legitimate strategy.
            for (provider in providers) {
                provider.beginBloomFilterCalculation()
                begunProviders.add(provider)
            }
            val result = Result()
            result.earliestKeyTimeSecs = java.lang.Long.MAX_VALUE
            var elements = 0
            var requiresUpdateAll = false
            for (p in providers) {
                result.earliestKeyTimeSecs = Math.min(result.earliestKeyTimeSecs, p.earliestKeyCreationTime)
                elements += p.bloomFilterElementCount
                requiresUpdateAll = requiresUpdateAll || p.isRequiringUpdateAllBloomFilter
            }

            if (elements > 0) {
                // We stair-step our element count so that we avoid creating a filter with different parameters
                // as much as possible as that results in a loss of privacy.
                // The constant 100 here is somewhat arbitrary, but makes sense for small to medium wallets -
                // it will likely mean we never need to create a filter with different parameters.
                lastBloomFilterElementCount = if (elements > lastBloomFilterElementCount) elements + 100 else lastBloomFilterElementCount
                val bloomFlags = if (requiresUpdateAll) BloomFilter.BloomUpdate.UPDATE_ALL else BloomFilter.BloomUpdate.UPDATE_P2PUBKEY_ONLY
                val fpRate = bloomFilterFPRate
                val filter = BloomFilter(lastBloomFilterElementCount, fpRate, bloomFilterTweak, bloomFlags)
                for (p in providers)
                    filter.merge(p.getBloomFilter(lastBloomFilterElementCount, fpRate, bloomFilterTweak))

                result.changed = filter != lastFilter
                lastFilter = filter
                result.filter = lastFilter
            }
            // Now adjust the earliest key time backwards by a week to handle the case of clock drift. This can occur
            // both in block header timestamps and if the users clock was out of sync when the key was first created
            // (to within a small amount of tolerance).
            result.earliestKeyTimeSecs -= (86400 * 7).toLong()
            return result
        } finally {
            for (provider in begunProviders) {
                provider.endBloomFilterCalculation()
            }
        }
    }
}
