package com.nchain.tools

import java.util.logging.Logger

/*
 * @author Alberto Vilches
 * @date 18/07/2018
 */


fun <T> loggerFor(clazz: Class<T>) = KLog(Logger.getLogger(clazz.toString()))

// TOODO: use ls4j
class KLog(val logger:Logger) {
    fun error(s: String, e: NullPointerException) {
        logger.severe("$e: $s")
    }

    fun info(s: String) {
        logger.info(s)
    }

    fun debug(s: String) {
        logger.info(s)
    }


}