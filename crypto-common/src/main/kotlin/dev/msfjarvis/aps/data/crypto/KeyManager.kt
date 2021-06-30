/*
 * Copyright © 2014-2021 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package dev.msfjarvis.aps.data.crypto

import com.github.michaelbull.result.Result

public interface KeyManager<T : KeyPair> {

  public suspend fun addKey(stringKey: String): Result<String, Throwable>
  public suspend fun addKey(key: T): Result<String, Throwable>
  public suspend fun removeKey(key: T): Result<String, Throwable>
  public suspend fun findKeyById(id: String): Result<T, Throwable>
  public suspend fun listKeys(): Result<List<T>, Throwable>
  public suspend fun listKeyIds(): Result<List<String>, Throwable>
}
