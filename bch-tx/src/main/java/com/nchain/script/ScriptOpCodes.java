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

package com.nchain.script;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Various constants that define the assembly-like scripting language that forms part of the Bitcoin protocol.
 * See {@link com.nchain.script.Script} for details. Also provides a method to convert them to a string.
 */
public class ScriptOpCodes {
    // push value
    public static final int OP_0 = 0x00; // push empty vector
    public static final int OP_FALSE = OP_0;
    public static final int OP_PUSHDATA1 = 0x4c;
    public static final int OP_PUSHDATA2 = 0x4d;
    public static final int OP_PUSHDATA4 = 0x4e;
    public static final int OP_1NEGATE = 0x4f;
    public static final int OP_RESERVED = 0x50;
    public static final int OP_1 = 0x51;
    public static final int OP_TRUE = OP_1;
    public static final int OP_2 = 0x52;
    public static final int OP_3 = 0x53;
    public static final int OP_4 = 0x54;
    public static final int OP_5 = 0x55;
    public static final int OP_6 = 0x56;
    public static final int OP_7 = 0x57;
    public static final int OP_8 = 0x58;
    public static final int OP_9 = 0x59;
    public static final int OP_10 = 0x5a;
    public static final int OP_11 = 0x5b;
    public static final int OP_12 = 0x5c;
    public static final int OP_13 = 0x5d;
    public static final int OP_14 = 0x5e;
    public static final int OP_15 = 0x5f;
    public static final int OP_16 = 0x60;

    // control
    public static final int OP_NOP = 0x61;
    public static final int OP_VER = 0x62;
    public static final int OP_IF = 0x63;
    public static final int OP_NOTIF = 0x64;
    public static final int OP_VERIF = 0x65;
    public static final int OP_VERNOTIF = 0x66;
    public static final int OP_ELSE = 0x67;
    public static final int OP_ENDIF = 0x68;
    public static final int OP_VERIFY = 0x69;
    public static final int OP_RETURN = 0x6a;

    // stack ops
    public static final int OP_TOALTSTACK = 0x6b;
    public static final int OP_FROMALTSTACK = 0x6c;
    public static final int OP_2DROP = 0x6d;
    public static final int OP_2DUP = 0x6e;
    public static final int OP_3DUP = 0x6f;
    public static final int OP_2OVER = 0x70;
    public static final int OP_2ROT = 0x71;
    public static final int OP_2SWAP = 0x72;
    public static final int OP_IFDUP = 0x73;
    public static final int OP_DEPTH = 0x74;
    public static final int OP_DROP = 0x75;
    public static final int OP_DUP = 0x76;
    public static final int OP_NIP = 0x77;
    public static final int OP_OVER = 0x78;
    public static final int OP_PICK = 0x79;
    public static final int OP_ROLL = 0x7a;
    public static final int OP_ROT = 0x7b;
    public static final int OP_SWAP = 0x7c;
    public static final int OP_TUCK = 0x7d;

    // splice ops
    public static final int OP_CAT = 0x7e;
    public static final int OP_SPLIT = 0x7f;
    public static final int OP_NUM2BIN = 0x80;
    public static final int OP_BIN2NUM = 0x81;
    public static final int OP_SIZE = 0x82;

    // bit logic
    public static final int OP_INVERT = 0x83;
    public static final int OP_AND = 0x84;
    public static final int OP_OR = 0x85;
    public static final int OP_XOR = 0x86;
    public static final int OP_EQUAL = 0x87;
    public static final int OP_EQUALVERIFY = 0x88;
    public static final int OP_RESERVED1 = 0x89;
    public static final int OP_RESERVED2 = 0x8a;

