package dev.msfjarvis.aps.ui.onboarding.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.msfjarvis.aps.data.crypto.GPGKeyManager
import dev.msfjarvis.aps.data.repo.PasswordRepository
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class KeySelectionViewModel @Inject constructor(private val keyManager: GPGKeyManager) :
  ViewModel() {

  private val _importKeyStatus = MutableSharedFlow<Result<Unit, Throwable>>()
  val importKeyStatus = _importKeyStatus.asSharedFlow()
  fun importKey(keyInputStream: InputStream) {
    viewModelScope.launch {
      val lines = keyInputStream.bufferedReader().readLines()
      // Validate the incoming InputStream
      if (!validateKey(lines)) {
        _importKeyStatus.emit(
          Err<Throwable>(
            IllegalStateException("Selected file does not appear to be an GPG private key.")
          )
        )
        return@launch
      }
      // Join InputStream and add key to KeyManager
      val fileContent = lines.joinToString("\n")
      keyManager.addKey(fileContent).onFailure { throwable ->
        _importKeyStatus.emit(Err(throwable))
        return@launch
      }
      // Create `.gpg-id` file, if it does not exist
      createGpgIdFile().onFailure { throwable ->
        _importKeyStatus.emit(Err(throwable))
        return@launch
      }

      _importKeyStatus.emit(Ok(Unit))
    }
  }

  private fun validateKey(lines: List<String>): Boolean {
    // The file must have more than 2 lines, and the first and last line must have private key
    // markers.
    return (lines.size > 2 ||
      Regex("BEGIN .* PRIVATE KEY").containsMatchIn(lines.first()) &&
        Regex("END .* PRIVATE KEY").containsMatchIn(lines.last()))
  }

  private suspend fun createGpgIdFile(): Result<Unit, Throwable> =
    withContext(Dispatchers.IO) {
      return@withContext keyManager.listKeyIds().map { keys ->
        val idFile = File(PasswordRepository.getRepositoryDirectory(), ".gpg-id")
        if (idFile.exists()) return@map

        idFile.createNewFile()
        idFile.writeText((keys + "").joinToString("\n"))
      }
    }
}
