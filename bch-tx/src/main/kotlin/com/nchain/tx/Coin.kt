/*
 * Copyright 2014 Andreas Schildbach
 * Copyright 2018 nChain Ltd.
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
 *
 * This file has been modified for the bitcoinkt project.
 * The original file was from the bitcoinj-cash project (https://github.com/bitcoinj-cash/bitcoinj).
 */

package com.nchain.tx

import com.nchain.tools.LongMath
import java.io.Serializable
import java.math.BigDecimal

import java.text.DecimalFormat

/**
 * Represents a monetary Bitcoin Cash value. Coin objects are immutable.
 */
/**
 * The number of satoshis of this monetary value.
 */
class Coin private constructor(val value: Long): Comparable<Coin>, Serializable {

    /**
     * The sign of the value
     */
    val signum: Int
        get() {
            if (this.value == 0L) return 0
            return if (this.value < 0) -1 else 1
        }

    /**
     * True if and only if this instance represents a monetary value greater than zero,
     * otherwise false.
     */
    val isPositive: Boolean
        get() = signum == 1

    /**
     * True if and only if this instance represents a monetary value less than zero,
     * otherwise false.
     */
    val isNegative: Boolean
        get() = signum == -1

    /**
     * True if and only if this instance represents zero monetary value,
     * otherwise false.
     */
    val isZero: Boolean
        get() = signum == 0

    /**
     * Number of decimals for one Bitcoin. This constant is useful for quick adapting to other coins because a lot of
     * constants derive from it.
     */
//    val smallestUnitExponent = SMALLEST_UNIT_EXPONENT


    fun add(value: Coin): Coin {
        return Coin(LongMath.checkedAdd(this.value, value.value))
    }

    fun subtract(value: Coin): Coin {
        return Coin(LongMath.checkedSubtract(this.value, value.value))
    }

    fun multiply(factor: Long): Coin {
        return Coin(LongMath.checkedMultiply(this.value, factor))
    }

    fun divide(divisor: Long): Coin {
        return Coin(this.value / divisor)
    }

    fun divide(divisor: Coin): Long {
        return this.value / divisor.value
    }

    fun divideAndRemainder(divisor: Long): Array<Coin> {
        return arrayOf(Coin(this.value / divisor), Coin(this.value % divisor))
    }

    /**
     * Returns true if the monetary value represented by this instance is greater than that
     * of the given other Coin, otherwise false.
     */
    fun isGreaterThan(other: Coin): Boolean {
        return compareTo(other) > 0
    }

    fun isGreaterThanOrEquals(other: Coin): Boolean {
        return compareTo(other) >= 0
    }

    /**
     * Returns true if the monetary value represented by this instance is less than that
     * of the given other Coin, otherwise false.
     */
    fun isLessThan(other: Coin): Boolean {
        return compareTo(other) < 0
    }

    fun isLessThanOrEquals(other: Coin): Boolean {
        return compareTo(other) <= 0
    }

    fun shiftLeft(n: Int): Coin {
        return Coin(this.value shl n)
    }

    fun shiftRight(n: Int): Coin {
        return Coin(this.value shr n)
    }

    fun negate(): Coin {
        return Coin(-this.value)
    }

    /**
     * Returns the value as a 0.12 type string. More digits after the decimal place will be used
     * if necessary, but two will always be present.
     */
    fun toFriendlyString(): String {
        return DecimalFormat("0.00######").format(value)
    }

    /**
     * Returns the value as a plain string denominated in BCH.
     * The result is unformatted with no trailing zeroes.
     * For instance, a value of 150000 satoshis gives an output string of "0.0015"
     */
//    fun toPlainString(): String {
//        return PLAIN_FORMAT.format(this).toString()
//    }

    override fun toString(): String {
        return java.lang.Long.toString(value)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return if (other == null || javaClass != other.javaClass) false else this.value == (other as Coin).value
    }

    override fun hashCode(): Int {
        return this.value.toInt()
    }

    override fun compareTo(other: Coin): Int {
        return value.compareTo(other.value)
    }

    companion object {

        /**
         * Number of decimals for one Bitcoin. This constant is useful for quick adapting to other coins because a lot of
         * constants derive from it.
         */
        const val SMALLEST_UNIT_EXPONENT = 8

        /**
         * The number of satoshis equal to one bitcoin.
         */
        private val COIN_VALUE = LongMath.pow(10, SMALLEST_UNIT_EXPONENT)

        /**
         * Zero Bitcoins.
         */
        @JvmStatic val ZERO = Coin.valueOf(0)

        /**
         * One Bitcoin.
         */
        @JvmStatic val COIN = Coin.valueOf(COIN_VALUE)

        /**
         * 0.01 Bitcoins. This unit is not really used much.
         */
        @JvmStatic val CENT = COIN.divide(100)

        /**
         * 0.001 Bitcoins, also known as 1 mBCH.
         */
        @JvmStatic val MILLICOIN = COIN.divide(1000)

        /**
         * 0.000001 Bitcoins, also known as 1 µBCH or 1 uBCH.
         */
        @JvmStatic val MICROCOIN = MILLICOIN.divide(1000)

        /**
         * A satoshi is the smallest unit that can be transferred. 100 million of them fit into a Bitcoin.
         */
        @JvmStatic val SATOSHI = Coin.valueOf(1)

        @JvmStatic val FIFTY_COINS = COIN.multiply(50)

        /**
         * Represents a monetary value of minus one satoshi.
         */
        @JvmStatic val NEGATIVE_SATOSHI = Coin.valueOf(-1)

        @JvmStatic fun valueOf(satoshis: Long): Coin {
            return Coin(satoshis)
        }

        /**
         * Convert an amount expressed in the way humans are used to into satoshis.
         */
        @JvmStatic fun valueOf(coins: Int, cents: Int): Coin {
            check(cents < 100)
            check(cents >= 0)
            check(coins >= 0)
            return COIN.multiply(coins.toLong()).add(CENT.multiply(cents.toLong()))
        }

        /**
         * Parses an amount expressed in the way humans are used to.
         *
         * This takes string in a format understood by [BigDecimal.BigDecimal],
         * for example "0", "1", "0.10", "1.23E3", "1234.5E-5".
         *
         * @throws IllegalArgumentException if you try to specify fractional satoshis, or a value out of range.
         */
        @JvmStatic fun parseCoin(str: String): Coin {
            try {
                val satoshis = BigDecimal(str).movePointRight(SMALLEST_UNIT_EXPONENT).toBigIntegerExact().toLong()
                return Coin.valueOf(satoshis)
            } catch (e: ArithmeticException) {
                throw IllegalArgumentException(e) // Repackage exception to honor method contract
            }

        }

//        private val FRIENDLY_FORMAT = MonetaryFormat.BCH.minDecimals(2).repeatOptionalDecimals(1, 6).postfixCode()
//        private val PLAIN_FORMAT = MonetaryFormat.BCH.minDecimals(0).repeatOptionalDecimals(1, 8).noCode()
    }
}
