package com.nchain.shared

import com.nchain.bitcoinkt.utils.Utils
import java.security.SecureRandom

/*
 * @author Alberto Vilches
 * @date 17/07/2018
 */


// TODO: review a better way configure with Android
object Randomizer {

    init {
        // Init proper random number generator, as some old Android installations have bugs that make it unsecure.
        if (Utils.isAndroidRuntime)
            LinuxSecureRandom()

    }

    var random:SecureRandom = SecureRandom()
    fun nextBytes(bytes:ByteArray) = random.nextBytes(bytes)
}