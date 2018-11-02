package com.nchain.tools

import java.io.InputStream

/*
 * @author Alberto Vilches
 * @date 10/08/2018
 */
class InputStreamIterator(val inputStream: InputStream) : Iterator<Int>, InputStream() {
    var nextInt: Int = -1
    var hasNextInt: Boolean = false
    override fun next(): Int {
        return read()
    }

    override fun hasNext(): Boolean {
        if (!hasNextInt) {
            nextInt = read()
            hasNextInt = true
        }
        return nextInt != -1
    }

    override fun read(): Int {
        if (hasNextInt) {
            hasNextInt = false
            return nextInt
        }
        return inputStream.read()
    }
}