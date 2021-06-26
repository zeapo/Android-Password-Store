/*
 * Copyright © 2014-2021 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package dev.msfjarvis.aps.ui.onboarding.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import dev.msfjarvis.aps.R

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity(R.layout.activity_onboarding) {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    supportActionBar?.hide()
  }

  override fun onBackPressed() {
    if (supportFragmentManager.backStackEntryCount == 0) {
      finishAffinity()
    } else {
      super.onBackPressed()
    }
  }
}
