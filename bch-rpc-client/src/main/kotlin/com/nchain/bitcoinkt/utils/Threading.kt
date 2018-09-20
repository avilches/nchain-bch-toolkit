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

import com.google.common.util.concurrent.CycleDetectingLockFactory
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.Uninterruptibles
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantLock

/**
 * Various threading related utilities. Provides a wrapper around explicit lock creation that lets you control whether
 * bitcoinj performs cycle detection or not. Cycle detection is useful to detect bugs but comes with a small cost.
 * Also provides a worker thread that is designed for event listeners to be dispatched on.
 */
object Threading {

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // User thread/event handling utilities
    //
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * An executor with one thread that is intended for running event listeners on. This ensures all event listener code
     * runs without any locks being held. It's intended for the API user to run things on. Callbacks registered by
     * bitcoinj internally shouldn't normally run here, although currently there are a few exceptions.
     */
    @JvmField
    var USER_THREAD: Executor

    /**
     * A dummy executor that just invokes the runnable immediately. Use this over
     * [com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor] because the latter creates a new
     * object each time in order to implement the more complex [ExecutorService] interface, which is overkill
     * for our needs.
     */
    val SAME_THREAD: Executor

    /**
     * An exception handler that will be invoked for any exceptions that occur in the user thread, and
     * any unhandled exceptions that are caught whilst the framework is processing network traffic or doing other
     * background tasks. The purpose of this is to allow you to report back unanticipated crashes from your users
     * to a central collection center for analysis and debugging. You should configure this **before** any
     * bitcoinj library code is run, setting it after you started network traffic and other forms of processing
     * may result in the change not taking effect.
     */
    @Volatile
    var uncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Cycle detecting lock factories
    //
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private var policy: CycleDetectingLockFactory.Policy? = null
    lateinit var factory: CycleDetectingLockFactory

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Generic worker pool.
    //
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /** A caching thread pool that creates daemon threads, which won't keep the JVM alive waiting for more work.  */
    @JvmField var THREAD_POOL = MoreExecutors.listeningDecorator(
            Executors.newCachedThreadPool { r ->
                val t = Thread(r)
                t.name = "Threading.THREAD_POOL worker"
                t.isDaemon = true
                t
            }
    )

    /**
     * Put a dummy task into the queue and wait for it to be run. Because it's single threaded, this means all
     * tasks submitted before this point are now completed. Usually you won't want to use this method - it's a
     * convenience primarily used in unit testing. If you want to wait for an event to be called the right thing
     * to do is usually to create a [com.google.common.util.concurrent.SettableFuture] and then call set
     * on it. You can then either block on that future, compose it, add listeners to it and so on.
     */
    fun waitForUserCode() {
        val latch = CountDownLatch(1)
        USER_THREAD.execute { latch.countDown() }
        Uninterruptibles.awaitUninterruptibly(latch)
    }

    class UserThread : Thread("bitcoinj user thread"), Executor {
        private val tasks: LinkedBlockingQueue<Runnable>

        init {
            isDaemon = true
            tasks = LinkedBlockingQueue()
            start()
        }

        override fun run() {
            while (true) {
                val task = Uninterruptibles.takeUninterruptibly(tasks)
                try {
                    task.run()
                } catch (throwable: Throwable) {
                    log.warn("Exception in user thread", throwable)
                    val handler = Threading.uncaughtExceptionHandler
                    handler?.uncaughtException(this, throwable)
                }

            }
        }

        override fun execute(command: Runnable) {
            val size = tasks.size
            if (size == WARNING_THRESHOLD) {
                log.warn(
                        "User thread has {} pending tasks, memory exhaustion may occur.\n" +
                                "If you see this message, check your memory consumption and see if it's problematic or excessively spikey.\n" +
                                "If it is, check for deadlocked or slow event handlers. If it isn't, try adjusting the constant \n" +
                                "Threading.UserThread.WARNING_THRESHOLD upwards until it's a suitable level for your app, or Integer.MAX_VALUE to disable.", size)
            }
            Uninterruptibles.putUninterruptibly(tasks, command)
        }

        companion object {
            private val log = LoggerFactory.getLogger(UserThread::class.java)
            // 10,000 pending tasks is entirely arbitrary and may or may not be appropriate for the device we're
            // running on.
            var WARNING_THRESHOLD = 10000
        }
    }

    init {
        // Default policy goes here. If you want to change this, use one of the static methods before
        // instantiating any bitcoinj objects. The policy change will take effect only on new objects
        // from that point onwards.
        throwOnLockCycles()

        USER_THREAD = UserThread()
        SAME_THREAD = Executor { runnable -> runnable.run() }
    }


    fun lock(name: String): ReentrantLock {
        // TODO vilches
//        return if (Utils.isAndroidRuntime)
//            ReentrantLock(true)
//        else
            return factory.newReentrantLock(name)
    }

    fun warnOnLockCycles() {
        setPolicy(CycleDetectingLockFactory.Policies.WARN)
    }

    fun throwOnLockCycles() {
        setPolicy(CycleDetectingLockFactory.Policies.THROW)
    }

    fun ignoreLockCycles() {
        setPolicy(CycleDetectingLockFactory.Policies.DISABLED)
    }

    fun setPolicy(policy: CycleDetectingLockFactory.Policy) {
        Threading.policy = policy
        factory = CycleDetectingLockFactory.newInstance(policy)
    }

    fun getPolicy(): CycleDetectingLockFactory.Policy? {
        return policy
    }

}
