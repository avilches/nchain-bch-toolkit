/*
 * Copyright 2011 Google Inc.
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

package com.nchain.key

open class VerificationException : RuntimeException {
    constructor(msg: String) : super(msg) {}
    constructor(msg:String, e: Exception) : super(msg, e) {}
    constructor(e: Exception) : super(e) {}

    class BlockVersionOutOfDate(version: Long) : VerificationException("Block version #"
            + version + " is outdated.")

    class CoinbaseHeightMismatch(message: String) : VerificationException(message)

    class CoinbaseScriptSizeOutOfRange : VerificationException("Coinbase script size out of range")

    class DuplicatedOutPoint : VerificationException("Duplicated outpoint")

    class EmptyInputsOrOutputs : VerificationException("Transaction had no inputs or no outputs.")

    class ExcessiveValue : VerificationException("Total transaction output value greater than possible")

    class LargerThanMaxBlockSize : VerificationException("Transaction larger than MAX_BLOCK_SIZE")

    class NegativeValueOutput : VerificationException("Transaction output negative")

    class SignatureFormatError : VerificationException {
        constructor(msg: String) : super(msg) {}
        constructor(e: Exception) : super(e) {}
    }

    class UnexpectedCoinbaseInput : VerificationException("Coinbase input as input in non-coinbase transaction")

}
