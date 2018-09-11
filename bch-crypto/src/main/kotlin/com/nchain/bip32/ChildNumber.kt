/*
 * Copyright 2013 Matija Mazi.
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

package com.nchain.bip32
import com.nchain.key.DumpedPrivateKey
import java.util.*

/**
 *
 * This is just a wrapper for the i (child number) as per BIP 32 with a boolean getter for the most significant bit
 * and a getter for the actual 0-based child number. A [java.util.List] of these forms a *path* through a
 * [DeterministicHierarchy]. This class is immutable.
 */
class ChildNumber : Comparable<ChildNumber> {

    /** Integer i as per BIP 32 spec, including the MSB denoting derivation type (0 = public, 1 = private)  */
    /** Returns the uint32 encoded form of the path element, including the most significant bit.  */
    val i: Int

    val isHardened: Boolean
        get() = hasHardenedBit(i)

    constructor(childNumber: Int, isHardened: Boolean) {
        if (hasHardenedBit(childNumber))
            throw IllegalArgumentException("Most significant bit is reserved and shouldn't be set: $childNumber")
        i = if (isHardened) childNumber or HARDENED_BIT else childNumber
    }

    constructor(i: Int) {
        this.i = i
    }

    /** Returns the uint32 encoded form of the path element, including the most significant bit.  */
    fun i(): Int {
        return i
    }

    /** Returns the child number without the hardening bit set (i.e. index in that part of the tree).  */
    fun num(): Int {
        return i and HARDENED_BIT.inv()
    }

    override fun toString(): String {
        return String.format(Locale.US, "%d%s", num(), if (isHardened) "H" else "")
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        return if (o != null && o is ChildNumber)
            o.i == i
        else
            false
    }

    override fun hashCode(): Int {
        return i
    }

    override fun compareTo(other: ChildNumber): Int {
        // note that in this implementation compareTo() is not consistent with equals()
        return Integer.compare(this.num(), other.num())
    }

    companion object {
        /**
         * The bit that's set in the child number to indicate whether this key is "hardened". Given a hardened key, it is
         * not possible to derive a child public key if you know only the hardened public key. With a non-hardened key this
         * is possible, so you can derive trees of public keys given only a public parent, but the downside is that it's
         * possible to leak private keys if you disclose a parent public key and a child private key (elliptic curve maths
         * allows you to work upwards).
         */
        val HARDENED_BIT = -0x80000000

        val ZERO = ChildNumber(0)
        val ONE = ChildNumber(1)
        val ZERO_HARDENED = ChildNumber(0, true)

        private fun hasHardenedBit(a: Int): Boolean {
            return a and HARDENED_BIT != 0
        }
    }
}
