/*
 * Copyright © 2014-2021 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package dev.msfjarvis.aps.injection.crypto

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.msfjarvis.aps.data.crypto.GPGKeyManager
import dev.msfjarvis.aps.data.crypto.GPGKeyPair
import dev.msfjarvis.aps.injection.context.FilesDirPath
import dev.msfjarvis.aps.injection.coroutines.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher

@Module
@InstallIn(SingletonComponent::class)
abstract class KeyManagerModule {

  internal companion object {

    @Provides
    fun providesGPGKeyManager(
      @FilesDirPath filesDirPath: String,
      @IODispatcher dispatcher: CoroutineDispatcher,
      gpgKeyFactory: GPGKeyPair.Factory,
    ): GPGKeyManager {
      return GPGKeyManager(
        filesDirPath,
        dispatcher,
        gpgKeyFactory,
      )
    }
  }
}
