package com.nchain.tools

import com.nchain.shared.VarInt
import spock.lang.Specification

import java.nio.charset.Charset


/*
 * @author Alberto Vilches
 * @date 19/07/2018
 */

class InputStreamIteratorSpec extends Specification {

    void "test as stream"() {
        InputStream is = new InputStreamIterator(new ByteArrayInputStream("ABC".getBytes(Charset.forName("UTF-8"))))

        expect:
        is.read() == (char)'A'
        is.read() == (char)'B'
        is.read() == (char)'C'
        is.read() == -1
    }

    void "test as iterator redundant calls to hasNext"() {
        def is = new InputStreamIterator(new ByteArrayInputStream("ABC".getBytes(Charset.forName("UTF-8"))))

        expect:
        is.hasNext()
        is.hasNext()
        is.hasNext()
        is.next() == (char)'A'
        is.hasNext()
        is.hasNext()
        is.hasNext()
        is.next() == (char)'B'
        is.hasNext()
        is.hasNext()
        is.next() == (char)'C'
        !is.hasNext()
        !is.hasNext()
        !is.hasNext()
        is.next() == -1
        is.next() == -1
        !is.hasNext()
        !is.hasNext()
    }

    void "test as iterator"() {
        def is = new InputStreamIterator(new ByteArrayInputStream("ABC".getBytes(Charset.forName("UTF-8"))))

        expect:
        is.hasNext()
        is.next() == (char)'A'
        is.hasNext()
        is.next() == (char)'B'
        is.hasNext()
        is.next() == (char)'C'
        !is.hasNext()
        is.next() == -1
        !is.hasNext()
    }

}