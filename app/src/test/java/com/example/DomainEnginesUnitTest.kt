package com.example

import com.example.domain.engine.*
import com.example.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class DomainEnginesUnitTest {

    @Test
    fun `xp engine enforces idempotency, daily cap of 5, and non-negative totals`() {
        val xpEngine = XpEngine()
        val day = 20000L
        val txs = mutableListOf<XPTransaction>()

        // Start mission 01 -> +2 XP
        val tx1 = xpEngine.computeAwardablePoints(txs, "mission_01", XpEventType.START_ACTIVITY, day)
        assertNotNull(tx1)
        assertEquals(2, tx1!!.points)
        txs.add(tx1)

        // Duplicate start on mission 01 -> null (no duplicate XP)
        val dup = xpEngine.computeAwardablePoints(txs, "mission_01", XpEventType.START_ACTIVITY, day)
        assertNull(dup)

        // Continue steps -> +2 XP (total today = 4)
        val tx2 = xpEngine.computeAwardablePoints(txs, "mission_01", XpEventType.CONTINUE_STEPS, day)
        assertNotNull(tx2)
        assertEquals(2, tx2!!.points)
        txs.add(tx2)

        // Submit result -> +1 XP (total today = 5, hits daily cap)
        val tx3 = xpEngine.computeAwardablePoints(txs, "mission_01", XpEventType.SUBMIT_RESULT, day)
        assertNotNull(tx3)
        assertEquals(1, tx3!!.points)
        txs.add(tx3)

        // Starting another mission same day -> null because daily cap 5 reached
        val tx4 = xpEngine.computeAwardablePoints(txs, "mission_02", XpEventType.START_ACTIVITY, day)
        assertNull(tx4)

        // Next day -> allowed again
        val nextDayTx = xpEngine.computeAwardablePoints(txs, "mission_02", XpEventType.START_ACTIVITY, day + 1)
        assertNotNull(nextDayTx)
        assertEquals(2, nextDayTx!!.points)
        assertEquals(5, xpEngine.calculateTodayXp(txs, day))
    }

    @Test
    fun `hero levels progress smoothly across thresholds`() {
        val xpEngine = XpEngine()
        assertEquals(HeroLevel.NEWCOMER, xpEngine.calculateLevelProgress(0).first)
        assertEquals(HeroLevel.SEEKER, xpEngine.calculateLevelProgress(15).first)
        assertEquals(HeroLevel.EXPLORER, xpEngine.calculateLevelProgress(40).first)
        assertEquals(HeroLevel.BUILDER, xpEngine.calculateLevelProgress(75).first)
        assertEquals(HeroLevel.PATH_MASTER, xpEngine.calculateLevelProgress(115).first)
    }

    @Test
    fun `recommendation engine returns 3 distinct offline suggestions`() {
        val engine = RecommendationEngine()
        val missions = listOf(
            sampleMission("m1", 1, listOf("int_coding"), listOf("skill_curiosity")),
            sampleMission("m2", 2, listOf("int_art"), listOf("skill_focus")),
            sampleMission("m3", 3, listOf("int_nature"), listOf("skill_planning"))
        )
        val interests = listOf(Interest("int_coding", "کد", "Coding", "code", isSelected = true))
        val skills = mapOf(
            "skill_curiosity" to SkillProgress("skill_curiosity", experiencePoints = 10),
            "skill_focus" to SkillProgress("skill_focus", experiencePoints = 0),
            "skill_planning" to SkillProgress("skill_planning", experiencePoints = 5)
        )

        val recs = engine.generateDailyRecommendations(
            missions = missions,
            progressMap = emptyMap(),
            selectedInterests = interests,
            skillProgressMap = skills,
            preferences = UserPreferences()
        )
        assertNotNull(recs)
        assertEquals("m1", recs!!.primaryMission.id)
        assertEquals("m2", recs.skillGrowthMission?.id)
        assertEquals("m3", recs.discoveryMission?.id)
    }

    @Test
    fun `skill progress engine increments level cleanly`() {
        val engine = SkillProgressEngine()
        val initial = SkillProgress("skill_focus", experiencePoints = 8, level = 1, practicedMissionsCount = 2)
        val updated = engine.computeUpdatedSkill(initial, addedPoints = 3, experienceLabel = "Focus Lab")
        assertEquals(11, updated.experiencePoints)
        assertEquals(2, updated.level)
        assertEquals(3, updated.practicedMissionsCount)
        assertTrue("Focus Lab" in updated.recordedExperiences)
    }

    @Test
    fun `parent security hashes with salt and verifies correctly`() {
        val sec = ParentSecurityEngine()
        val salt = sec.generateSaltHex()
        val hash = sec.hashSecret("2468", salt)
        assertNotEquals("2468", hash)
        assertTrue(sec.verifySecret("2468", hash, salt))
        assertFalse(sec.verifySecret("0000", hash, salt))
    }

    private fun sampleMission(
        id: String,
        day: Int,
        interests: List<String>,
        skills: List<String>
    ) = Mission(
        id = id,
        dayNumber = day,
        titleFa = id,
        titleEn = id,
        shortDescFa = "",
        shortDescEn = "",
        storyFa = "",
        storyEn = "",
        goalFa = "",
        goalEn = "",
        durationMinutes = 15,
        durationCategory = DurationPreference.MEDIUM,
        difficulty = DifficultyLevel.EASY,
        activityStyle = ActivityStylePreference.BALANCED,
        socialStyle = SocialPreference.FLEXIBLE,
        relatedSkills = skills,
        relatedInterests = interests,
        toolsFa = emptyList(),
        toolsEn = emptyList(),
        stepsFa = listOf("s1"),
        stepsEn = listOf("s1"),
        expectedOutputFa = "",
        expectedOutputEn = "",
        shortVersionFa = "",
        shortVersionEn = "",
        reflectionQuestionsFa = emptyList(),
        reflectionQuestionsEn = emptyList(),
        startGuideFa = "",
        startGuideEn = "",
        territoryId = "territory_discovery",
        imageKey = "img_territory_discovery"
    )
}
