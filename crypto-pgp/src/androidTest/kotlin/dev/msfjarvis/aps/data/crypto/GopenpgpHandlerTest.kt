/*
 * Copyright © 2014-2021 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package dev.msfjarvis.aps.data.crypto

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.proton.Gopenpgp.crypto.Crypto
import com.proton.Gopenpgp.helper.Helper
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dev.msfjarvis.aps.cryptopgp.test.R
import dev.msfjarvis.aps.test.util.CryptoConstants
import javax.inject.Inject
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
public class GopenpgpHandlerTest {
  @get:Rule public val hiltRule: HiltAndroidRule = HiltAndroidRule(this)

  @Inject public lateinit var keyFactory: GPGKeyPair.Factory

  private fun getKeyManager(): GPGKeyManager {
    return GPGKeyManager(
      ApplicationProvider.getApplicationContext<Context>().filesDir.absolutePath,
      TestCoroutineDispatcher(),
      keyFactory,
    )
  }

  @Before
  public fun init() {
    hiltRule.inject()
  }

  @Test
  public fun testManualDecrypt() {
    val pass = CryptoConstants.KEY_PASSPHRASE.encodeToByteArray()
    val plainData = CryptoConstants.PLAIN_TEXT.encodeToByteArray()
    val armored =
      ApplicationProvider.getApplicationContext<Context>()
        .resources
        .openRawResource(R.raw.private_key)
        .readBytes()
        .decodeToString()
    val key = Crypto.newKeyFromArmored(armored)
    val keyring = Crypto.newKeyRing(key.unlock(pass))
    assertTrue(key.canEncrypt())
    assertEquals(1, keyring.countEntities())
    assertEquals(CryptoConstants.KEY_ID, key.hexKeyID)
    val ident = keyring.identities.getAt(0)
    assertEquals(CryptoConstants.KEY_EMAIL, ident.email)
    assertEquals(CryptoConstants.KEY_NAME, ident.name)
    val encrypted = Helper.encryptBinaryMessage(key.armoredPublicKey, plainData)
    val decrypted = Helper.decryptBinaryMessage(key.armor(), pass, encrypted)
    assertContentEquals(decrypted, plainData)
  }
}
