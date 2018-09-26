/*
 * Copyright 2013 Google Inc.
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

package com.nchain.bitcoinkt.utils

import com.google.common.primitives.Longs

import com.google.common.base.Preconditions.checkArgument

/**
 *
 * Tracks successes and failures and calculates a time to retry the operation.
 *
 *
 * The retries are exponentially backed off, up to a maximum interval.  On success the back off interval is reset.
 */
class ExponentialBackoff(private val params: Params) : Comparable<ExponentialBackoff> {

    private var backoff: Float = 0.toFloat()
    /** Get the next time to retry, in milliseconds since the epoch  */
    var retryTime: Long = 0
        private set

    /**
     * Parameters to configure a particular kind of exponential backoff.
     */
    class Params {
        val initial: Float
        val multiplier: Float
        val maximum: Float

        /**
         * @param initialMillis the initial interval to wait, in milliseconds
         * @param multiplier the multiplier to apply on each failure
         * @param maximumMillis the maximum interval to wait, in milliseconds
         */
        constructor(initialMillis: Long, multiplier: Float, maximumMillis: Long) {
            checkArgument(multiplier > 1.0f, "multiplier must be greater than 1.0")
            checkArgument(maximumMillis >= initialMillis, "maximum must not be less than initial")

            this.initial = initialMillis.toFloat()
            this.multiplier = multiplier
            this.maximum = maximumMillis.toFloat()
        }

        /**
         * Construct params with default values.
         */
        constructor() {
            initial = DEFAULT_INITIAL_MILLIS.toFloat()
            multiplier = DEFAULT_MULTIPLIER
            maximum = DEFAULT_MAXIMUM_MILLIS.toFloat()
        }
    }

    init {
        trackSuccess()
    }

    /** Track a success - reset back off interval to the initial value  */
    fun trackSuccess() {
        backoff = params.initial
        retryTime = Utils.currentTimeMillis()
    }

    /** Track a failure - multiply the back off interval by the multiplier  */
    fun trackFailure() {
        retryTime = Utils.currentTimeMillis() + backoff.toLong()
        backoff = Math.min(backoff * params.multiplier, params.maximum)
    }

    override fun compareTo(other: ExponentialBackoff): Int {
        // note that in this implementation compareTo() is not consistent with equals()
        return Longs.compare(retryTime, other.retryTime)
    }

    override fun toString(): String {
        return "ExponentialBackoff retry=$retryTime backoff=$backoff"
    }

    companion object {
        val DEFAULT_INITIAL_MILLIS = 100
        val DEFAULT_MULTIPLIER = 1.1f
        val DEFAULT_MAXIMUM_MILLIS = 30 * 1000
    }
}
