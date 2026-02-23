package com.example.livinglifemmo

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import androidx.datastore.preferences.core.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.KeyStore
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
val Context.dataStore by preferencesDataStore(name = "living_life_mmo")

object Keys {
    val DATA_VERSION = intPreferencesKey("data_version")
    val TOTAL_XP = intPreferencesKey("total_xp")
    val GOLD = intPreferencesKey("gold")
    val STREAK = intPreferencesKey("streak")
    val BEST_STREAK = intPreferencesKey("best_streak")
    val GRIMOIRE = stringPreferencesKey("grimoire_pages")

    val LAST_DAY = longPreferencesKey("last_day_epoch")
    val QUESTS = stringPreferencesKey("quests_serialized")
    val COMPLETED = stringPreferencesKey("completed_ids")
    val EARNED = stringPreferencesKey("earned_ids")
    val REFRESH_COUNT = intPreferencesKey("refresh_count")

    val AVATAR_PRESET = stringPreferencesKey("avatar_preset")
    val AVATAR_URI = stringPreferencesKey("avatar_uri")

    // Character Colors
    val CHAR_HEAD = longPreferencesKey("char_head")
    val CHAR_BODY = longPreferencesKey("char_body")
    val CHAR_LEGS = longPreferencesKey("char_legs")
    val CHAR_SHOES = longPreferencesKey("char_shoes")

    // Main Quests & Tutorials
    val MAIN_QUESTS = stringPreferencesKey("main_quests_v2")
    val TUTORIAL_INV = booleanPreferencesKey("tut_inv")
    val TUTORIAL_MQ = booleanPreferencesKey("tut_mq")
    val TUTORIAL_POOL = booleanPreferencesKey("tut_pool")
    val TUTORIAL_SHOP = booleanPreferencesKey("tut_shop_seen")
    val TUTORIAL_CALENDAR = booleanPreferencesKey("tut_calendar_seen")
    val TUTORIAL_QUESTS = booleanPreferencesKey("tut_quests_seen")
    val SHOP_HOLD_HINT_SEEN = booleanPreferencesKey("shop_hold_hint_seen")

    val HISTORY = stringPreferencesKey("history_serialized")
    val INVENTORY = stringPreferencesKey("inventory_serialized")

    val APP_THEME = stringPreferencesKey("app_theme")
    val ACCENT_ARGB = intPreferencesKey("accent_argb")
    val AUTO_NEW_DAY = booleanPreferencesKey("auto_new_day")
    val CONFIRM_COMPLETE = booleanPreferencesKey("confirm_complete")
    val REFRESH_INCOMPLETE_ONLY = booleanPreferencesKey("refresh_incomplete_only")
    val REST_MODE = booleanPreferencesKey("rest_mode")
    val ADMIN_MODE = booleanPreferencesKey("admin_mode")

    val CUSTOM_TEMPLATES = stringPreferencesKey("custom_templates")
    val ACHIEVEMENTS = stringPreferencesKey("achievements_serialized")

    // NEW: Template Library Storage
    val SAVED_TEMPLATES = stringPreferencesKey("saved_templates")

    // NEW: Bosses & Skill Decay

    val BOSSES = stringPreferencesKey("bosses_serialized")
    val PLAYER_ATTRIBUTES = stringPreferencesKey("player_attributes")
    val LAST_ACTIVITY_TIMESTAMPS = stringPreferencesKey("last_activity_timestamps")

    val COMMUNITY_POSTS = stringPreferencesKey("community_posts")
    val COMMUNITY_FOLLOWS = stringPreferencesKey("community_follows")
    val COMMUNITY_MY_RATINGS = stringPreferencesKey("community_my_ratings")
    val COMMUNITY_USER_ID = stringPreferencesKey("community_user_id")
    val COMMUNITY_USER_NAME = stringPreferencesKey("community_user_name")
    val COMMUNITY_SYNC_QUEUE = stringPreferencesKey("community_sync_queue")
    val COMMUNITY_MUTED_AUTHORS = stringPreferencesKey("community_muted_authors")
    val COMMUNITY_BLOCKED_AUTHORS = stringPreferencesKey("community_blocked_authors")
    val COMMUNITY_LAST_PUBLISH_AT = longPreferencesKey("community_last_publish_at")
    val SHOP_ITEMS = stringPreferencesKey("shop_items")
    val CALENDAR_PLANS = stringPreferencesKey("calendar_plans")
    val GOLD_PER_MINUTE = intPreferencesKey("gold_per_minute")
    val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    val ONBOARDING_GOAL = stringPreferencesKey("onboarding_goal")
    val ONBOARDING_DIFFICULTY = stringPreferencesKey("onboarding_difficulty")
    val PREMIUM_UNLOCKED = booleanPreferencesKey("premium_unlocked")
    val DAILY_QUEST_TARGET = intPreferencesKey("daily_quest_target")
    val CLOUD_SYNC_ENABLED = booleanPreferencesKey("cloud_sync_enabled")
    val CLOUD_ACCOUNT_EMAIL = stringPreferencesKey("cloud_account_email")
    val CLOUD_ACCESS_TOKEN = stringPreferencesKey("cloud_access_token")
    val CLOUD_LAST_SYNC_AT = longPreferencesKey("cloud_last_sync_at")
    val CLOUD_LAST_SNAPSHOT = stringPreferencesKey("cloud_last_snapshot")
    val ATTRIBUTES_RAW = stringPreferencesKey("attributes_raw")

