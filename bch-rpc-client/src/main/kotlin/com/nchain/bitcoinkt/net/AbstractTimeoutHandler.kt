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

package com.nchain.bitcoinkt.net

import java.util.*

/**
 *
 * A base class which provides basic support for socket timeouts. It is used instead of integrating timeouts into the
 * NIO select thread both for simplicity and to keep code shared between NIO and blocking sockets as much as possible.
 *
 */
abstract class AbstractTimeoutHandler {
    // TimerTask and timeout value which are added to a timer to kill the connection on timeout
    private var timeoutTask: TimerTask? = null
    private var timeoutMillis: Long = 0
    private var timeoutEnabled = true

    /**
     *
     * Enables or disables the timeout entirely. This may be useful if you want to store the timeout value but wish
     * to temporarily disable/enable timeouts.
     *
     *
     * The default is for timeoutEnabled to be true but timeoutMillis to be set to 0 (ie disabled).
     *
     *
     * This call will reset the current progress towards the timeout.
     */
    @Synchronized
    fun setTimeoutEnabled(timeoutEnabled: Boolean) {
        this.timeoutEnabled = timeoutEnabled
        resetTimeout()
    }

    /**
     *
     * Sets the receive timeout to the given number of milliseconds, automatically killing the connection if no
     * messages are received for this long
     *
     *
     * A timeout of 0 is interpreted as no timeout.
     *
     *
     * The default is for timeoutEnabled to be true but timeoutMillis to be set to 0 (ie disabled).
     *
     *
     * This call will reset the current progress towards the timeout.
     */
    @Synchronized
    fun setSocketTimeout(timeoutMillis: Int) {
        this.timeoutMillis = timeoutMillis.toLong()
        resetTimeout()
    }

    /**
     * Resets the current progress towards timeout to 0.
     */
    @Synchronized
    protected fun resetTimeout() {
        if (timeoutTask != null)
            timeoutTask!!.cancel()
        if (timeoutMillis == 0L || !timeoutEnabled)
            return
        timeoutTask = object : TimerTask() {
            override fun run() {
                timeoutOccurred()
            }
        }
        timeoutTimer.schedule(timeoutTask!!, timeoutMillis)
    }

    protected abstract fun timeoutOccurred()

    companion object {

        // A timer which manages expiring channels as their timeouts occur (if configured).
        private val timeoutTimer = Timer("AbstractTimeoutHandler timeouts", true)
    }
}
