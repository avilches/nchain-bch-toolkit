/*
 * Copyright 2013 Matija Mazi.
 * Copyright 2014 Andreas Schildbach
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

package com.nchain.bip32

import com.nchain.address.Base58
import com.nchain.key.ECKey
import org.spongycastle.math.ec.ECPoint
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.Arrays
import java.util.Comparator

import com.nchain.key.LazyECPoint
import com.nchain.keycrypter.KeyCrypterException
import com.nchain.params.NetworkParameters
import com.nchain.shared.Sha256Hash
import com.nchain.tools.ByteUtils

/**
 * A deterministic key is a node in a [DeterministicHierarchy]. As per
 * [the BIP 32 specification](https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki) it is a pair
 * (key, chaincode). If you know its path in the tree and its chain code you can derive more keys from this. To obtain
 * one of these, you can call [HDKeyDerivation.createMasterPrivateKey].
 */
class DeterministicKey {
    
    val key: ECKey

    val parent: DeterministicKey?
    /**
     * Returns the path through some [DeterministicHierarchy] which reaches this keys position in the tree.
     * A path can be written as 1/2/1 which means the first child of the root, the second child of that node, then
     * the first child of that node.
     */
    val path: List<ChildNumber>
    /**
     * Return this key's depth in the hierarchy, where the root node is at depth zero.
     * This may be different than the number of segments in the path if this key was
     * deserialized without access to its parent.
     */
    val depth: Int
    /**
     * Return the fingerprint of the key from which this key was derived, if this is a
     * child key, or else an array of four zero-value bytes.
     */
    var parentFingerprint: Int = 0
        private set // 0 if this key is root node of key hierarchy

    /** 32 bytes  */
    /**
     * Returns the chain code associated with this key. See the specification to learn more about chain codes.
     */
    val chainCode: ByteArray

    /**
     * Returns the path of this key as a human readable string starting with M to indicate the master key.
     */
    val pathAsString: String
        get() = HDUtils.formatPath(path)

    /** Returns the last element of the path returned by [DeterministicKey.getPath]  */
    val childNumber: ChildNumber
        get() = if (path.size == 0) ChildNumber.ZERO else path[path.size - 1]

    /**
     * Returns RIPE-MD160(SHA256(pub key bytes)).
     */
    val identifier: ByteArray
        get() = ByteUtils.sha256hash160(key.pubKey)

    /** Returns the first 32 bits of the result of [.getIdentifier].  */
    // TODO: why is this different than armory's fingerprint? BIP 32: "The first 32 bits of the identifier are called the fingerprint."
    val fingerprint: Int
        get() = ByteBuffer.wrap(Arrays.copyOfRange(identifier, 0, 4)).int

    /**
     * Returns private key bytes, padded with zeros to 33 bytes.
     * @throws java.lang.IllegalStateException if the private key bytes are missing.
     */
    val privKeyBytes33: ByteArray
        get() {
            val bytes33 = ByteArray(33)
            val priv = key.privKeyBytes
            System.arraycopy(priv!!, 0, bytes33, 33 - priv.size, priv.size)
            return bytes33
        }

    /**
     * A deterministic key is considered to be 'public key only' if it hasn't got a private key part and it cannot be
     * rederived. If the hierarchy is encrypted this returns true.
     */
    val isPubKeyOnly: Boolean
        get() = key.isPubKeyOnly && (parent == null || parent.isPubKeyOnly)

    /**
     * Returns this keys [com.nchain.bitcoinkt.crypto.KeyCrypter] **or** the keycrypter of its parent key.
     */
//    fun findKeyCrypter():KeyCrypter? =
//        if (keyCrypter != null)
//            keyCrypter
//        else parent?.keyCrypter

    /**
     * Returns the private key of this deterministic key. Even if this object isn't storing the private key,
     * it can be re-derived by walking up to the parents if necessary and this is what will happen.
     * @throws java.lang.IllegalStateException if the parents are encrypted or a watching chain.
     */
    val privKey: BigInteger?
        get() {
            return findOrDerivePrivateKey()
        }

