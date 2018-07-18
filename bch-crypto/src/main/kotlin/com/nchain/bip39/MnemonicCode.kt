/*
 * Copyright 2013 Ken Sedgwick
 * Copyright 2014 Andreas Schildbach
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

package com.nchain.bip39

import com.nchain.tools.loggerFor
import com.nchain.shared.Sha256Hash
import com.nchain.tools.Stopwatch
import com.nchain.tools.toHex
import java.io.IOException
import java.io.StringReader
import java.util.*
import kotlin.experimental.or

/**
 * A MnemonicCode object may be used to convert between binary seed values and
 * lists of words per [the BIP 39
 * specification](https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki)
 */

class MnemonicCode
@Throws(IOException::class, IllegalArgumentException::class)
private constructor(val wordList:List<String>) {

    init {
        check(wordList.size == 2048)
    }
    /**
     * Convert mnemonic word list to original entropy value.
     */
    @Throws(MnemonicException.MnemonicLengthException::class, MnemonicException.MnemonicWordException::class, MnemonicException.MnemonicChecksumException::class)
    fun toEntropy(words: List<String>): ByteArray {
        if (words.size % 3 > 0)
            throw MnemonicException.MnemonicLengthException("Word list size must be multiple of three words.")

        if (words.size == 0)
            throw MnemonicException.MnemonicLengthException("Word list is empty.")

        // Look up all the words in the list and construct the
        // concatenation of the original entropy and the checksum.
        //
        val concatLenBits = words.size * 11
        val concatBits = BooleanArray(concatLenBits)
        var wordindex = 0
        for (word in words) {
            // Find the words index in the wordlist.
            val ndx = Collections.binarySearch(this.wordList, word)
            if (ndx < 0)
                throw MnemonicException.MnemonicWordException(word)

            // Set the next 11 bits to the value of the index.
            for (ii in 0..10)
                concatBits[wordindex * 11 + ii] = ndx and (1 shl 10 - ii) != 0
            ++wordindex
        }

        val checksumLengthBits = concatLenBits / 33
        val entropyLengthBits = concatLenBits - checksumLengthBits

        // Extract original entropy as bytes.
        val entropy = ByteArray(entropyLengthBits / 8)
        for (ii in entropy.indices)
            for (jj in 0..7)
                if (concatBits[ii * 8 + jj])
                    entropy[ii] = entropy[ii] or (1 shl 7 - jj).toByte()

        // Take the digest of the entropy.
        val hash = Sha256Hash.hash(entropy)
        val hashBits = bytesToBits(hash)

        // Check all the checksum bits.
        for (i in 0 until checksumLengthBits)
            if (concatBits[entropyLengthBits + i] != hashBits[i])
                throw MnemonicException.MnemonicChecksumException()

        return entropy
    }

    /**
     * Convert entropy data to mnemonic word list.
     */
    @Throws(MnemonicException.MnemonicLengthException::class)
    fun toMnemonic(entropy: ByteArray): List<String> {
        if (entropy.size % 4 > 0)
            throw MnemonicException.MnemonicLengthException("Entropy length not multiple of 32 bits.")

        if (entropy.size == 0)
            throw MnemonicException.MnemonicLengthException("Entropy is empty.")

        // We take initial entropy of ENT bits and compute its
        // checksum by taking first ENT / 32 bits of its SHA256 hash.

        val hash = Sha256Hash.hash(entropy)
        val hashBits = bytesToBits(hash)

        val entropyBits = bytesToBits(entropy)
        val checksumLengthBits = entropyBits.size / 32

        // We append these bits to the end of the initial entropy.
        val concatBits = BooleanArray(entropyBits.size + checksumLengthBits)
        System.arraycopy(entropyBits, 0, concatBits, 0, entropyBits.size)
        System.arraycopy(hashBits, 0, concatBits, entropyBits.size, checksumLengthBits)

        // Next we take these concatenated bits and split them into
        // groups of 11 bits. Each group encodes number from 0-2047
        // which is a position in a wordlist.  We convert numbers into
        // words and use joined words as mnemonic sentence.

        val words = ArrayList<String>()
        val nwords = concatBits.size / 11
        for (i in 0 until nwords) {
            var index = 0
            for (j in 0..10) {
                index = index shl 1
                if (concatBits[i * 11 + j])
                    index = index or 0x1
            }
            words.add(this.wordList[index])
        }

        return words
    }

    /**
     * Check to see if a mnemonic word list is valid.
     */
    @Throws(MnemonicException::class)
    fun check(words: List<String>) {
        toEntropy(words)
    }

    companion object {
        private val log = loggerFor(MnemonicCode::class.java)

        private val BIP39_ENGLISH_RESOURCE_NAME = "/mnemonic/english.txt"
        private val BIP39_ENGLISH_SHA256 = "ad90bf3beb7b0eb7e5acd74727dc0da96e0a280a258354e7293fb7e211ac03db"

        /** UNIX time for when the BIP39 standard was finalised. This can be used as a default seed birthday.  */
        var BIP39_STANDARDISATION_TIME_SECS: Long = 1381276800

        private val PBKDF2_ROUNDS = 2048

        var ENGLISH: MnemonicCode = MnemonicCode(openDefaultWords(BIP39_ENGLISH_RESOURCE_NAME, BIP39_ENGLISH_SHA256))

        @Throws(IOException::class)
        private fun openDefaultWords(resource:String, wordListDigestHex: String): List<String> {
            val englishWords = MnemonicCode::class.java.getResource(resource).readText()
            val words = StringReader(englishWords).readLines()

            val md = Sha256Hash.newDigest()
            words.forEach {
                md.update(it.toByteArray())
            }
            val hexdigest = md.digest().toHex()
            check(hexdigest.equals(wordListDigestHex, true))
            return words
        }

        /**
         * Convert mnemonic word list to seed.
         */
        @JvmStatic fun toSeed(words: List<String>, passphrase: String): ByteArray {

            // To create binary seed from mnemonic, we use PBKDF2 function
            // with mnemonic sentence (in UTF-8) used as a password and
            // string "mnemonic" + passphrase (again in UTF-8) used as a
            // salt. Iteration count is set to 4096 and HMAC-SHA512 is
            // used as a pseudo-random function. Desired length of the
            // derived key is 512 bits (= 64 bytes).
            //
            val pass = words.joinToString(" ")
            val salt = "mnemonic$passphrase"

            val watch = Stopwatch().start()
            val seed = PBKDF2SHA512.derive(pass, salt, PBKDF2_ROUNDS, 64)
            watch.stop()
            log.info("PBKDF2 took ${watch.elapsed}ms")
            return seed
        }

        private fun bytesToBits(data: ByteArray): BooleanArray {
            val bits = BooleanArray(data.size * 8)
            for (i in data.indices)
                for (j in 0..7)
                    bits[i * 8 + j] = data[i].toInt() and (1 shl 7 - j) != 0
            return bits
        }
    }
}
/** Initialise from the included word list. Won't work on Android.  */
