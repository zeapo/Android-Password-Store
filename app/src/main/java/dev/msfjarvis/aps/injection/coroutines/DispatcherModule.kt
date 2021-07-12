/*
 * Copyright © 2014-2021 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package dev.msfjarvis.aps.injection.coroutines

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
  @IODispatcher
  @Provides
  fun provideIODispatcher(): CoroutineDispatcher {
    return Dispatchers.IO
  }

  @MainDispatcher
  @Provides
  fun provideMainDispatcher(): CoroutineDispatcher {
    return Dispatchers.Main
  }
}

@Qualifier @Retention(AnnotationRetention.RUNTIME) annotation class IODispatcher

@Qualifier @Retention(AnnotationRetention.RUNTIME) annotation class MainDispatcher