    // Advanced settings
    val ADVANCED_OPTIONS = booleanPreferencesKey("advanced_options")
    val HIGH_CONTRAST_TEXT = booleanPreferencesKey("high_contrast_text")
    val COMPACT_MODE = booleanPreferencesKey("compact_mode")
    val LARGE_TOUCH_TARGETS = booleanPreferencesKey("large_touch_targets")
    val REDUCE_ANIMATIONS = booleanPreferencesKey("reduce_animations")
    val DECORATIVE_BORDERS = booleanPreferencesKey("decorative_borders")
    val NEON_LIGHT_BOOST = booleanPreferencesKey("neon_light_boost")
    val NEON_FLOW_ENABLED = booleanPreferencesKey("neon_flow_enabled")
    val NEON_FLOW_SPEED = intPreferencesKey("neon_flow_speed")
    val NEON_GLOW_PALETTE = stringPreferencesKey("neon_glow_palette")
    val ALWAYS_SHOW_QUEST_PROGRESS = booleanPreferencesKey("always_show_quest_progress")
    val HIDE_COMPLETED_QUESTS = booleanPreferencesKey("hide_completed_quests")
    val CONFIRM_DESTRUCTIVE = booleanPreferencesKey("confirm_destructive")
    val DAILY_RESET_HOUR = intPreferencesKey("daily_reset_hour")
    val DAILY_REMINDERS_ENABLED = booleanPreferencesKey("daily_reminders_enabled")
    val HAPTICS = booleanPreferencesKey("haptics")
    val SOUND_EFFECTS = booleanPreferencesKey("sound_effects")
    val FONT_STYLE = stringPreferencesKey("font_style")
    val FONT_SCALE_PERCENT = intPreferencesKey("font_scale_percent")
    val BACKGROUND_IMAGE_URI = stringPreferencesKey("background_image_uri")
    val TEXT_COLOR_ARGB = intPreferencesKey("text_color_argb")
    val APP_BACKGROUND_ARGB = intPreferencesKey("app_background_argb")
    val CHROME_BACKGROUND_ARGB = intPreferencesKey("chrome_background_argb")
    val CARD_COLOR_ARGB = intPreferencesKey("card_color_argb")
    val BUTTON_COLOR_ARGB = intPreferencesKey("button_color_argb")
    val JOURNAL_PAGE_COLOR_ARGB = intPreferencesKey("journal_page_color_argb")
    val JOURNAL_ACCENT_COLOR_ARGB = intPreferencesKey("journal_accent_color_argb")
    val JOURNAL_NAME = stringPreferencesKey("journal_name")
    val APP_LANGUAGE = stringPreferencesKey("app_language")
}

const val CURRENT_DATA_VERSION = 6
private const val MAX_TEMPLATE_PAYLOAD_CHARS = 250_000
private const val MAX_TEMPLATE_COMPRESSED_BYTES = 256 * 1024
private const val MAX_TEMPLATE_JSON_BYTES = 1_000_000
private const val MAX_BACKUP_BLOB_CHARS = 2_000_000
private const val MAX_BACKUP_PACKED_BYTES = 1_500_000
private const val MAX_BACKUP_JSON_BYTES = 2_000_000
private const val BACKUP_BLOB_PREFIX = "v4:"
private const val BACKUP_KEY_ALIAS = "questify_backup_key_v4"

/* ===================== SERIALIZERS ===================== */

private val gson = Gson()

fun serializeInventory(list: List<InventoryItem>): String {
    return list.joinToString(";;") { i ->
        val safeName = i.name.replace("|", " ").replace(";;", " ")
        val safeDesc = i.description.replace("|", " ").replace(";;", " ")
        "${i.id}|${i.icon}|${i.cost}|${i.ownedCount}|${if (i.isConsumable) 1 else 0}|$safeName|$safeDesc"
    }
}

fun deserializeInventory(serialized: String): List<InventoryItem> {
    if (serialized.isBlank()) return emptyList()
    return serialized.split(";;").mapNotNull { part ->
        val bits = part.split("|")
        if (bits.size < 7) return@mapNotNull null
        val id = bits[0]
        val icon = bits[1]
        val cost = bits[2].toIntOrNull() ?: 0
        val owned = bits[3].toIntOrNull() ?: 0
        val cons = (bits[4].toIntOrNull() ?: 1) == 1
        val name = bits[5]
        val desc = bits.subList(6, bits.size).joinToString("|")
        InventoryItem(id, name, icon, desc, cost, owned, cons)
    }
}

fun serializeQuests(list: List<Quest>): String {
    return list.joinToString(";;") { q ->
        val safeTitle = q.title.replace("|", " ").replace(";;", " ")
        val safeUri = q.imageUri ?: ""
        // Added |$safeUri at the end
        "${q.id}|${q.category.name}|${q.difficulty}|${q.xpReward}|${q.icon}|$safeTitle|${q.target}|${q.currentProgress}|$safeUri"
    }
}

