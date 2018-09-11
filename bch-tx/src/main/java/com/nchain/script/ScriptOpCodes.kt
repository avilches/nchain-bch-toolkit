/*
 * Copyright 2013 Google Inc.
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
 *
 * This file has been modified by the bitcoinj-cash developers for the bitcoinj-cash project.
 * The original file was from the bitcoinj project (https://github.com/bitcoinj/bitcoinj).
 */

package com.nchain.script

import java.util.Collections
import java.util.HashMap

/**
 * Various constants that define the assembly-like scripting language that forms part of the Bitcoin protocol.
 * See [com.nchain.script.Script] for details. Also provides a method to convert them to a string.
 */
object ScriptOpCodes {
    // push value
    const val OP_0 = 0x00 // push empty vector
    const val OP_FALSE = OP_0
    const val OP_PUSHDATA1 = 0x4c
    const val OP_PUSHDATA2 = 0x4d
    const val OP_PUSHDATA4 = 0x4e
    const val OP_1NEGATE = 0x4f
    const val OP_RESERVED = 0x50
    const val OP_1 = 0x51
    const val OP_TRUE = OP_1
    const val OP_2 = 0x52
    const val OP_3 = 0x53
    const val OP_4 = 0x54
    const val OP_5 = 0x55
    const val OP_6 = 0x56
    const val OP_7 = 0x57
    const val OP_8 = 0x58
    const val OP_9 = 0x59
    const val OP_10 = 0x5a
    const val OP_11 = 0x5b
    const val OP_12 = 0x5c
    const val OP_13 = 0x5d
    const val OP_14 = 0x5e
    const val OP_15 = 0x5f
    const val OP_16 = 0x60

    // control
    const val OP_NOP = 0x61
    const val OP_VER = 0x62
    const val OP_IF = 0x63
    const val OP_NOTIF = 0x64
    const val OP_VERIF = 0x65
    const val OP_VERNOTIF = 0x66
    const val OP_ELSE = 0x67
    const val OP_ENDIF = 0x68
    const val OP_VERIFY = 0x69
    const val OP_RETURN = 0x6a

    // stack ops
    const val OP_TOALTSTACK = 0x6b
    const val OP_FROMALTSTACK = 0x6c
    const val OP_2DROP = 0x6d
    const val OP_2DUP = 0x6e
    const val OP_3DUP = 0x6f
    const val OP_2OVER = 0x70
    const val OP_2ROT = 0x71
    const val OP_2SWAP = 0x72
    const val OP_IFDUP = 0x73
    const val OP_DEPTH = 0x74
    const val OP_DROP = 0x75
    const val OP_DUP = 0x76
    const val OP_NIP = 0x77
    const val OP_OVER = 0x78
    const val OP_PICK = 0x79
    const val OP_ROLL = 0x7a
    const val OP_ROT = 0x7b
    const val OP_SWAP = 0x7c
    const val OP_TUCK = 0x7d

    // splice ops
    const val OP_CAT = 0x7e
    const val OP_SPLIT = 0x7f
    const val OP_NUM2BIN = 0x80
    const val OP_BIN2NUM = 0x81
    const val OP_SIZE = 0x82

    // bit logic
    const val OP_INVERT = 0x83
    const val OP_AND = 0x84
    const val OP_OR = 0x85
    const val OP_XOR = 0x86
    const val OP_EQUAL = 0x87
    const val OP_EQUALVERIFY = 0x88
    const val OP_RESERVED1 = 0x89
    const val OP_RESERVED2 = 0x8a

    // numeric
    const val OP_1ADD = 0x8b
    const val OP_1SUB = 0x8c
    const val OP_2MUL = 0x8d
    const val OP_2DIV = 0x8e
    const val OP_NEGATE = 0x8f
    const val OP_ABS = 0x90
    const val OP_NOT = 0x91
    const val OP_0NOTEQUAL = 0x92
    const val OP_ADD = 0x93
    const val OP_SUB = 0x94
    const val OP_MUL = 0x95
    const val OP_DIV = 0x96
    const val OP_MOD = 0x97
    const val OP_LSHIFT = 0x98
    const val OP_RSHIFT = 0x99
    const val OP_BOOLAND = 0x9a
    const val OP_BOOLOR = 0x9b
    const val OP_NUMEQUAL = 0x9c
    const val OP_NUMEQUALVERIFY = 0x9d
    const val OP_NUMNOTEQUAL = 0x9e
    const val OP_LESSTHAN = 0x9f
    const val OP_GREATERTHAN = 0xa0
    const val OP_LESSTHANOREQUAL = 0xa1
    const val OP_GREATERTHANOREQUAL = 0xa2
    const val OP_MIN = 0xa3
    const val OP_MAX = 0xa4
    const val OP_WITHIN = 0xa5

