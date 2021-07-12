/*
 * Copyright © 2014-2021 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package dev.msfjarvis.aps.injection.crypto

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dev.msfjarvis.aps.data.crypto.GPGKeyManager
import dev.msfjarvis.aps.data.crypto.GPGKeyPair
import dev.msfjarvis.aps.data.crypto.KeyManager
import dev.msfjarvis.aps.data.crypto.KeyPair
import dev.msfjarvis.aps.injection.context.FilesDirPath
import dev.msfjarvis.aps.injection.coroutines.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher

/**
 * This module adds all [KeyManager] implementations into a Set which makes it easier to build
 * generic UIs which are not tied to a specific implementation because of injection.
 */
@Module
@InstallIn(SingletonComponent::class)
object KeyManagerModule {

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

  @Suppress("UNCHECKED_CAST")
  @Provides
  @IntoSet
  fun provideKeyManager(
    gpgKeyManager: GPGKeyManager,
  ): KeyManager<KeyPair> {
    return gpgKeyManager as KeyManager<KeyPair>
  }
}

/** Typealias for a [Set] of [KeyManager] instances injected by Dagger. */
typealias KeyManagerSet = Set<@JvmSuppressWildcards KeyManager<KeyPair>>
