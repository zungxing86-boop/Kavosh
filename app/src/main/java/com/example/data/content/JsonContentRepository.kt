package com.example.data.content

import android.content.Context
import com.example.domain.model.*
import com.example.domain.repository.ContentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class JsonContentRepository(private val context: Context) : ContentRepository {
    private var cachedTerritories: List<Territory>? = null
    private var cachedMissions: List<Mission>? = null
    private var cachedSkills: List<Skill>? = null
    private var cachedInterests: List<Interest>? = null
    private var cachedAchievements: List<Achievement>? = null
    private var cachedCompanion: List<CompanionModule>? = null

    override suspend fun getTerritories(): List<Territory> = withContext(Dispatchers.IO) {
        cachedTerritories?.let { return@withContext it }
        val raw = readAsset("territories/territories.json")
        val arr = JSONArray(raw)
        val list = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Territory(
                id = o.getString("id"),
                order = o.getInt("order"),
                nameFa = o.getString("nameFa"),
                nameEn = o.getString("nameEn"),
                subtitleFa = o.getString("subtitleFa"),
                subtitleEn = o.getString("subtitleEn"),
                descriptionFa = o.getString("descriptionFa"),
                descriptionEn = o.getString("descriptionEn"),
                dayStart = o.getInt("dayStart"),
                dayEnd = o.getInt("dayEnd"),
                themeColorHex = parseHexColor(o.getString("themeColorHex")),
                visualKey = o.getString("visualKey")
            )
        }.sortedBy { it.order }
        cachedTerritories = list
        list
    }

    override suspend fun getMissions(): List<Mission> = withContext(Dispatchers.IO) {
        cachedMissions?.let { return@withContext it }
        val raw = readAsset("missions/missions.json")
        val arr = JSONArray(raw)
        val list = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Mission(
                id = o.getString("id"),
                dayNumber = o.getInt("dayNumber"),
                titleFa = o.getString("titleFa"),
                titleEn = o.getString("titleEn"),
                shortDescFa = o.getString("shortDescFa"),
                shortDescEn = o.getString("shortDescEn"),
                storyFa = o.getString("storyFa"),
                storyEn = o.getString("storyEn"),
                goalFa = o.getString("goalFa"),
                goalEn = o.getString("goalEn"),
                durationMinutes = o.getInt("durationMinutes"),
                durationCategory = runCatching {
                    DurationPreference.valueOf(o.getString("durationCategory"))
                }.getOrDefault(DurationPreference.MEDIUM),
                difficulty = runCatching {
                    DifficultyLevel.valueOf(o.getString("difficulty"))
                }.getOrDefault(DifficultyLevel.EASY),
                activityStyle = runCatching {
                    ActivityStylePreference.valueOf(o.getString("activityStyle"))
                }.getOrDefault(ActivityStylePreference.BALANCED),
                socialStyle = runCatching {
                    SocialPreference.valueOf(o.getString("socialStyle"))
                }.getOrDefault(SocialPreference.FLEXIBLE),
                relatedSkills = jsonArrayToList(o.getJSONArray("relatedSkills")),
                relatedInterests = jsonArrayToList(o.getJSONArray("relatedInterests")),
                toolsFa = jsonArrayToList(o.getJSONArray("toolsFa")),
                toolsEn = jsonArrayToList(o.getJSONArray("toolsEn")),
                stepsFa = jsonArrayToList(o.getJSONArray("stepsFa")),
                stepsEn = jsonArrayToList(o.getJSONArray("stepsEn")),
                expectedOutputFa = o.getString("expectedOutputFa"),
                expectedOutputEn = o.getString("expectedOutputEn"),
                shortVersionFa = o.getString("shortVersionFa"),
                shortVersionEn = o.getString("shortVersionEn"),
                reflectionQuestionsFa = jsonArrayToList(o.getJSONArray("reflectionQuestionsFa")),
                reflectionQuestionsEn = jsonArrayToList(o.getJSONArray("reflectionQuestionsEn")),
                startGuideFa = o.getString("startGuideFa"),
                startGuideEn = o.getString("startGuideEn"),
                territoryId = o.getString("territoryId"),
                imageKey = o.getString("imageKey")
            )
        }.sortedBy { it.dayNumber }
        require(list.size == 30) { "Expected 30 validated missions in assets/missions/missions.json" }
        cachedMissions = list
        list
    }

    override suspend fun getMissionById(id: String): Mission? {
        return getMissions().firstOrNull { it.id == id }
    }

    override suspend fun getSkills(): List<Skill> = withContext(Dispatchers.IO) {
        cachedSkills?.let { return@withContext it }
        loadSkillsAndInterests()
        cachedSkills ?: emptyList()
    }

    override suspend fun getDefaultInterests(): List<Interest> = withContext(Dispatchers.IO) {
        cachedInterests?.let { return@withContext it }
        loadSkillsAndInterests()
        cachedInterests ?: emptyList()
    }

    private fun loadSkillsAndInterests() {
        val raw = readAsset("skills/skills.json")
        val root = JSONObject(raw)
        val skillsArr = root.getJSONArray("skills")
        cachedSkills = (0 until skillsArr.length()).map { i ->
            val o = skillsArr.getJSONObject(i)
            Skill(
                id = o.getString("id"),
                nameFa = o.getString("nameFa"),
                nameEn = o.getString("nameEn"),
                descriptionFa = o.getString("descriptionFa"),
                descriptionEn = o.getString("descriptionEn"),
                iconKey = o.getString("iconKey"),
                colorHex = parseHexColor(o.getString("colorHex"))
            )
        }
        val intArr = root.getJSONArray("interests")
        cachedInterests = (0 until intArr.length()).map { i ->
            val o = intArr.getJSONObject(i)
            Interest(
                id = o.getString("id"),
                nameFa = o.getString("nameFa"),
                nameEn = o.getString("nameEn"),
                iconKey = o.getString("iconKey"),
                isSelected = false
            )
        }
    }

    override suspend fun getAchievements(): List<Achievement> = withContext(Dispatchers.IO) {
        cachedAchievements?.let { return@withContext it }
        val raw = readAsset("achievements/achievements.json")
        val arr = JSONArray(raw)
        val list = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Achievement(
                id = o.getString("id"),
                titleFa = o.getString("titleFa"),
                titleEn = o.getString("titleEn"),
                descriptionFa = o.getString("descriptionFa"),
                descriptionEn = o.getString("descriptionEn"),
                criteriaFa = o.getString("criteriaFa"),
                criteriaEn = o.getString("criteriaEn"),
                territoryId = if (o.isNull("territoryId")) null else o.optString("territoryId"),
                iconKey = o.getString("iconKey")
            )
        }
        cachedAchievements = list
        list
    }

    override suspend fun getCompanionModules(): List<CompanionModule> = withContext(Dispatchers.IO) {
        cachedCompanion?.let { return@withContext it }
        val raw = readAsset("companion/companion_modules.json")
        val arr = JSONArray(raw)
        val list = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val stepsArr = o.getJSONArray("steps")
            val steps = (0 until stepsArr.length()).map { j ->
                val s = stepsArr.getJSONObject(j)
                CompanionStep(
                    id = s.getString("id"),
                    titleFa = s.getString("titleFa"),
                    titleEn = s.getString("titleEn"),
                    promptFa = s.getString("promptFa"),
                    promptEn = s.getString("promptEn"),
                    tipFa = s.getString("tipFa"),
                    tipEn = s.getString("tipEn")
                )
            }
            CompanionModule(
                id = o.getString("id"),
                titleFa = o.getString("titleFa"),
                titleEn = o.getString("titleEn"),
                subtitleFa = o.getString("subtitleFa"),
                subtitleEn = o.getString("subtitleEn"),
                iconKey = o.getString("iconKey"),
                steps = steps
            )
        }
        cachedCompanion = list
        list
    }

    private fun readAsset(path: String): String {
        return context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun jsonArrayToList(arr: JSONArray): List<String> {
        return (0 until arr.length()).map { arr.getString(it) }
    }

    private fun parseHexColor(raw: String): Long {
        val clean = raw.removePrefix("0x").removePrefix("0X").removePrefix("#")
        return clean.toLong(16)
    }
}