    // numeric
    public static final int OP_1ADD = 0x8b;
    public static final int OP_1SUB = 0x8c;
    public static final int OP_2MUL = 0x8d;
    public static final int OP_2DIV = 0x8e;
    public static final int OP_NEGATE = 0x8f;
    public static final int OP_ABS = 0x90;
    public static final int OP_NOT = 0x91;
    public static final int OP_0NOTEQUAL = 0x92;
    public static final int OP_ADD = 0x93;
    public static final int OP_SUB = 0x94;
    public static final int OP_MUL = 0x95;
    public static final int OP_DIV = 0x96;
    public static final int OP_MOD = 0x97;
    public static final int OP_LSHIFT = 0x98;
    public static final int OP_RSHIFT = 0x99;
    public static final int OP_BOOLAND = 0x9a;
    public static final int OP_BOOLOR = 0x9b;
    public static final int OP_NUMEQUAL = 0x9c;
    public static final int OP_NUMEQUALVERIFY = 0x9d;
    public static final int OP_NUMNOTEQUAL = 0x9e;
    public static final int OP_LESSTHAN = 0x9f;
    public static final int OP_GREATERTHAN = 0xa0;
    public static final int OP_LESSTHANOREQUAL = 0xa1;
    public static final int OP_GREATERTHANOREQUAL = 0xa2;
    public static final int OP_MIN = 0xa3;
    public static final int OP_MAX = 0xa4;
    public static final int OP_WITHIN = 0xa5;

    // crypto
    public static final int OP_RIPEMD160 = 0xa6;
    public static final int OP_SHA1 = 0xa7;
    public static final int OP_SHA256 = 0xa8;
    public static final int OP_HASH160 = 0xa9;
    public static final int OP_HASH256 = 0xaa;
    public static final int OP_CODESEPARATOR = 0xab;
    public static final int OP_CHECKSIG = 0xac;
    public static final int OP_CHECKSIGVERIFY = 0xad;
    public static final int OP_CHECKMULTISIG = 0xae;
    public static final int OP_CHECKMULTISIGVERIFY = 0xaf;

    // block state
    /**
     * Check lock time of the block. Introduced in BIP 65, replacing OP_NOP2
     */
    public static final int OP_CHECKLOCKTIMEVERIFY = 0xb1;
    public static final int OP_CHECKSEQUENCEVERIFY = 0xb2;

    // expansion
    public static final int OP_NOP1 = 0xb0;
    /**
     * Deprecated by BIP 65
     */
    @Deprecated
    public static final int OP_NOP2 = OP_CHECKLOCKTIMEVERIFY;
    @Deprecated
    public static final int OP_NOP3 = OP_CHECKSEQUENCEVERIFY;
    public static final int OP_NOP4 = 0xb3;
    public static final int OP_NOP5 = 0xb4;
    public static final int OP_NOP6 = 0xb5;
    public static final int OP_NOP7 = 0xb6;
    public static final int OP_NOP8 = 0xb7;
    public static final int OP_NOP9 = 0xb8;
    public static final int OP_NOP10 = 0xb9;

    public static final int OP_INVALIDOPCODE = 0xff;

