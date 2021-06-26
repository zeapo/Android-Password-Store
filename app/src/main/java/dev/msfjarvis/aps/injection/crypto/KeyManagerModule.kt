/*
 * Copyright © 2014-2021 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package dev.msfjarvis.aps.injection.crypto

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.msfjarvis.aps.data.crypto.AndroidKeyManager
import dev.msfjarvis.aps.data.crypto.KeyManager
import dev.msfjarvis.aps.injection.context.FilesDirPath

@Module
@InstallIn(SingletonComponent::class)
abstract class KeyManagerModule {

  @Binds abstract fun bindKeyManager(androidKeyManager: AndroidKeyManager): KeyManager

  internal companion object {

    @Provides
    fun providesAndroidKeyManager(@FilesDirPath filesDirPath: String): AndroidKeyManager {
      // TODO: Use dagger for coroutine dispatchers
      return AndroidKeyManager(filesDirPath)
    }
  }
}
