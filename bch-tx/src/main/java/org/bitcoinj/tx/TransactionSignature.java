/*
 * Copyright 2013 Google Inc.
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

package org.bitcoinj.tx;

import com.google.common.base.Preconditions;
import com.nchain.key.ECKey;
import com.nchain.shared.VerificationException;
import com.nchain.tools.DER;
import com.nchain.tx.Transaction;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

/**
 * A TransactionSignature wraps an {@link ECKey.ECDSASignature} and adds methods for handling
 * the additional SIGHASH mode byte that is used.
 */
public class TransactionSignature {

    public ECKey.ECDSASignature signature;
    /**
     * A byte that controls which parts of a transaction are signed. This is exposed because signatures
     * parsed off the wire may have sighash flags that aren't "normal" serializations of the enum values.
     * Because Bitcoin Core works via bit testing, we must not lose the exact value when round-tripping
     * otherwise we'll fail to verify signature hashes.
     */
    public final int sighashFlags;

    /** Constructs a signature with the given components and SIGHASH_ALL. */
    public TransactionSignature(BigInteger r, BigInteger s) {
        this(r, s, Transaction.SigHash.ALL.getValue());
    }

    /** Constructs a signature with the given components and raw sighash flag bytes (needed for rule compatibility). */
    public TransactionSignature(BigInteger r, BigInteger s, int sighashFlags) {
        this.signature = new ECKey.ECDSASignature(r, s);
        this.sighashFlags = sighashFlags;
    }

    /** Constructs a transaction signature based on the ECDSA signature. */
    public TransactionSignature(ECKey.ECDSASignature signature, Transaction.SigHash mode, boolean anyoneCanPay) {
        this.signature = new ECKey.ECDSASignature(signature.getR(), signature.getS());
        sighashFlags = calcSigHashValue(mode, anyoneCanPay);
    }

    public TransactionSignature(ECKey.ECDSASignature signature, Transaction.SigHash mode, boolean anyoneCanPay, boolean useForkId) {
        this.signature = new ECKey.ECDSASignature(signature.getR(), signature.getS());
        sighashFlags = calcSigHashValue(mode, anyoneCanPay, useForkId);
    }

    /**
     * Returns a dummy invalid signature whose R/S values are set such that they will take up the same number of
     * encoded bytes as a real signature. This can be useful when you want to fill out a transaction to be of the
     * right size (e.g. for fee calculations) but don't have the requisite signing key yet and will fill out the
     * real signature later.
     */
    public static TransactionSignature dummy() {
        BigInteger val = ECKey.getHALF_CURVE_ORDER();
        return new TransactionSignature(val, val);
    }

    /** Calculates the byte used in the protocol to represent the combination of mode and anyoneCanPay. */
    public static int calcSigHashValue(Transaction.SigHash mode, boolean anyoneCanPay) {
        Preconditions.checkArgument(Transaction.SigHash.ALL == mode || Transaction.SigHash.NONE == mode || Transaction.SigHash.SINGLE == mode); // enforce compatibility since this code was made before the SigHash enum was updated
        int sighashFlags = mode.getValue();
        if (anyoneCanPay)
            sighashFlags |= Transaction.SigHash.ANYONECANPAY.getValue();
        return sighashFlags;
    }

    public static int calcSigHashValue(Transaction.SigHash mode, boolean anyoneCanPay, boolean useForkId) {
        Preconditions.checkArgument(Transaction.SigHash.ALL == mode || Transaction.SigHash.NONE == mode || Transaction.SigHash.SINGLE == mode); // enforce compatibility since this code was made before the SigHash enum was updated
        int sighashFlags = mode.getValue();
        if (anyoneCanPay)
            sighashFlags |= Transaction.SigHash.ANYONECANPAY.getValue();
        if(useForkId)
            sighashFlags |= Transaction.SigHash.FORKID.getValue();
        return sighashFlags;
    }