fun deserializeQuests(serialized: String): List<Quest> {
    if (serialized.isBlank()) return emptyList()
    return serialized.split(";;").mapNotNull { part ->
        val bits = part.split("|")
        if (bits.size < 6) return@mapNotNull null
        val id = bits[0].toIntOrNull() ?: return@mapNotNull null
        val cat = runCatching { QuestCategory.valueOf(bits[1]) }.getOrNull() ?: return@mapNotNull null
        val diff = bits[2].toIntOrNull() ?: 1
        val xp = bits[3].toIntOrNull() ?: 10
        val icon = bits[4]
        val title = bits[5]
        val target = if (bits.size > 6) bits[6].toIntOrNull() ?: 1 else 1
        val progress = if (bits.size > 7) bits[7].toIntOrNull() ?: 0 else 0
        val imageUri = if (bits.size > 8 && bits[8].isNotBlank()) bits[8] else null

        Quest(id, title, xp, icon, cat, diff, target, progress, false, imageUri)
    }
}

fun parseIds(serialized: String?): Set<Int> {
    if (serialized.isNullOrBlank()) return emptySet()
    return serialized.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
}

fun serializeHistory(map: Map<Long, HistoryEntry>): String {
    return map.entries.joinToString(";") { (day, e) ->
        "$day|${e.done}|${e.total}|${if (e.allDone) 1 else 0}"
    }
}

fun parseHistory(raw: String): Map<Long, HistoryEntry> {
    if (raw.isBlank()) return emptyMap()
    val out = mutableMapOf<Long, HistoryEntry>()
    raw.split(";").forEach { part ->
        val bits = part.split("|")
        if (bits.size < 4) return@forEach
        val day = bits[0].toLongOrNull() ?: return@forEach
        val done = bits[1].toIntOrNull() ?: 0
        val total = bits[2].toIntOrNull() ?: 0
        val allDone = (bits[3].toIntOrNull() ?: 0) == 1
        out[day] = HistoryEntry(done = done, total = total, allDone = allDone)
    }
    return out
}

/* ===================== UPDATED SERIALIZERS (With Package ID) ===================== */

fun serializeCustomTemplates(list: List<CustomTemplate>): String {
    return list.joinToString(";;") { t ->
        val safeTitle = t.title.replace("|", " ").replace(";;", " ")
        val safeIcon = t.icon.replace("|", " ").replace(";;", " ")
        val safeUri = t.imageUri ?: ""
        // Added |${if(t.isActive) 1 else 0} at the end
        "${t.id}|${t.category.name}|${t.difficulty}|${t.xp}|$safeIcon|$safeTitle|${if(t.isPinned) 1 else 0}|${t.target}|$safeUri|${t.packageId}|${if(t.isActive) 1 else 0}"
    }
}

fun deserializeCustomTemplates(serialized: String): List<CustomTemplate> {
    if (serialized.isBlank()) return emptyList()
    return serialized.split(";;").mapNotNull { part ->
        val bits = part.split("|")
        if (bits.size < 6) return@mapNotNull null
        val id = bits[0]
        val cat = runCatching { QuestCategory.valueOf(bits[1]) }.getOrNull() ?: return@mapNotNull null
        val diff = bits[2].toIntOrNull() ?: 1
        val xp = bits[3].toIntOrNull() ?: 20
        val icon = bits[4]
        val title = bits[5]
        val isPinned = if (bits.size > 6) (bits[6].toIntOrNull() ?: 0) == 1 else false
        val target = if (bits.size > 7) bits[7].toIntOrNull() ?: 1 else 1
        val imageUri = if (bits.size > 8 && bits[8].isNotBlank()) bits[8] else null
        val packageId = if (bits.size > 9 && bits[9].isNotBlank()) bits[9] else "user_created"
        // NEW: Read isActive
        val isActive = if (bits.size > 10) (bits[10].toIntOrNull() ?: 1) == 1 else true

        CustomTemplate(id, cat, diff, title, icon, xp, target, isPinned, imageUri, packageId, isActive)
    }
}
fun serializeMainQuests(list: List<CustomMainQuest>): String {
    return list.joinToString(";;") { q ->
        val safeTitle = q.title.replace("|", " ").replace(";;", " ")
        val safeDesc = q.description.replace("|", " ").replace(";;", " ")
        val stepsStr = q.steps.joinToString("~") { it.replace("~", " ") }
        val safeIcon = q.icon.replace("|", " ").replace(";;", " ")
        val safeImageUri = (q.imageUri ?: "").replace("|", " ").replace(";;", " ")
        // Added |${if(q.isActive) 1 else 0} at the end
        "${q.id}|${q.xpReward}|${q.currentStep}|${if(q.isClaimed) 1 else 0}|$safeTitle|$safeDesc|$stepsStr|${if(q.hasStarted) 1 else 0}|${q.prerequisiteId ?: ""}|${q.packageId}|${if(q.isActive) 1 else 0}|$safeIcon|$safeImageUri"
    }
}

