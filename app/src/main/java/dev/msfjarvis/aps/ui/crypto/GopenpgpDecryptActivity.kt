/*
 * Copyright © 2014-2021 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package dev.msfjarvis.aps.ui.crypto

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import com.github.michaelbull.result.unwrap
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import dev.msfjarvis.aps.R
import dev.msfjarvis.aps.data.passfile.PasswordEntry
import dev.msfjarvis.aps.data.password.FieldItem
import dev.msfjarvis.aps.data.repo.PasswordRepository
import dev.msfjarvis.aps.databinding.DecryptLayoutBinding
import dev.msfjarvis.aps.databinding.DialogPassphraseInputBinding
import dev.msfjarvis.aps.injection.crypto.CryptoSet
import dev.msfjarvis.aps.injection.crypto.KeyManagerSet
import dev.msfjarvis.aps.injection.password.PasswordEntryFactory
import dev.msfjarvis.aps.ui.adapters.FieldItemAdapter
import dev.msfjarvis.aps.util.extensions.findTillRoot
import dev.msfjarvis.aps.util.extensions.unsafeLazy
import dev.msfjarvis.aps.util.extensions.viewBinding
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class GopenpgpDecryptActivity : BasePgpActivity() {

  private val binding by viewBinding(DecryptLayoutBinding::inflate)
  @Inject lateinit var passwordEntryFactory: PasswordEntryFactory
  @Inject lateinit var cryptos: CryptoSet
  @Inject lateinit var keyManagers: KeyManagerSet
  private val relativeParentPath by unsafeLazy { getParentPath(fullPath, repoPath) }
  private var passwordEntry: PasswordEntry? = null
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    title = name
    with(binding) {
      setContentView(root)
      passwordCategory.text = relativeParentPath
      passwordFile.text = name
      passwordFile.setOnLongClickListener {
        copyTextToClipboard(name)
        true
      }
    }

    showPassphraseDialog()
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.pgp_handler, menu)
    passwordEntry?.let { entry ->
      if (menu != null) {
        menu.findItem(R.id.edit_password).isVisible = true
        if (!entry.password.isNullOrBlank()) {
          menu.findItem(R.id.share_password_as_plaintext).isVisible = true
          menu.findItem(R.id.copy_password).isVisible = true
        }
      }
    }
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      android.R.id.home -> onBackPressed()
      R.id.edit_password -> editPassword()
      R.id.share_password_as_plaintext -> shareAsPlaintext()
      R.id.copy_password -> copyPasswordToClipboard(passwordEntry?.password)
      else -> return super.onOptionsItemSelected(item)
    }
    return true
  }

  private fun showPassphraseDialog() {
    val view = layoutInflater.inflate(R.layout.dialog_passphrase_input, binding.root, false)
    val dialogBinding = DialogPassphraseInputBinding.bind(view)

    MaterialAlertDialogBuilder(this)
      .setView(view)
      .setPositiveButton("Unlock") { dialog, _ ->
        dialog.dismiss()
        val passphrase = dialogBinding.input.text.toString().toByteArray()
        decrypt(passphrase)
      }
      .setNegativeButton("Cancel") { dialog, _ ->
        dialog.dismiss()
        finish()
      }
      .setOnCancelListener { finish() }
      .show()
  }

  /**
   * Automatically finishes the activity 60 seconds after decryption succeeded to prevent
   * information leaks from stale activities.
   */
  @OptIn(ExperimentalTime::class)
  private fun startAutoDismissTimer() {
    lifecycleScope.launch {
      delay(Duration.seconds(60))
      finish()
    }
  }

  /**
   * Edit the current password and hide all the fields populated by encrypted data so that when the
   * result triggers they can be repopulated with new data.
   */
  private fun editPassword() {
    val intent = Intent(this, PasswordCreationActivity::class.java)
    intent.putExtra("FILE_PATH", relativeParentPath)
    intent.putExtra("REPO_PATH", repoPath)
    intent.putExtra(PasswordCreationActivity.EXTRA_FILE_NAME, name)
    intent.putExtra(PasswordCreationActivity.EXTRA_PASSWORD, passwordEntry?.password)
    intent.putExtra(PasswordCreationActivity.EXTRA_EXTRA_CONTENT, passwordEntry?.extraContentString)
    intent.putExtra(PasswordCreationActivity.EXTRA_EDITING, true)
    startActivity(intent)
    finish()
  }

  private fun shareAsPlaintext() {
    val sendIntent =
      Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, passwordEntry?.password)
        type = "text/plain"
      }
    // Always show a picker to give the user a chance to cancel
    startActivity(
      Intent.createChooser(sendIntent, resources.getText(R.string.send_plaintext_password_to))
    )
  }

  private fun decrypt(passphrase: ByteArray) {
    lifecycleScope.launch {
      // TODO(msfjarvis): native methods are fallible, add error handling once out of testing
      val message = withContext(Dispatchers.IO) { File(fullPath).readBytes() }
      val crypto = cryptos.first { it.canHandle(fullPath) }
      val keyManager = keyManagers.first { it.canHandle(fullPath) }
      val keyIds = getKeyIds(fullPath)
      if (keyIds.isEmpty()) {
        // TODO: Show option to import gpg key here
        return@launch
      }
      val result =
        withContext(Dispatchers.IO) {
          val privateKey =
            keyManager.getKeyById(keyIds[0]).unwrap().getPrivateKey().decodeToString()

          // TODO: this throws an error if passphrase is incorrect
          crypto.decrypt(
            privateKey,
            passphrase,
            message,
          )
        }
      startAutoDismissTimer()
      val entry = passwordEntryFactory.create(lifecycleScope, result)
      passwordEntry = entry
      invalidateOptionsMenu()
      val items = arrayListOf<FieldItem>()
      val adapter = FieldItemAdapter(emptyList(), true) { text -> copyTextToClipboard(text) }
      if (!entry.password.isNullOrBlank()) {
        items.add(FieldItem.createPasswordField(entry.password!!))
      }

      if (entry.hasTotp()) {
        lifecycleScope.launch {
          items.add(FieldItem.createOtpField(entry.totp.value))
          entry.totp.collect { code ->
            withContext(Dispatchers.Main) { adapter.updateOTPCode(code) }
          }
        }
      }

      if (!entry.username.isNullOrBlank()) {
        items.add(FieldItem.createUsernameField(entry.username!!))
      }

      entry.extraContent.forEach { (key, value) ->
        items.add(FieldItem(key, value, FieldItem.ActionType.COPY))
      }

      binding.recyclerView.adapter = adapter
      adapter.updateItems(items)
    }
  }

  private fun getKeyIds(currentFilePath: String): List<String> {
    val repoRoot = PasswordRepository.getRepositoryDirectory()
    val directory = File(currentFilePath).parentFile ?: return emptyList()
    val gpgIdentifierFile = directory.findTillRoot(".gpg-id", repoRoot) ?: return emptyList()

    return gpgIdentifierFile.readText().split("\n")
  }

  companion object {

    // TODO(msfjarvis): source these from storage and user input
    const val PRIV_KEY = ""
    const val PASS = ""
  }
}
