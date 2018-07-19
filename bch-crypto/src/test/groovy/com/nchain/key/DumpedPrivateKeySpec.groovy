package com.nchain.key

import com.nchain.params.MainNetParams
import com.nchain.params.TestNet3Params
import com.nchain.tools.ByteUtils
import org.junit.Assert
import spock.lang.Specification


/*
 * @author Alberto Vilches
 * @date 19/07/2018
 */

class DumpedPrivateKeySpec extends Specification {

    void checkNetwork() {
        when: "network matches"
        def dumped = DumpedPrivateKey.fromBase58(MainNetParams.INSTANCE, "5HtUCLMFWNueqN9unpgX2DzjMg6SDNZyKRb8s3LJgpFg5ubuMrk")
        then:
        dumped.version == MainNetParams.INSTANCE.dumpedPrivateKeyHeader
        notThrown()

        when: "auto discover network ok"
        dumped = DumpedPrivateKey.fromBase58("5HtUCLMFWNueqN9unpgX2DzjMg6SDNZyKRb8s3LJgpFg5ubuMrk")
        then:
        notThrown()
        dumped.version == MainNetParams.INSTANCE.dumpedPrivateKeyHeader

        when: "network doesn't match"
        DumpedPrivateKey.fromBase58(TestNet3Params.INSTANCE, "5HtUCLMFWNueqN9unpgX2DzjMg6SDNZyKRb8s3LJgpFg5ubuMrk")
        then:
        thrown(WrongNetworkException)
    }

    void testJavaSerialization() {
        when:
        def key = DumpedPrivateKey.createFromPrivKey(MainNetParams.INSTANCE, ECKey.create().privKeyBytes, true)
        def os = new ByteArrayOutputStream()
        new ObjectOutputStream(os).writeObject(key)
        def keyCopy = new ObjectInputStream(new ByteArrayInputStream(os.toByteArray())).readObject() as DumpedPrivateKey

        then:
        key == keyCopy
        Assert.assertNotSame(key, keyCopy)

        when:
        def keyCloned = ByteUtils.serializeRound(key)

        then:
        key == keyCloned
        Assert.assertNotSame(key, keyCloned)

    }

    void cloning() {
        when:
        def a = DumpedPrivateKey.fromBase58("5HtUCLMFWNueqN9unpgX2DzjMg6SDNZyKRb8s3LJgpFg5ubuMrk") // new DumpedPrivateKey(MainNetParams.INSTANCE, ECKey.create().privKeyBytes, true)
        def b = a.clone()

        then:
        a == b
        a.hashCode() == b.hashCode()
        a.compareTo(b) == 0
        Assert.assertNotSame(a, b)
    }

    void roundtripBase58() {
        when:
        def base58 = "5HtUCLMFWNueqN9unpgX2DzjMg6SDNZyKRb8s3LJgpFg5ubuMrk"
        then:
        base58 == DumpedPrivateKey.fromBase58(null, base58).toBase58()
    }

    void exportAndImportNew() {
        when:
        def key = ECKey.create()
        then:
        key.dumpPrivKey(MainNetParams.INSTANCE)

    }
    void exportAndImport() {
        when:
        def key = ECKey.create()
        def dumpedKey = key.dumpPrivKey(MainNetParams.INSTANCE)

        then:
        dumpedKey == ECKey.fromPrivateDump(dumpedKey).dumpPrivKey(MainNetParams.INSTANCE)

        and:
        dumpedKey == ECKey.fromPrivateDump(MainNetParams.INSTANCE, dumpedKey.toBase58()).dumpPrivKey(MainNetParams.INSTANCE)
        dumpedKey == ECKey.fromPrivateDump(null, dumpedKey.toBase58()).dumpPrivKey(MainNetParams.INSTANCE)
        dumpedKey == DumpedPrivateKey.fromBase58(null, dumpedKey.toBase58()).key.dumpPrivKey(MainNetParams.INSTANCE)
    }

}