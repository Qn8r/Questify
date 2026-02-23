package com.example.livinglifemmo

import android.net.Uri
import com.google.gson.annotations.SerializedName

/* ===================== DATA MODELS ===================== */

enum class QuestCategory { FITNESS, STUDY, HYDRATION, DISCIPLINE, MIND }

enum class AppTheme { DEFAULT, LIGHT, CYBERPUNK }
enum class AppFontStyle {
    DEFAULT,
    SANS,
    SERIF,
    MONO,
    DISPLAY,
    ROUNDED,
    TERMINAL,
    ELEGANT,
    HANDWRITTEN
}
enum class OnboardingGoal { BALANCE, FITNESS, STUDY, DISCIPLINE, WELLNESS }
enum class DifficultyPreference { CHILL, NORMAL, HARDCORE }

data class OnboardingSetup(
    val name: String,
    val avatar: String,
    val avatarImageUri: String? = null,
    val templateId: String,
    val theme: AppTheme = AppTheme.DEFAULT,
    val accentArgb: Long? = null,
    val goal: OnboardingGoal,
    val difficultyPreference: DifficultyPreference,
    val reminderHour: Int
)

data class TemplateSettings(
    val autoNewDay: Boolean = true,
    val confirmComplete: Boolean = true,
    val refreshIncompleteOnly: Boolean = true,
    val customMode: Boolean = false,
    val advancedOptions: Boolean = false,
    val highContrastText: Boolean = false,
    val compactMode: Boolean = false,
    val largerTouchTargets: Boolean = false,
    val reduceAnimations: Boolean = false,
    val decorativeBorders: Boolean = false,
    val neonLightBoost: Boolean = false,
    val neonFlowEnabled: Boolean = false,
    val neonFlowSpeed: Int = 0,
    val neonGlowPalette: String = "magenta",
    val alwaysShowQuestProgress: Boolean = true,
    val hideCompletedQuests: Boolean = false,
    val confirmDestructiveActions: Boolean = true,
    val dailyResetHour: Int = 0,
    val dailyRemindersEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val soundEffectsEnabled: Boolean = true,
    val fontStyle: AppFontStyle = AppFontStyle.DEFAULT,
    val fontScalePercent: Int = 100,
    val backgroundImageUri: String? = null,
    val backgroundImageTransparencyPercent: Int? = null,
    val textColorArgb: Long? = null,
    val appBackgroundArgb: Long? = null,
    val chromeBackgroundArgb: Long? = null,
    val cardColorArgb: Long? = null,
    val buttonColorArgb: Long? = null,
    val journalPageColorArgb: Long? = null,
    val journalAccentColorArgb: Long? = null,
    val journalName: String = "Journal"
)

data class Quest(
    val id: Int,
    val title: String,
    val xpReward: Int,
    val icon: String,
    val category: QuestCategory,
    val difficulty: Int,
    val target: Int = 1,
    val currentProgress: Int = 0,
    val completed: Boolean = false,
    val imageUri: String? = null,
    val packageId: String = "user_created" // NEW: Active quests need this tag too!
)

// Updated for Gson serialization
data class QuestTemplate(
    val category: QuestCategory,
    val difficulty: Int,
    val title: String,
    val icon: String,
    val xp: Int,
    val target: Int = 1,
    val isPinned: Boolean = false,
    val imageUri: String? = null,
    val packageId: String = "user_created" // NEW: Templates need this tag!
)

sealed class Avatar {
    data class Preset(val emoji: String) : Avatar()
    data class Custom(val uri: Uri) : Avatar()
}

// Pixel Character Data
data class CharacterData(
    val headColor: Long = 0xFFFACE8D, // Skin
    val bodyColor: Long = 0xFF3F51B5, // Shirt
    val legsColor: Long = 0xFF212121, // Pants
    val shoesColor: Long = 0xFF4E342E  // Shoes
)


// NEW: Boss Battle Event
data class Boss(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val totalHp: Long,
    val currentHp: Long,
    val deadlineMillis: Long,
    val xpReward: Int,
    val isDefeated: Boolean = false
)

// NEW: Non-Player Character
data class Npc(
    val id: String,
    val name: String,
    val icon: String, // Emoji
    val dialogue: List<String>
)

data class TutorialFlags(
    val seenInventoryHelp: Boolean = false,
    val seenMainQuestHelp: Boolean = false,
    val seenPoolHelp: Boolean = false
)

data class LevelInfo(
    val level: Int,
    val currentXpInLevel: Int,
    val xpForNextLevel: Int,
    val totalXp: Int
)

data class PlayerAttributes(
    val str: Int, val int: Int, val vit: Int, val end: Int, val fth: Int
)

data class InventoryItem(
    val id: String,
    val name: String,
    val icon: String,
    val description: String,
    val cost: Int,
    val ownedCount: Int = 0,
    val isConsumable: Boolean = true
)

data class ShopItem(
    val id: String,
    val name: String,
    val icon: String,
    val description: String,
    val cost: Int,
    val stock: Int = 5,
    val maxStock: Int = 5,
    val isConsumable: Boolean = true,
    val imageUri: String? = null
)

data class HistoryEntry(val done: Int, val total: Int, val allDone: Boolean)

// Daily Quest Template
data class CustomTemplate(
    val id: String,
    val category: QuestCategory,
    val difficulty: Int,
    val title: String,
    val icon: String,
    val xp: Int,
    val target: Int = 1,
    val isPinned: Boolean = false,
    val imageUri: String? = null,
    val packageId: String = "user_created",
    val isActive: Boolean = true // NEW: The Toggle Switch
)

