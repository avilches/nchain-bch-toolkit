package com.nchain.tools

/*
 * @author Alberto Vilches
 * @date 16/07/2018
 */

//private val HEX_CHARS = "0123456789ABCDEF".toCharArray()
private val HEX_CHARS = "00112233445566778899aAbBcCdDeEfF".toCharArray()

fun ByteArray.toHex(): String {
    val result = StringBuffer()
    forEach {
        val octet = it.toInt()
        val firstIndex = (octet and 0xF0).ushr(4)
        val secondIndex = octet and 0x0F
        result.append(HEX_CHARS[firstIndex*2])
        result.append(HEX_CHARS[secondIndex*2])
    }

    return result.toString()
}

fun String.hexStringToByteArray(): ByteArray {
    val result = ByteArray(length / 2)
    for (i in 0 until length step 2) {
        val firstIndex = HEX_CHARS.indexOf(this[i]) / 2
        val secondIndex = HEX_CHARS.indexOf(this[i + 1]) / 2

        val octet = firstIndex.shl(4).or(secondIndex)
        result.set(i.shr(1), octet.toByte())
    }
    return result
}

object HEX {
    fun hexToBytes(hex: String): ByteArray = hex.hexStringToByteArray()
    fun bytesToHex(bytes: ByteArray): String = bytes.toHex()
}