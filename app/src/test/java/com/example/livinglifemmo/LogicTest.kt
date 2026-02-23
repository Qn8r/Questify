package com.example.livinglifemmo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {

    @Test
    fun generateDailyQuests_isDeterministicForSeed() {
        val pool = listOf(
            QuestTemplate(QuestCategory.FITNESS, 1, "Push-ups", "💪", 20),
            QuestTemplate(QuestCategory.STUDY, 1, "Read", "📚", 20),
            QuestTemplate(QuestCategory.HYDRATION, 1, "Water", "💧", 20),
            QuestTemplate(QuestCategory.DISCIPLINE, 1, "Clean desk", "🧹", 20),
            QuestTemplate(QuestCategory.MIND, 1, "Meditate", "🧘", 20),
            QuestTemplate(QuestCategory.FITNESS, 2, "Run", "🏃", 30)
        )

        val first = generateDailyQuests(seed = 12345L, playerLevel = 10, pool = pool)
        val second = generateDailyQuests(seed = 12345L, playerLevel = 10, pool = pool)

        assertEquals(first.map { it.id }, second.map { it.id })
        assertEquals(first.map { it.title }, second.map { it.title })
    }

    @Test
    fun generateDailyQuests_respectsDifficultyCap() {
        val pool = listOf(
            QuestTemplate(QuestCategory.FITNESS, 1, "Easy", "✅", 10),
            QuestTemplate(QuestCategory.FITNESS, 4, "Hard", "🔥", 60)
        )

        val quests = generateDailyQuests(seed = 77L, playerLevel = 2, pool = pool)

        assertTrue(quests.all { it.difficulty <= 1 })
    }

    @Test
    fun stableQuestId_isStableAndPositive() {
        val template = QuestTemplate(QuestCategory.STUDY, 2, "Deep Work", "🧠", 40)
        val id1 = stableQuestId(QuestCategory.STUDY, template)
        val id2 = stableQuestId(QuestCategory.STUDY, template)

        assertEquals(id1, id2)
        assertTrue(id1 > 0)
    }

    @Test
    fun generateDailyQuestsAdaptive_respectsChillPreference() {
        val pool = listOf(
            QuestTemplate(QuestCategory.FITNESS, 1, "Easy", "✅", 10),
            QuestTemplate(QuestCategory.FITNESS, 3, "Medium", "🔥", 40)
        )
        val history = mapOf(
            1L to HistoryEntry(done = 0, total = 5, allDone = false),
            2L to HistoryEntry(done = 1, total = 5, allDone = false)
        )
        val quests = generateDailyQuestsAdaptive(
            seed = 42L,
            playerLevel = 6,
            pool = pool,
            history = history,
            recentFailedTitles = emptySet(),
            difficultyPreference = DifficultyPreference.CHILL
        )
        assertTrue(quests.all { it.difficulty <= 2 })
    }

    @Test
    fun refreshKeepingCompleted_respectsDesiredCount() {
        val pool = listOf(
            QuestTemplate(QuestCategory.FITNESS, 1, "Push", "💪", 10),
            QuestTemplate(QuestCategory.FITNESS, 1, "Run", "🏃", 12),
            QuestTemplate(QuestCategory.STUDY, 1, "Read", "📘", 10),
            QuestTemplate(QuestCategory.STUDY, 2, "Review", "🧠", 16),
            QuestTemplate(QuestCategory.HYDRATION, 1, "Water", "💧", 10),
            QuestTemplate(QuestCategory.HYDRATION, 2, "Electrolytes", "🫗", 14),
            QuestTemplate(QuestCategory.DISCIPLINE, 1, "Tidy", "🧹", 10),
            QuestTemplate(QuestCategory.DISCIPLINE, 2, "Budget", "📊", 16),
            QuestTemplate(QuestCategory.MIND, 1, "Breathe", "🌬️", 10),
            QuestTemplate(QuestCategory.MIND, 2, "Journal", "📖", 14)
        )
        val current = generateDailyQuests(
            seed = 8L,
            playerLevel = 20,
            pool = pool,
            desiredCount = 8
        )
        val refreshed = refreshKeepingCompleted(
            current = current,
            playerLevel = 20,
            seed = 9L,
            pool = pool,
            desiredCount = 8
        )
        assertEquals(8, refreshed.size)
    }

    @Test
    fun bestWeekdayByCompletion_returnsReadableName() {
        val history = mapOf(
            epochDayFromYmd(2026, 2, 9) to HistoryEntry(5, 5, true),
            epochDayFromYmd(2026, 2, 10) to HistoryEntry(2, 5, false)
        )
        val best = bestWeekdayByCompletion(history)
        assertTrue(best.isNotBlank())
    }
}
