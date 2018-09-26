/*
 * Copyright 2011 Google Inc.
 * Copyright 2014 Andreas Schildbach
 * Copyright 2018 nChain Ltd
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file has been modified for the bitcoinkt project.
 * The original file was from the bitcoinj-cash project (https://github.com/bitcoinj-cash/bitcoinj).
 */

package com.nchain.bitcoinkt.utils

import com.google.common.base.Charsets
import com.google.common.base.Joiner
import com.google.common.base.Preconditions.checkArgument
import com.google.common.collect.Lists
import com.google.common.collect.Ordering
import com.google.common.io.BaseEncoding
import com.google.common.io.Resources
import com.google.common.primitives.Ints
import com.google.common.primitives.UnsignedLongs
import com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly
import com.nchain.address.AddressFormatException
import com.nchain.address.Base58
import com.nchain.shared.Sha256Hash
import com.nchain.shared.VarInt
import org.spongycastle.crypto.digests.RIPEMD160Digest
import java.io.*
import java.math.BigInteger
import java.net.URL
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.experimental.and
import kotlin.experimental.or

/**
 * A collection of various utility methods that are helpful for working with the Bitcoin protocol.
 * To enable debug logging from the library, run with -Dbitcoinj.logging=true on your command line.
 */
object Utils {

    private var mockSleepQueue: BlockingQueue<Boolean>? = null
   /**
     * If non-null, overrides the return value of now().
     */
    @Volatile
    var mockTime: Date? = null

    private val UTC = TimeZone.getTimeZone("UTC")

    val isWindows: Boolean
        get() = System.getProperty("os.name").toLowerCase().contains("win")

    // 00000001, 00000010, 00000100, 00001000, ...
    private val bitMask = intArrayOf(0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80)

    private var isAndroid = -1
    val isAndroidRuntime: Boolean
        get() {
            if (isAndroid == -1) {
                val runtime = System.getProperty("java.runtime.name")
                isAndroid = if (runtime != null && runtime == "Android Runtime") 1 else 0
            }
            return isAndroid == 1
        }

    internal var ForkBlockTime: Long = 1501593374 // 6 blocks after the fork time


    /**
     * Advances (or rewinds) the mock clock by the given number of seconds.
     */
    fun rollMockClock(seconds: Int): Date? {
        return rollMockClockMillis((seconds * 1000).toLong())
    }

    /**
     * Advances (or rewinds) the mock clock by the given number of milliseconds.
     */
    fun rollMockClockMillis(millis: Long): Date? {
        if (mockTime == null)
            throw IllegalStateException("You need to use setMockClock() first.")
        mockTime = Date(mockTime!!.time + millis)
        return mockTime
    }

    /**
     * Sets the mock clock to the current time.
     */
    fun setMockClock() {
        mockTime = Date()
    }

    /**
     * Sets the mock clock to the given time (in seconds).
     */
    fun setMockClock(mockClockSeconds: Long) {
        mockTime = Date(mockClockSeconds * 1000)
    }

    /**
     * Returns the current time, or a mocked out equivalent.
     */
    fun now(): Date? {
        return if (mockTime != null) mockTime else Date()
    }

    // TODO: Replace usages of this where the result is / 1000 with currentTimeSeconds.
    /** Returns the current time in milliseconds since the epoch, or a mocked out equivalent.  */
    fun currentTimeMillis(): Long {
        return if (mockTime != null) mockTime!!.time else System.currentTimeMillis()
    }

    fun currentTimeSeconds(): Long {
        return currentTimeMillis() / 1000
    }

    /**
     * Formats a given date+time value to an ISO 8601 string.
     * @param dateTime value to format, as a Date
     */
    fun dateTimeFormat(dateTime: Date): String {
        val iso8601 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        iso8601.timeZone = UTC
        return iso8601.format(dateTime)
    }

    /**
     * Formats a given date+time value to an ISO 8601 string.
     * @param dateTime value to format, unix time (ms)
     */
    fun dateTimeFormat(dateTime: Long): String {
        val iso8601 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        iso8601.timeZone = UTC
        return iso8601.format(dateTime)
    }




    /**
     * Encodes the given string into a sequence of bytes using the named charset.
     *
     *
     * This is a convenience method which wraps the checked exception with a RuntimeException.
     * The exception can never occur given the charsets
     * US-ASCII, ISO-8859-1, UTF-8, UTF-16, UTF-16LE or UTF-16BE.
     *
     * @param str the string to encode into bytes
     * @param charsetName the name of a supported [charset][java.nio.charset.Charset]
     * @return the encoded bytes
     */
    fun toBytes(str: CharSequence, charsetName: String): ByteArray {
        try {
            return str.toString().toByteArray(charset(charsetName))
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }
    }

    /** Sleep for a span of time, or mock sleep if enabled  */
    fun sleep(millis: Long) {
        if (mockSleepQueue == null) {
            sleepUninterruptibly(millis, TimeUnit.MILLISECONDS)
        } else {
            try {
                val isMultiPass = mockSleepQueue!!.take()
                rollMockClockMillis(millis)
                if (isMultiPass)
                    mockSleepQueue!!.offer(true)
            } catch (e: InterruptedException) {
                // Ignored.
            }
        }
    }

    /** Enable or disable mock sleep.  If enabled, set mock time to current time.  */
    fun setMockSleep(isEnable: Boolean) {
        if (isEnable) {
            mockSleepQueue = ArrayBlockingQueue(1)
            mockTime = Date(System.currentTimeMillis())
        } else {
            mockSleepQueue = null
        }
    }

    /** Let sleeping thread pass the synchronization point.   */
    fun passMockSleep() {
        mockSleepQueue!!.offer(false)
    }

    /** Let the sleeping thread pass the synchronization point any number of times.  */
    fun finishMockSleep() {
        if (mockSleepQueue != null) {
            mockSleepQueue!!.offer(true)
        }
    }

    private class Pair(internal var item: Int, internal var count: Int) : Comparable<Pair> {
        // note that in this implementation compareTo() is not consistent with equals()
        override fun compareTo(o: Pair): Int {
            return -Ints.compare(count, o.count)
        }
    }

    fun maxOfMostFreq(items: List<Int>): Int {
        var items = items
        if (items.isEmpty())
            return 0
        // This would be much easier in a functional language (or in Java 8).
        items = Ordering.natural<Int>().reverse<Int>().sortedCopy(items)
        val pairs = Lists.newLinkedList<Pair>()
        pairs.add(Pair(items[0], 0))
        for (item in items) {
            var pair = pairs.last
            if (pair.item != item)
                pairs.add(Utils.Pair(item, 1))
            else
                pair.count++
        }
        // pairs now contains a uniqified list of the sorted inputs, with counts for how often that item appeared.
        // Now sort by how frequently they occur, and pick the max of the most frequent.
        pairs.sort()
        val maxCount = pairs.first.count
        var maxItem = pairs.first.item
        for (pair in pairs) {
            if (pair.count != maxCount)
                break
            maxItem = Math.max(maxItem, pair.item)
        }
        return maxItem
    }

}