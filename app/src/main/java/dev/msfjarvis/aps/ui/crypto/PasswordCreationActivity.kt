/*
 * Copyright © 2014-2021 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package dev.msfjarvis.aps.ui.crypto

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.github.ajalt.timberkt.e
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.runCatching
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentIntegrator.QR_CODE
import dagger.hilt.android.AndroidEntryPoint
import dev.msfjarvis.aps.R
import dev.msfjarvis.aps.data.repo.PasswordRepository
import dev.msfjarvis.aps.databinding.PasswordCreationActivityBinding
import dev.msfjarvis.aps.injection.password.PasswordEntryFactory
import dev.msfjarvis.aps.ui.dialogs.OtpImportDialogFragment
import dev.msfjarvis.aps.ui.dialogs.PasswordGeneratorDialogFragment
import dev.msfjarvis.aps.ui.dialogs.XkPasswordGeneratorDialogFragment
import dev.msfjarvis.aps.util.autofill.AutofillPreferences
import dev.msfjarvis.aps.util.autofill.DirectoryStructure
import dev.msfjarvis.aps.util.crypto.GpgIdentifier
import dev.msfjarvis.aps.util.extensions.base64
import dev.msfjarvis.aps.util.extensions.commitChange
import dev.msfjarvis.aps.util.extensions.findTillRoot
import dev.msfjarvis.aps.util.extensions.getString
import dev.msfjarvis.aps.util.extensions.isInsideRepository
import dev.msfjarvis.aps.util.extensions.snackbar
import dev.msfjarvis.aps.util.extensions.unsafeLazy
import dev.msfjarvis.aps.util.extensions.viewBinding
import dev.msfjarvis.aps.util.settings.PreferenceKeys
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.msfjarvis.openpgpktx.util.OpenPgpApi
import me.msfjarvis.openpgpktx.util.OpenPgpServiceConnection

@AndroidEntryPoint
class PasswordCreationActivity : BasePgpActivity(), OpenPgpServiceConnection.OnBound {

  private val binding by viewBinding(PasswordCreationActivityBinding::inflate)
  @Inject lateinit var passwordEntryFactory: PasswordEntryFactory

  private val suggestedName by unsafeLazy { intent.getStringExtra(EXTRA_FILE_NAME) }
  private val suggestedPass by unsafeLazy { intent.getStringExtra(EXTRA_PASSWORD) }
  private val suggestedExtra by unsafeLazy { intent.getStringExtra(EXTRA_EXTRA_CONTENT) }
  private val shouldGeneratePassword by unsafeLazy {
    intent.getBooleanExtra(EXTRA_GENERATE_PASSWORD, false)
  }
  private val editing by unsafeLazy { intent.getBooleanExtra(EXTRA_EDITING, false) }
  private val oldFileName by unsafeLazy { intent.getStringExtra(EXTRA_FILE_NAME) }
  private var oldCategory: String? = null
  private var copy: Boolean = false
  private var encryptionIntent: Intent = Intent()

  private val userInteractionRequiredResult =
    registerForActivityResult(StartIntentSenderForResult()) { result ->
      if (result.data == null) {
        setResult(RESULT_CANCELED, null)
        finish()
        return@registerForActivityResult
      }

      when (result.resultCode) {
        RESULT_OK -> encrypt(result.data)
        RESULT_CANCELED -> {
          setResult(RESULT_CANCELED, result.data)
          finish()
        }
      }
    }

  private val otpImportAction =
    registerForActivityResult(StartActivityForResult()) { result ->
      if (result.resultCode == RESULT_OK) {
        binding.otpImportButton.isVisible = false
        val intentResult = IntentIntegrator.parseActivityResult(RESULT_OK, result.data)
        val contents = "${intentResult.contents}\n"
        val currentExtras = binding.extraContent.text.toString()
        if (currentExtras.isNotEmpty() && currentExtras.last() != '\n')
          binding.extraContent.append("\n$contents")
        else binding.extraContent.append(contents)
        snackbar(message = getString(R.string.otp_import_success))
      } else {
        snackbar(message = getString(R.string.otp_import_failure))
      }
    }

  private val gpgKeySelectAction =
    registerForActivityResult(StartActivityForResult()) { result ->
      if (result.resultCode == RESULT_OK) {
        result.data?.getStringArrayExtra(OpenPgpApi.EXTRA_KEY_IDS)?.let { keyIds ->
          lifecycleScope.launch {
            val gpgIdentifierFile = File(PasswordRepository.getRepositoryDirectory(), ".gpg-id")
            withContext(Dispatchers.IO) {
              gpgIdentifierFile.writeText((keyIds + "").joinToString("\n"))
            }
            commitChange(
              getString(
                R.string.git_commit_gpg_id,
                getLongName(
                  gpgIdentifierFile.parentFile!!.absolutePath,
                  repoPath,
                  gpgIdentifierFile.name
                )
              )
            )
              .onSuccess { encrypt(encryptionIntent) }
          }
        }
      } else {
        snackbar(
          message = getString(R.string.gpg_key_select_mandatory),
          length = Snackbar.LENGTH_LONG
        )
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    bindToOpenKeychain(this)
    title =
      if (editing) getString(R.string.edit_password) else getString(R.string.new_password_title)
    with(binding) {
      setContentView(root)
      generatePassword.setOnClickListener { generatePassword() }
      otpImportButton.setOnClickListener {
        supportFragmentManager.setFragmentResultListener(
          OTP_RESULT_REQUEST_KEY,
          this@PasswordCreationActivity
        ) { requestKey, bundle ->
          if (requestKey == OTP_RESULT_REQUEST_KEY) {
            val contents = bundle.getString(RESULT)
            val currentExtras = binding.extraContent.text.toString()
            if (currentExtras.isNotEmpty() && currentExtras.last() != '\n')
              binding.extraContent.append("\n$contents")
            else binding.extraContent.append(contents)
          }
        }
        val hasCamera = packageManager?.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) == true
        if (hasCamera) {
          val items =
            arrayOf(
              getString(R.string.otp_import_qr_code),
              getString(R.string.otp_import_manual_entry)
            )
          MaterialAlertDialogBuilder(this@PasswordCreationActivity)
            .setItems(items) { _, index ->
              when (index) {
                0 ->
                  otpImportAction.launch(
                    IntentIntegrator(this@PasswordCreationActivity)
                      .setOrientationLocked(false)
                      .setBeepEnabled(false)
                      .setDesiredBarcodeFormats(QR_CODE)
                      .createScanIntent()
                  )
                1 -> OtpImportDialogFragment().show(supportFragmentManager, "OtpImport")
              }
            }
            .show()
        } else {
          OtpImportDialogFragment().show(supportFragmentManager, "OtpImport")
        }
      }

      directoryInputLayout.apply {
        if (suggestedName != null || suggestedPass != null || shouldGeneratePassword) {
          isEnabled = true
        } else {
          setBackgroundColor(getColor(android.R.color.transparent))
        }
        val path = getRelativePath(fullPath, repoPath)
        // Keep empty path field visible if it is editable.
        if (path.isEmpty() && !isEnabled) visibility = View.GONE
        else {
          directory.setText(path)
          oldCategory = path
        }
      }
      if (suggestedName != null) {
        filename.setText(suggestedName)
      } else {
        filename.requestFocus()
      }
      // Allow the user to quickly switch between storing the username as the filename or
      // in the encrypted extras. This only makes sense if the directory structure is
      // FileBased.
      if (suggestedName == null &&
          AutofillPreferences.directoryStructure(this@PasswordCreationActivity) ==
            DirectoryStructure.FileBased
      ) {
        encryptUsername.apply {
          visibility = View.VISIBLE
          setOnClickListener {
            if (isChecked) {
              // User wants to enable username encryption, so we add it to the
              // encrypted extras as the first line.
              val username = filename.text.toString()
              val extras = "username:$username\n${extraContent.text}"

              filename.text?.clear()
              extraContent.setText(extras)
            } else {
              // User wants to disable username encryption, so we extract the
              // username from the encrypted extras and use it as the filename.
              val entry =
                passwordEntryFactory.create(
                  lifecycleScope,
                  "PASSWORD\n${extraContent.text}".encodeToByteArray()
                )
              val username = entry.username

              // username should not be null here by the logic in
              // updateViewState, but it could still happen due to
              // input lag.
              if (username != null) {
                filename.setText(username)
                extraContent.setText(entry.extraContentWithoutAuthData)
              }
            }
          }
        }
        listOf(filename, extraContent).forEach {
          it.doOnTextChanged { _, _, _, _ -> updateViewState() }
        }
      }
      suggestedPass?.let {
        password.setText(it)
        password.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
      }
      suggestedExtra?.let { extraContent.setText(it) }
      if (shouldGeneratePassword) {
        generatePassword()
        password.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
      }
    }
    updateViewState()
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.pgp_handler_new_password, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      android.R.id.home -> {
        setResult(RESULT_CANCELED)
        onBackPressed()
      }
      R.id.save_password -> {
        copy = false
        encrypt()
      }
      R.id.save_and_copy_password -> {
        copy = true
        encrypt()
      }
      else -> return super.onOptionsItemSelected(item)
    }
    return true
  }

  private fun generatePassword() {
    supportFragmentManager.setFragmentResultListener(PASSWORD_RESULT_REQUEST_KEY, this) {
      requestKey,
      bundle ->
      if (requestKey == PASSWORD_RESULT_REQUEST_KEY) {
        binding.password.setText(bundle.getString(RESULT))
      }
    }
    when (settings.getString(PreferenceKeys.PREF_KEY_PWGEN_TYPE) ?: KEY_PWGEN_TYPE_CLASSIC) {
      KEY_PWGEN_TYPE_CLASSIC ->
        PasswordGeneratorDialogFragment().show(supportFragmentManager, "generator")
      KEY_PWGEN_TYPE_XKPASSWD ->
        XkPasswordGeneratorDialogFragment().show(supportFragmentManager, "xkpwgenerator")
    }
  }

  private fun updateViewState() =
    with(binding) {
      // Use PasswordEntry to parse extras for username
      val entry =
        passwordEntryFactory.create(
          lifecycleScope,
          "PLACEHOLDER\n${extraContent.text}".encodeToByteArray()
        )
      encryptUsername.apply {
        if (visibility != View.VISIBLE) return@apply
        val hasUsernameInFileName = filename.text.toString().isNotBlank()
        val hasUsernameInExtras = !entry.username.isNullOrBlank()
        isEnabled = hasUsernameInFileName xor hasUsernameInExtras
        isChecked = hasUsernameInExtras
      }
      otpImportButton.isVisible = !entry.hasTotp()
    }

  /** Encrypts the password and the extra content */
  private fun encrypt(receivedIntent: Intent? = null) {
    with(binding) {
      val editName = filename.text.toString().trim()
      val editPass = password.text.toString()
      val editExtra = extraContent.text.toString()

      if (editName.isEmpty()) {
        snackbar(message = resources.getString(R.string.file_toast_text))
        return@with
      } else if (editName.contains('/')) {
        snackbar(message = resources.getString(R.string.invalid_filename_text))
        return@with
      }

      if (editPass.isEmpty() && editExtra.isEmpty()) {
        snackbar(message = resources.getString(R.string.empty_toast_text))
        return@with
      }

      if (copy) {
        copyPasswordToClipboard(editPass)
      }

      encryptionIntent = receivedIntent ?: Intent()
      encryptionIntent.action = OpenPgpApi.ACTION_ENCRYPT

      // pass enters the key ID into `.gpg-id`.
      val repoRoot = PasswordRepository.getRepositoryDirectory()
      val gpgIdentifierFile =
        File(repoRoot, directory.text.toString()).findTillRoot(".gpg-id", repoRoot)
          ?: File(repoRoot, ".gpg-id").apply { createNewFile() }
      val gpgIdentifiers =
        gpgIdentifierFile.readLines().filter { it.isNotBlank() }.map { line ->
          GpgIdentifier.fromString(line)
            ?: run {
              // The line being empty means this is most likely an empty `.gpg-id`
              // file
              // we created. Skip the validation so we can make the user add a real
              // ID.
              if (line.isEmpty()) return@run
              if (line.removePrefix("0x").matches("[a-fA-F0-9]{8}".toRegex())) {
                snackbar(message = resources.getString(R.string.short_key_ids_unsupported))
              } else {
                snackbar(message = resources.getString(R.string.invalid_gpg_id))
              }
              return@with
            }
        }
      if (gpgIdentifiers.isEmpty()) {
        gpgKeySelectAction.launch(
          Intent(this@PasswordCreationActivity, GetKeyIdsActivity::class.java)
        )
        return@with
      }
      val keyIds =
        gpgIdentifiers.filterIsInstance<GpgIdentifier.KeyId>().map { it.id }.toLongArray()
      if (keyIds.isNotEmpty()) {
        encryptionIntent.putExtra(OpenPgpApi.EXTRA_KEY_IDS, keyIds)
      }
      val userIds =
        gpgIdentifiers.filterIsInstance<GpgIdentifier.UserId>().map { it.email }.toTypedArray()
      if (userIds.isNotEmpty()) {
        encryptionIntent.putExtra(OpenPgpApi.EXTRA_USER_IDS, userIds)
      }

      encryptionIntent.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, false)

      val content = "$editPass\n$editExtra"
      val inputStream = ByteArrayInputStream(content.toByteArray())
      val outputStream = ByteArrayOutputStream()

      val path =
        when {
          // If we allowed the user to edit the relative path, we have to consider it here
          // instead
          // of fullPath.
          directoryInputLayout.isEnabled -> {
            val editRelativePath = directory.text.toString().trim()
            if (editRelativePath.isEmpty()) {
              snackbar(message = resources.getString(R.string.path_toast_text))
              return
            }
            val passwordDirectory = File("$repoPath/${editRelativePath.trim('/')}")
            if (!passwordDirectory.exists() && !passwordDirectory.mkdir()) {
              snackbar(message = "Failed to create directory ${editRelativePath.trim('/')}")
              return
            }

            "${passwordDirectory.path}/$editName.gpg"
          }
          else -> "$fullPath/$editName.gpg"
        }

      lifecycleScope.launch(Dispatchers.Main) {
        val result =
          withContext(Dispatchers.IO) {
            checkNotNull(api).executeApi(encryptionIntent, inputStream, outputStream)
          }
        when (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
          OpenPgpApi.RESULT_CODE_SUCCESS -> {
            runCatching {
              val file = File(path)
              // If we're not editing, this file should not already exist!
              // Additionally, if we were editing and the incoming and outgoing
              // filenames differ, it means we renamed. Ensure that the target
              // doesn't already exist to prevent an accidental overwrite.
              if ((!editing || (editing && suggestedName != file.nameWithoutExtension)) &&
                  file.exists()
              ) {
                snackbar(message = getString(R.string.password_creation_duplicate_error))
                return@runCatching
              }

              if (!file.isInsideRepository()) {
                snackbar(message = getString(R.string.message_error_destination_outside_repo))
                return@runCatching
              }

              withContext(Dispatchers.IO) {
                file.outputStream().use { it.write(outputStream.toByteArray()) }
              }

              // associate the new password name with the last name's timestamp in
              // history
              val preference = getSharedPreferences("recent_password_history", Context.MODE_PRIVATE)
              val oldFilePathHash = "$repoPath/${oldCategory?.trim('/')}/$oldFileName.gpg".base64()
              val timestamp = preference.getString(oldFilePathHash)
              if (timestamp != null) {
                preference.edit {
                  remove(oldFilePathHash)
                  putString(file.absolutePath.base64(), timestamp)
                }
              }

              val returnIntent = Intent()
              returnIntent.putExtra(RETURN_EXTRA_CREATED_FILE, path)
              returnIntent.putExtra(RETURN_EXTRA_NAME, editName)
              returnIntent.putExtra(
                RETURN_EXTRA_LONG_NAME,
                getLongName(fullPath, repoPath, editName)
              )

              if (shouldGeneratePassword) {
                val directoryStructure = AutofillPreferences.directoryStructure(applicationContext)
                val entry = passwordEntryFactory.create(lifecycleScope, content.encodeToByteArray())
                returnIntent.putExtra(RETURN_EXTRA_PASSWORD, entry.password)
                val username = entry.username ?: directoryStructure.getUsernameFor(file)
                returnIntent.putExtra(RETURN_EXTRA_USERNAME, username)
              }

              if (directoryInputLayout.isVisible &&
                  directoryInputLayout.isEnabled &&
                  oldFileName != null
              ) {
                val oldFile = File("$repoPath/${oldCategory?.trim('/')}/$oldFileName.gpg")
                if (oldFile.path != file.path && !oldFile.delete()) {
                  setResult(RESULT_CANCELED)
                  MaterialAlertDialogBuilder(this@PasswordCreationActivity)
                    .setTitle(R.string.password_creation_file_fail_title)
                    .setMessage(
                      getString(R.string.password_creation_file_delete_fail_message, oldFileName)
                    )
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
                    .show()
                  return@runCatching
                }
              }

              val commitMessageRes =
                if (editing) R.string.git_commit_edit_text else R.string.git_commit_add_text
              lifecycleScope.launch {
                commitChange(
                  resources.getString(commitMessageRes, getLongName(fullPath, repoPath, editName))
                )
                  .onSuccess {
                    setResult(RESULT_OK, returnIntent)
                    finish()
                  }
              }
            }
              .onFailure { e ->
                if (e is IOException) {
                  e(e) { "Failed to write password file" }
                  setResult(RESULT_CANCELED)
                  MaterialAlertDialogBuilder(this@PasswordCreationActivity)
                    .setTitle(getString(R.string.password_creation_file_fail_title))
                    .setMessage(getString(R.string.password_creation_file_write_fail_message))
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
                    .show()
                } else {
                  e(e)
                }
              }
          }
          OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED -> {
            val sender = getUserInteractionRequestIntent(result)
            userInteractionRequiredResult.launch(IntentSenderRequest.Builder(sender).build())
          }
          OpenPgpApi.RESULT_CODE_ERROR -> handleError(result)
        }
      }
    }
  }

  companion object {

    private const val KEY_PWGEN_TYPE_CLASSIC = "classic"
    private const val KEY_PWGEN_TYPE_XKPASSWD = "xkpasswd"
    const val PASSWORD_RESULT_REQUEST_KEY = "PASSWORD_GENERATOR"
    const val OTP_RESULT_REQUEST_KEY = "OTP_IMPORT"
    const val RESULT = "RESULT"
    const val RETURN_EXTRA_CREATED_FILE = "CREATED_FILE"
    const val RETURN_EXTRA_NAME = "NAME"
    const val RETURN_EXTRA_LONG_NAME = "LONG_NAME"
    const val RETURN_EXTRA_USERNAME = "USERNAME"
    const val RETURN_EXTRA_PASSWORD = "PASSWORD"
    const val EXTRA_FILE_NAME = "FILENAME"
    const val EXTRA_PASSWORD = "PASSWORD"
    const val EXTRA_EXTRA_CONTENT = "EXTRA_CONTENT"
    const val EXTRA_GENERATE_PASSWORD = "GENERATE_PASSWORD"
    const val EXTRA_EDITING = "EDITING"
  }
}