    /**
     * Checkes if the Hashtype is properly set in the signature.
     * @param signature Signature
     * @return          True (correct Hashtype)/ False
     */
    public static boolean isValidHashType(byte[] signature) {
        boolean result = true;
        int hashType = (signature[signature.length-1] & 0xff) & ~(Transaction.SigHash.ANYONECANPAY.getValue()| Transaction.SigHash.FORKID.getValue()); // mask the byte to prevent sign-extension hurting us
        if (hashType < Transaction.SigHash.ALL.getValue() || hashType > Transaction.SigHash.SINGLE.getValue())
            result = false;
        return result;
    }
    /**
     * Returns true if the given signature is has canonical encoding, and will thus be accepted as standard by
     * Bitcoin Core. DER and the SIGHASH encoding allow for quite some flexibility in how the same structures
     * are encoded, and this can open up novel attacks in which a man in the middle takes a transaction and then
     * changes its signature such that the transaction hash is different but it's still valid. This can confuse wallets
     * and generally violates people's mental model of how Bitcoin should work, thus, non-canonical signatures are now
     * not relayed by default.
     */
    public static boolean isEncodingCanonical(byte[] signature) {
        // See Bitcoin Core's IsCanonicalSignature, https://bitcointalk.org/index.php?topic=8392.msg127623#msg127623
        // A canonical signature exists of: <30> <total len> <02> <len R> <R> <02> <len S> <S> <hashtype>
        // Where R and S are not negative (their first byte has its highest bit not set), and not
        // excessively padded (do not start with a 0 byte, unless an otherwise negative number follows,
        // in which case a single 0 byte is necessary and even required).

        if (signature.length < 9 || signature.length > 73)
            return false;

        //                   "wrong type"                  "wrong length marker"
        if ((signature[0] & 0xff) != 0x30 || (signature[1] & 0xff) != signature.length-3)
            return false;

        int lenR = signature[3] & 0xff;
        if (5 + lenR >= signature.length || lenR == 0)
            return false;
        int lenS = signature[5+lenR] & 0xff;
        if (lenR + lenS + 7 != signature.length || lenS == 0)
            return false;

        //    R value type mismatch          R value negative
        if (signature[4-2] != 0x02 || (signature[4] & 0x80) == 0x80)
            return false;
        if (lenR > 1 && signature[4] == 0x00 && (signature[4+1] & 0x80) != 0x80)
            return false; // R value excessively padded

        //       S value type mismatch                    S value negative
        if (signature[6 + lenR - 2] != 0x02 || (signature[6 + lenR] & 0x80) == 0x80)
            return false;
        if (lenS > 1 && signature[6 + lenR] == 0x00 && (signature[6 + lenR + 1] & 0x80) != 0x80)
            return false; // S value excessively padded

        return true;
    }

    public static boolean hasForkId (byte[] signature) {
        int forkId = (signature[signature.length-1] & 0xff) & Transaction.SigHash.FORKID.getValue(); // mask the byte to prevent sign-extension hurting us

        return forkId == Transaction.SigHash.FORKID.getValue();
    }

    public boolean anyoneCanPay() {
        return (sighashFlags & Transaction.SigHash.ANYONECANPAY.getValue()) != 0;
    }
    public boolean useForkId() {
        return (sighashFlags & Transaction.SigHash.FORKID.getValue()) != 0;
    }

    public Transaction.SigHash sigHashMode() {
        final int mode = sighashFlags & 0x1f;
        if (mode == Transaction.SigHash.NONE.getValue())
            return Transaction.SigHash.NONE;
        else if (mode == Transaction.SigHash.SINGLE.getValue())
            return Transaction.SigHash.SINGLE;
        else
            return Transaction.SigHash.ALL;
    }

    /**
     * What we get back from the signer are the two components of a signature, r and s. To get a flat byte stream
     * of the type used by Bitcoin we have to encode them using DER encoding, which is just a way to pack the two
     * components into a structure, and then we append a byte to the end for the sighash flags.
     */
    public byte[] bitcoinSerialize() {
        try {
            ByteArrayOutputStream bos = DER.createByteStream(signature);
            bos.write(sighashFlags);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }

    public ECKey.ECDSASignature toCanonicalised() {
        return new TransactionSignature(signature.toCanonicalised(), sigHashMode(), anyoneCanPay(), useForkId()).signature;
    }

    /**
     * Returns a decoded signature.
     *
     * @param requireCanonicalEncoding if the encoding of the signature must
     * be canonical.
     * @throws RuntimeException if the signature is invalid or unparseable in some way.
     * @deprecated use {@link #decodeFromBitcoin(byte[], boolean, boolean)} instead.
     */
    @Deprecated
    public static TransactionSignature decodeFromBitcoin(byte[] bytes,
                                                         boolean requireCanonicalEncoding) throws VerificationException {
        return decodeFromBitcoin(bytes, requireCanonicalEncoding, false);
    }

    /**
     * Returns a decoded signature.
     *
     * @param requireCanonicalEncoding if the encoding of the signature must
     * be canonical.
     * @param requireCanonicalSValue if the S-value must be canonical (below half
     * the order of the curve).
     * @throws RuntimeException if the signature is invalid or unparseable in some way.
     */
    public static TransactionSignature decodeFromBitcoin(byte[] bytes,
                                                         boolean requireCanonicalEncoding,
                                                         boolean requireCanonicalSValue) throws VerificationException {
        // Bitcoin encoding is DER signature + sighash byte.
        if (requireCanonicalEncoding && !isEncodingCanonical(bytes))
            throw new VerificationException("Signature encoding is not canonical.");
        ECKey.ECDSASignature sig;
        try {
            sig = DER.decodeSignature(bytes);
        } catch (IllegalArgumentException e) {
            throw new VerificationException("Could not decode DER: "+e.getMessage());
        }
        if (requireCanonicalSValue && !sig.isCanonical())
            throw new VerificationException("S-value is not canonical.");

        // In Bitcoin, any value of the final byte is valid, but not necessarily canonical. See javadocs for
        // isEncodingCanonical to learn more about this. So we must store the exact byte found.
        return new TransactionSignature(sig.getR(), sig.getS(), bytes[bytes.length - 1]);
    }
}
