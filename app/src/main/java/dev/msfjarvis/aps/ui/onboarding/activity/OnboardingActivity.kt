/*
 * Copyright © 2014-2021 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package dev.msfjarvis.aps.ui.onboarding.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commitNow
import dagger.hilt.android.AndroidEntryPoint
import dev.msfjarvis.aps.R
import dev.msfjarvis.aps.ui.onboarding.fragments.GopenpgpKeySelectionFragment
import dev.msfjarvis.aps.ui.onboarding.fragments.WelcomeFragment

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity(R.layout.activity_onboarding) {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    supportActionBar?.hide()
    val import = intent.extras?.getBoolean(KEY_IMPORT) ?: false
    supportFragmentManager.commitNow {
      if (import) {
        replace(R.id.fragment_first_run, GopenpgpKeySelectionFragment.newInstance())
      } else {
        replace(R.id.fragment_first_run, WelcomeFragment.newInstance())
      }
    }
  }

  override fun onBackPressed() {
    if (supportFragmentManager.backStackEntryCount == 0) {
      finishAffinity()
    } else {
      super.onBackPressed()
    }
  }

  companion object {
    private const val KEY_IMPORT = "KEY_IMPORT"

    fun createKeyImportIntent(context: Context) =
      Intent(
          context,
          OnboardingActivity::class.java,
        )
        .putExtra(KEY_IMPORT, true)
  }
}