// Main Quest Template
data class CustomMainQuest(
    val id: String,
    val title: String,
    val description: String,
    val xpReward: Int,
    val steps: List<String> = listOf("Preparation", "Execution", "Completion"),
    val currentStep: Int = 0,
    val isClaimed: Boolean = false,
    val hasStarted: Boolean = false,
    val prerequisiteId: String? = null,
    val packageId: String = "user_created",
    val isActive: Boolean = true, // NEW: The Toggle Switch
    val icon: String = "🏆",
    val imageUri: String? = null
)


// NEW: Wrapper for JSON export
data class QuestPool(
    @SerializedName("quest_templates") val templates: List<QuestTemplate>
)

data class JournalPage(
    val dateEpochDay: Long,
    val text: String,
    val title: String = "",
    val editedAtMillis: Long = System.currentTimeMillis(),
    val voiceNoteUri: String? = null, // legacy single-note field
    val voiceNoteUris: List<String> = emptyList(),
    val voiceNoteSubmittedAt: Map<String, Long> = emptyMap(),
    val voiceNoteNames: Map<String, String> = emptyMap(),
    val voiceTranscript: String? = null, // legacy single-transcript field
    val voiceNoteTranscripts: Map<String, String> = emptyMap()
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val unlocked: Boolean = false
)

data class CommunityPost(
    val id: String = java.util.UUID.randomUUID().toString(),
    val authorId: String,
    val authorName: String,
    val title: String,
    val description: String,
    val tags: List<String> = emptyList(),
    val template: GameTemplate,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val ratingAverage: Double = 0.0,
    val ratingCount: Int = 0,
    val remixCount: Int = 0,
    val sourcePostId: String? = null
)

enum class CommunitySyncTaskType { PUBLISH_POST, FOLLOW_AUTHOR, UNFOLLOW_AUTHOR, RATE_POST, INCREMENT_REMIX }

data class CommunitySyncTask(
    val type: CommunitySyncTaskType,
    val post: CommunityPost? = null,
    val authorId: String? = null,
    val postId: String? = null,
    val stars: Int? = null,
    val currentRemixCount: Int? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val attemptCount: Int = 0
)

// NEW: Complete Game Template for Export/Import
data class GameTemplate(
    val templateName: String = "My Custom RPG",
    val appTheme: AppTheme = AppTheme.DEFAULT,
    val dailyQuests: List<QuestTemplate> = emptyList(),
    val mainQuests: List<CustomMainQuest> = emptyList(),
    val shopItems: List<ShopItem> = emptyList(),
    val packageId: String = java.util.UUID.randomUUID().toString(), // NEW: The Brand Tag
    val templateSettings: TemplateSettings? = null,
    val accentArgb: Long? = null,
    val isPremium: Boolean = false
)

data class FullBackupPayload(
    val generatedAtMillis: Long = System.currentTimeMillis(),
    val version: Int = CURRENT_DATA_VERSION,
    val dataStoreDump: Map<String, String> = emptyMap()
)

data class AdvancedTemplateGuide(
    val summary: String = "Edit daily_quests, main_quests, and optional shop_items with AI, then import this file in Settings > Advanced Templates.",
    val ai_prompt_example: String = "Add 100 daily quests, 30 main quests, and 20 balanced shop items. Also set app_theme and accent_argb.",
    val notes: List<String> = listOf(
        "Use category: FITNESS, STUDY, HYDRATION, DISCIPLINE, MIND",
        "difficulty must be 1..5",
        "target should be >= 1",
        "main quest steps should be 1..8 items"
    )
)

data class AdvancedDailyQuestEntry(
    val title: String = "",
    val category: String = "DISCIPLINE",
    val difficulty: Int = 2,
    val xp: Int = 20,
    val target: Int = 1,
    val icon: String = "✅",
    val pinned: Boolean = false,
    val image_uri: String? = null
)

data class AdvancedMainQuestEntry(
    val ref: String = "",
    val title: String = "",
    val description: String = "",
    val xp_reward: Int = 200,
    val steps: List<String> = listOf("Preparation", "Execution", "Completion"),
    val prerequisite_ref: String? = null,
    val icon: String = "🏆",
    val image_uri: String? = null
)

data class AdvancedShopItemEntry(
    val id: String? = null,
    val name: String = "",
    val icon: String = "🧩",
    val description: String = "",
    val cost: Int = 100,
    val stock: Int = 5,
    val max_stock: Int = 5,
    val consumable: Boolean = true,
    val image_uri: String? = null
)

data class AdvancedTemplateFile(
    val schema_version: Int = 1,
    val template_name: String = "AI Generated Template",
    val app_theme: String = "DEFAULT",
    val accent_argb: Long? = null,
    val ai_instructions: List<String> = listOf(
        "This JSON file is from the Questify app.",
        "Read the user's request and update daily_quests/main_quests accordingly.",
        "Keep top-level keys and structure unchanged.",
        "Return ONLY the updated JSON file content (no markdown, no explanation)."
    ),
    val guide: AdvancedTemplateGuide = AdvancedTemplateGuide(),
    val daily_quests: List<AdvancedDailyQuestEntry> = emptyList(),
    val main_quests: List<AdvancedMainQuestEntry> = emptyList(),
    val shop_items: List<AdvancedShopItemEntry> = emptyList()
)

data class AdvancedTemplateImportResult(
    val success: Boolean,
    val templateName: String,
    val dailyAdded: Int,
    val mainAdded: Int,
    val packageId: String? = null,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList()
)
