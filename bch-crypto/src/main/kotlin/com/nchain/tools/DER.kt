package com.nchain.tools

import com.nchain.key.ECKey
import com.nchain.shared.VerificationException
import org.spongycastle.asn1.ASN1InputStream
import org.spongycastle.asn1.ASN1Integer
import org.spongycastle.asn1.DERSequenceGenerator
import org.spongycastle.asn1.DLSequence
import org.spongycastle.util.Properties
import java.io.ByteArrayOutputStream
import java.io.IOException

/*
 * @author Alberto Vilches
 * @date 23/07/2018
 */

object DER {
    @Throws(IOException::class)
    @JvmStatic
    fun encode(signature: ECKey.ECDSASignature): ByteArray {
        return createByteStream(signature).toByteArray()
    }

    @Throws(IOException::class)
    @JvmStatic
    fun createByteStream(signature: ECKey.ECDSASignature): ByteArrayOutputStream {
        // Usually 70-72 bytes.
        val bos = ByteArrayOutputStream(72)
        val seq = DERSequenceGenerator(bos)
        seq.addObject(ASN1Integer(signature.r))
        seq.addObject(ASN1Integer(signature.s))
        seq.close()
        return bos
    }

    @JvmStatic
    fun decodeSignature(bytes: ByteArray): ECKey.ECDSASignature {
        var decoder = ASN1InputStream(bytes)
        try {
            // BouncyCastle by default is strict about parsing ASN.1 integers. We relax this check, because some
            // Bitcoin signatures would not parse.
            Properties.setThreadOverride("org.spongycastle.asn1.allow_unsafe_integer", true)
            val seq = decoder.readObject() as DLSequence
                    ?: throw VerificationException.SignatureFormatError("Reached past end of ASN.1 stream.")
            val r: ASN1Integer
            val s: ASN1Integer
            try {
                r = seq.getObjectAt(0) as ASN1Integer
                s = seq.getObjectAt(1) as ASN1Integer
            } catch (e: ClassCastException) {
                throw IllegalArgumentException(e)
            }

            // OpenSSL deviates from the DER spec by interpreting these values as unsigned, though they should not be
            // Thus, we always use the positive versions. See: http://r6.ca/blog/20111119T211504Z.html
            return ECKey.ECDSASignature(r.positiveValue, s.positiveValue)
        } catch (e: Exception) {
            throw VerificationException.SignatureFormatError(e)
        } finally {
            Properties.removeThreadOverride("org.spongycastle.asn1.allow_unsafe_integer")
            try {
                decoder.close()
            } catch (x: IOException) {
            }
        }
    }
}