    // crypto
    const val OP_RIPEMD160 = 0xa6
    const val OP_SHA1 = 0xa7
    const val OP_SHA256 = 0xa8
    const val OP_HASH160 = 0xa9
    const val OP_HASH256 = 0xaa
    const val OP_CODESEPARATOR = 0xab
    const val OP_CHECKSIG = 0xac
    const val OP_CHECKSIGVERIFY = 0xad
    const val OP_CHECKMULTISIG = 0xae
    const val OP_CHECKMULTISIGVERIFY = 0xaf

    // block state
    /**
     * Check lock time of the block. Introduced in BIP 65, replacing OP_NOP2
     */
    const val OP_CHECKLOCKTIMEVERIFY = 0xb1
    const val OP_CHECKSEQUENCEVERIFY = 0xb2

    // expansion
    const val OP_NOP1 = 0xb0
    /**
     * Deprecated by BIP 65
     */
    @Deprecated("")
    const val OP_NOP2 = OP_CHECKLOCKTIMEVERIFY
    @Deprecated("")
    const val OP_NOP3 = OP_CHECKSEQUENCEVERIFY
    const val OP_NOP4 = 0xb3
    const val OP_NOP5 = 0xb4
    const val OP_NOP6 = 0xb5
    const val OP_NOP7 = 0xb6
    const val OP_NOP8 = 0xb7
    const val OP_NOP9 = 0xb8
    const val OP_NOP10 = 0xb9

    const val OP_INVALIDOPCODE = 0xff

    private val opCodeMap = opCodesMap()

    private val opCodeNameMap = opCodesNameMap()
    private fun opCodesMap(): Map<Int, String> {
        val map = HashMap<Int, String>()
        map[OP_0] = "0"
        map[OP_PUSHDATA1] = "PUSHDATA1"
        map[OP_PUSHDATA2] = "PUSHDATA2"
        map[OP_PUSHDATA4] = "PUSHDATA4"
        map[OP_1NEGATE] = "1NEGATE"
        map[OP_RESERVED] = "RESERVED"
        map[OP_1] = "1"
        map[OP_2] = "2"
        map[OP_3] = "3"
        map[OP_4] = "4"
        map[OP_5] = "5"
        map[OP_6] = "6"
        map[OP_7] = "7"
        map[OP_8] = "8"
        map[OP_9] = "9"
        map[OP_10] = "10"
        map[OP_11] = "11"
        map[OP_12] = "12"
        map[OP_13] = "13"
        map[OP_14] = "14"
        map[OP_15] = "15"
        map[OP_16] = "16"
        map[OP_NOP] = "NOP"
        map[OP_VER] = "VER"
        map[OP_IF] = "IF"
        map[OP_NOTIF] = "NOTIF"
        map[OP_VERIF] = "VERIF"
        map[OP_VERNOTIF] = "VERNOTIF"
        map[OP_ELSE] = "ELSE"
        map[OP_ENDIF] = "ENDIF"
        map[OP_VERIFY] = "VERIFY"
        map[OP_RETURN] = "RETURN"
        map[OP_TOALTSTACK] = "TOALTSTACK"
        map[OP_FROMALTSTACK] = "FROMALTSTACK"
        map[OP_2DROP] = "2DROP"
        map[OP_2DUP] = "2DUP"
        map[OP_3DUP] = "3DUP"
        map[OP_2OVER] = "2OVER"
        map[OP_2ROT] = "2ROT"
        map[OP_2SWAP] = "2SWAP"
        map[OP_IFDUP] = "IFDUP"
        map[OP_DEPTH] = "DEPTH"
        map[OP_DROP] = "DROP"
        map[OP_DUP] = "DUP"
        map[OP_NIP] = "NIP"
        map[OP_OVER] = "OVER"
        map[OP_PICK] = "PICK"
        map[OP_ROLL] = "ROLL"
        map[OP_ROT] = "ROT"
        map[OP_SWAP] = "SWAP"
        map[OP_TUCK] = "TUCK"
        map[OP_CAT] = "CAT"
        map[OP_SPLIT] = "SPLIT"
        map[OP_NUM2BIN] = "NUM2BIN"
        map[OP_BIN2NUM] = "BIN2NUM"
        map[OP_SIZE] = "SIZE"
        map[OP_INVERT] = "INVERT"
        map[OP_AND] = "AND"
        map[OP_OR] = "OR"
        map[OP_XOR] = "XOR"
        map[OP_EQUAL] = "EQUAL"
        map[OP_EQUALVERIFY] = "EQUALVERIFY"
        map[OP_RESERVED1] = "RESERVED1"
        map[OP_RESERVED2] = "RESERVED2"
        map[OP_1ADD] = "1ADD"
        map[OP_1SUB] = "1SUB"
        map[OP_2MUL] = "2MUL"
        map[OP_2DIV] = "2DIV"
        map[OP_NEGATE] = "NEGATE"
        map[OP_ABS] = "ABS"
        map[OP_NOT] = "NOT"
        map[OP_0NOTEQUAL] = "0NOTEQUAL"
        map[OP_ADD] = "ADD"
        map[OP_SUB] = "SUB"
        map[OP_MUL] = "MUL"
        map[OP_DIV] = "DIV"
        map[OP_MOD] = "MOD"
        map[OP_LSHIFT] = "LSHIFT"
        map[OP_RSHIFT] = "RSHIFT"
        map[OP_BOOLAND] = "BOOLAND"
        map[OP_BOOLOR] = "BOOLOR"
        map[OP_NUMEQUAL] = "NUMEQUAL"
        map[OP_NUMEQUALVERIFY] = "NUMEQUALVERIFY"
        map[OP_NUMNOTEQUAL] = "NUMNOTEQUAL"
        map[OP_LESSTHAN] = "LESSTHAN"
        map[OP_GREATERTHAN] = "GREATERTHAN"
        map[OP_LESSTHANOREQUAL] = "LESSTHANOREQUAL"
        map[OP_GREATERTHANOREQUAL] = "GREATERTHANOREQUAL"
        map[OP_MIN] = "MIN"
        map[OP_MAX] = "MAX"
        map[OP_WITHIN] = "WITHIN"
        map[OP_RIPEMD160] = "RIPEMD160"
        map[OP_SHA1] = "SHA1"
        map[OP_SHA256] = "SHA256"
        map[OP_HASH160] = "HASH160"
        map[OP_HASH256] = "HASH256"
        map[OP_CODESEPARATOR] = "CODESEPARATOR"
        map[OP_CHECKSIG] = "CHECKSIG"
        map[OP_CHECKSIGVERIFY] = "CHECKSIGVERIFY"
        map[OP_CHECKMULTISIG] = "CHECKMULTISIG"
        map[OP_CHECKMULTISIGVERIFY] = "CHECKMULTISIGVERIFY"
        map[OP_NOP1] = "NOP1"
        map[OP_CHECKLOCKTIMEVERIFY] = "CHECKLOCKTIMEVERIFY"
        map[OP_CHECKSEQUENCEVERIFY] = "CHECKSEQUENCEVERIFY"
        map[OP_NOP4] = "NOP4"
        map[OP_NOP5] = "NOP5"
        map[OP_NOP6] = "NOP6"
        map[OP_NOP7] = "NOP7"
        map[OP_NOP8] = "NOP8"
        map[OP_NOP9] = "NOP9"
        map[OP_NOP10] = "NOP10"
        return Collections.unmodifiableMap(map)
    }