    private static final Map<Integer, String> opCodeMap = opCodesMap();
    private static Map<Integer, String> opCodesMap() {
        Map<Integer, String> map = new HashMap<>();
        map.put(OP_0, "0");
        map.put(OP_PUSHDATA1, "PUSHDATA1");
        map.put(OP_PUSHDATA2, "PUSHDATA2");
        map.put(OP_PUSHDATA4, "PUSHDATA4");
        map.put(OP_1NEGATE, "1NEGATE");
        map.put(OP_RESERVED, "RESERVED");
        map.put(OP_1, "1");
        map.put(OP_2, "2");
        map.put(OP_3, "3");
        map.put(OP_4, "4");
        map.put(OP_5, "5");
        map.put(OP_6, "6");
        map.put(OP_7, "7");
        map.put(OP_8, "8");
        map.put(OP_9, "9");
        map.put(OP_10, "10");
        map.put(OP_11, "11");
        map.put(OP_12, "12");
        map.put(OP_13, "13");
        map.put(OP_14, "14");
        map.put(OP_15, "15");
        map.put(OP_16, "16");
        map.put(OP_NOP, "NOP");
        map.put(OP_VER, "VER");
        map.put(OP_IF, "IF");
        map.put(OP_NOTIF, "NOTIF");
        map.put(OP_VERIF, "VERIF");
        map.put(OP_VERNOTIF, "VERNOTIF");
        map.put(OP_ELSE, "ELSE");
        map.put(OP_ENDIF, "ENDIF");
        map.put(OP_VERIFY, "VERIFY");
        map.put(OP_RETURN, "RETURN");
        map.put(OP_TOALTSTACK, "TOALTSTACK");
        map.put(OP_FROMALTSTACK, "FROMALTSTACK");
        map.put(OP_2DROP, "2DROP");
        map.put(OP_2DUP, "2DUP");
        map.put(OP_3DUP, "3DUP");
        map.put(OP_2OVER, "2OVER");
        map.put(OP_2ROT, "2ROT");
        map.put(OP_2SWAP, "2SWAP");
        map.put(OP_IFDUP, "IFDUP");
        map.put(OP_DEPTH, "DEPTH");
        map.put(OP_DROP, "DROP");
        map.put(OP_DUP, "DUP");
        map.put(OP_NIP, "NIP");
        map.put(OP_OVER, "OVER");
        map.put(OP_PICK, "PICK");
        map.put(OP_ROLL, "ROLL");
        map.put(OP_ROT, "ROT");
        map.put(OP_SWAP, "SWAP");
        map.put(OP_TUCK, "TUCK");
        map.put(OP_CAT, "CAT");
        map.put(OP_SPLIT, "SPLIT");
        map.put(OP_NUM2BIN, "NUM2BIN");
        map.put(OP_BIN2NUM, "BIN2NUM");
        map.put(OP_SIZE, "SIZE");
        map.put(OP_INVERT, "INVERT");
        map.put(OP_AND, "AND");
        map.put(OP_OR, "OR");
        map.put(OP_XOR, "XOR");
        map.put(OP_EQUAL, "EQUAL");
        map.put(OP_EQUALVERIFY, "EQUALVERIFY");
        map.put(OP_RESERVED1, "RESERVED1");
        map.put(OP_RESERVED2, "RESERVED2");
        map.put(OP_1ADD, "1ADD");
        map.put(OP_1SUB, "1SUB");
        map.put(OP_2MUL, "2MUL");
        map.put(OP_2DIV, "2DIV");
        map.put(OP_NEGATE, "NEGATE");
        map.put(OP_ABS, "ABS");
        map.put(OP_NOT, "NOT");
        map.put(OP_0NOTEQUAL, "0NOTEQUAL");
        map.put(OP_ADD, "ADD");
        map.put(OP_SUB, "SUB");
        map.put(OP_MUL, "MUL");
        map.put(OP_DIV, "DIV");
        map.put(OP_MOD, "MOD");
        map.put(OP_LSHIFT, "LSHIFT");
        map.put(OP_RSHIFT, "RSHIFT");
        map.put(OP_BOOLAND, "BOOLAND");
        map.put(OP_BOOLOR, "BOOLOR");
        map.put(OP_NUMEQUAL, "NUMEQUAL");
        map.put(OP_NUMEQUALVERIFY, "NUMEQUALVERIFY");
        map.put(OP_NUMNOTEQUAL, "NUMNOTEQUAL");
        map.put(OP_LESSTHAN, "LESSTHAN");
        map.put(OP_GREATERTHAN, "GREATERTHAN");
        map.put(OP_LESSTHANOREQUAL, "LESSTHANOREQUAL");
        map.put(OP_GREATERTHANOREQUAL, "GREATERTHANOREQUAL");
        map.put(OP_MIN, "MIN");
        map.put(OP_MAX, "MAX");
        map.put(OP_WITHIN, "WITHIN");
        map.put(OP_RIPEMD160, "RIPEMD160");
        map.put(OP_SHA1, "SHA1");
        map.put(OP_SHA256, "SHA256");
        map.put(OP_HASH160, "HASH160");
        map.put(OP_HASH256, "HASH256");
        map.put(OP_CODESEPARATOR, "CODESEPARATOR");
        map.put(OP_CHECKSIG, "CHECKSIG");
        map.put(OP_CHECKSIGVERIFY, "CHECKSIGVERIFY");
        map.put(OP_CHECKMULTISIG, "CHECKMULTISIG");
        map.put(OP_CHECKMULTISIGVERIFY, "CHECKMULTISIGVERIFY");
        map.put(OP_NOP1, "NOP1");
        map.put(OP_CHECKLOCKTIMEVERIFY, "CHECKLOCKTIMEVERIFY");
        map.put(OP_CHECKSEQUENCEVERIFY, "CHECKSEQUENCEVERIFY");
        map.put(OP_NOP4, "NOP4");
        map.put(OP_NOP5, "NOP5");
        map.put(OP_NOP6, "NOP6");
        map.put(OP_NOP7, "NOP7");
        map.put(OP_NOP8, "NOP8");
        map.put(OP_NOP9, "NOP9");
        map.put(OP_NOP10, "NOP10");
        return Collections.unmodifiableMap(map);
    };

