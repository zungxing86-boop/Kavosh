package com.example.data.backup

import android.content.Context
import android.net.Uri
import android.os.StatFs
import androidx.room.withTransaction
import com.example.data.database.JourneyDatabase
import com.example.data.entity.*
import com.example.data.storage.InternalFileStorage
import com.example.data.storage.SettingsPreferencesStore
import com.example.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class BackupSummary(
    val formatVersion: Int,
    val createdAt: Long,
    val heroName: String,
    val missionsCompleted: Int,
    val projectsCount: Int,
    val journalCount: Int,
    val attachmentsCount: Int,
    val includesJournal: Boolean,
    val isEncrypted: Boolean
)

class BackupManager(
    private val context: Context,
    private val db: JourneyDatabase,
    private val settingsStore: SettingsPreferencesStore,
    private val fileStorage: InternalFileStorage
) {
    companion object {
        const val FORMAT_VERSION = 1
        private const val PBKDF2_ITERATIONS = 16000
        private const val KEY_BITS = 256
        private const val MIN_REQUIRED_BYTES = 2L * 1024L * 1024L // 2 MB headroom check
    }

    fun hasEnoughStorageSpace(): Boolean {
        return runCatching {
            val usable = context.filesDir.usableSpace
            if (usable > 0L) usable >= MIN_REQUIRED_BYTES else {
                val stat = StatFs(context.filesDir.absolutePath)
                stat.availableBytes == 0L || stat.availableBytes >= MIN_REQUIRED_BYTES
            }
        }.getOrDefault(true)
    }

    suspend fun createBackupToUri(
        uri: Uri,
        includeJournal: Boolean,
        password: String?
    ): Result<BackupSummary> = withContext(Dispatchers.IO) {
        runCatching {
            if (!hasEnoughStorageSpace()) {
                throw IllegalStateException("LOW_STORAGE")
            }

            val profile = db.userProfileDao().getProfileOnce()?.toDomain()
            val interests = db.interestDao().observeAll().first().map { it.toDomain() }
            val missionProgress = db.missionProgressDao().observeAll().first().map { it.toDomain() }
            val xpTransactions = db.xpTransactionDao().getAllOnce().map { it.toDomain() }
            val skillProgress = db.skillProgressDao().observeAll().first().map { it.toDomain() }
            val userAchievements = db.userAchievementDao().observeAll().first().map { it.toDomain() }
            val projects = db.projectDao().observeAll().first().map { it.toDomain() }
            val attachments = db.projectAttachmentDao().getAllOnce().map { it.toDomain() }
            val journalEntries = if (includeJournal) {
                db.journalEntryDao().observeAll().first().map { it.toDomain() }
            } else {
                emptyList()
            }
            val parentSettings = db.parentSettingsDao().observe().first()?.toDomain()
            val supportMessages = db.supportMessageDao().observeAll().first().map { it.toDomain() }
            val appSettings = settingsStore.settingsFlow.first()

            val createdAt = System.currentTimeMillis()
            val isEncrypted = !password.isNullOrBlank()
            val summary = BackupSummary(
                formatVersion = FORMAT_VERSION,
                createdAt = createdAt,
                heroName = profile?.displayName ?: "Explorer",
                missionsCompleted = missionProgress.count { it.status == MissionStatus.COMPLETED },
                projectsCount = projects.size,
                journalCount = journalEntries.size,
                attachmentsCount = attachments.size,
                includesJournal = includeJournal,
                isEncrypted = isEncrypted
            )

            // Build inner payload ZIP containing all JSON + binary attachments/avatar
            val innerZipBytes = buildInnerPayloadZip(
                profile = profile,
                interests = interests,
                missionProgress = missionProgress,
                xpTransactions = xpTransactions,
                skillProgress = skillProgress,
                userAchievements = userAchievements,
                projects = projects,
                attachments = attachments,
                journalEntries = journalEntries,
                appSettings = appSettings,
                parentSettings = parentSettings,
                supportMessages = supportMessages
            )

            val manifestJson = JSONObject().apply {
                put("app", "MyJourney")
                put("formatVersion", FORMAT_VERSION)
                put("createdAt", createdAt)
                put("heroName", summary.heroName)
                put("missionsCompleted", summary.missionsCompleted)
                put("projectsCount", summary.projectsCount)
                put("journalCount", summary.journalCount)
                put("attachmentsCount", summary.attachmentsCount)
                put("includesJournal", includeJournal)
                put("encrypted", isEncrypted)
            }.toString(2)

            context.contentResolver.openOutputStream(uri)?.use { out ->
                ZipOutputStream(out).use { zos ->
                    zos.putNextEntry(ZipEntry("manifest.json"))
                    zos.write(manifestJson.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()

                    if (isEncrypted) {
                        val encrypted = encryptBytes(innerZipBytes, password!!)
                        zos.putNextEntry(ZipEntry("payload.enc"))
                        zos.write(encrypted)
                        zos.closeEntry()
                    } else {
                        zos.putNextEntry(ZipEntry("payload.zip"))
                        zos.write(innerZipBytes)
                        zos.closeEntry()
                    }
                }
            } ?: throw IllegalStateException("OPEN_OUTPUT_FAILED")

            summary
        }
    }

    suspend fun inspectBackup(uri: Uri): Result<BackupSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val entries = readOuterEntries(uri)
            val manifestBytes = entries["manifest.json"]
                ?: throw IllegalArgumentException("INVALID_MANIFEST")
            val manifest = JSONObject(String(manifestBytes, Charsets.UTF_8))
            if (manifest.optString("app") != "MyJourney") {
                throw IllegalArgumentException("WRONG_APP")
            }
            val version = manifest.optInt("formatVersion", -1)
            if (version <= 0 || version > FORMAT_VERSION) {
                throw IllegalArgumentException("UNSUPPORTED_VERSION")
            }
            val encrypted = manifest.optBoolean("encrypted", false)
            if (encrypted && !entries.containsKey("payload.enc")) {
                throw IllegalArgumentException("MISSING_PAYLOAD")
            }
            if (!encrypted && !entries.containsKey("payload.zip")) {
                throw IllegalArgumentException("MISSING_PAYLOAD")
            }

            BackupSummary(
                formatVersion = version,
                createdAt = manifest.optLong("createdAt", 0L),
                heroName = manifest.optString("heroName", "Explorer"),
                missionsCompleted = manifest.optInt("missionsCompleted", 0),
                projectsCount = manifest.optInt("projectsCount", 0),
                journalCount = manifest.optInt("journalCount", 0),
                attachmentsCount = manifest.optInt("attachmentsCount", 0),
                includesJournal = manifest.optBoolean("includesJournal", false),
                isEncrypted = encrypted
            )
        }
    }

    suspend fun restoreBackup(uri: Uri, password: String?): Result<BackupSummary> = withContext(Dispatchers.IO) {
        runCatching {
            if (!hasEnoughStorageSpace()) {
                throw IllegalStateException("LOW_STORAGE")
            }
            val summary = inspectBackup(uri).getOrThrow()
            val outer = readOuterEntries(uri)

            val innerZipBytes = if (summary.isEncrypted) {
                if (password.isNullOrBlank()) throw IllegalArgumentException("PASSWORD_REQUIRED")
                val encBytes = outer["payload.enc"] ?: throw IllegalArgumentException("MISSING_PAYLOAD")
                decryptBytes(encBytes, password)
            } else {
                outer["payload.zip"] ?: throw IllegalArgumentException("MISSING_PAYLOAD")
            }

            // Parse & validate ALL inner entries in memory BEFORE modifying DB/files
            val innerMap = mutableMapOf<String, ByteArray>()
            ZipInputStream(ByteArrayInputStream(innerZipBytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        innerMap[entry.name] = zis.readBytes()
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val profile = innerMap["profile.json"]?.let { decodeProfile(JSONObject(String(it))) }
            val interests = innerMap["interests.json"]?.let { decodeInterests(JSONArray(String(it))) } ?: emptyList()
            val missionProgress = innerMap["progress.json"]?.let { decodeMissionProgress(JSONArray(String(it))) } ?: emptyList()
            val xpTransactions = innerMap["xp_transactions.json"]?.let { decodeXpTransactions(JSONArray(String(it))) } ?: emptyList()
            val skillProgress = innerMap["skills.json"]?.let { decodeSkillProgress(JSONArray(String(it))) } ?: emptyList()
            val achievements = innerMap["achievements.json"]?.let { decodeAchievements(JSONArray(String(it))) } ?: emptyList()
            val projects = innerMap["projects.json"]?.let { decodeProjects(JSONArray(String(it))) } ?: emptyList()
            val attachmentsMeta = innerMap["attachments.json"]?.let { decodeAttachments(JSONArray(String(it))) } ?: emptyList()
            val journalEntries = innerMap["journal.json"]?.let { decodeJournal(JSONArray(String(it))) } ?: emptyList()
            val parentSettings = innerMap["parent_settings.json"]?.let { decodeParentSettings(JSONObject(String(it))) }
            val supportMessages = innerMap["support_messages.json"]?.let { decodeSupportMessages(JSONArray(String(it))) } ?: emptyList()
            val appSettings = innerMap["settings.json"]?.let { decodeAppSettings(JSONObject(String(it))) }

            // Restore binary files to a staging folder first, then swap
            fileStorage.clearAllUserFiles()
            var restoredAvatarPath: String? = null
            val avatarEntryKey = innerMap.keys.firstOrNull { it.startsWith("avatar/") }
            if (avatarEntryKey != null) {
                val targetAvatar = File(fileStorage.avatarsDir(), File(avatarEntryKey).name)
                targetAvatar.writeBytes(innerMap.getValue(avatarEntryKey))
                restoredAvatarPath = targetAvatar.absolutePath
            }

            val restoredAttachments = attachmentsMeta.map { att ->
                val zipKey = "attachments/${File(att.internalPath).name}"
                val bytes = innerMap[zipKey]
                val targetPath = if (bytes != null) {
                    val dest = File(fileStorage.attachmentsDir(), File(att.internalPath).name)
                    dest.writeBytes(bytes)
                    dest.absolutePath
                } else {
                    att.internalPath
                }
                att.copy(internalPath = targetPath)
            }

            // Atomic Room replacement
            db.withTransaction {
                db.userProfileDao().clear()
                db.interestDao().clear()
                db.missionProgressDao().clear()
                db.xpTransactionDao().clear()
                db.skillProgressDao().clear()
                db.userAchievementDao().clear()
                db.projectAttachmentDao().clear()
                db.projectDao().clear()
                db.journalEntryDao().clear()
                db.parentSettingsDao().clear()
                db.supportMessageDao().clear()

                profile?.let {
                    db.userProfileDao().upsert(
                        UserProfileEntity.fromDomain(
                            it.copy(customAvatarPath = restoredAvatarPath ?: it.customAvatarPath)
                        )
                    )
                }
                if (interests.isNotEmpty()) {
                    db.interestDao().insertAll(interests.map { InterestEntity.fromDomain(it) })
                }
                if (missionProgress.isNotEmpty()) {
                    db.missionProgressDao().insertAll(missionProgress.map { MissionProgressEntity.fromDomain(it) })
                }
                if (xpTransactions.isNotEmpty()) {
                    db.xpTransactionDao().insertAll(xpTransactions.map { XPTransactionEntity.fromDomain(it) })
                }
                if (skillProgress.isNotEmpty()) {
                    db.skillProgressDao().insertAll(skillProgress.map { SkillProgressEntity.fromDomain(it) })
                }
                if (achievements.isNotEmpty()) {
                    db.userAchievementDao().insertAll(achievements.map { UserAchievementEntity.fromDomain(it) })
                }
                val projectIdMap = mutableMapOf<Long, Long>()
                projects.forEach { p ->
                    val newId = db.projectDao().upsert(ProjectEntity.fromDomain(p.copy(id = 0L)))
                    projectIdMap[p.id] = newId
                }
                restoredAttachments.forEach { att ->
                    val mappedProjectId = projectIdMap[att.projectId] ?: att.projectId
                    db.projectAttachmentDao().insert(
                        ProjectAttachmentEntity.fromDomain(att.copy(id = 0L, projectId = mappedProjectId))
                    )
                }
                journalEntries.forEach { j ->
                    db.journalEntryDao().upsert(JournalEntryEntity.fromDomain(j.copy(id = 0L)))
                }
                parentSettings?.let { db.parentSettingsDao().upsert(ParentSettingsEntity.fromDomain(it)) }
                supportMessages.forEach { m ->
                    db.supportMessageDao().insert(SupportMessageEntity.fromDomain(m.copy(id = 0L)))
                }
            }

            appSettings?.let {
                settingsStore.setLanguage(it.language)
                settingsStore.setThemeMode(it.themeMode)
                settingsStore.setNotificationsEnabled(it.notificationsEnabled)
                settingsStore.setQuietHours(it.quietHoursStartHour, it.quietHoursEndHour)
            }

            summary
        }
    }

    private fun readOuterEntries(uri: Uri): Map<String, ByteArray> {
        val map = mutableMapOf<String, ByteArray>()
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        map[entry.name] = zis.readBytes()
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } ?: throw IllegalStateException("UNABLE_TO_OPEN_BACKUP")
        return map
    }

    private fun buildInnerPayloadZip(
        profile: UserProfile?,
        interests: List<Interest>,
        missionProgress: List<MissionProgress>,
        xpTransactions: List<XPTransaction>,
        skillProgress: List<SkillProgress>,
        userAchievements: List<UserAchievement>,
        projects: List<Project>,
        attachments: List<ProjectAttachment>,
        journalEntries: List<JournalEntry>,
        appSettings: AppSettings,
        parentSettings: ParentSettings?,
        supportMessages: List<SupportMessage>
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            profile?.let { writeZipText(zos, "profile.json", encodeProfile(it).toString()) }
            writeZipText(zos, "interests.json", encodeInterests(interests).toString())
            writeZipText(zos, "progress.json", encodeMissionProgress(missionProgress).toString())
            writeZipText(zos, "xp_transactions.json", encodeXpTransactions(xpTransactions).toString())
            writeZipText(zos, "skills.json", encodeSkillProgress(skillProgress).toString())
            writeZipText(zos, "achievements.json", encodeAchievements(userAchievements).toString())
            writeZipText(zos, "projects.json", encodeProjects(projects).toString())
            writeZipText(zos, "attachments.json", encodeAttachments(attachments).toString())
            writeZipText(zos, "journal.json", encodeJournal(journalEntries).toString())
            writeZipText(zos, "settings.json", encodeAppSettings(appSettings).toString())
            parentSettings?.let { writeZipText(zos, "parent_settings.json", encodeParentSettings(it).toString()) }
            writeZipText(zos, "support_messages.json", encodeSupportMessages(supportMessages).toString())

            profile?.customAvatarPath?.let { path ->
                val f = File(path)
                if (f.exists()) {
                    zos.putNextEntry(ZipEntry("avatar/${f.name}"))
                    zos.write(f.readBytes())
                    zos.closeEntry()
                }
            }
            attachments.forEach { att ->
                val f = File(att.internalPath)
                if (f.exists()) {
                    zos.putNextEntry(ZipEntry("attachments/${f.name}"))
                    zos.write(f.readBytes())
                    zos.closeEntry()
                }
            }
        }
        return baos.toByteArray()
    }

    private fun writeZipText(zos: ZipOutputStream, name: String, content: String) {
        zos.putNextEntry(ZipEntry(name))
        zos.write(content.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
    }

    private fun encryptBytes(plain: ByteArray, password: String): ByteArray {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val key = deriveAesKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val cipherText = cipher.doFinal(plain)
        return ByteArrayOutputStream().apply {
            write("MJENC1".toByteArray(Charsets.US_ASCII))
            write(salt)
            write(iv)
            write(cipherText)
        }.toByteArray()
    }

    private fun decryptBytes(encrypted: ByteArray, password: String): ByteArray {
        val headerLen = 6
        if (encrypted.size < headerLen + 16 + 12 + 16) throw IllegalArgumentException("CORRUPT_ENCRYPTED")
        val header = String(encrypted, 0, headerLen, Charsets.US_ASCII)
        if (header != "MJENC1") throw IllegalArgumentException("CORRUPT_ENCRYPTED")
        val salt = encrypted.copyOfRange(headerLen, headerLen + 16)
        val iv = encrypted.copyOfRange(headerLen + 16, headerLen + 28)
        val cipherText = encrypted.copyOfRange(headerLen + 28, encrypted.size)
        val key = deriveAesKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(cipherText)
    }

    private fun deriveAesKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    // JSON codecs
    private fun encodeProfile(p: UserProfile) = JSONObject().apply {
        put("displayName", p.displayName)
        put("avatarId", p.avatarId)
        put("avatarColorHex", p.avatarColorHex)
        put("avatarAccessory", p.avatarAccessory)
        put("customAvatarPath", p.customAvatarPath ?: "")
        put("duration", p.preferences.duration.name)
        put("activityStyle", p.preferences.activityStyle.name)
        put("social", p.preferences.social.name)
        put("difficulty", p.preferences.difficulty.name)
        put("onboardingCompleted", p.onboardingCompleted)
        put("createdAt", p.createdAt)
    }

    private fun decodeProfile(o: JSONObject) = UserProfile(
        displayName = o.getString("displayName"),
        avatarId = o.getString("avatarId"),
        avatarColorHex = o.getLong("avatarColorHex"),
        avatarAccessory = o.optString("avatarAccessory", "compass"),
        customAvatarPath = o.optString("customAvatarPath", "").ifBlank { null },
        preferences = UserPreferences(
            duration = runCatching { DurationPreference.valueOf(o.getString("duration")) }.getOrDefault(DurationPreference.ANY),
            activityStyle = runCatching { ActivityStylePreference.valueOf(o.getString("activityStyle")) }.getOrDefault(ActivityStylePreference.BALANCED),
            social = runCatching { SocialPreference.valueOf(o.getString("social")) }.getOrDefault(SocialPreference.FLEXIBLE),
            difficulty = runCatching { DifficultyLevel.valueOf(o.getString("difficulty")) }.getOrDefault(DifficultyLevel.EASY)
        ),
        onboardingCompleted = o.optBoolean("onboardingCompleted", true),
        createdAt = o.optLong("createdAt", System.currentTimeMillis())
    )

    private fun encodeInterests(items: List<Interest>) = JSONArray().apply {
        items.forEach { i ->
            put(JSONObject().apply {
                put("id", i.id)
                put("nameFa", i.nameFa)
                put("nameEn", i.nameEn)
                put("iconKey", i.iconKey)
                put("isSelected", i.isSelected)
            })
        }
    }

    private fun decodeInterests(arr: JSONArray) = (0 until arr.length()).map { idx ->
        val o = arr.getJSONObject(idx)
        Interest(o.getString("id"), o.getString("nameFa"), o.getString("nameEn"), o.getString("iconKey"), o.getBoolean("isSelected"))
    }

    private fun encodeMissionProgress(items: List<MissionProgress>) = JSONArray().apply {
        items.forEach { p ->
            put(JSONObject().apply {
                put("missionId", p.missionId)
                put("status", p.status.key)
                put("completedSteps", JSONArray(p.completedSteps.toList()))
                put("isShortVersion", p.isShortVersion)
                put("resultNote", p.resultNote)
                put("reflectionAnswers", JSONArray(p.reflectionAnswers))
                put("feedback", p.feedback?.key ?: "")
                put("skipReason", p.skipReason?.key ?: "")
                put("skipNote", p.skipNote)
                put("startedAt", p.startedAt ?: -1L)
                put("completedAt", p.completedAt ?: -1L)
                put("updatedAt", p.updatedAt)
            })
        }
    }

    private fun decodeMissionProgress(arr: JSONArray) = (0 until arr.length()).map { idx ->
        val o = arr.getJSONObject(idx)
        val stepsArr = o.getJSONArray("completedSteps")
        val ansArr = o.getJSONArray("reflectionAnswers")
        val started = o.optLong("startedAt", -1L)
        val completed = o.optLong("completedAt", -1L)
        MissionProgress(
            missionId = o.getString("missionId"),
            status = MissionStatus.fromKey(o.getString("status")),
            completedSteps = (0 until stepsArr.length()).map { stepsArr.getInt(it) }.toSet(),
            isShortVersion = o.optBoolean("isShortVersion", false),
            resultNote = o.optString("resultNote", ""),
            reflectionAnswers = (0 until ansArr.length()).map { ansArr.getString(it) },
            feedback = MissionFeedback.fromKey(o.optString("feedback", "").ifBlank { null }),
            skipReason = SkipReason.fromKey(o.optString("skipReason", "").ifBlank { null }),
            skipNote = o.optString("skipNote", ""),
            startedAt = if (started > 0) started else null,
            completedAt = if (completed > 0) completed else null,
            updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
        )
    }

    private fun encodeXpTransactions(items: List<XPTransaction>) = JSONArray().apply {
        items.forEach { t ->
            put(JSONObject().apply {
                put("eventId", t.eventId)
                put("missionId", t.missionId)
                put("eventType", t.eventType.key)
                put("points", t.points)
                put("dayEpoch", t.dayEpoch)
                put("createdAt", t.createdAt)
            })
        }
    }

    private fun decodeXpTransactions(arr: JSONArray) = (0 until arr.length()).map { idx ->
        val o = arr.getJSONObject(idx)
        XPTransaction(
            eventId = o.getString("eventId"),
            missionId = o.getString("missionId"),
            eventType = XpEventType.fromKey(o.getString("eventType")) ?: XpEventType.START_ACTIVITY,
            points = o.getInt("points"),
            dayEpoch = o.getLong("dayEpoch"),
            createdAt = o.getLong("createdAt")
        )
    }

    private fun encodeSkillProgress(items: List<SkillProgress>) = JSONArray().apply {
        items.forEach { s ->
            put(JSONObject().apply {
                put("skillId", s.skillId)
                put("experiencePoints", s.experiencePoints)
                put("level", s.level)
                put("practicedMissionsCount", s.practicedMissionsCount)
                put("recordedExperiences", JSONArray(s.recordedExperiences))
                put("updatedAt", s.updatedAt)
            })
        }
    }

    private fun decodeSkillProgress(arr: JSONArray) = (0 until arr.length()).map { idx ->
        val o = arr.getJSONObject(idx)
        val expArr = o.getJSONArray("recordedExperiences")
        SkillProgress(
            skillId = o.getString("skillId"),
            experiencePoints = o.getInt("experiencePoints"),
            level = o.getInt("level"),
            practicedMissionsCount = o.getInt("practicedMissionsCount"),
            recordedExperiences = (0 until expArr.length()).map { expArr.getString(it) },
            updatedAt = o.getLong("updatedAt")
        )
    }

    private fun encodeAchievements(items: List<UserAchievement>) = JSONArray().apply {
        items.forEach { a ->
            put(JSONObject().apply {
                put("achievementId", a.achievementId)
                put("unlockedAt", a.unlockedAt)
            })
        }
    }

    private fun decodeAchievements(arr: JSONArray) = (0 until arr.length()).map { idx ->
        val o = arr.getJSONObject(idx)
        UserAchievement(o.getString("achievementId"), o.getLong("unlockedAt"))
    }

    private fun encodeProjects(items: List<Project>) = JSONArray().apply {
        items.forEach { p ->
            put(JSONObject().apply {
                put("id", p.id)
                put("title", p.title)
                put("description", p.description)
                put("goal", p.goal)
                put("steps", JSONArray().apply {
                    p.steps.forEach { s ->
                        put(JSONObject().apply {
                            put("id", s.id)
                            put("title", s.title)
                            put("isDone", s.isDone)
                        })
                    }
                })
                put("notes", p.notes)
                put("status", p.status.key)
                put("finalOutcome", p.finalOutcome)
                put("sharedWithFamily", p.sharedWithFamily)
                put("startedAt", p.startedAt)
                put("updatedAt", p.updatedAt)
            })
        }
    }

    private fun decodeProjects(arr: JSONArray) = (0 until arr.length()).map { idx ->
        val o = arr.getJSONObject(idx)
        val stArr = o.getJSONArray("steps")
        Project(
            id = o.getLong("id"),
            title = o.getString("title"),
            description = o.getString("description"),
            goal = o.getString("goal"),
            steps = (0 until stArr.length()).map { j ->
                val so = stArr.getJSONObject(j)
                ProjectStep(so.getString("id"), so.getString("title"), so.getBoolean("isDone"))
            },
            notes = o.optString("notes", ""),
            status = ProjectStatus.fromKey(o.getString("status")),
            finalOutcome = o.optString("finalOutcome", ""),
            sharedWithFamily = o.optBoolean("sharedWithFamily", false),
            startedAt = o.optLong("startedAt", System.currentTimeMillis()),
            updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
        )
    }

    private fun encodeAttachments(items: List<ProjectAttachment>) = JSONArray().apply {
        items.forEach { a ->
            put(JSONObject().apply {
                put("id", a.id)
                put("projectId", a.projectId)
                put("fileName", a.fileName)
                put("mimeType", a.mimeType)
                put("internalPath", a.internalPath)
                put("fileSizeBytes", a.fileSizeBytes)
                put("createdAt", a.createdAt)
            })
        }
    }

    private fun decodeAttachments(arr: JSONArray) = (0 until arr.length()).map { idx ->
        val o = arr.getJSONObject(idx)
        ProjectAttachment(
            id = o.getLong("id"),
            projectId = o.getLong("projectId"),
            fileName = o.getString("fileName"),
            mimeType = o.getString("mimeType"),
            internalPath = o.getString("internalPath"),
            fileSizeBytes = o.getLong("fileSizeBytes"),
            createdAt = o.getLong("createdAt")
        )
    }

    private fun encodeJournal(items: List<JournalEntry>) = JSONArray().apply {
        items.forEach { j ->
            put(JSONObject().apply {
                put("id", j.id)
                put("title", j.title)
                put("content", j.content)
                put("category", j.category.key)
                put("createdAt", j.createdAt)
                put("updatedAt", j.updatedAt)
            })
        }
    }

    private fun decodeJournal(arr: JSONArray) = (0 until arr.length()).map { idx ->
        val o = arr.getJSONObject(idx)
        JournalEntry(
            id = o.getLong("id"),
            title = o.getString("title"),
            content = o.getString("content"),
            category = JournalCategory.fromKey(o.getString("category")),
            createdAt = o.getLong("createdAt"),
            updatedAt = o.getLong("updatedAt")
        )
    }

    private fun encodeParentSettings(s: ParentSettings) = JSONObject().apply {
        put("pinHash", s.pinHash)
        put("pinSalt", s.pinSalt)
        put("recoveryQuestion", s.recoveryQuestion)
        put("recoveryAnswerHash", s.recoveryAnswerHash)
        put("recoveryAnswerSalt", s.recoveryAnswerSalt)
        put("isConfigured", s.isConfigured)
    }

    private fun decodeParentSettings(o: JSONObject) = ParentSettings(
        pinHash = o.optString("pinHash", ""),
        pinSalt = o.optString("pinSalt", ""),
        recoveryQuestion = o.optString("recoveryQuestion", ""),
        recoveryAnswerHash = o.optString("recoveryAnswerHash", ""),
        recoveryAnswerSalt = o.optString("recoveryAnswerSalt", ""),
        isConfigured = o.optBoolean("isConfigured", false)
    )

    private fun encodeSupportMessages(items: List<SupportMessage>) = JSONArray().apply {
        items.forEach { m ->
            put(JSONObject().apply {
                put("senderLabel", m.senderLabel)
                put("message", m.message)
                put("createdAt", m.createdAt)
                put("isRead", m.isRead)
            })
        }
    }

    private fun decodeSupportMessages(arr: JSONArray) = (0 until arr.length()).map { idx ->
        val o = arr.getJSONObject(idx)
        SupportMessage(
            senderLabel = o.getString("senderLabel"),
            message = o.getString("message"),
            createdAt = o.getLong("createdAt"),
            isRead = o.optBoolean("isRead", false)
        )
    }

    private fun encodeAppSettings(s: AppSettings) = JSONObject().apply {
        put("language", s.language.code)
        put("themeMode", s.themeMode.key)
        put("notificationsEnabled", s.notificationsEnabled)
        put("quietHoursStartHour", s.quietHoursStartHour)
        put("quietHoursEndHour", s.quietHoursEndHour)
    }

    private fun decodeAppSettings(o: JSONObject) = AppSettings(
        language = AppLanguage.fromCode(o.optString("language", "fa")),
        themeMode = ThemeMode.fromKey(o.optString("themeMode", "system")),
        notificationsEnabled = o.optBoolean("notificationsEnabled", false),
        quietHoursStartHour = o.optInt("quietHoursStartHour", 21),
        quietHoursEndHour = o.optInt("quietHoursEndHour", 8)
    )
}
