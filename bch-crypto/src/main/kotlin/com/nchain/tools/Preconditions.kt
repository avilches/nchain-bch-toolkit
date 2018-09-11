package com.nchain.tools

/*
 * @author Alberto Vilches
 * @date 11/09/2018
 */
object Preconditions {

    @JvmStatic
    fun checkArgument(condition: Boolean) {
        check(condition)
    }

    @JvmStatic
    fun checkArgument(condition: Boolean, message: Any) {
        check(condition, { message.toString() })
    }

    @JvmStatic
    fun checkState(condition: Boolean) {
        check(condition)
    }

    @JvmStatic
    fun checkState(condition: Boolean, message: Any) {
        check(condition, { message.toString() })
    }
}