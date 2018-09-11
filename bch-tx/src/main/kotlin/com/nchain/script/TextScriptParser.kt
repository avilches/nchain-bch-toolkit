/*
 * Copyright 2018 the bitcoinj-cash developers
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
package com.nchain.script

import com.nchain.tools.HEX
import com.nchain.script.Script
import com.nchain.script.ScriptOpCodes

import java.util.HashMap

/**
 * Created by shadders on 8/02/18.
 */
class TextScriptParser @JvmOverloads constructor(val isEnforceHexPrefix: Boolean, variables: Map<String, String>? = null) {
    private val variables = HashMap<String, String>()

    init {
        if (variables != null)
            addVariables(variables)
    }

    fun parse(textScript: String): Script {

        val builder = ScriptBuilder()
        val parts = textScript.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        for (i in parts.indices) {

            var part = parts[i].toUpperCase()

            if (part.startsWith("OP_"))
                part = part.substring(3)

            val opcode = ScriptOpCodes.getOpCode(part)
            if (opcode != ScriptOpCodes.OP_INVALIDOPCODE) {

                builder.op(opcode)

            } else {
                //must be a element be a data element
                if (part.startsWith("<") && part.endsWith(">")) {
                    //variable
                    val key = part.substring(1, part.length - 1)
                    val data = variables[key]
                    builder.data(maybeDecodeHex(data!!))

                } else {
                    //assume hex encoded
                    builder.data(maybeDecodeHex(part))

                }
            }
        }
        return builder.build()
    }

    private fun maybeDecodeHex(data: String): ByteArray {
        var data = data
        if (data.startsWith("0X")) {
            data = data.substring(2)
        } else if (isEnforceHexPrefix) {
            throw RuntimeException("Data element without hex prefix (0x).")
        }
        return HEX.decode(data) //will throw exception on bad data
    }

    fun addVariable(key: String, value: String): String? {
        return variables.put(key.toUpperCase(), value.toUpperCase())
    }

    fun addVariables(map: Map<String, String>) {
        for ((key, value) in map) {
            addVariable(key, value)
        }
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {

            val parser = TextScriptParser(false, null)
            parser.addVariable("barry", "0x00112233")
            val script = parser.parse("<barry> 2 add 4 sub")
            println("script = $script")
        }
    }
}