    /**
     * The creation time of a deterministic key is equal to that of its parent, unless this key is the root of a tree
     * in which case the time is stored alongside the key as per normal, see [com.nchain.bitcoinkt.core.ECKey.getCreationTimeSeconds].
     */
    /**
     * The creation time of a deterministic key is equal to that of its parent, unless this key is the root of a tree.
     * Thus, setting the creation time on a leaf is forbidden.
     */
//    fun findCreationTimeSeconds(): Long = parent?.findCreationTimeSeconds() ?: super.findCreationTimeSeconds()

/*
    fun updateCreationTimeSeconds(newCreationTimeSeconds:Long) {
        if (parent != null)
            throw IllegalStateException("Creation time can only be set on root keys.")
        else
            super.updateCreationTimeSeconds(newCreationTimeSeconds)
    }
*/

    /** Constructs a key from its components. This is not normally something you should use.  */
    constructor(childNumberPath: List<ChildNumber>,
                chainCode: ByteArray,
                publicAsPoint: LazyECPoint?,
                priv: BigInteger?,
                parent: DeterministicKey?) {
        key = ECKey(priv, ECKey.compressPoint(publicAsPoint!!))
        check(chainCode.size == 32)
        this.parent = parent
        this.path = childNumberPath
        this.chainCode = Arrays.copyOf(chainCode, chainCode.size)
        this.depth = if (parent == null) 0 else parent.depth + 1
        this.parentFingerprint = parent?.fingerprint ?: 0
    }

/*
    constructor(childNumberPath: List<ChildNumber>,
                chainCode: ByteArray,
                publicAsPoint: ECPoint,
                priv: BigInteger?,
                parent: DeterministicKey?) : this(childNumberPath, chainCode, LazyECPoint(publicAsPoint), priv, parent) {
    }
*/

    /** Constructs a key from its components. This is not normally something you should use.  */
    constructor(childNumberPath: List<ChildNumber>,
                chainCode: ByteArray,
                priv: BigInteger,
                parent: DeterministicKey?) {
        key = ECKey(priv, ECKey.compressPoint(ECKey.publicPointFromPrivate(priv))) 
        check(chainCode.size == 32)
        this.parent = parent
        this.path = childNumberPath
        this.chainCode = Arrays.copyOf(chainCode, chainCode.size)
        this.depth = if (parent == null) 0 else parent.depth + 1
        this.parentFingerprint = parent?.fingerprint ?: 0
    }

    /** Constructs a key from its components. This is not normally something you should use.  */
//    constructor(childNumberPath: ImmutableList<ChildNumber>,
//                chainCode: ByteArray,
//                crypter: KeyCrypter,
//                pub: LazyECPoint,
//                priv: EncryptedData,
//                parent: DeterministicKey?) : this(childNumberPath, chainCode, pub, null, parent) {
//        this.encryptedPrivateKey = priv
//        this.keyCrypter = crypter
//    }

    /**
     * Return the fingerprint of this key's parent as an int value, or zero if this key is the
     * root node of the key hierarchy.  Raise an exception if the arguments are inconsistent.
     * This method exists to avoid code repetition in the constructors.
     */
    @Throws(IllegalArgumentException::class)
    private fun ascertainParentFingerprint(parentKey: DeterministicKey?, parentFingerprint: Int): Int {
        if (parentFingerprint != 0) {
            if (parent != null)
                check(parent.fingerprint == parentFingerprint,
                        {"parent fingerprint mismatch ${Integer.toHexString(parent.fingerprint)} ${Integer.toHexString(parentFingerprint)}"})
            return parentFingerprint
        } else
            return 0
    }

