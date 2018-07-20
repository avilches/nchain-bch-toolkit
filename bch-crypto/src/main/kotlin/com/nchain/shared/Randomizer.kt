package com.nchain.shared

import java.security.SecureRandom

/*
 * @author Alberto Vilches
 * @date 17/07/2018
 */


// TODO: review a better way configure with Android
object Randomizer {
    var random:SecureRandom = SecureRandom()
    fun nextBytes(bytes:ByteArray) = random.nextBytes(bytes)
}