fun deserializeMainQuests(raw: String): List<CustomMainQuest> {
    if (raw.isBlank()) return emptyList()
    return raw.split(";;").mapNotNull { part ->
        val bits = part.split("|")
        if (bits.size < 7) return@mapNotNull null
        val id = bits[0]
        val xp = bits[1].toIntOrNull() ?: 100
        val step = bits[2].toIntOrNull() ?: 0
        val claimed = (bits[3].toIntOrNull() ?: 0) == 1
        val title = bits[4]
        val desc = bits[5]
        val stepsRaw = bits[6]
        val steps = if (stepsRaw.isBlank()) emptyList() else stepsRaw.split("~")
        val hasStarted = if (bits.size > 7) (bits[7].toIntOrNull() ?: 0) == 1 else (step > 0)
        val prereq = if (bits.size > 8 && bits[8].isNotBlank()) bits[8] else null
        val packageId = if (bits.size > 9 && bits[9].isNotBlank()) bits[9] else "user_created"
        // NEW: Read isActive
        val isActive = if (bits.size > 10) (bits[10].toIntOrNull() ?: 1) == 1 else true
        val icon = if (bits.size > 11 && bits[11].isNotBlank()) bits[11] else "🏆"
        val imageUri = if (bits.size > 12 && bits[12].isNotBlank()) bits[12] else null
        CustomMainQuest(id, title, desc, xp, steps, step, claimed, hasStarted, prereq, packageId, isActive, icon, imageUri)
    }
}

fun parseUnlockedAchievements(raw: String): Set<String> {
    if (raw.isBlank()) return emptySet()
    return raw.split(";;").mapNotNull { part ->
        val bits = part.split("|")
        if (bits.size >= 2 && bits[1] == "1") bits[0] else null
    }.toSet()
}

fun getAchievementDefinitions(): List<Achievement> {
    return listOf(
        Achievement("first_quest", "First Win", "Complete your first daily quest.", "🌱"),
        Achievement("journal_entry", "First Journal", "Write your first journal entry.", "📖"),
        Achievement("level_5", "Level 5 Reached", "Reach Level 5.", "⚔️"),
        Achievement("streak_7", "7-Day Streak", "Maintain a 7-day streak.", "🔥"),
        Achievement("all_daily", "Full Clear", "Complete all your daily quests in one day.", "🌟"),
        Achievement("wealthy", "Gold Saver", "Reach 1000 Gold.", "💰"),
        Achievement("dark_soul", "Neon Shift", "Switch to the Cyberpunk theme.", "🟣"),
        Achievement("streak_30", "30-Day Streak", "Maintain a 30-day streak.", "🛡️"),
        Achievement("focus_master", "Level 10 Reached", "Reach Level 10.", "⏱️"),
        Achievement("community_builder", "Community Builder", "Publish your first community challenge.", "🏗️"),
        Achievement("season_winter", "Winter Winner", "Complete all daily quests on a winter day.", "❄️"),
        Achievement("secret_speedrun", "Daily Speedrun", "Complete 10 quests in one day.", "⚡")
    )
}

suspend fun loadJournal(context: Context): List<JournalPage> {
    val prefs = context.dataStore.data.first()
    val raw = prefs[Keys.GRIMOIRE].orEmpty()
    if (raw.isBlank()) return emptyList()
    val jsonLoaded = runCatching {
        val type = object : TypeToken<List<JournalPage>>() {}.type
        gson.fromJson<List<JournalPage>>(raw, type)
    }.getOrNull()
    if (!jsonLoaded.isNullOrEmpty()) {
        return jsonLoaded.mapNotNull { page ->
            runCatching {
                val allUris = (page.voiceNoteUris + listOfNotNull(page.voiceNoteUri))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                val cleanMap = page.voiceNoteTranscripts
                    .mapNotNull { (k, v) ->
                        val key = k.trim()
                        val value = v.trim()
                        if (key.isBlank() || value.isBlank()) null else key to value
                    }
                    .toMap()
                    .filterKeys { allUris.contains(it) }
                val cleanSubmittedAt = page.voiceNoteSubmittedAt
                    .filterKeys { allUris.contains(it) }
                    .mapNotNull { (k, v) ->
                        val key = k.trim()
                        if (key.isBlank() || v <= 0L) null else key to v
                    }
                    .toMap()
                val cleanNames = page.voiceNoteNames
                    .filterKeys { allUris.contains(it) }
                    .mapNotNull { (k, v) ->
                        val key = k.trim()
                        val value = v.trim()
                        if (key.isBlank() || value.isBlank()) null else key to value
                    }
                    .toMap()
                page.copy(
                    voiceNoteUri = null,
                    voiceNoteUris = allUris,
                    voiceNoteSubmittedAt = cleanSubmittedAt,
                    voiceNoteNames = cleanNames,
                    voiceTranscript = null,
                    voiceNoteTranscripts = cleanMap
                )
            }.getOrNull()
        }
    }

    // Legacy fallback parser.
    return raw.split(";;").mapNotNull { part ->
        if (part.isBlank()) return@mapNotNull null
        val bits = part.split("|")
        if (bits.size < 3) return@mapNotNull null
        val day = bits[0].toLongOrNull() ?: return@mapNotNull null
        val edited = bits[1].toLongOrNull() ?: System.currentTimeMillis()
        if (bits.size >= 4) {
            val title = bits[2]
            val text = bits.subList(3, bits.size).joinToString("|")
            JournalPage(dateEpochDay = day, text = text, title = title, editedAtMillis = edited)
        } else {
            val text = bits.subList(2, bits.size).joinToString("|")
            JournalPage(dateEpochDay = day, text = text, title = "", editedAtMillis = edited)
        }
    }
}