    /**
     * Constructs a key from its components, including its public key data and possibly-redundant
     * information about its parent key.  Invoked when deserializing, but otherwise not something that
     * you normally should use.
     */
    private constructor(childNumberPath: List<ChildNumber>,
                        chainCode: ByteArray,
                        publicAsPoint: LazyECPoint,
                        parent: DeterministicKey?,
                        depth: Int,
                        parentFingerprint: Int) {
        key = ECKey(null, ECKey.compressPoint(publicAsPoint))
        check(chainCode.size == 32)
        this.parent = parent
        this.path = childNumberPath
        this.chainCode = Arrays.copyOf(chainCode, chainCode.size)
        this.depth = depth
        this.parentFingerprint = ascertainParentFingerprint(parent, parentFingerprint)
    }

    /**
     * Constructs a key from its components, including its private key data and possibly-redundant
     * information about its parent key.  Invoked when deserializing, but otherwise not something that
     * you normally should use.
     */
    private constructor(childNumberPath: List<ChildNumber>,
                        chainCode: ByteArray,
                        priv: BigInteger,
                        parent: DeterministicKey?,
                        depth: Int,
                        parentFingerprint: Int) {
        key = ECKey(priv, ECKey.compressPoint(ECKey.publicPointFromPrivate(priv)))
        check(chainCode.size == 32)
        this.parent = parent
        this.path = childNumberPath
        this.chainCode = Arrays.copyOf(chainCode, chainCode.size)
        this.depth = depth
        this.parentFingerprint = ascertainParentFingerprint(parent, parentFingerprint)
    }


    /** Clones the key  */
/*
    constructor(keyToClone: DeterministicKey, newParent: DeterministicKey) {
        key = ECKey(keyToClone.key.priv, keyToClone.key.pub!!.get())
        this.parent = newParent
        this.path = keyToClone.path
        this.chainCode = keyToClone.chainCode
//        this.encryptedPrivateKey = keyToClone.encryptedPrivateKey
        this.depth = this.path.size
        this.parentFingerprint = this.parent!!.fingerprint
    }
*/

    /**
     * Returns the same key with the private bytes removed. May return the same instance. The purpose of this is to save
     * memory: the private key can always be very efficiently rederived from a parent that a private key, so storing
     * all the private keys in RAM is a poor tradeoff especially on constrained devices. This means that the returned
     * key may still be usable for signing and so on, so don't expect it to be a true pubkey-only object! If you want
     * that then you should follow this call with a call to [.dropParent].
     */
    fun dropPrivateBytes(): DeterministicKey {
        return if (isPubKeyOnly)
            this
        else
            DeterministicKey(path, chainCode, key.pub, null, parent)
    }

    /**
     *
     * Returns the same key with the parent pointer removed (it still knows its own path and the parent fingerprint).
     *
     *
     * If this key doesn't have private key bytes stored/cached itself, but could rederive them from the parent, then
     * the new key returned by this method won't be able to do that. Thus, using dropPrivateBytes().dropParent() on a
     * regular DeterministicKey will yield a new DeterministicKey that cannot sign or do other things involving the
     * private key at all.
     */
    fun dropParent(): DeterministicKey {
        val key = DeterministicKey(path, chainCode, key.pub, key.priv, null)
        key.parentFingerprint = parentFingerprint
        return key
    }

//    @Throws(KeyCrypterException::class)
//    override fun encrypt(keyCrypter: KeyCrypter, aesKey: KeyParameter): DeterministicKey {
//        throw UnsupportedOperationException("Must supply a new parent for encryption")
//    }

/*
    @Throws(KeyCrypterException::class)
    fun encrypt(keyCrypter: KeyCrypter, aesKey: KeyParameter, newParent: DeterministicKey?): DeterministicKey {
        // Same as the parent code, except we construct a DeterministicKey instead of an ECKey.
        if (newParent != null)
            check(newParent.isEncrypted)
        val privKeyBytes = privKeyBytes
        check(privKeyBytes != null, "Private key is not available")
        val encryptedPrivateKey = keyCrypter.encrypt(privKeyBytes!!, aesKey)
        val key = DeterministicKey(path, chainCode, keyCrypter, pub!!, encryptedPrivateKey, newParent)
        if (newParent == null)
            key.updateCreationTimeSeconds(findCreationTimeSeconds())
        return key
    }
*/

