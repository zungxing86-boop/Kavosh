package com.example.data.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppSettings
import com.example.domain.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "my_journey_settings")

class SettingsPreferencesStore(private val context: Context) {
    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val QUIET_START = intPreferencesKey("quiet_start_hour")
        val QUIET_END = intPreferencesKey("quiet_end_hour")
        val PRIVACY_SHOW_PROJECTS_DEFAULT = booleanPreferencesKey("privacy_show_projects_default")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            AppSettings(
                language = AppLanguage.fromCode(prefs[Keys.LANGUAGE] ?: AppLanguage.PERSIAN.code),
                themeMode = ThemeMode.fromKey(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.key),
                notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: false,
                quietHoursStartHour = prefs[Keys.QUIET_START] ?: 21,
                quietHoursEndHour = prefs[Keys.QUIET_END] ?: 8,
                privacyShowProjectsDefault = prefs[Keys.PRIVACY_SHOW_PROJECTS_DEFAULT] ?: false
            )
        }

    suspend fun setLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = themeMode.key }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setQuietHours(startHour: Int, endHour: Int) {
        context.settingsDataStore.edit {
            it[Keys.QUIET_START] = startHour
            it[Keys.QUIET_END] = endHour
        }
    }

    suspend fun reset() {
        context.settingsDataStore.edit { it.clear() }
    }
}

data class CopiedInternalFile(
    val fileName: String,
    val mimeType: String,
    val internalPath: String,
    val sizeBytes: Long
)

class InternalFileStorage(private val context: Context) {
    fun attachmentsDir(): File = File(context.filesDir, "attachments").apply { mkdirs() }
    fun avatarsDir(): File = File(context.filesDir, "avatars").apply { mkdirs() }

    suspend fun copyUriToAttachments(uri: Uri): Result<CopiedInternalFile> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: "application/octet-stream"
            val originalName = queryDisplayName(uri) ?: "file_${System.currentTimeMillis()}"
            val sanitizedName = originalName.replace(Regex("[^A-Za-z0-9._\\-\\u0600-\\u06FF ]"), "_")
            val targetFile = File(attachmentsDir(), "${UUID.randomUUID()}_$sanitizedName")

            resolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IOException("Unable to open selected file")

            CopiedInternalFile(
                fileName = originalName,
                mimeType = mimeType,
                internalPath = targetFile.absolutePath,
                sizeBytes = targetFile.length()
            )
        }
    }

    suspend fun copyAvatarImage(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val targetFile = File(avatarsDir(), "hero_avatar_${System.currentTimeMillis()}.jpg")
            // Clean older custom avatar files
            avatarsDir().listFiles()?.forEach { old ->
                if (old.name.startsWith("hero_avatar_")) old.delete()
            }
            resolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IOException("Unable to read selected image")
            targetFile.absolutePath
        }
    }

    fun deleteFileIfExists(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching {
            val f = File(path)
            if (f.exists() && (f.absolutePath.startsWith(context.filesDir.absolutePath))) {
                f.delete()
            }
        }
    }

    fun clearAllUserFiles() {
        attachmentsDir().deleteRecursively()
        avatarsDir().deleteRecursively()
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
        }.getOrNull()
    }
}
