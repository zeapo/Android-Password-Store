/*
 * Copyright © 2014-2021 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package dev.msfjarvis.aps.ui.onboarding.fragments

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import dev.msfjarvis.aps.R
import dev.msfjarvis.aps.databinding.FragmentCloneBinding
import dev.msfjarvis.aps.ui.git.config.GitServerConfigActivity
import dev.msfjarvis.aps.util.extensions.performTransactionWithBackStack
import dev.msfjarvis.aps.util.extensions.sharedPrefs
import dev.msfjarvis.aps.util.extensions.unsafeLazy
import dev.msfjarvis.aps.util.extensions.viewBinding
import dev.msfjarvis.aps.util.settings.PreferenceKeys

class CloneFragment : Fragment(R.layout.fragment_clone) {

  private val binding by viewBinding(FragmentCloneBinding::bind)
  private val settings by unsafeLazy { requireActivity().applicationContext.sharedPrefs }
  private val cloneAction =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == AppCompatActivity.RESULT_OK) {
        settings.edit { putBoolean(PreferenceKeys.REPOSITORY_INITIALIZED, true) }
        parentFragmentManager.performTransactionWithBackStack(
          GopenpgpKeySelectionFragment.newInstance()
        )
      }
    }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.cloneRemote.setOnClickListener { cloneToHiddenDir() }
    binding.createLocal.setOnClickListener {
      parentFragmentManager.performTransactionWithBackStack(RepoLocationFragment.newInstance())
    }
  }

  /** Clones a remote Git repository to the app's private directory */
  private fun cloneToHiddenDir() {
    settings.edit { putBoolean(PreferenceKeys.GIT_EXTERNAL, false) }
    cloneAction.launch(GitServerConfigActivity.createCloneIntent(requireContext()))
  }

  companion object {

    fun newInstance(): CloneFragment = CloneFragment()
  }
}