    /** {@inheritDoc}  */
    fun hasPrivKey(): Boolean {
        return findParentWithPrivKey() != null
    }

    val secretBytes: ByteArray?
        get() = if (key.priv != null) key.privKeyBytes else null

    /**
     * A deterministic key is considered to be encrypted if it has access to encrypted private key bytes, OR if its
     * parent does. The reason is because the parent would be encrypted under the same key and this key knows how to
     * rederive its own private key bytes from the parent, if needed.
     */
//    override val isEncrypted: Boolean
//        get() = priv == null && (super.isEncrypted || parent != null && parent.isEncrypted)

/*
    @Throws(KeyCrypterException::class)
    override fun sign(input: Sha256Hash, aesKey: KeyParameter?): ECKey.ECDSASignature {
        if (isEncrypted) {
            // If the key is encrypted, ECKey.sign will decrypt it first before rerunning sign. Decryption walks the
            // key heirarchy to find the private key (see below), so, we can just run the inherited method.
            return super.sign(input, aesKey)
        } else {
            // If it's not encrypted, derive the private via the parents.
            val privateKey = findOrDerivePrivateKey()
                    ?: // This key is a part of a public-key only heirarchy and cannot be used for signing
                    throw ECKey.MissingPrivateKeyException()
            return super.doSign(input, privateKey)
        }
    }
*/

/*
    @Throws(KeyCrypterException::class)
    override fun decrypt(keyCrypter: KeyCrypter, aesKey: KeyParameter): DeterministicKey {
        // Check that the keyCrypter matches the one used to encrypt the keys, if set.
        if (this.keyCrypter != null && this.keyCrypter != keyCrypter)
            throw KeyCrypterException("The keyCrypter being used to decrypt the key is different to the one that was used to encrypt it")
        val privKey = findOrDeriveEncryptedPrivateKey(keyCrypter, aesKey)
        val key = DeterministicKey(path, chainCode, privKey, parent)
        if (!Arrays.equals(key.pubKey, pubKey))
            throw KeyCrypterException("Provided AES key is wrong")
        if (parent == null)
            key.updateCreationTimeSeconds(findCreationTimeSeconds())
        return key
    }

    @Throws(KeyCrypterException::class)
    override fun decrypt(aesKey: KeyParameter): DeterministicKey {
        return super.decrypt(aesKey) as DeterministicKey
    }
*/

    // For when a key is encrypted, either decrypt our encrypted private key bytes, or work up the tree asking parents
    // to decrypt and re-derive.
/*
    private fun findOrDeriveEncryptedPrivateKey(keyCrypter: KeyCrypter, aesKey: KeyParameter): BigInteger {
        if (encryptedPrivateKey != null)
            return BigInteger(1, keyCrypter.decrypt(encryptedPrivatekey, aesKey))
        // Otherwise we don't have it, but maybe we can figure it out from our parents. Walk up the tree looking for
        // the first key that has some encrypted private key data.
        var cursor = parent
        while (cursor != null) {
            if (cursor.encryptedPrivateKey != null) break
            cursor = cursor.parent
        }
        if (cursor == null)
            throw KeyCrypterException("Neither this key nor its parents have an encrypted private key")
        val parentalPrivateKeyBytes = keyCrypter.decrypt(cursor.encryptedPrivatekey, aesKey)
        return derivePrivateKeyDownwards(cursor, parentalPrivateKeyBytes)
    }
*/

    private fun findParentWithPrivKey(): DeterministicKey? {
        var cursor: DeterministicKey? = this
        while (cursor != null) {
            if (cursor.key.priv != null) break
            cursor = cursor.parent
        }
        return cursor
    }

