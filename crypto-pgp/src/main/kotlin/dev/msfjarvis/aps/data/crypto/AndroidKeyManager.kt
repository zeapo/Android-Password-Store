package dev.msfjarvis.aps.data.crypto

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import com.proton.Gopenpgp.crypto.Crypto
import com.proton.Gopenpgp.crypto.Key
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

public class AndroidKeyManager(
  filesDirPath: String,
  private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : KeyManager {

  private val keyDir = File(filesDirPath, KeyDir)

  override suspend fun addKey(stringKey: String): Result<String, Throwable> =
    withContext(dispatcher) {
      return@withContext addKey(Crypto.newKeyFromArmored(stringKey))
    }

  override suspend fun addKey(key: Key): Result<String, Throwable> =
    withContext(dispatcher) {
      if (!keyDirExists())
        return@withContext Err(IllegalStateException("Key directory does not exist"))

      return@withContext runCatching {
        val keyFile = File(keyDir, "${key.hexKeyID}.key")
        if (keyFile.exists()) keyFile.delete()

        keyFile.writeText(key.armor())

        key.hexKeyID
      }
    }

  override suspend fun removeKey(key: Key): Result<String, Throwable> =
    withContext(dispatcher) {
      if (!keyDirExists())
        return@withContext Err(IllegalStateException("Key directory does not exist"))

      return@withContext runCatching {
        val hexID = key.hexKeyID
        val keyFile = File(keyDir, "${key.hexKeyID}.key")
        if (!keyFile.exists()) {
          error("Key does not exist")
        }
        if (!keyFile.delete()) {
          error("Error deleting key file")
        }

        hexID
      }
    }

  override suspend fun findKeyById(id: String): Result<Key, Throwable> =
    withContext(dispatcher) {
      if (!keyDirExists())
        return@withContext Err(IllegalStateException("Key directory does not exist"))

      return@withContext runCatching {
        keyDir.listFiles()?.forEach { file ->
          if (file.isFile && file.nameWithoutExtension == id) {
            val fileContent = file.readText()
            return@runCatching Key(fileContent)
          }
        }

        error("Key with id: $id not found in directory")
      }
    }

  override suspend fun listKeys(): Result<List<Key>, Throwable> =
    withContext(dispatcher) {
      if (!keyDirExists())
        return@withContext Err(IllegalStateException("Key directory does not exist"))

      return@withContext runCatching {
        val keyList = arrayListOf<Key>()

        keyDir.listFiles()?.forEach { file ->
          if (file.isFile && file.extension == "key") {
            val fileContent = file.readText()
            keyList.add(Key(fileContent))
          }
        }

        keyList
      }
    }

  override suspend fun listKeyIds(): Result<List<String>, Throwable> =
    withContext(dispatcher) {
      if (!keyDirExists())
        return@withContext Err(IllegalStateException("Key directory does not exist"))

      return@withContext runCatching {
        val keyIdList = arrayListOf<String>()

        keyDir.listFiles()?.forEach { file ->
          if (file.isFile && file.extension == "key") {
            val fileContent = file.readText()
            keyIdList.add(Key(fileContent).hexKeyID)
          }
        }

        keyIdList
      }
    }

  private fun keyDirExists(): Boolean {
    return keyDir.exists() || keyDir.mkdir()
  }

  private companion object {

    private const val KeyDir = "gpg-keys"
  }
}