suspend fun persistJournal(context: Context, pages: List<JournalPage>) {
    val serialized = gson.toJson(pages.take(365))
    context.dataStore.edit { prefs -> prefs[Keys.GRIMOIRE] = serialized }
}

/* ===================== NEW SERIALIZERS ===================== */

fun serializeBosses(list: List<Boss>): String {
    return list.joinToString(";;") { b ->
        val safeName = b.name.replace("|", " ").replace(";;", " ")
        val safeDesc = b.description.replace("|", " ").replace(";;", " ")
        "${b.id}|${b.icon}|${b.totalHp}|${b.currentHp}|${b.deadlineMillis}|${b.xpReward}|${if (b.isDefeated) 1 else 0}|$safeName|$safeDesc"
    }
}

fun deserializeBosses(serialized: String): List<Boss> {
    if (serialized.isBlank()) return emptyList()
    return serialized.split(";;").mapNotNull { part ->
        val bits = part.split("|")
        if (bits.size < 9) return@mapNotNull null
        val id = bits[0]
        val icon = bits[1]
        val totalHp = bits[2].toLongOrNull() ?: 100L
        val currentHp = bits[3].toLongOrNull() ?: 100L
        val deadline = bits[4].toLongOrNull() ?: 0L
        val xp = bits[5].toIntOrNull() ?: 500
        val defeated = bits[6] == "1"
        val name = bits[7]
        val desc = bits[8]
        Boss(id, name, desc, icon, totalHp, currentHp, deadline, xp, defeated)
    }
}

fun serializeAttributes(attr: PlayerAttributes): String {
    return "${attr.str}|${attr.int}|${attr.vit}|${attr.end}|${attr.fth}"
}

fun deserializeAttributes(raw: String?): PlayerAttributes {
    if (raw.isNullOrBlank()) return PlayerAttributes(1, 1, 1, 1, 1)
    val bits = raw.split("|")
    if (bits.size < 5) return PlayerAttributes(1, 1, 1, 1, 1)
    return PlayerAttributes(
        bits[0].toIntOrNull() ?: 1,
        bits[1].toIntOrNull() ?: 1,
        bits[2].toIntOrNull() ?: 1,
        bits[3].toIntOrNull() ?: 1,
        bits[4].toIntOrNull() ?: 1
    )
}

fun serializeActivityTimestamps(map: Map<QuestCategory, Long>): String {
    return map.entries.joinToString(";") { "${it.key.name}|${it.value}" }
}

fun deserializeActivityTimestamps(raw: String?): Map<QuestCategory, Long> {
    if (raw.isNullOrBlank()) return emptyMap()
    return raw.split(";").mapNotNull { part ->
        val bits = part.split("|")
        if (bits.size < 2) return@mapNotNull null
        val cat = runCatching { QuestCategory.valueOf(bits[0]) }.getOrNull() ?: return@mapNotNull null
        val time = bits[1].toLongOrNull() ?: return@mapNotNull null
        cat to time
    }.toMap()
}

/* ===================== GSON FOR IMPORT/EXPORT ===================== */

fun exportQuestPool(templates: List<QuestTemplate>): String {
    return gson.toJson(QuestPool(templates))
}

fun importQuestPool(json: String): List<QuestTemplate> {
    return try {
        gson.fromJson(json, QuestPool::class.java).templates
    } catch (e: Exception) {
        emptyList()
    }
}

// ===================== TEMPLATE SHARING (GSON) =====================

// ===================== TEMPLATE SHARING (COMPRESSED) =====================

fun exportGameTemplate(template: GameTemplate): String {
    return try {
        val json = gson.toJson(template)
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(json.toByteArray(Charsets.UTF_8)) }
        // URL_SAFE makes sure there are no weird characters that break WhatsApp links!
        Base64.encodeToString(bos.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
    } catch (e: Exception) {
        ""
    }
}

fun importGameTemplate(payload: String): GameTemplate? {
    val trimmed = payload.trim()
    if (trimmed.isBlank() || trimmed.length > MAX_TEMPLATE_PAYLOAD_CHARS) return null
    return try {
        val bytes = Base64.decode(trimmed, Base64.URL_SAFE or Base64.NO_WRAP)
        if (bytes.size > MAX_TEMPLATE_COMPRESSED_BYTES) return null
        val json = gunzipToUtf8Limited(bytes, MAX_TEMPLATE_JSON_BYTES) ?: return null
        gson.fromJson(json, GameTemplate::class.java)
    } catch (e: Exception) {
        if (trimmed.length > MAX_TEMPLATE_JSON_BYTES) return null
        try {
            gson.fromJson(trimmed, GameTemplate::class.java)
        } catch (e2: Exception) {
            null
        }
    }
}


fun serializeSavedTemplates(list: List<GameTemplate>): String {
    return try { gson.toJson(list) } catch(e: Exception) { "" }
}

fun deserializeSavedTemplates(json: String?): List<GameTemplate> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val type = object : com.google.gson.reflect.TypeToken<List<GameTemplate>>() {}.type
        gson.fromJson(json, type) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

fun serializeCommunityPosts(list: List<CommunityPost>): String {
    return try { gson.toJson(list) } catch (e: Exception) { "" }
}