    private fun findOrDerivePrivateKey(): BigInteger? {
        val cursor = findParentWithPrivKey()
        if (cursor == null) return null
        return derivePrivateKeyDownwards(cursor, cursor.key.priv!!.toByteArray())
    }

    private fun derivePrivateKeyDownwards(cursor: DeterministicKey, parentalPrivateKeyBytes: ByteArray): BigInteger {
        var downCursor = DeterministicKey(cursor.path, cursor.chainCode,
                cursor.key.pub, BigInteger(1, parentalPrivateKeyBytes), cursor.parent)
        // Now we have to rederive the keys along the path back to ourselves. That path can be found by just truncating
        // our path with the length of the parents path.
        val path = this.path.subList(cursor.path.size, this.path.size)
        for (num in path) {
            downCursor = HDKeyDerivation.deriveChildKey(downCursor, num)
        }
        // downCursor is now the same key as us, but with private key bytes.
        // If it's not, it means we tried decrypting with an invalid password and earlier checks e.g. for padding didn't
        // catch it.
        if (downCursor.key.pub != key.pub)
            throw KeyCrypterException("Could not decrypt bytes")
        return checkNotNull<BigInteger>(downCursor.key.priv)
    }

    /**
     * Derives a child at the given index using hardened derivation.  Note: `index` is
     * not the "i" value.  If you want the softened derivation, then use instead
     * `HDKeyDerivation.deriveChildKey(this, new ChildNumber(child, false))`.
     */
    fun derive(child: Int): DeterministicKey {
        return HDKeyDerivation.deriveChildKey(this, ChildNumber(child, true))
    }

    fun serializePublic(params: NetworkParameters): ByteArray {
        return serialize(params, true)
    }

    fun serializePrivate(params: NetworkParameters): ByteArray {
        return serialize(params, false)
    }

    private fun serialize(params: NetworkParameters, pub: Boolean): ByteArray {
        val ser = ByteBuffer.allocate(78)
        ser.putInt(if (pub) params.bip32HeaderPub else params.bip32HeaderPriv)
        ser.put(depth.toByte())
        ser.putInt(parentFingerprint)
        ser.putInt(childNumber.i())
        ser.put(chainCode)
        ser.put(if (pub) key.pubKey else privKeyBytes33)
        check(ser.position() == 78)
        return ser.array()
    }

    fun serializePubB58(params: NetworkParameters): String {
        return toBase58(serialize(params, true))
    }

    fun serializePrivB58(params: NetworkParameters): String {
        return toBase58(serialize(params, false))
    }

    /**
     * Verifies equality of all fields but NOT the parent pointer (thus the same key derived in two separate heirarchy
     * objects will equal each other.
     */
    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o != null && o is DeterministicKey) {
            return ( //* super.equals(other) &&
                    Arrays.equals(chainCode, o.chainCode)
                    && path == o.path)
        }
        return false
    }

