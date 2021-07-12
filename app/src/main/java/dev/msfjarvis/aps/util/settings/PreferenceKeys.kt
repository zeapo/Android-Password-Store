/*
 * Copyright © 2014-2021 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package dev.msfjarvis.aps.util.settings

object PreferenceKeys {

  const val APP_THEME = "app_theme"
  const val APP_VERSION = "app_version"
  const val AUTOFILL_ENABLE = "autofill_enable"
  const val BIOMETRIC_AUTH = "biometric_auth"
  @Deprecated(
    message = "Use CLEAR_CLIPBOARD_HISTORY instead",
    replaceWith = ReplaceWith("PreferenceKeys.CLEAR_CLIPBOARD_HISTORY"),
  )
  const val CLEAR_CLIPBOARD_20X = "clear_clipboard_20x"
  const val CLEAR_CLIPBOARD_HISTORY = "clear_clipboard_history"
  const val CLEAR_SAVED_PASS = "clear_saved_pass"
  const val COPY_ON_DECRYPT = "copy_on_decrypt"
  const val ENABLE_DEBUG_LOGGING = "enable_debug_logging"
  const val EXPORT_PASSWORDS = "export_passwords"
  const val FILTER_RECURSIVELY = "filter_recursively"
  const val GENERAL_SHOW_TIME = "general_show_time"
  const val GIT_CONFIG = "git_config"
  const val GIT_CONFIG_AUTHOR_EMAIL = "git_config_user_email"
  const val GIT_CONFIG_AUTHOR_NAME = "git_config_user_name"
  const val GIT_EXTERNAL = "git_external"
  const val GIT_EXTERNAL_REPO = "git_external_repo"
  const val GIT_REMOTE_AUTH = "git_remote_auth"
  const val GIT_REMOTE_KEY_TYPE = "git_remote_key_type"

  @Deprecated("Use GIT_REMOTE_URL instead") const val GIT_REMOTE_LOCATION = "git_remote_location"
  const val GIT_REMOTE_USE_MULTIPLEXING = "git_remote_use_multiplexing"

  @Deprecated("Use GIT_REMOTE_URL instead") const val GIT_REMOTE_PORT = "git_remote_port"

  @Deprecated("Use GIT_REMOTE_URL instead") const val GIT_REMOTE_PROTOCOL = "git_remote_protocol"
  const val GIT_DELETE_REPO = "git_delete_repo"

  @Deprecated("Use GIT_REMOTE_URL instead") const val GIT_REMOTE_SERVER = "git_remote_server"
  const val GIT_REMOTE_URL = "git_remote_url"

  @Deprecated("Use GIT_REMOTE_URL instead") const val GIT_REMOTE_USERNAME = "git_remote_username"
  const val GIT_SERVER_INFO = "git_server_info"
  const val GIT_BRANCH_NAME = "git_branch"
  const val HTTPS_PASSWORD = "https_password"
  const val LENGTH = "length"
  const val OREO_AUTOFILL_CUSTOM_PUBLIC_SUFFIXES = "oreo_autofill_custom_public_suffixes"
  const val OREO_AUTOFILL_DEFAULT_USERNAME = "oreo_autofill_default_username"
  const val OREO_AUTOFILL_DIRECTORY_STRUCTURE = "oreo_autofill_directory_structure"
  const val PREF_KEY_CUSTOM_DICT = "pref_key_custom_dict"
  const val PREF_KEY_IS_CUSTOM_DICT = "pref_key_is_custom_dict"
  const val PREF_KEY_PWGEN_TYPE = "pref_key_pwgen_type"
  const val PREF_SELECT_EXTERNAL = "pref_select_external"
  const val REPOSITORY_INITIALIZED = "repository_initialized"
  const val REPO_CHANGED = "repo_changed"
  const val SEARCH_ON_START = "search_on_start"

  @Deprecated(
    message = "Use SHOW_HIDDEN_CONTENTS instead",
    replaceWith = ReplaceWith("PreferenceKeys.SHOW_HIDDEN_CONTENTS")
  )
  const val SHOW_HIDDEN_FOLDERS = "show_hidden_folders"
  const val SHOW_HIDDEN_CONTENTS = "show_hidden_contents"
  const val SORT_ORDER = "sort_order"
  const val SHOW_PASSWORD = "show_password"
  const val SSH_KEY = "ssh_key"
  const val SSH_KEYGEN = "ssh_keygen"
  const val SSH_KEY_LOCAL_PASSPHRASE = "ssh_key_local_passphrase"
  const val SSH_OPENKEYSTORE_CLEAR_KEY_ID = "ssh_openkeystore_clear_keyid"
  const val SSH_OPENKEYSTORE_KEYID = "ssh_openkeystore_keyid"
  const val SSH_SEE_KEY = "ssh_see_key"

  @Deprecated("To be used only in Migrations.kt") const val USE_GENERATED_KEY = "use_generated_key"

  const val PROXY_SETTINGS = "proxy_settings"
  const val PROXY_HOST = "proxy_host"
  const val PROXY_PORT = "proxy_port"
  const val PROXY_USERNAME = "proxy_username"
  const val PROXY_PASSWORD = "proxy_password"

  const val REBASE_ON_PULL = "rebase_on_pull"
  const val IMPORT_GPG_KEY = "import_gpg_key"
}
