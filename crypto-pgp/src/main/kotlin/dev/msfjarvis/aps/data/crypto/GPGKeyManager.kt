package dev.msfjarvis.aps.data.crypto

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import com.proton.Gopenpgp.crypto.Crypto
import com.proton.Gopenpgp.crypto.Key
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

public class GPGKeyManager(
  filesDirPath: String,
  private val dispatcher: CoroutineDispatcher,
  private val keyFactory: GPGKeyPair.Factory,
) : KeyManager<GPGKeyPair> {

  private val keyDir = File(filesDirPath, KeyDir)

  override suspend fun addKey(stringKey: String): Result<String, Throwable> =
    withContext(dispatcher) {
      return@withContext addKey(keyFactory.create(Crypto.newKeyFromArmored(stringKey)))
    }

  override suspend fun addKey(key: GPGKeyPair): Result<String, Throwable> =
    withContext(dispatcher) {
      if (!keyDirExists())
        return@withContext Err(IllegalStateException("Key directory does not exist"))

      return@withContext runCatching {
        val keyFile = File(keyDir, "${key.getKeyId()}.key")
        if (keyFile.exists()) keyFile.delete()

        keyFile.writeText(key.getPrivateKey().decodeToString())

        key.getKeyId()
      }
    }

  override suspend fun removeKey(key: GPGKeyPair): Result<String, Throwable> =
    withContext(dispatcher) {
      if (!keyDirExists())
        return@withContext Err(IllegalStateException("Key directory does not exist"))

      return@withContext runCatching {
        val hexID = key.getKeyId()
        val keyFile = File(keyDir, "$hexID.key")
        if (!keyFile.exists()) {
          error("Key does not exist")
        }
        if (!keyFile.delete()) {
          error("Error deleting key file")
        }

        hexID
      }
    }

  override suspend fun findKeyById(id: String): Result<GPGKeyPair, Throwable> =
    withContext(dispatcher) {
      if (!keyDirExists())
        return@withContext Err(IllegalStateException("Key directory does not exist"))

      return@withContext runCatching {
        keyDir.listFiles()?.forEach { file ->
          if (file.isFile && file.nameWithoutExtension == id) {
            val fileContent = file.readText()
            return@runCatching keyFactory.create(Key(fileContent))
          }
        }

        error("Key with id: $id not found in directory")
      }
    }

  override suspend fun listKeys(): Result<List<GPGKeyPair>, Throwable> =
    withContext(dispatcher) {
      if (!keyDirExists())
        return@withContext Err(IllegalStateException("Key directory does not exist"))

      return@withContext runCatching {
        val keyList = arrayListOf<GPGKeyPair>()

        keyDir.listFiles()?.forEach { file ->
          if (file.isFile && file.extension == "key") {
            val fileContent = file.readText()
            keyList.add(keyFactory.create(Key(fileContent)))
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

  override fun canHandle(fileName: String): Boolean {
    println("Checking in " + javaClass.simpleName)
    return fileName.split('.').last() == "gpg"
  }

  private fun keyDirExists(): Boolean {
    return keyDir.exists() || keyDir.mkdir()
  }

  private companion object {

    private const val KeyDir = "gpg-keys"
  }
}