/*
    override fun hashCode(): Int {
        return Objects.hashCode(super.hashCode(), Arrays.hashCode(chainCode), path)
    }

    override fun toString(): String {
        val helper = MoreObjects.toStringHelper(this).omitNullValues()
        helper.add("pub", Utils.HEX.encode(pub!!.encoded))
        helper.add("chainCode", HEX.encode(chainCode))
        helper.add("path", pathAsString)
        if (creationTimeSeconds > 0)
            helper.add("creationTimeSeconds", creationTimeSeconds)
        helper.add("isEncrypted", isEncrypted)
        helper.add("isPubKeyOnly", isPubKeyOnly)
        return helper.toString()
    }

*/
/*
    fun formatKeyWithAddress(includePrivateKeys: Boolean, builder: StringBuilder, params: NetworkParameters) {
        val address = toAddress(params)
        builder.append("  addr:").append(address)
        builder.append("  hash160:").append(Utils.HEX.encode(getPubKeyHash()))
        builder.append("  (").append(pathAsString).append(")\n")
        if (includePrivateKeys) {
            builder.append("  ").append(toStringWithPrivate(params)).append("\n")
        }
    }
*/

    companion object {

        /** Sorts deterministic keys in the order of their child number. That's *usually* the order used to derive them.  */
        val CHILDNUM_ORDER: Comparator<ECKey> = Comparator { k1, k2 ->
            val cn1 = (k1 as DeterministicKey).childNumber
            val cn2 = (k2 as DeterministicKey).childNumber
            cn1.compareTo(cn2)
        }

        internal fun addChecksum(input: ByteArray): ByteArray {
            val inputLength = input.size
            val checksummed = ByteArray(inputLength + 4)
            System.arraycopy(input, 0, checksummed, 0, inputLength)
            val checksum = Sha256Hash.hashTwice(input)
            System.arraycopy(checksum, 0, checksummed, inputLength, 4)
            return checksummed
        }

        internal fun toBase58(ser: ByteArray): String {
            return Base58.encode(addChecksum(ser))
        }

        /** Deserialize a base-58-encoded HD Key with no parent  */
        fun deserializeB58(base58: String, params: NetworkParameters): DeterministicKey {
            return deserializeB58(null, base58, params)
        }

        /**
         * Deserialize a base-58-encoded HD Key.
         * @param parent The parent node in the given key's deterministic hierarchy.
         * @throws IllegalArgumentException if the base58 encoded key could not be parsed.
         */
        fun deserializeB58(parent: DeterministicKey?, base58: String, params: NetworkParameters): DeterministicKey {
            return deserialize(params, Base58.decodeChecked(base58), parent)
        }

        /**
         * Deserialize an HD Key.
         * @param parent The parent node in the given key's deterministic hierarchy.
         */
        @JvmOverloads
        fun deserialize(params: NetworkParameters, serializedKey: ByteArray, parent: DeterministicKey? = null): DeterministicKey {
            val buffer = ByteBuffer.wrap(serializedKey)
            val header = buffer.int
            if (header != params.bip32HeaderPriv && header != params.bip32HeaderPub)
                throw IllegalArgumentException("Unknown header bytes: " + toBase58(serializedKey).substring(0, 4))
            val pub = header == params.bip32HeaderPub
            val depth = buffer.get().toInt() and 0xFF // convert signed byte to positive int since depth cannot be negative
            val parentFingerprint = buffer.int
            val i = buffer.int
            val childNumber = ChildNumber(i)
            val path: List<ChildNumber>
            if (parent != null) {
                if (parentFingerprint == 0)
                    throw IllegalArgumentException("Parent was provided but this key doesn't have one")
                if (parent.fingerprint != parentFingerprint)
                    throw IllegalArgumentException("Parent fingerprints don't match")
                path = HDUtils.append(parent.path, childNumber)
                if (path.size != depth.toInt())
                    throw IllegalArgumentException("Depth does not match")
            } else {
                if (depth >= 1)
                // We have been given a key that is not a root key, yet we lack the object representing the parent.
                // This can happen when deserializing an account key for a watching wallet.  In this case, we assume that
                // the client wants to conceal the key's position in the hierarchy.  The path is truncated at the
                // parent's node.
                    path = listOf(childNumber)
                else
                    path = emptyList()
            }
            val chainCode = ByteArray(32)
            buffer.get(chainCode)
            val data = ByteArray(33)
            buffer.get(data)
            check(!buffer.hasRemaining(), {"Found unexpected data in key"})
            return if (pub) {
                DeterministicKey(path, chainCode, LazyECPoint(ECKey.CURVE.curve, data), parent, depth.toInt(), parentFingerprint)
            } else {
                DeterministicKey(path, chainCode, BigInteger(1, data), parent, depth.toInt(), parentFingerprint)
            }
        }
    }
}
