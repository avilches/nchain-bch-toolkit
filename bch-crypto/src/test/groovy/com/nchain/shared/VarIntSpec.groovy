package com.nchain.shared

import spock.lang.Specification


/*
 * @author Alberto Vilches
 * @date 19/07/2018
 */

class VarIntSpec extends Specification {

    void testBytes() {
        when: "with widening conversion"
        def a = new VarInt(10)
        then:
        1 == a.sizeInBytes
        1 == a.encode().size()
        10L == new VarInt(a.encode(), 0).value
        10L == new VarInt(new ByteArrayInputStream(a.encode())).value
    }

    void testShorts() {
        when: "with widening conversion"
        def a = new VarInt(64000)
        then:
        3 == a.sizeInBytes
        3 == a.encode().size()
        64000L == new VarInt(a.encode(), 0).value
        64000L == new VarInt(new ByteArrayInputStream(a.encode())).value
    }

    void testShortFFFF() {
        when:
        def a = new VarInt(0xFFFFL)
        then:
        3 == a.sizeInBytes
        3 == a.encode().size()
        0xFFFFL == new VarInt(a.encode(), 0).value
        0xFFFFL == new VarInt(new ByteArrayInputStream(a.encode())).value
    }

    void testInts() {
        when:
        def a = new VarInt(0xAABBCCDDL)
        then:
        5 == a.sizeInBytes
        5 == a.encode().size()

        when:
        def bytes = a.encode()
        then:
        0xAABBCCDDL == (0xFFFFFFFFL & new VarInt(bytes, 0).value)
        0xAABBCCDDL == (0xFFFFFFFFL & new VarInt(new ByteArrayInputStream(bytes)).value)
    }

    void testIntFFFFFFFF() {
        when:
        def a = new VarInt(0xFFFFFFFFL)
        then:
        5 == a.sizeInBytes
        5 == a.encode().size()

        when:
        def bytes = a.encode()
        then:
        0xFFFFFFFFL == (0xFFFFFFFFL & new VarInt(bytes, 0).value)
        0xFFFFFFFFL == (0xFFFFFFFFL & new VarInt(new ByteArrayInputStream(bytes)).value)
    }

    void testLong() {
        when:
        def a = new VarInt(-0x3501454121524111L)
        then:
        9 == a.sizeInBytes
        9 == a.encode().size()

        when:
        def bytes = a.encode()
        then:
        -0x3501454121524111L == new VarInt(bytes, 0).value
        -0x3501454121524111L == new VarInt(new ByteArrayInputStream(bytes)).value
    }

    void testSizeOfNegativeInt() {
        expect: "shouldn't normally be passed, but at least stay consistent (bug regression test)"
        VarInt.sizeOf(-1) == new VarInt(-1).encode().size()
    }
    
}