/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.nchain.tools;

 public class LongMath {

     /**
      * Returns the sum of {@code a} and {@code b}, provided it does not overflow.
      *
      * @throws ArithmeticException if {@code a + b} overflows in signed {@code long} arithmetic
      */
     public static long checkedAdd(long a, long b) {
         long result = a + b;
         checkNoOverflow((a ^ b) < 0 | (a ^ result) >= 0, "checkedAdd", a, b);
         return result;
     }

     /**
      * Returns the difference of {@code a} and {@code b}, provided it does not overflow.
      *
      * @throws ArithmeticException if {@code a - b} overflows in signed {@code long} arithmetic
      */
     public static long checkedSubtract(long a, long b) {
         long result = a - b;
         checkNoOverflow((a ^ b) >= 0 | (a ^ result) >= 0, "checkedSubtract", a, b);
         return result;
     }

     /**
      * Returns the product of {@code a} and {@code b}, provided it does not overflow.
      *
      * @throws ArithmeticException if {@code a * b} overflows in signed {@code long} arithmetic
      */
     public static long checkedMultiply(long a, long b) {
         // Hacker's Delight, Section 2-12
         int leadingZeros =
                 Long.numberOfLeadingZeros(a)
                         + Long.numberOfLeadingZeros(~a)
                         + Long.numberOfLeadingZeros(b)
                         + Long.numberOfLeadingZeros(~b);
         /*
          * If leadingZeros > Long.SIZE + 1 it's definitely fine, if it's < Long.SIZE it's definitely
          * bad. We do the leadingZeros check to avoid the division below if at all possible.
          *
          * Otherwise, if b == Long.MIN_VALUE, then the only allowed values of a are 0 and 1. We take
          * care of all a < 0 with their own check, because in particular, the case a == -1 will
          * incorrectly pass the division check below.
          *
          * In all other cases, we check that either a is 0 or the result is consistent with division.
          */
         if (leadingZeros > Long.SIZE + 1) {
             return a * b;
         }
         checkNoOverflow(leadingZeros >= Long.SIZE, "checkedMultiply", a, b);
         checkNoOverflow(a >= 0 | b != Long.MIN_VALUE, "checkedMultiply", a, b);
         long result = a * b;
         checkNoOverflow(a == 0 || result / a == b, "checkedMultiply", a, b);
         return result;
     }

     public static long pow(long b, int k) {
         checkNonNegative("exponent", k);
         if (-2 <= b && b <= 2) {
             switch ((int) b) {
                 case 0:
                     return (k == 0) ? 1 : 0;
                 case 1:
                     return 1;
                 case (-1):
                     return ((k & 1) == 0) ? 1 : -1;
                 case 2:
                     return (k < Long.SIZE) ? 1L << k : 0;
                 case (-2):
                     if (k < Long.SIZE) {
                         return ((k & 1) == 0) ? 1L << k : -(1L << k);
                     } else {
                         return 0;
                     }
                 default:
                     throw new AssertionError();
             }
         }
         for (long accum = 1; ; k >>= 1) {
             switch (k) {
                 case 0:
                     return accum;
                 case 1:
                     return accum * b;
                 default:
                     accum *= ((k & 1) == 0) ? 1 : b;
                     b *= b;
             }
         }
     }

     static final long FLOOR_SQRT_MAX_LONG = 3037000499L;

     /**
      * Returns the {@code b} to the {@code k}th power, provided it does not overflow.
      *
      * @throws ArithmeticException if {@code b} to the {@code k}th power overflows in signed {@code
      *                             long} arithmetic
      */
     public static long checkedPow(long b, int k) {
         checkNonNegative("exponent", k);
         if (b >= -2 & b <= 2) {
             switch ((int) b) {
                 case 0:
                     return (k == 0) ? 1 : 0;
                 case 1:
                     return 1;
                 case (-1):
                     return ((k & 1) == 0) ? 1 : -1;
                 case 2:
                     checkNoOverflow(k < Long.SIZE - 1, "checkedPow", b, k);
                     return 1L << k;
                 case (-2):
                     checkNoOverflow(k < Long.SIZE, "checkedPow", b, k);
                     return ((k & 1) == 0) ? (1L << k) : (-1L << k);
                 default:
                     throw new AssertionError();
             }
         }
         long accum = 1;
         while (true) {
             switch (k) {
                 case 0:
                     return accum;
                 case 1:
                     return checkedMultiply(accum, b);
                 default:
                     if ((k & 1) != 0) {
                         accum = checkedMultiply(accum, b);
                     }
                     k >>= 1;
                     if (k > 0) {
                         checkNoOverflow(
                                 -FLOOR_SQRT_MAX_LONG <= b && b <= FLOOR_SQRT_MAX_LONG, "checkedPow", b, k);
                         b *= b;
                     }
             }
         }
     }

     static void checkNoOverflow(boolean condition, String methodName, long a, long b) {
         if (!condition) {
             throw new ArithmeticException("overflow: " + methodName + "(" + a + ", " + b + ")");
         }
     }

     static double checkNonNegative(String role, double x) {
         if (!(x >= 0)) { // not x < 0, to work with NaN.
             throw new IllegalArgumentException(role + " (" + x + ") must be >= 0");
         }
         return x;
     }

 }
