package com.ojhdtapp.parabox.core.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object DataStoreKeys{
    val SEND_MESSAGE_ID = longPreferencesKey("send_message_id")
    val USER_NAME = stringPreferencesKey("user_name")
    val USER_AVATAR = stringPreferencesKey("user_avatar")

    val SETTINGS_DEFAULT_BACKUP_SERVICE = intPreferencesKey("settings_default_backup_service")
    val SETTINGS_AUTO_BACKUP = booleanPreferencesKey("settings_auto_backup")
    val SETTINGS_AUTO_BACKUP_FILE_MAX_SIZE = floatPreferencesKey("settings_auto_backup_file_max_size")
    val SETTINGS_AUTO_DELETE_LOCAL_FILE = booleanPreferencesKey("settings_auto_delete_local_file")
    val SETTINGS_ENABLE_FCM = booleanPreferencesKey("settings_enable_fcm")
    val SETTINGS_ENABLE_FCM_CUSTOM_URL = booleanPreferencesKey("settings_enable_fcm_custom_url")
    val SETTINGS_FCM_OFFICIAL_URL = stringPreferencesKey("settings_fcm_official_url")
    val SETTINGS_FCM_URL = stringPreferencesKey("settings_fcm_url")
    val SETTINGS_FCM_HTTPS = booleanPreferencesKey("settings_fcm_https")
    val SETTINGS_FCM_ROLE = intPreferencesKey("settings_fcm_role")
    val SETTINGS_FCM_CLOUD_STORAGE = intPreferencesKey("settings_fcm_cloud_storage")
    val SETTINGS_ENABLE_DYNAMIC_COLOR = booleanPreferencesKey("settings_enable_dynamic_color")
    val SETTINGS_THEME = intPreferencesKey("settings_theme")
    val SETTINGS_ML_KIT_ENTITY_EXTRACTION = booleanPreferencesKey("settings_ml_kit_entity_extraction")
    val SETTINGS_ALLOW_BUBBLE_HOME = booleanPreferencesKey("settings_allow_bubble_home")
    val SETTINGS_ALLOW_FOREGROUND_NOTIFICATION = booleanPreferencesKey("settings_allow_foreground_notification")
    val GOOGLE_MAIL = stringPreferencesKey("google_mail")
    val GOOGLE_NAME = stringPreferencesKey("google_name")
    val GOOGLE_LOGIN = booleanPreferencesKey("google_login")
    val GOOGLE_AVATAR = stringPreferencesKey("google_avatar")
    val GOOGLE_WORK_FOLDER_ID = stringPreferencesKey("google_work_folder_id")
    val GOOGLE_TOTAL_SPACE = longPreferencesKey("google_total_space")
    val GOOGLE_USED_SPACE = longPreferencesKey("google_used_space")
    val GOOGLE_APP_USED_SPACE = longPreferencesKey("google_app_used_space")

    val REQUEST_NOTIFICATION_PERMISSION_FIRST_TIME = booleanPreferencesKey("show_notification_first_time")

    val FCM_TOKEN = stringPreferencesKey("fcm_token")
    val FCM_TARGET_TOKENS = stringSetPreferencesKey("fcm_target_tokens")
    val FCM_LOOPBACK_TOKEN = stringPreferencesKey("fcm_loopback_token")

    const val DEFAULT_USER_NAME = "Me"
}