fun deserializeCommunityPosts(json: String?): List<CommunityPost> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val type = object : com.google.gson.reflect.TypeToken<List<CommunityPost>>() {}.type
        gson.fromJson(json, type) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

fun serializeStringSet(values: Set<String>): String {
    return try { gson.toJson(values.toList()) } catch (e: Exception) { "[]" }
}

fun deserializeStringSet(json: String?): Set<String> {
    if (json.isNullOrBlank()) return emptySet()
    return try {
        val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
        (gson.fromJson<List<String>>(json, type) ?: emptyList()).toSet()
    } catch (e: Exception) {
        emptySet()
    }
}

fun serializeRatingsMap(map: Map<String, Int>): String {
    return try { gson.toJson(map) } catch (e: Exception) { "{}" }
}

fun deserializeRatingsMap(json: String?): Map<String, Int> {
    if (json.isNullOrBlank()) return emptyMap()
    return try {
        val type = object : com.google.gson.reflect.TypeToken<Map<String, Int>>() {}.type
        gson.fromJson<Map<String, Int>>(json, type) ?: emptyMap()
    } catch (e: Exception) {
        emptyMap()
    }
}

fun serializeShopItems(list: List<ShopItem>): String {
    return try { gson.toJson(list) } catch (e: Exception) { "[]" }
}

fun deserializeShopItems(json: String?): List<ShopItem> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val type = object : com.google.gson.reflect.TypeToken<List<ShopItem>>() {}.type
        gson.fromJson<List<ShopItem>>(json, type) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

fun serializeCalendarPlans(map: Map<Long, List<String>>): String {
    return try { gson.toJson(map) } catch (e: Exception) { "{}" }
}

fun serializeCommunitySyncQueue(tasks: List<CommunitySyncTask>): String {
    return try { gson.toJson(normalizeCommunitySyncQueue(tasks)) } catch (e: Exception) { "[]" }
}

fun deserializeCommunitySyncQueue(json: String?): List<CommunitySyncTask> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val type = object : com.google.gson.reflect.TypeToken<List<CommunitySyncTask>>() {}.type
        normalizeCommunitySyncQueue(gson.fromJson<List<CommunitySyncTask>>(json, type) ?: emptyList())
    } catch (e: Exception) {
        emptyList()
    }
}

fun normalizeCommunitySyncQueue(
    tasks: List<CommunitySyncTask>,
    nowMillis: Long = System.currentTimeMillis(),
    maxQueueSize: Int = 120,
    maxAgeMillis: Long = 1000L * 60L * 60L * 24L * 7L
): List<CommunitySyncTask> {
    if (tasks.isEmpty()) return emptyList()
    val minCreated = nowMillis - maxAgeMillis
    return tasks.asSequence()
        .map {
            it.copy(
                stars = it.stars?.coerceIn(1, 5),
                attemptCount = it.attemptCount.coerceIn(0, 10)
            )
        }
        .filter { it.createdAtMillis >= minCreated }
        .distinctBy { task ->
            when (task.type) {
                CommunitySyncTaskType.PUBLISH_POST -> "publish:${task.post?.id.orEmpty()}"
                CommunitySyncTaskType.FOLLOW_AUTHOR -> "follow:${task.authorId.orEmpty()}"
                CommunitySyncTaskType.UNFOLLOW_AUTHOR -> "unfollow:${task.authorId.orEmpty()}"
                CommunitySyncTaskType.RATE_POST -> "rate:${task.postId.orEmpty()}:${task.stars ?: 0}"
                CommunitySyncTaskType.INCREMENT_REMIX -> "remix:${task.postId.orEmpty()}:${task.currentRemixCount ?: 0}"
            }
        }
        .toList()
        .takeLast(maxQueueSize)
}

suspend fun runDataMigrations(context: Context) {
    val prefs = context.dataStore.data.first()
    val fromVersion = prefs[Keys.DATA_VERSION] ?: 0
    if (fromVersion >= CURRENT_DATA_VERSION) return

    context.dataStore.edit { p ->
        if (fromVersion < 1) {
            if (p[Keys.COMMUNITY_SYNC_QUEUE].isNullOrBlank()) {
                p[Keys.COMMUNITY_SYNC_QUEUE] = "[]"
            }
        }
        if (fromVersion < 2) {
            val safeHour = (p[Keys.DAILY_RESET_HOUR] ?: 0).coerceIn(0, 23)
            p[Keys.DAILY_RESET_HOUR] = safeHour
            val safeScale = (p[Keys.FONT_SCALE_PERCENT] ?: 100).coerceIn(80, 140)
            p[Keys.FONT_SCALE_PERCENT] = safeScale
        }
        if (fromVersion < 3) {
            if (p[Keys.DAILY_REMINDERS_ENABLED] == null) {
                p[Keys.DAILY_REMINDERS_ENABLED] = true
            }
            p[Keys.COMMUNITY_SYNC_QUEUE] = serializeCommunitySyncQueue(
                deserializeCommunitySyncQueue(p[Keys.COMMUNITY_SYNC_QUEUE])
            )
        }
        if (fromVersion < 4) {
            if (p[Keys.COMMUNITY_MUTED_AUTHORS].isNullOrBlank()) p[Keys.COMMUNITY_MUTED_AUTHORS] = "[]"
            if (p[Keys.COMMUNITY_BLOCKED_AUTHORS].isNullOrBlank()) p[Keys.COMMUNITY_BLOCKED_AUTHORS] = "[]"
        }
        if (fromVersion < 5) {
            if (p[Keys.DAILY_QUEST_TARGET] == null) p[Keys.DAILY_QUEST_TARGET] = 5
            if (p[Keys.CLOUD_SYNC_ENABLED] == null) p[Keys.CLOUD_SYNC_ENABLED] = false
            if (p[Keys.CLOUD_ACCOUNT_EMAIL].isNullOrBlank()) p[Keys.CLOUD_ACCOUNT_EMAIL] = ""
            if (p[Keys.CLOUD_ACCESS_TOKEN].isNullOrBlank()) p[Keys.CLOUD_ACCESS_TOKEN] = ""
        }
        if (fromVersion < 6) {
            if (p[Keys.TUTORIAL_SHOP] == null) p[Keys.TUTORIAL_SHOP] = false
            if (p[Keys.TUTORIAL_CALENDAR] == null) p[Keys.TUTORIAL_CALENDAR] = false
            if (p[Keys.TUTORIAL_QUESTS] == null) p[Keys.TUTORIAL_QUESTS] = false
            if (p[Keys.SHOP_HOLD_HINT_SEEN] == null) p[Keys.SHOP_HOLD_HINT_SEEN] = false
        }
        p[Keys.DATA_VERSION] = CURRENT_DATA_VERSION
    }
}

