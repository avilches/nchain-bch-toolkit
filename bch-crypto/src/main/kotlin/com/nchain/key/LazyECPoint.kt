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

package com.nchain.key

import org.spongycastle.math.ec.ECCurve
import org.spongycastle.math.ec.ECFieldElement
import org.spongycastle.math.ec.ECPoint
import java.math.BigInteger
import java.util.*

/**
 * A wrapper around ECPoint that delays decoding of the point for as long as possible. This is useful because point
 * encode/decode in Bouncy Castle is quite slow especially on Dalvik, as it often involves decompression/recompression.
 */
class LazyECPoint {
    // If curve is set, bits is also set. If curve is unset, point is set and bits is unset. Point can be set along
    // with curve and bits when the cached form has been accessed and thus must have been converted.

    private val curve: ECCurve?
    private val bits: ByteArray?

    // This field is effectively final - once set it won't change again. However it can be set after
    // construction.
    private var point: ECPoint? = null

    // Delegated methods.

    val detachedPoint: ECPoint
        get() = get().detachedPoint

    val encoded: ByteArray
        get() = if (bits != null)
            Arrays.copyOf(bits, bits.size)
        else
            get().encoded

    val isInfinity: Boolean
        get() = get().isInfinity

    val yCoord: ECFieldElement
        get() = get().yCoord

    val zCoords: Array<ECFieldElement>
        get() = get().zCoords

    val isNormalized: Boolean
        get() = get().isNormalized

    val isCompressed: Boolean
        get() = if (bits != null)
            bits[0].toInt() == 2 || bits[0].toInt() == 3
        else
            get().isCompressed

    val isValid: Boolean
        get() = get().isValid

    val xCoord: ECFieldElement
        get() = get().xCoord

    val y: ECFieldElement
        get() = this.normalize().yCoord

    val affineYCoord: ECFieldElement
        get() = get().affineYCoord

    val affineXCoord: ECFieldElement
        get() = get().affineXCoord

    val x: ECFieldElement
        get() = this.normalize().xCoord

    private val canonicalEncoding: ByteArray
        get() = getEncoded(true)

    constructor(curve: ECCurve, bits: ByteArray) {
        this.curve = curve
        this.bits = bits
    }

    constructor(point: ECPoint) {
        this.point = point
        this.curve = null
        this.bits = null
    }

    fun get(): ECPoint {
        if (point == null)
            point = curve!!.decodePoint(bits!!)
        return point!!
    }

    fun timesPow2(e: Int): ECPoint {
        return get().timesPow2(e)
    }

    fun multiply(k: BigInteger): ECPoint {
        return get().multiply(k)
    }

    fun subtract(b: ECPoint): ECPoint {
        return get().subtract(b)
    }

    fun scaleY(scale: ECFieldElement): ECPoint {
        return get().scaleY(scale)
    }

    fun scaleX(scale: ECFieldElement): ECPoint {
        return get().scaleX(scale)
    }

    fun equals(other: ECPoint): Boolean {
        return get().equals(other)
    }

    fun negate(): ECPoint {
        return get().negate()
    }

    fun threeTimes(): ECPoint {
        return get().threeTimes()
    }

    fun getZCoord(index: Int): ECFieldElement {
        return get().getZCoord(index)
    }

    fun getEncoded(compressed: Boolean): ByteArray {
        return if (compressed == isCompressed && bits != null)
            Arrays.copyOf(bits, bits.size)
        else
            get().getEncoded(compressed)
    }

    fun add(b: ECPoint): ECPoint {
        return get().add(b)
    }

    fun twicePlus(b: ECPoint): ECPoint {
        return get().twicePlus(b)
    }

    fun getCurve(): ECCurve {
        return get().curve
    }

    fun normalize(): ECPoint {
        return get().normalize()
    }

    fun twice(): ECPoint {
        return get().twice()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        return if (o == null || javaClass != o.javaClass) false else Arrays.equals(canonicalEncoding, (o as LazyECPoint).canonicalEncoding)
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(canonicalEncoding)
    }
}
