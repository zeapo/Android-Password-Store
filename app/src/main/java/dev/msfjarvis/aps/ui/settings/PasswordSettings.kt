/*
 * Copyright © 2014-2021 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package dev.msfjarvis.aps.ui.settings

import android.text.InputType
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import de.Maxr1998.modernpreferences.Preference
import de.Maxr1998.modernpreferences.PreferenceScreen
import de.Maxr1998.modernpreferences.helpers.checkBox
import de.Maxr1998.modernpreferences.helpers.editText
import de.Maxr1998.modernpreferences.helpers.onCheckedChange
import de.Maxr1998.modernpreferences.helpers.onClick
import de.Maxr1998.modernpreferences.helpers.onSelectionChange
import de.Maxr1998.modernpreferences.helpers.pref
import de.Maxr1998.modernpreferences.helpers.singleChoice
import de.Maxr1998.modernpreferences.preferences.CheckBoxPreference
import de.Maxr1998.modernpreferences.preferences.choice.SelectionItem
import dev.msfjarvis.aps.R
import dev.msfjarvis.aps.ui.onboarding.activity.OnboardingActivity
import dev.msfjarvis.aps.util.extensions.getString
import dev.msfjarvis.aps.util.extensions.sharedPrefs
import dev.msfjarvis.aps.util.extensions.unsafeLazy
import dev.msfjarvis.aps.util.pwgenxkpwd.XkpwdDictionary
import dev.msfjarvis.aps.util.settings.PreferenceKeys
import java.io.File

class PasswordSettings(private val activity: FragmentActivity) : SettingsProvider {

  private val sharedPrefs by unsafeLazy { activity.sharedPrefs }
  private val storeCustomXkpwdDictionaryAction =
    activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri == null) return@registerForActivityResult

      Toast.makeText(
          activity,
          activity.resources.getString(R.string.xkpwgen_custom_dict_imported, uri.path),
          Toast.LENGTH_SHORT
        )
        .show()

      sharedPrefs.edit { putString(PreferenceKeys.PREF_KEY_CUSTOM_DICT, uri.toString()) }

      val inputStream = activity.contentResolver.openInputStream(uri)
      val customDictFile =
        File(activity.filesDir.toString(), XkpwdDictionary.XKPWD_CUSTOM_DICT_FILE).outputStream()
      inputStream?.copyTo(customDictFile, 1024)
      inputStream?.close()
      customDictFile.close()
    }

  override fun provideSettings(builder: PreferenceScreen.Builder) {
    builder.apply {
      val customDictPref =
        CheckBoxPreference(PreferenceKeys.PREF_KEY_IS_CUSTOM_DICT).apply {
          titleRes = R.string.pref_xkpwgen_custom_wordlist_enabled_title
          summaryRes = R.string.pref_xkpwgen_custom_dict_summary_off
          summaryOnRes = R.string.pref_xkpwgen_custom_dict_summary_on
          visible = sharedPrefs.getString(PreferenceKeys.PREF_KEY_PWGEN_TYPE) == "xkpasswd"
          onCheckedChange {
            requestRebind()
            true
          }
        }
      val customDictPathPref =
        Preference(PreferenceKeys.PREF_KEY_CUSTOM_DICT).apply {
          dependency = PreferenceKeys.PREF_KEY_IS_CUSTOM_DICT
          titleRes = R.string.pref_xkpwgen_custom_dict_picker_title
          summary =
            sharedPrefs.getString(PreferenceKeys.PREF_KEY_CUSTOM_DICT)
              ?: activity.resources.getString(R.string.pref_xkpwgen_custom_dict_picker_summary)
          visible = sharedPrefs.getString(PreferenceKeys.PREF_KEY_PWGEN_TYPE) == "xkpasswd"
          onClick {
            storeCustomXkpwdDictionaryAction.launch(arrayOf("*/*"))
            true
          }
        }
      val values = activity.resources.getStringArray(R.array.pwgen_provider_values)
      val labels = activity.resources.getStringArray(R.array.pwgen_provider_labels)
      val items = values.zip(labels).map { SelectionItem(it.first, it.second, null) }
      singleChoice(
        PreferenceKeys.PREF_KEY_PWGEN_TYPE,
        items,
      ) {
        initialSelection = "classic"
        titleRes = R.string.pref_password_generator_type_title
        onSelectionChange { selection ->
          val xkpasswdEnabled = selection == "xkpasswd"
          customDictPathPref.visible = xkpasswdEnabled
          customDictPref.visible = xkpasswdEnabled
          customDictPref.requestRebind()
          customDictPathPref.requestRebind()
          true
        }
      }
      // We initialize them early and add them manually to be able to manually force a rebind
      // when the password generator type is changed.
      addPreferenceItem(customDictPref)
      addPreferenceItem(customDictPathPref)
      editText(PreferenceKeys.GENERAL_SHOW_TIME) {
        titleRes = R.string.pref_clipboard_timeout_title
        summaryProvider =
          { timeout ->
            activity.getString(R.string.pref_clipboard_timeout_summary, timeout ?: "45")
          }
        textInputType = InputType.TYPE_CLASS_NUMBER
      }
      checkBox(PreferenceKeys.SHOW_PASSWORD) {
        titleRes = R.string.show_password_pref_title
        summaryRes = R.string.show_password_pref_summary
        defaultValue = true
      }
      checkBox(PreferenceKeys.COPY_ON_DECRYPT) {
        titleRes = R.string.pref_copy_title
        summaryRes = R.string.pref_copy_summary
        defaultValue = false
      }
      pref(PreferenceKeys.IMPORT_GPG_KEY) {
        titleRes = R.string.pref_import_gpg_key_title
        summaryRes = R.string.pref_import_gpg_key_summary
        onClick {
          activity.startActivity(OnboardingActivity.createKeyImportIntent(activity))
          true
        }
      }
    }
  }
}
