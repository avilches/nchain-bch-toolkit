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

package com.nchain.bitcoinkt.utils

import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer
import java.text.MessageFormat
import java.util.Date
import java.util.logging.*

/**
 * A Java logging formatter that writes more compact output than the default.
 */
class BriefLogFormatter : Formatter() {

    override fun format(logRecord: LogRecord): String {
        val arguments = arrayOfNulls<Any>(6)
        arguments[0] = logRecord.threadID
        val fullClassName = logRecord.sourceClassName
        val lastDot = fullClassName.lastIndexOf('.')
        val className = fullClassName.substring(lastDot + 1)
        arguments[1] = className
        arguments[2] = logRecord.sourceMethodName
        arguments[3] = Date(logRecord.millis)
        arguments[4] = logRecord.message
        if (logRecord.thrown != null) {
            val result = StringWriter()
            logRecord.thrown.printStackTrace(PrintWriter(result))
            arguments[5] = result.toString()
        } else {
            arguments[5] = ""
        }
        return messageFormat.format(arguments)
    }

    companion object {
        private val messageFormat = MessageFormat("{3,date,HH:mm:ss} {0} {1}.{2}: {4}\n{5}")

        // OpenJDK made a questionable, backwards incompatible change to the Logger implementation. It internally uses
        // weak references now which means simply fetching the logger and changing its configuration won't work. We must
        // keep a reference to our custom logger around.
        private var logger: Logger? = null

        /** Configures JDK logging to use this class for everything.  */
        fun init() {
            logger = Logger.getLogger("")
            val handlers = logger!!.handlers
            // In regular Java there is always a handler. Avian doesn't install one however.
            if (handlers.size > 0)
                handlers[0].formatter = BriefLogFormatter()
        }

        fun initVerbose() {
            init()
            logger!!.level = Level.ALL
            logger!!.log(Level.FINE, "test")
        }

        fun initWithSilentBitcoinJ() {
            init()
            Logger.getLogger("org.bitcoinj").level = Level.SEVERE
        }
    }
}
