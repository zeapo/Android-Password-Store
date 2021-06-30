/*
 * Copyright © 2014-2021 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package dev.msfjarvis.aps.data.crypto

import com.proton.Gopenpgp.crypto.Key
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/** Wraps a Gopenpgp [Key] to implement [KeyPair]. */
public class GPGKeyPair
@AssistedInject
constructor(
  @Assisted private val wrappedKey: Key,
) : KeyPair {

  override fun getPrivateKey(): ByteArray {
    return wrappedKey.armor().encodeToByteArray()
  }
  override fun getPublicKey(): ByteArray {
    return wrappedKey.armoredPublicKey.encodeToByteArray()
  }
  override fun getKeyId(): String {
    return wrappedKey.hexKeyID
  }

  @AssistedFactory
  public interface Factory {
    public fun create(wrappedKey: Key): GPGKeyPair
  }
}