    private static final Map<String, Integer> opCodeNameMap = opCodesNameMap();

    private static final Map<String, Integer> opCodesNameMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("0", OP_0);
        map.put("PUSHDATA1", OP_PUSHDATA1);
        map.put("PUSHDATA2", OP_PUSHDATA2);
        map.put("PUSHDATA4", OP_PUSHDATA4);
        map.put("1NEGATE", OP_1NEGATE);
        map.put("RESERVED", OP_RESERVED);
        map.put("1", OP_1);
        map.put("2", OP_2);
        map.put("3", OP_3);
        map.put("4", OP_4);
        map.put("5", OP_5);
        map.put("6", OP_6);
        map.put("7", OP_7);
        map.put("8", OP_8);
        map.put("9", OP_9);
        map.put("10", OP_10);
        map.put("11", OP_11);
        map.put("12", OP_12);
        map.put("13", OP_13);
        map.put("14", OP_14);
        map.put("15", OP_15);
        map.put("16", OP_16);
        map.put("NOP", OP_NOP);
        map.put("VER", OP_VER);
        map.put("IF", OP_IF);
        map.put("NOTIF", OP_NOTIF);
        map.put("VERIF", OP_VERIF);
        map.put("VERNOTIF", OP_VERNOTIF);
        map.put("ELSE", OP_ELSE);
        map.put("ENDIF", OP_ENDIF);
        map.put("VERIFY", OP_VERIFY);
        map.put("RETURN", OP_RETURN);
        map.put("TOALTSTACK", OP_TOALTSTACK);
        map.put("FROMALTSTACK", OP_FROMALTSTACK);
        map.put("2DROP", OP_2DROP);
        map.put("2DUP", OP_2DUP);
        map.put("3DUP", OP_3DUP);
        map.put("2OVER", OP_2OVER);
        map.put("2ROT", OP_2ROT);
        map.put("2SWAP", OP_2SWAP);
        map.put("IFDUP", OP_IFDUP);
        map.put("DEPTH", OP_DEPTH);
        map.put("DROP", OP_DROP);
        map.put("DUP", OP_DUP);
        map.put("NIP", OP_NIP);
        map.put("OVER", OP_OVER);
        map.put("PICK", OP_PICK);
        map.put("ROLL", OP_ROLL);
        map.put("ROT", OP_ROT);
        map.put("SWAP", OP_SWAP);
        map.put("TUCK", OP_TUCK);
        map.put("CAT", OP_CAT);
        map.put("SPLIT", OP_SPLIT);
        map.put("NUM2BIN", OP_NUM2BIN);
        map.put("BIN2NUM", OP_BIN2NUM);
        map.put("SIZE", OP_SIZE);
        map.put("INVERT", OP_INVERT);
        map.put("AND", OP_AND);
        map.put("OR", OP_OR);
        map.put("XOR", OP_XOR);
        map.put("EQUAL", OP_EQUAL);
        map.put("EQUALVERIFY", OP_EQUALVERIFY);
        map.put("RESERVED1", OP_RESERVED1);
        map.put("RESERVED2", OP_RESERVED2);
        map.put("1ADD", OP_1ADD);
        map.put("1SUB", OP_1SUB);
        map.put("2MUL", OP_2MUL);
        map.put("2DIV", OP_2DIV);
        map.put("NEGATE", OP_NEGATE);
        map.put("ABS", OP_ABS);
        map.put("NOT", OP_NOT);
        map.put("0NOTEQUAL", OP_0NOTEQUAL);
        map.put("ADD", OP_ADD);
        map.put("SUB", OP_SUB);
        map.put("MUL", OP_MUL);
        map.put("DIV", OP_DIV);
        map.put("MOD", OP_MOD);
        map.put("LSHIFT", OP_LSHIFT);
        map.put("RSHIFT", OP_RSHIFT);
        map.put("BOOLAND", OP_BOOLAND);
        map.put("BOOLOR", OP_BOOLOR);
        map.put("NUMEQUAL", OP_NUMEQUAL);
        map.put("NUMEQUALVERIFY", OP_NUMEQUALVERIFY);
        map.put("NUMNOTEQUAL", OP_NUMNOTEQUAL);
        map.put("LESSTHAN", OP_LESSTHAN);
        map.put("GREATERTHAN", OP_GREATERTHAN);
        map.put("LESSTHANOREQUAL", OP_LESSTHANOREQUAL);
        map.put("GREATERTHANOREQUAL", OP_GREATERTHANOREQUAL);
        map.put("MIN", OP_MIN);
        map.put("MAX", OP_MAX);
        map.put("WITHIN", OP_WITHIN);
        map.put("RIPEMD160", OP_RIPEMD160);
        map.put("SHA1", OP_SHA1);
        map.put("SHA256", OP_SHA256);
        map.put("HASH160", OP_HASH160);
        map.put("HASH256", OP_HASH256);
        map.put("CODESEPARATOR", OP_CODESEPARATOR);
        map.put("CHECKSIG", OP_CHECKSIG);
        map.put("CHECKSIGVERIFY", OP_CHECKSIGVERIFY);
        map.put("CHECKMULTISIG", OP_CHECKMULTISIG);
        map.put("CHECKMULTISIGVERIFY", OP_CHECKMULTISIGVERIFY);
        map.put("NOP1", OP_NOP1);
        map.put("CHECKLOCKTIMEVERIFY", OP_CHECKLOCKTIMEVERIFY);
        map.put("NOP2", OP_NOP2);
        map.put("CHECKSEQUENCEVERIFY", OP_CHECKSEQUENCEVERIFY);
        map.put("NOP3", OP_NOP3);
        map.put("NOP4", OP_NOP4);
        map.put("NOP5", OP_NOP5);
        map.put("NOP6", OP_NOP6);
        map.put("NOP7", OP_NOP7);
        map.put("NOP8", OP_NOP8);
        map.put("NOP9", OP_NOP9);
        map.put("NOP10", OP_NOP10);
        return Collections.unmodifiableMap(map);
    }

    /**
     * Converts the given OpCode into a string (eg "0", "PUSHDATA", or "NON_OP(10)")
     */
    public static String getOpCodeName(int opcode) {
        if (opCodeMap.containsKey(opcode))
            return opCodeMap.get(opcode);

        return "NON_OP(" + opcode + ")";
    }

    /**
     * Converts the given pushdata OpCode into a string (eg "PUSHDATA2", or "PUSHDATA(23)")
     */
    public static String getPushDataName(int opcode) {
        if (opCodeMap.containsKey(opcode))
            return opCodeMap.get(opcode);

        return "PUSHDATA(" + opcode + ")";
    }

    /**
     * Converts the given OpCodeName into an int
     */
    public static int getOpCode(String opCodeName) {
        if (opCodeNameMap.containsKey(opCodeName))
            return opCodeNameMap.get(opCodeName);

        return OP_INVALIDOPCODE;
    }
}
