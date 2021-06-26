/*
 * Copyright © 2014-2021 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package dev.msfjarvis.aps.ui.onboarding.fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import dagger.hilt.android.AndroidEntryPoint
import dev.msfjarvis.aps.R
import dev.msfjarvis.aps.databinding.FragmentKeySelectionBinding
import dev.msfjarvis.aps.ui.onboarding.viewmodel.KeySelectionViewModel
import dev.msfjarvis.aps.util.extensions.commitChange
import dev.msfjarvis.aps.util.extensions.finish
import dev.msfjarvis.aps.util.extensions.sharedPrefs
import dev.msfjarvis.aps.util.extensions.unsafeLazy
import dev.msfjarvis.aps.util.extensions.viewBinding
import dev.msfjarvis.aps.util.settings.PreferenceKeys
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class GopenpgpKeySelectionFragment : Fragment(R.layout.fragment_key_selection) {

  private val settings by unsafeLazy { requireActivity().applicationContext.sharedPrefs }
  private val binding by viewBinding(FragmentKeySelectionBinding::bind)
  private val viewModel: KeySelectionViewModel by viewModels()

  private val gpgKeySelectAction = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    if (uri == null) {
      // TODO: Use string resources here
      showError("No files chosen")
      return@registerForActivityResult
    }

    val fis = requireContext().contentResolver.openInputStream(uri)
    if (fis == null) {
      showError("Error resolving content uri")
      return@registerForActivityResult
    }

      viewModel.importKey(fis)
    }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.selectKey.setOnClickListener { gpgKeySelectAction.launch("*/*") }

    viewModel.importKeyStatus.onEach { result ->
      result.onSuccess {
        settings.edit { putBoolean(PreferenceKeys.REPOSITORY_INITIALIZED, true) }
        requireActivity().commitChange(getString(R.string.git_commit_gpg_id, getString(R.string.app_name)))
        finish()
      }.onFailure {
        showError(it.message!!)
      }
    }
  }

  private fun showError(message: String) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
  }

  companion object {

    fun newInstance() = GopenpgpKeySelectionFragment()
  }
}
