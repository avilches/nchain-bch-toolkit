package com.nchain.keycrypter

import com.nchain.key.ECKey
import org.spongycastle.crypto.params.KeyParameter
import java.util.*

/*
 * @author Alberto Vilches
 * @date 17/07/2018
 */

class ECKeyCrypted(val encryptedPrivateKey: EncryptedData, val keyCrypter: KeyCrypter, val ecKey: ECKey) {

    /**
     * Create an encrypted private key with the keyCrypter and the AES key supplied.
     * This method returns a new encrypted key and leaves the original unchanged.
     * The ECKey inside this object is a new cloned ECKey without the private key
     *
     * @param keyCrypter The keyCrypter that specifies exactly how the encrypted bytes are created.
     * @param aesKey The KeyParameter with the AES encryption key (usually constructed with keyCrypter#deriveKey and cached as it is slow to create).
     * @return encryptedKey
     */
    companion object {
        fun create(ecKey: ECKey, keyCrypter: KeyCrypter, password: String): ECKeyCrypted {
            return create(ecKey, keyCrypter, keyCrypter.deriveKey(password))
        }

        fun create(ecKey: ECKey, keyCrypter: KeyCrypter, aesKey: KeyParameter): ECKeyCrypted {
            val encryptedPrivateKey = keyCrypter.encrypt(ecKey.privKeyBytes, aesKey)
            val ecKeyWithNoPrivKey = ECKey.fromPublicOnly(ecKey.pubKey)
            val crypted = ECKeyCrypted(encryptedPrivateKey, keyCrypter, ecKeyWithNoPrivKey)
            crypted.decrypt(aesKey)
            return crypted
        }
    }

    /**
     * Create a decrypted private key with the keyCrypter and AES key supplied. Note that if the aesKey is wrong, this
     * has some chance of throwing KeyCrypterException due to the corrupted padding that will result, but it can also
     * just yield a garbage key.
     *
     * @param keyCrypter The keyCrypter that specifies exactly how the decrypted bytes are created.
     * @param aesKey The KeyParameter with the AES encryption key (usually constructed with keyCrypter#deriveKey and cached).
     */
    fun decrypt(password: String): ECKey {
        return decrypt(keyCrypter.deriveKey(password))
    }

    fun decrypt(aesKey: KeyParameter): ECKey {
        val unencryptedPrivateKey = keyCrypter.decrypt(encryptedPrivateKey, aesKey)
        val key = ECKey.fromPrivate(unencryptedPrivateKey)
        check(Arrays.equals(key.pubKey, ecKey.pubKey), { "Provided AES key is wrong" })
        return key
    }

}
