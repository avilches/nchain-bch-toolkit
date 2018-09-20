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

package com.nchain.bitcoinkt.utils

import com.google.common.base.*
import com.nchain.bitcoinkt.core.*
import org.slf4j.*

import java.util.concurrent.*

/**
 * A [java.util.concurrent.ThreadFactory] that propagates a [com.nchain.bitcoinkt.core.Context] from the creating
 * thread into the new thread. This factory creates daemon threads.
 */
class ContextPropagatingThreadFactory @JvmOverloads constructor(private val name: String, private val priority: Int = Thread.NORM_PRIORITY) : ThreadFactory {

    override fun newThread(r: Runnable): Thread {
        val context = Context.get()
        val thread = Thread(Runnable {
            try {
                Context.propagate(context!!)
                r.run()
            } catch (e: Exception) {
                log.error("Exception in thread", e)
                Throwables.propagate(e)
            }
        }, name)
        thread.priority = priority
        thread.isDaemon = true
        val handler = Threading.uncaughtExceptionHandler
        if (handler != null)
            thread.uncaughtExceptionHandler = handler
        return thread
    }

    companion object {
        private val log = LoggerFactory.getLogger(ContextPropagatingThreadFactory::class.java)
    }
}
