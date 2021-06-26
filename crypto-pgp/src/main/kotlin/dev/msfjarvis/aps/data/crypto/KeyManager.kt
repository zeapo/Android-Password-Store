package dev.msfjarvis.aps.data.crypto

import com.github.michaelbull.result.Result
import com.proton.Gopenpgp.crypto.Key

public interface KeyManager {

  public suspend fun addKey(stringKey: String): Result<String, Throwable>
  public suspend fun addKey(key: Key): Result<String, Throwable>
  public suspend fun removeKey(key: Key): Result<String, Throwable>
  public suspend fun findKeyById(id: String): Result<Key, Throwable>
  public suspend fun listKeys(): Result<List<Key>, Throwable>
  public suspend fun listKeyIds(): Result<List<String>, Throwable>
}
