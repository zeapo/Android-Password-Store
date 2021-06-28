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
import dev.msfjarvis.aps.data.crypto.AndroidKeyManager
import dev.msfjarvis.aps.data.crypto.CryptoHandler
import dev.msfjarvis.aps.data.crypto.GopenpgpCryptoHandler

/**
 * This module adds all [CryptoHandler] implementations into a Set which makes it easier to build
 * generic UIs which are not tied to a specific implementation because of injection.
 */
@Module
@InstallIn(SingletonComponent::class)
object CryptoHandlerModule {
  @Provides
  @IntoSet
  fun providePgpCryptoHandler(
    gpgKeyManager: AndroidKeyManager,
  ): CryptoHandler {
    return GopenpgpCryptoHandler(
      gpgKeyManager,
    )
  }
}

/** Typealias for a [Set] of [CryptoHandler] instances injected by Dagger. */
typealias CryptoSet = Set<@JvmSuppressWildcards CryptoHandler>
