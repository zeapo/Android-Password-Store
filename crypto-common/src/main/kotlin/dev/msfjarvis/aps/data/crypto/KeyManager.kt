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
  public suspend fun getKeyById(id: String): Result<T, Throwable>
  public suspend fun getAllKeys(): Result<List<T>, Throwable>
  public suspend fun getAllKeyIds(): Result<List<String>, Throwable>

  /** Given a [fileName], return whether this instance can handle it. */
  public fun canHandle(fileName: String): Boolean
}