fun deserializeCalendarPlans(json: String?): Map<Long, List<String>> {
    if (json.isNullOrBlank()) return emptyMap()
    return try {
        val type = object : com.google.gson.reflect.TypeToken<Map<String, List<String>>>() {}.type
        val raw = gson.fromJson<Map<String, List<String>>>(json, type) ?: emptyMap()
        raw.mapNotNull { (k, v) -> k.toLongOrNull()?.let { it to v.filter { s -> s.isNotBlank() } } }.toMap()
    } catch (e: Exception) {
        // Backward-compat with old single-plan format
        try {
            val oldType = object : com.google.gson.reflect.TypeToken<Map<String, String>>() {}.type
            val old = gson.fromJson<Map<String, String>>(json, oldType) ?: emptyMap()
            old.mapNotNull { (k, v) -> k.toLongOrNull()?.let { it to listOf(v) } }.toMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }
}


private fun backupKey(packageName: String, includeVersionName: Boolean): ByteArray {
    val material = if (includeVersionName) {
        "$packageName|questify_backup_v2|${BuildConfig.VERSION_NAME}"
    } else {
        "$packageName|questify_backup_v3"
    }
    return MessageDigest.getInstance("SHA-256").digest(material.toByteArray(StandardCharsets.UTF_8))
}

fun exportFullBackupEncrypted(payload: FullBackupPayload, packageName: String): String {
    val jsonBytes = runCatching { gson.toJson(payload).toByteArray(StandardCharsets.UTF_8) }.getOrNull() ?: return ""
    if (jsonBytes.size > MAX_BACKUP_JSON_BYTES) return ""
    val key = getOrCreateBackupKey() ?: return exportLegacyBackup(payload, packageName)
    return try {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(jsonBytes)
        val packed = iv + encrypted
        if (packed.size > MAX_BACKUP_PACKED_BYTES) return ""
        BACKUP_BLOB_PREFIX + Base64.encodeToString(packed, Base64.URL_SAFE or Base64.NO_WRAP)
    } catch (_: Exception) {
        ""
    }
}

fun importFullBackupEncrypted(blob: String, packageName: String): FullBackupPayload? {
    val trimmed = blob.trim()
    if (trimmed.isBlank() || trimmed.length > MAX_BACKUP_BLOB_CHARS) return null
    return if (trimmed.startsWith(BACKUP_BLOB_PREFIX)) {
        val encoded = trimmed.removePrefix(BACKUP_BLOB_PREFIX)
        importKeystoreBackup(encoded) ?: importLegacyBackup(encoded, packageName)
    } else {
        importLegacyBackup(trimmed, packageName)
    }
}

fun preferencesToStringMap(prefs: androidx.datastore.preferences.core.Preferences): Map<String, String> {
    return prefs.asMap().mapKeys { it.key.name }.mapValues { (_, v) -> v?.toString().orEmpty() }
}

fun parseBackupMap(json: String): Map<String, String> {
    return runCatching {
        val type = object : TypeToken<Map<String, String>>() {}.type
        gson.fromJson<Map<String, String>>(json, type) ?: emptyMap()
    }.getOrDefault(emptyMap())
}

private fun gunzipToUtf8Limited(compressed: ByteArray, maxBytes: Int): String? {
    if (compressed.size > MAX_TEMPLATE_COMPRESSED_BYTES) return null
    return try {
        GZIPInputStream(ByteArrayInputStream(compressed)).use { gzip ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            var total = 0
            while (true) {
                val read = gzip.read(buffer)
                if (read < 0) break
                total += read
                if (total > maxBytes) return null
                out.write(buffer, 0, read)
            }
            String(out.toByteArray(), StandardCharsets.UTF_8)
        }
    } catch (_: Exception) {
        null
    }
}

private fun getOrCreateBackupKey(): SecretKey? {
    return runCatching {
        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)
        val existing = ks.getKey(BACKUP_KEY_ALIAS, null) as? SecretKey
        if (existing != null) return@runCatching existing
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            BACKUP_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        keyGenerator.generateKey()
    }.getOrNull()
}

