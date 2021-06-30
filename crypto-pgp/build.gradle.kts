/*
 * Copyright © 2014-2021 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */
plugins {
  id("com.android.library")
  kotlin("android")
  `aps-plugin`
}

dependencies {
  api(projects.cryptoCommon)
  implementation(libs.aps.gopenpgp)
  implementation(libs.dagger.hilt.core)
  implementation(libs.thirdparty.kotlinResult)
  implementation(libs.kotlin.coroutines.core)
}
