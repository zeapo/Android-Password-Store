/*
 * Copyright © 2014-2021 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package dev.msfjarvis.aps.data.crypto

/** Defines expectations for a keypair used in public key cryptography. */
public interface KeyPair {
  public fun getPrivateKey(): ByteArray

  public fun getPublicKey(): ByteArray

  public fun getKeyId(): String
}