private fun parseFullBackupPayload(jsonBytes: ByteArray): FullBackupPayload? {
    if (jsonBytes.size > MAX_BACKUP_JSON_BYTES) return null
    val json = String(jsonBytes, StandardCharsets.UTF_8)
    return runCatching { gson.fromJson(json, FullBackupPayload::class.java) }.getOrNull()
}

private fun importKeystoreBackup(encoded: String): FullBackupPayload? {
    val packed = runCatching { Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP) }.getOrNull() ?: return null
    if (packed.size <= 12 || packed.size > MAX_BACKUP_PACKED_BYTES) return null
    val key = getOrCreateBackupKey() ?: return null
    return try {
        val iv = packed.copyOfRange(0, 12)
        val encrypted = packed.copyOfRange(12, packed.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        parseFullBackupPayload(cipher.doFinal(encrypted))
    } catch (_: Exception) {
        null
    }
}

private fun exportLegacyBackup(payload: FullBackupPayload, packageName: String): String {
    return runCatching {
        val json = gson.toJson(payload)
        val jsonBytes = json.toByteArray(StandardCharsets.UTF_8)
        if (jsonBytes.size > MAX_BACKUP_JSON_BYTES) return@runCatching ""
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = SecretKeySpec(backupKey(packageName, includeVersionName = false).copyOf(16), "AES")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(jsonBytes)
        val packed = iv + encrypted
        if (packed.size > MAX_BACKUP_PACKED_BYTES) return@runCatching ""
        Base64.encodeToString(packed, Base64.URL_SAFE or Base64.NO_WRAP)
    }.getOrDefault("")
}

private fun importLegacyBackup(blob: String, packageName: String): FullBackupPayload? {
    val packed = runCatching { Base64.decode(blob, Base64.URL_SAFE or Base64.NO_WRAP) }.getOrNull() ?: return null
    if (packed.size <= 12 || packed.size > MAX_BACKUP_PACKED_BYTES) return null
    val iv = packed.copyOfRange(0, 12)
    val encrypted = packed.copyOfRange(12, packed.size)

    val candidateKeys = listOf(
        SecretKeySpec(backupKey(packageName, includeVersionName = false).copyOf(16), "AES"),
        SecretKeySpec(backupKey(packageName, includeVersionName = true).copyOf(16), "AES")
    )
    for (key in candidateKeys) {
        val decoded = runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            parseFullBackupPayload(cipher.doFinal(encrypted))
        }.getOrNull()
        if (decoded != null) return decoded
    }
    return null
}

suspend fun completeNextQuestFromExternalAction(context: Context): Boolean {
    val prefs = context.dataStore.data.first()
    val base = deserializeQuests(prefs[Keys.QUESTS].orEmpty())
    if (base.isEmpty()) return false

    val completed = parseIds(prefs[Keys.COMPLETED])
    val nextQuest = base.firstOrNull { !completed.contains(it.id) } ?: return false
    val updatedCompleted = completed + nextQuest.id

    val streak = prefs[Keys.STREAK] ?: 0
    val earned = parseIds(prefs[Keys.EARNED]).toMutableSet()
    var totalXp = prefs[Keys.TOTAL_XP] ?: 0
    var gold = prefs[Keys.GOLD] ?: 0
    var attrs = deserializeAttributes(prefs[Keys.ATTRIBUTES_RAW])

    if (!earned.contains(nextQuest.id)) {
        totalXp += nextQuest.xpReward
        gold += calculateGoldReward(nextQuest.difficulty, streak)
        attrs = when (nextQuest.category) {
            QuestCategory.FITNESS -> attrs.copy(str = attrs.str + 1)
            QuestCategory.STUDY -> attrs.copy(int = attrs.int + 1)
            QuestCategory.HYDRATION -> attrs.copy(vit = attrs.vit + 1)
            QuestCategory.DISCIPLINE -> attrs.copy(end = attrs.end + 1)
            QuestCategory.MIND -> attrs.copy(fth = attrs.fth + 1)
        }
        earned += nextQuest.id
    }

    val resetHour = (prefs[Keys.DAILY_RESET_HOUR] ?: 0).coerceIn(0, 23)
    val day = prefs[Keys.LAST_DAY] ?: epochDayNowAtHour(resetHour)
    val history = parseHistory(prefs[Keys.HISTORY].orEmpty()).toMutableMap()
    val total = base.size
    val done = updatedCompleted.size.coerceAtMost(total)
    history[day] = HistoryEntry(done = done, total = total, allDone = total > 0 && done == total)
    val trimmedHistory = history.toList()
        .sortedByDescending { it.first }
        .take(60)
        .associate { it.first to it.second }

    context.dataStore.edit { p ->
        p[Keys.COMPLETED] = updatedCompleted.joinToString(",")
        p[Keys.EARNED] = earned.joinToString(",")
        p[Keys.TOTAL_XP] = totalXp
        p[Keys.GOLD] = gold
        p[Keys.ATTRIBUTES_RAW] = serializeAttributes(attrs)
        p[Keys.HISTORY] = serializeHistory(trimmedHistory)
    }
    return true
}
