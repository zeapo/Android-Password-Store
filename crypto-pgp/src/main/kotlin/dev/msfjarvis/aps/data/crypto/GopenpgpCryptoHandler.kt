/*
 * Copyright © 2014-2021 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package dev.msfjarvis.aps.data.crypto

import com.github.michaelbull.result.unwrap
import com.proton.Gopenpgp.crypto.Crypto
import com.proton.Gopenpgp.helper.Helper
import javax.inject.Inject

/** Gopenpgp backed implementation of [CryptoHandler]. */
public class GopenpgpCryptoHandler @Inject constructor(private val gpgKeyManager: GPGKeyManager) :
  CryptoHandler {

  /**
   * Decrypt the given [ciphertext] using the given PGP [privateKey] and corresponding [passphrase].
   */
  override fun decrypt(
    privateKey: String,
    passphrase: ByteArray,
    ciphertext: ByteArray,
  ): ByteArray {
    // Decode the incoming cipher into a string and try to guess if it's armored.
    val cipherString = ciphertext.decodeToString()
    val isArmor = cipherString.startsWith("-----BEGIN PGP MESSAGE-----")
    val message =
      if (isArmor) {
        Crypto.newPGPMessageFromArmored(cipherString)
      } else {
        Crypto.newPGPMessage(ciphertext)
      }
    return Helper.decryptBinaryMessageArmored(
      privateKey,
      passphrase,
      message.armored,
    )
  }

  override fun encrypt(publicKey: String, plaintext: ByteArray): ByteArray {
    return Helper.encryptBinaryMessage(
      publicKey,
      plaintext,
    )
  }

  override fun canHandle(fileName: String): Boolean {
    return fileName.split('.').last() == "gpg"
  }

  /**
   * TODO: Find a better place for this method Utility method to decrypt the given [ciphertext]
   * using the given PGP key [id] and corresponding [passphrase].
   */
  public suspend fun decryptFromKeyId(
    id: String,
    passphrase: ByteArray,
    ciphertext: String,
  ): ByteArray {
    // TODO(aditya): Handle error cases
    val key = gpgKeyManager.findKeyById(id).unwrap()
    val message = Crypto.newPGPMessageFromArmored(ciphertext)
    return Helper.decryptMessageArmored(
        key.getPrivateKey().decodeToString(),
        passphrase,
        message.armored,
      )
      .toByteArray()
  }
}
