package com.example.core.common

import com.example.domain.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun isFa(language: AppLanguage): Boolean = language == AppLanguage.PERSIAN

fun t(language: AppLanguage, fa: String, en: String): String =
    if (language == AppLanguage.PERSIAN) fa else en

fun formatTimestamp(millis: Long, language: AppLanguage): String {
    val locale = if (language == AppLanguage.PERSIAN) Locale("fa", "IR") else Locale.ENGLISH
    val fmt = SimpleDateFormat("yyyy/MM/dd  HH:mm", locale)
    return fmt.format(Date(millis))
}

fun formatFileSize(bytes: Long, language: AppLanguage): String {
    return when {
        bytes < 1024 -> if (isFa(language)) "$bytes بایت" else "$bytes B"
        bytes < 1024 * 1024 -> {
            val kb = bytes / 1024
            if (isFa(language)) "$kb کیلوبایت" else "$kb KB"
        }
        else -> {
            val mb = String.format(Locale.US, "%.1f", bytes / (1024f * 1024f))
            if (isFa(language)) "$mb مگابایت" else "$mb MB"
        }
    }
}

fun MissionStatus.label(language: AppLanguage): String = when (this) {
    MissionStatus.SUGGESTED -> t(language, "پیشنهادشده", "Suggested")
    MissionStatus.SELECTED -> t(language, "انتخاب‌شده", "Selected")
    MissionStatus.IN_PROGRESS -> t(language, "در حال انجام", "In Progress")
    MissionStatus.PAUSED -> t(language, "متوقف‌شده", "Paused")
    MissionStatus.COMPLETED -> t(language, "تکمیل‌شده", "Completed")
    MissionStatus.SAVED_FOR_LATER -> t(language, "ذخیره برای بعد", "Saved for Later")
}

fun DifficultyLevel.label(language: AppLanguage): String = when (this) {
    DifficultyLevel.EASY -> t(language, "ساده", "Easy")
    DifficultyLevel.MEDIUM -> t(language, "متوسط", "Medium")
    DifficultyLevel.CHALLENGING -> t(language, "چالشی", "Challenging")
}

fun MissionFeedback.label(language: AppLanguage): String = when (this) {
    MissionFeedback.LOVED_IT -> t(language, "خیلی دوست داشتم", "Loved it")
    MissionFeedback.INTERESTING -> t(language, "جالب بود", "Interesting")
    MissionFeedback.OKAY -> t(language, "معمولی بود", "It was okay")
    MissionFeedback.NOT_FOR_ME -> t(language, "دوست نداشتم", "Not for me")
}

fun SkipReason.label(language: AppLanguage): String = when (this) {
    SkipReason.NO_TIME -> t(language, "وقت نداشتم", "Didn't have time")
    SkipReason.TOO_HARD -> t(language, "سخت بود", "Felt too hard")
    SkipReason.TIRED -> t(language, "خسته بودم", "Felt tired")
    SkipReason.DIDNT_KNOW_WHERE_TO_START -> t(language, "نمی‌دانستم از کجا شروع کنم", "Didn't know where to start")
    SkipReason.NOT_APPEALING -> t(language, "جذاب نبود", "Wasn't appealing")
    SkipReason.PREFERRED_REST -> t(language, "استراحت را ترجیح دادم", "Preferred to rest")
    SkipReason.OTHER -> t(language, "دلیل دیگر", "Other reason")
}

fun ProjectStatus.label(language: AppLanguage): String = when (this) {
    ProjectStatus.INITIAL_IDEA -> t(language, "ایده اولیه", "Initial Idea")
    ProjectStatus.READY_TO_START -> t(language, "آماده شروع", "Ready to Start")
    ProjectStatus.IN_PROGRESS -> t(language, "در حال انجام", "In Progress")
    ProjectStatus.PAUSED -> t(language, "متوقف‌شده", "Paused")
    ProjectStatus.COMPLETED -> t(language, "تکمیل‌شده", "Completed")
    ProjectStatus.ARCHIVED -> t(language, "بایگانی‌شده", "Archived")
}

fun JournalCategory.label(language: AppLanguage): String = when (this) {
    JournalCategory.THOUGHTS -> t(language, "افکار", "Thoughts")
    JournalCategory.FEELINGS -> t(language, "احساسات", "Feelings")
    JournalCategory.IDEAS -> t(language, "ایده‌ها", "Ideas")
    JournalCategory.EXPERIENCES -> t(language, "تجربه‌ها", "Experiences")
    JournalCategory.PLANS -> t(language, "برنامه‌ها", "Plans")
    JournalCategory.QUESTIONS -> t(language, "پرسش‌ها", "Questions")
    JournalCategory.DAILY_NOTES -> t(language, "یادداشت روزانه", "Daily Notes")
}
