package dev.msfjarvis.aps.data.crypto

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.expect
import com.github.michaelbull.result.map
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

  override suspend fun getKeyById(id: String): Result<GPGKeyPair, Throwable> =
    withContext(dispatcher) {
      if (!keyDirExists())
        return@withContext Err(IllegalStateException("Key directory does not exist"))

      runCatching {
        val keys = getGopenpgpKeys().expect { "Failed to get PGP keys" }
        return@runCatching keyFactory.create(
          keys.first { key ->
            return@first getKeyIdentities(key).matches(id)
          }
        )
      }
    }

  override suspend fun getAllKeys(): Result<List<GPGKeyPair>, Throwable> =
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

  override suspend fun getAllKeyIds(): Result<List<String>, Throwable> =
    withContext(dispatcher) { getAllKeys().map { keys -> keys.map { it.getKeyId() } } }

  override fun canHandle(fileName: String): Boolean {
    return fileName.split('.').last() == "gpg"
  }

  private suspend fun getGopenpgpKeys(): Result<List<Key>, Throwable> = runCatching {
    withContext(dispatcher) {
      (keyDir.listFiles() ?: emptyArray()).map { file -> Crypto.newKeyFromArmored(file.readText()) }
    }
  }

  private fun getKeyIdentities(key: Key): KeyIdentities {
    val keyId = key.hexKeyID
    val fingerprint = key.fingerprint
    val email = key.userEmail
    return KeyIdentities.create(keyId, fingerprint, email)
  }

  private fun keyDirExists(): Boolean {
    return keyDir.exists() || keyDir.mkdir()
  }

  /** Container class for identifying properties of a GPG key */
  private class KeyIdentities
  private constructor(val keyId: String, val fingerprint: String, val email: String) {
    fun matches(identifier: String): Boolean {
      val ident = identifier.lowercase()
      return ident == keyId || ident == fingerprint || ident == email || ident == "<$email>"
    }

    companion object {
      fun create(
        keyId: String,
        fingerprint: String,
        email: String,
      ): KeyIdentities {
        return KeyIdentities(
          keyId.lowercase(),
          fingerprint.lowercase(),
          email.lowercase(),
        )
      }
    }
  }

  private companion object {

    private const val KeyDir = "gpg-keys"
  }
}
