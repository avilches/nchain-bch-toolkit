package com.nchain.tools

/*
 * @author Alberto Vilches
 * @date 18/07/2018
 */
class Stopwatch {
    var start = 0L
    var end = 0L

    fun start(): Stopwatch {
        start = System.currentTimeMillis()
        return this
    }

    fun stop():Long {
        end = System.currentTimeMillis()
        return elapsed
    }

    val elapsed
        get() = end - start
}