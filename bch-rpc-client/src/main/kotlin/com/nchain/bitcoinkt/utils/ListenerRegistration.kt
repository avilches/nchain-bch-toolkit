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

import java.util.concurrent.Executor

import com.google.common.base.Preconditions.checkNotNull

/**
 * A simple wrapper around a listener and an executor, with some utility methods.
 */
class ListenerRegistration<T>(listener: T, executor: Executor) {
    val listener: T
    val executor: Executor

    init {
        this.listener = checkNotNull(listener)
        this.executor = checkNotNull(executor)
    }

    companion object {

        /** Returns true if the listener was removed, else false.  */
        fun <T> removeFromList(listener: T, list: MutableList<out ListenerRegistration<T>>): Boolean {
            checkNotNull(listener)

            var item: ListenerRegistration<T>? = null
            for (registration in list) {
                if (registration.listener === listener) {
                    item = registration
                    break
                }
            }
            return item != null && list.remove(item)
        }

    }
}