    private fun opCodesNameMap(): Map<String, Int> {
        val map = HashMap<String, Int>()
        map["0"] = OP_0
        map["PUSHDATA1"] = OP_PUSHDATA1
        map["PUSHDATA2"] = OP_PUSHDATA2
        map["PUSHDATA4"] = OP_PUSHDATA4
        map["1NEGATE"] = OP_1NEGATE
        map["RESERVED"] = OP_RESERVED
        map["1"] = OP_1
        map["2"] = OP_2
        map["3"] = OP_3
        map["4"] = OP_4
        map["5"] = OP_5
        map["6"] = OP_6
        map["7"] = OP_7
        map["8"] = OP_8
        map["9"] = OP_9
        map["10"] = OP_10
        map["11"] = OP_11
        map["12"] = OP_12
        map["13"] = OP_13
        map["14"] = OP_14
        map["15"] = OP_15
        map["16"] = OP_16
        map["NOP"] = OP_NOP
        map["VER"] = OP_VER
        map["IF"] = OP_IF
        map["NOTIF"] = OP_NOTIF
        map["VERIF"] = OP_VERIF
        map["VERNOTIF"] = OP_VERNOTIF
        map["ELSE"] = OP_ELSE
        map["ENDIF"] = OP_ENDIF
        map["VERIFY"] = OP_VERIFY
        map["RETURN"] = OP_RETURN
        map["TOALTSTACK"] = OP_TOALTSTACK
        map["FROMALTSTACK"] = OP_FROMALTSTACK
        map["2DROP"] = OP_2DROP
        map["2DUP"] = OP_2DUP
        map["3DUP"] = OP_3DUP
        map["2OVER"] = OP_2OVER
        map["2ROT"] = OP_2ROT
        map["2SWAP"] = OP_2SWAP
        map["IFDUP"] = OP_IFDUP
        map["DEPTH"] = OP_DEPTH
        map["DROP"] = OP_DROP
        map["DUP"] = OP_DUP
        map["NIP"] = OP_NIP
        map["OVER"] = OP_OVER
        map["PICK"] = OP_PICK
        map["ROLL"] = OP_ROLL
        map["ROT"] = OP_ROT
        map["SWAP"] = OP_SWAP
        map["TUCK"] = OP_TUCK
        map["CAT"] = OP_CAT
        map["SPLIT"] = OP_SPLIT
        map["NUM2BIN"] = OP_NUM2BIN
        map["BIN2NUM"] = OP_BIN2NUM
        map["SIZE"] = OP_SIZE
        map["INVERT"] = OP_INVERT
        map["AND"] = OP_AND
        map["OR"] = OP_OR
        map["XOR"] = OP_XOR
        map["EQUAL"] = OP_EQUAL
        map["EQUALVERIFY"] = OP_EQUALVERIFY
        map["RESERVED1"] = OP_RESERVED1
        map["RESERVED2"] = OP_RESERVED2
        map["1ADD"] = OP_1ADD
        map["1SUB"] = OP_1SUB
        map["2MUL"] = OP_2MUL
        map["2DIV"] = OP_2DIV
        map["NEGATE"] = OP_NEGATE
        map["ABS"] = OP_ABS
        map["NOT"] = OP_NOT
        map["0NOTEQUAL"] = OP_0NOTEQUAL
        map["ADD"] = OP_ADD
        map["SUB"] = OP_SUB
        map["MUL"] = OP_MUL
        map["DIV"] = OP_DIV
        map["MOD"] = OP_MOD
        map["LSHIFT"] = OP_LSHIFT
        map["RSHIFT"] = OP_RSHIFT
        map["BOOLAND"] = OP_BOOLAND
        map["BOOLOR"] = OP_BOOLOR
        map["NUMEQUAL"] = OP_NUMEQUAL
        map["NUMEQUALVERIFY"] = OP_NUMEQUALVERIFY
        map["NUMNOTEQUAL"] = OP_NUMNOTEQUAL
        map["LESSTHAN"] = OP_LESSTHAN
        map["GREATERTHAN"] = OP_GREATERTHAN
        map["LESSTHANOREQUAL"] = OP_LESSTHANOREQUAL
        map["GREATERTHANOREQUAL"] = OP_GREATERTHANOREQUAL
        map["MIN"] = OP_MIN
        map["MAX"] = OP_MAX
        map["WITHIN"] = OP_WITHIN
        map["RIPEMD160"] = OP_RIPEMD160
        map["SHA1"] = OP_SHA1
        map["SHA256"] = OP_SHA256
        map["HASH160"] = OP_HASH160
        map["HASH256"] = OP_HASH256
        map["CODESEPARATOR"] = OP_CODESEPARATOR
        map["CHECKSIG"] = OP_CHECKSIG
        map["CHECKSIGVERIFY"] = OP_CHECKSIGVERIFY
        map["CHECKMULTISIG"] = OP_CHECKMULTISIG
        map["CHECKMULTISIGVERIFY"] = OP_CHECKMULTISIGVERIFY
        map["NOP1"] = OP_NOP1
        map["CHECKLOCKTIMEVERIFY"] = OP_CHECKLOCKTIMEVERIFY
        map["NOP2"] = OP_NOP2
        map["CHECKSEQUENCEVERIFY"] = OP_CHECKSEQUENCEVERIFY
        map["NOP3"] = OP_NOP3
        map["NOP4"] = OP_NOP4
        map["NOP5"] = OP_NOP5
        map["NOP6"] = OP_NOP6
        map["NOP7"] = OP_NOP7
        map["NOP8"] = OP_NOP8
        map["NOP9"] = OP_NOP9
        map["NOP10"] = OP_NOP10
        return Collections.unmodifiableMap(map)
    }

    /**
     * Converts the given OpCode into a string (eg "0", "PUSHDATA", or "NON_OP(10)")
     */
    @JvmStatic
    fun getOpCodeName(opcode: Int): String {
        return opCodeMap[opcode] ?: "NON_OP($opcode)"

    }

    /**
     * Converts the given pushdata OpCode into a string (eg "PUSHDATA2", or "PUSHDATA(23)")
     */
    @JvmStatic
    fun getPushDataName(opcode: Int): String {
        return opCodeMap[opcode] ?: "PUSHDATA($opcode)"
    }

    /**
     * Converts the given OpCodeName into an int
     */
    @JvmStatic
    fun getOpCode(opCodeName: String): Int {
        return opCodeNameMap[opCodeName] ?: OP_INVALIDOPCODE
    }
}
