package com.example.core.navigation

object Routes {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val ADVENTURE_MAP = "adventure_map"
    const val MISSION_DETAIL = "mission_detail/{missionId}"
    const val PROJECTS = "projects"
    const val PROJECT_DETAIL = "project_detail/{projectId}"
    const val SKILLS = "skills"
    const val ACHIEVEMENTS = "achievements"
    const val JOURNAL = "journal"
    const val COMPANION = "companion"
    const val FAMILY_ROOM = "family_room"
    const val SETTINGS = "settings"

    fun missionDetail(missionId: String) = "mission_detail/$missionId"
    fun projectDetail(projectId: Long) = "project_detail/$projectId"
}
