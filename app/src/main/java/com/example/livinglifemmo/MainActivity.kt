package com.example.livinglifemmo

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.gson.Gson
import androidx.compose.animation.*
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.UUID
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.AsyncImage
import androidx.compose.runtime.collectAsState
class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val lang = newBase.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("selected_language", "system") ?: "system"
        val ctx = if (lang == "system") {
            newBase
        } else {
            val locale = Locale(lang)
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            newBase.createConfigurationContext(config)
        }
        super.attachBaseContext(ctx)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            AppLog.e("Uncaught crash on thread=${t.name}", e)
            previous?.uncaughtException(t, e)
        }
        super.onCreate(savedInstanceState)
        SoundManager.init(this)
        enableEdgeToEdge()
        setContent {
            AppRoot(appContext = this)
        }
    }
}

enum class Screen { HOME, MAINQUEST, CALENDAR, QUESTS, GRIMOIRE, STATS, INVENTORY, SETTINGS, ABOUT, COMMUNITY }
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
@Composable
fun AppRoot(appContext: Context) {
    val scope = rememberCoroutineScope()
    val systemPrefersDark = androidx.compose.foundation.isSystemInDarkTheme()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var swipeVisualProgress by remember { mutableFloatStateOf(0f) }
    BackHandler {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            screen != Screen.HOME -> screen = Screen.HOME
        }
    }

    fun sanitizeDisplayName(raw: String): String {
        return CommunityCoordinator.sanitizeDisplayName(raw)
    }
    fun sanitizeCommunityText(raw: String, maxLen: Int): String {
        return CommunityCoordinator.sanitizeCommunityText(raw, maxLen)
    }
    fun sanitizeTags(tags: List<String>): List<String> {
        return CommunityCoordinator.sanitizeTags(tags)
    }
    fun hasSuspiciousCommunityContent(raw: String): Boolean {
        return CommunityCoordinator.hasSuspiciousContent(raw)
    }
    fun evaluateTemplateTrust(raw: GameTemplate): Pair<GameTemplate, TemplateTrustLevel> {
        val normalized = normalizeGameTemplateSafe(raw)
        val suspiciousInRaw =
            hasSuspiciousCommunityContent(normalized.templateName) ||
                normalized.dailyQuests.any { hasSuspiciousCommunityContent(it.title) } ||
                normalized.mainQuests.any {
                    hasSuspiciousCommunityContent(it.title) ||
                        hasSuspiciousCommunityContent(it.description) ||
                        it.steps.any(::hasSuspiciousCommunityContent)
                } ||
                normalized.shopItems.any {
                    hasSuspiciousCommunityContent(it.name) ||
                        hasSuspiciousCommunityContent(it.description)
                }
        val safeName = sanitizeCommunityText(normalized.templateName, 60).ifBlank { "Community Template" }
        val safeDaily = normalized.dailyQuests
            .take(500)
            .map { it.copy(title = sanitizeCommunityText(it.title, 64), icon = it.icon.take(4)) }
            .filter { it.title.isNotBlank() && !hasSuspiciousCommunityContent(it.title) }
        val safeMain = normalized.mainQuests
            .take(200)
            .map { q ->
                q.copy(
                    title = sanitizeCommunityText(q.title, 64),
                    description = sanitizeCommunityText(q.description, 220),
                    steps = q.steps.map { sanitizeCommunityText(it, 64) }.filter { it.isNotBlank() }.take(8)
                )
            }
            .filter { it.title.isNotBlank() && !hasSuspiciousCommunityContent(it.title) && !hasSuspiciousCommunityContent(it.description) }
        val safeShop = normalized.shopItems
            .take(120)
            .map { it.copy(name = sanitizeCommunityText(it.name, 48), description = sanitizeCommunityText(it.description, 160)) }
            .filter { it.name.isNotBlank() && !hasSuspiciousCommunityContent(it.name) && !hasSuspiciousCommunityContent(it.description) }
        val sanitized = normalized.copy(
            templateName = safeName,
            dailyQuests = safeDaily,
            mainQuests = safeMain,
            shopItems = safeShop
        )
        val trust = when {
            suspiciousInRaw -> TemplateTrustLevel.FLAGGED
            sanitized != normalized -> TemplateTrustLevel.SANITIZED
            else -> TemplateTrustLevel.VERIFIED_SAFE
        }
        return sanitized to trust
    }
    fun sanitizeTemplateForCommunity(raw: GameTemplate): GameTemplate {
        return evaluateTemplateTrust(raw).first
    }
    fun secureCommunityUserId(raw: String): String {
        val candidate = raw.trim()
        val parsed = runCatching { UUID.fromString(candidate) }.getOrNull()
        return parsed?.toString() ?: UUID.randomUUID().toString()
    }

    // --- State Variables ---
    var journalPages by remember { mutableStateOf(emptyList<JournalPage>()) }
    var grimoirePageIndex by rememberSaveable { mutableIntStateOf(0) }
    var journalBookOpen by rememberSaveable { mutableStateOf(false) }
    var totalXp by remember { mutableIntStateOf(0) }
    var gold by remember { mutableIntStateOf(0) }
    var currentLevel by rememberSaveable { mutableIntStateOf(1) }
    var streak by remember { mutableIntStateOf(0) }
    var bestStreak by remember { mutableIntStateOf(0) }

// This loads the history data safely from the database
    val historyFlow = remember(appContext) {
        appContext.dataStore.data.map { prefs -> parseHistory(prefs[Keys.HISTORY].orEmpty()) }
    }
    val historyMapState = historyFlow.collectAsState(initial = emptyMap())

    val historyMap = historyMapState.value

    // Core Data
    var quests by remember { mutableStateOf(emptyList<Quest>()) }
    var bosses by remember { mutableStateOf(emptyList<Boss>()) } // FIXED: Added this back
    var avatar by remember { mutableStateOf<Avatar>(Avatar.Preset("🧑‍🚀")) }
    var characterData by remember { mutableStateOf(CharacterData()) }
    var inventory by remember { mutableStateOf(emptyList<InventoryItem>()) }
    var shopItems by remember { mutableStateOf(emptyList<ShopItem>()) }
    var calendarPlans by remember { mutableStateOf<Map<Long, List<String>>>(emptyMap()) }
    var attributes by remember { mutableStateOf(PlayerAttributes(1, 1, 1, 1, 1)) }

    var lastDayEpoch by remember { mutableLongStateOf(epochDayNowAtHour(0)) }
    var refreshCount by remember { mutableIntStateOf(0) }
    var homeRefreshInProgress by remember { mutableStateOf(false) }
    var earnedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    // Custom Main Quests
    var mainQuests by remember { mutableStateOf(emptyList<CustomMainQuest>()) }

    var autoNewDay by remember { mutableStateOf(true) }
    var confirmComplete by remember { mutableStateOf(true) }
    var refreshIncompleteOnly by remember { mutableStateOf(true) }
    var customMode by remember { mutableStateOf(false) }
    var advancedOptions by remember { mutableStateOf(false) }
    var highContrastText by remember { mutableStateOf(false) }
    var compactMode by remember { mutableStateOf(false) }
    var largerTouchTargets by remember { mutableStateOf(false) }
    var reduceAnimations by remember { mutableStateOf(false) }
    var decorativeBorders by remember { mutableStateOf(false) }
    var neonLightBoost by remember { mutableStateOf(false) }
    var neonFlowEnabled by remember { mutableStateOf(false) }
    var neonFlowSpeed by remember { mutableIntStateOf(0) }
    var neonGlowPalette by remember { mutableStateOf("magenta") }
    var alwaysShowQuestProgress by remember { mutableStateOf(true) }
    var hideCompletedQuests by remember { mutableStateOf(false) }
    var confirmDestructiveActions by remember { mutableStateOf(true) }
    var dailyResetHour by remember { mutableIntStateOf(0) }
    var dailyRemindersEnabled by remember { mutableStateOf(true) }
    var hapticsEnabled by remember { mutableStateOf(true) }
    var soundEffectsEnabled by remember { mutableStateOf(true) }
    var fontStyle by remember { mutableStateOf(AppFontStyle.DEFAULT) }
    var fontScalePercent by remember { mutableIntStateOf(100) }
    var appLanguage by remember { mutableStateOf("system") }
    var backgroundImageUri by remember { mutableStateOf<String?>(null) }
    var backgroundImageTransparencyPercent by remember { mutableIntStateOf(78) }
    var textColorOverride by remember { mutableStateOf<Color?>(null) }
    var appTheme by remember { mutableStateOf(if (systemPrefersDark) AppTheme.DEFAULT else AppTheme.LIGHT) }
    var accent by remember { mutableStateOf(AccentBurntOrange) }
    var appBackgroundColorOverride by remember { mutableStateOf<Color?>(null) }
    var chromeBackgroundColorOverride by remember { mutableStateOf<Color?>(null) }
    var cardColorOverride by remember { mutableStateOf<Color?>(null) }
    var buttonColorOverride by remember { mutableStateOf<Color?>(null) }
    var journalPageColorOverride by remember { mutableStateOf<Color?>(null) }
    var journalAccentColorOverride by remember { mutableStateOf<Color?>(null) }
    var journalName by remember { mutableStateOf("Journal") }
    val runtimeTheme = remember(appTheme, appBackgroundColorOverride) {
        appBackgroundColorOverride?.let {
            if (it.luminance() >= 0.56f) {
                AppTheme.LIGHT
            } else if (appTheme == AppTheme.CYBERPUNK) {
                AppTheme.CYBERPUNK
            } else {
                AppTheme.DEFAULT
            }
        } ?: appTheme
    }
    val baseThemeBg = ThemeEngine.getColors(runtimeTheme).second
    val themeBg = appBackgroundColorOverride ?: baseThemeBg
    val isThemeBgLight = remember(themeBg) { themeBg.luminance() >= 0.56f }
    val accentStrong = buttonColorOverride ?: accent
    val accentSoft = remember(accentStrong, themeBg) { mixForBackground(accentStrong, themeBg) }
    val defaultChromeBg = remember(themeBg, isThemeBgLight) {
        if (isThemeBgLight) {
            mixForBackground(Color(0xFFD2DEEE), themeBg).copy(alpha = 0.98f)
        } else {
            mixForBackground(Color(0xFF060A11), themeBg).copy(alpha = 0.97f)
        }
    }
    val drawerBg = remember(defaultChromeBg, chromeBackgroundColorOverride) {
        chromeBackgroundColorOverride ?: defaultChromeBg
    }
    val navBarBg = remember(drawerBg, chromeBackgroundColorOverride, isThemeBgLight, backgroundImageUri, backgroundImageTransparencyPercent) {
        val imageBlend = if (backgroundImageUri.isNullOrBlank()) 0f else (backgroundImageTransparencyPercent.coerceIn(0, 100) / 100f)
        val targetAlpha = (0.96f - (imageBlend * 0.18f)).coerceIn(0.74f, 0.96f)
        chromeBackgroundColorOverride ?: if (isThemeBgLight) {
            mixForBackground(Color(0xFFBDCCE3), drawerBg).copy(alpha = targetAlpha)
        } else {
            mixForBackground(Color(0xFF03060B), drawerBg).copy(alpha = targetAlpha)
        }
    }
    val drawerContentColor = remember(drawerBg) {
        if (drawerBg.luminance() >= 0.5f) Color(0xFF1B2430) else Color(0xFFE8EAF0)
    }
    val navContentColor = remember(navBarBg) {
        if (navBarBg.luminance() >= 0.5f) Color(0xFF1B2430) else Color(0xFFE8EAF0)
    }

    var showResetAll by remember { mutableStateOf(false) }
    var showRefreshDayConfirm by remember { mutableStateOf(false) }
    var showLevelUpDialog by remember { mutableStateOf(false) }
    var customTemplates by remember { mutableStateOf<List<CustomTemplate>>(emptyList()) }
    var pendingUncheckQuestId by remember { mutableStateOf<Int?>(null) }
    var showFocusTimer by remember { mutableStateOf(false) }
    var resetBackupBefore by remember { mutableStateOf(false) }
    var resetBackupName by remember { mutableStateOf("Pre-reset backup") }
    var unlockedAchievementIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingImportTemplate by remember { mutableStateOf<GameTemplate?>(null) }
    var savedTemplates by remember { mutableStateOf<List<GameTemplate>>(emptyList()) }
    var onboardingGoal by remember { mutableStateOf(OnboardingGoal.BALANCE) }
    var difficultyPreference by remember { mutableStateOf(DifficultyPreference.NORMAL) }
    var premiumUnlocked by remember { mutableStateOf(false) }
    var dailyQuestTarget by remember { mutableIntStateOf(5) }
    var settingsExpandedSection by rememberSaveable { mutableStateOf("gameplay") }
    var questsPreferredTab by rememberSaveable { mutableIntStateOf(0) }
    var cloudSyncEnabled by remember { mutableStateOf(true) }
    var cloudAccountEmail by remember { mutableStateOf("") }
    var cloudConnectedAccount by remember { mutableStateOf<GoogleSignInAccount?>(null) }
    var cloudLastSyncAt by remember { mutableLongStateOf(0L) }
    var cloudLastAutoAttemptAt by remember { mutableLongStateOf(0L) }
    var communityPosts by remember { mutableStateOf<List<CommunityPost>>(emptyList()) }
    var followedAuthorIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var mutedAuthorIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var blockedAuthorIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var myCommunityRatings by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var pendingCommunitySyncQueue by remember { mutableStateOf<List<CommunitySyncTask>>(emptyList()) }
    var communityRefreshInProgress by remember { mutableStateOf(false) }
    var communityUserId by remember { mutableStateOf("") }
    var communityUserName by remember { mutableStateOf("Player") }
    var lastCommunityPublishAt by remember { mutableLongStateOf(0L) }
    var promptApplyTemplate by remember { mutableStateOf<GameTemplate?>(null) }
    var importBackupBeforeApply by remember { mutableStateOf(true) }
    var importBackupName by remember { mutableStateOf("Backup") }
    var importClearExisting by remember { mutableStateOf(true) } // NEW: Option to wipe old quests
    var showIntroSplash by remember { mutableStateOf(true) }
    var showWelcomeSetup by remember { mutableStateOf(false) }
    var onboardingSkipIntroDefault by remember { mutableStateOf(false) }
    var shopTutorialSeen by remember { mutableStateOf(false) }
    var calendarTutorialSeen by remember { mutableStateOf(false) }
    var questsTutorialSeen by remember { mutableStateOf(false) }
    var shopHoldHintSeen by remember { mutableStateOf(false) }
    var showBackupImport by remember { mutableStateOf(false) }
    var backupImportPayload by remember { mutableStateOf("") }
    var remixPostPending by remember { mutableStateOf<CommunityPost?>(null) }
    var schemaDowngradeDetected by remember { mutableStateOf(false) }
    // --- Persistence Functions ---
    var activePackageIds by remember { mutableStateOf(setOf(REAL_DAILY_LIFE_PACKAGE_ID)) } // NEW
    val activePacksKey = stringPreferencesKey("active_packs") // NEW
    fun currentEpochDay(): Long = epochDayNowAtHour(dailyResetHour)
    fun isRealDailyLifeTemplateActive(): Boolean = activePackageIds.contains(REAL_DAILY_LIFE_PACKAGE_ID)
    fun daySeedForGeneration(day: Long): Long = if (isRealDailyLifeTemplateActive()) (day / 7L) else day
    fun secondSeedForGeneration(seconds: Long): Long = if (isRealDailyLifeTemplateActive()) (currentEpochDay() / 7L) else seconds
    fun normalizeTheme(theme: AppTheme): AppTheme = when (theme) {
        AppTheme.LIGHT -> AppTheme.LIGHT
        AppTheme.CYBERPUNK -> AppTheme.CYBERPUNK
        else -> AppTheme.DEFAULT
    }
    fun parseStoredTheme(themeName: String): AppTheme {
        return when (themeName.uppercase()) {
            "LIGHT", "LIGHT_SOFT", "LIGHT_WARM", "LIGHT_SKY", "LIGHT_MINT" -> AppTheme.LIGHT
            "CYBERPUNK", "CYPERPUNK", "CYBERPUNK_NEON", "NEON_CYBERPUNK" -> AppTheme.CYBERPUNK
            else -> AppTheme.DEFAULT
        }
    }
    fun fallbackAccentForTheme(theme: AppTheme): Color {
        return ThemeEngine.getColors(theme).first
    }

    fun currentTemplateSettings(): TemplateSettings {
        return TemplateSettings(
            autoNewDay = autoNewDay,
            confirmComplete = confirmComplete,
            refreshIncompleteOnly = refreshIncompleteOnly,
            customMode = customMode,
            advancedOptions = advancedOptions,
            highContrastText = highContrastText,
            compactMode = compactMode,
            largerTouchTargets = largerTouchTargets,
            reduceAnimations = reduceAnimations,
            decorativeBorders = neonFlowEnabled,
            neonLightBoost = neonLightBoost,
            neonFlowEnabled = neonFlowEnabled,
            neonFlowSpeed = neonFlowSpeed.coerceIn(0, 2),
            neonGlowPalette = neonGlowPalette,
            alwaysShowQuestProgress = alwaysShowQuestProgress,
            hideCompletedQuests = hideCompletedQuests,
            confirmDestructiveActions = confirmDestructiveActions,
            dailyResetHour = dailyResetHour,
            dailyRemindersEnabled = dailyRemindersEnabled,
            hapticsEnabled = hapticsEnabled,
            soundEffectsEnabled = soundEffectsEnabled,
            fontStyle = fontStyle,
            fontScalePercent = fontScalePercent,
            backgroundImageUri = backgroundImageUri,
            backgroundImageTransparencyPercent = backgroundImageTransparencyPercent.coerceIn(0, 100),
            textColorArgb = textColorOverride?.toArgbCompat()?.toLong(),
            appBackgroundArgb = appBackgroundColorOverride?.toArgbCompat()?.toLong(),
            chromeBackgroundArgb = chromeBackgroundColorOverride?.toArgbCompat()?.toLong(),
            cardColorArgb = cardColorOverride?.toArgbCompat()?.toLong(),
            buttonColorArgb = buttonColorOverride?.toArgbCompat()?.toLong(),
            journalPageColorArgb = journalPageColorOverride?.toArgbCompat()?.toLong(),
            journalAccentColorArgb = journalAccentColorOverride?.toArgbCompat()?.toLong(),
            journalName = journalName
        )
    }
    fun applyTemplateDailyQuestDefaults(packageId: String, clearExisting: Boolean = true) {
        if (!clearExisting) return
        if (packageId == REAL_DAILY_LIFE_PACKAGE_ID) {
            dailyQuestTarget = 5
        }
    }

    fun applyTemplateSettings(settings: TemplateSettings?) {
        if (settings == null) return
        autoNewDay = settings.autoNewDay
        confirmComplete = settings.confirmComplete
        refreshIncompleteOnly = settings.refreshIncompleteOnly
        customMode = settings.customMode
        advancedOptions = settings.advancedOptions
        highContrastText = settings.highContrastText
        compactMode = settings.compactMode
        largerTouchTargets = settings.largerTouchTargets
        reduceAnimations = settings.reduceAnimations
        val neonEnabledFromTemplate = settings.neonFlowEnabled || settings.decorativeBorders
        decorativeBorders = neonEnabledFromTemplate
        neonLightBoost = settings.neonLightBoost
        neonFlowEnabled = neonEnabledFromTemplate
        neonFlowSpeed = settings.neonFlowSpeed.coerceIn(0, 2)
        neonGlowPalette = settings.neonGlowPalette.ifBlank { "magenta" }
        alwaysShowQuestProgress = settings.alwaysShowQuestProgress
        hideCompletedQuests = settings.hideCompletedQuests
        confirmDestructiveActions = true
        dailyResetHour = settings.dailyResetHour.coerceIn(0, 23)
        dailyRemindersEnabled = settings.dailyRemindersEnabled
        hapticsEnabled = settings.hapticsEnabled
        soundEffectsEnabled = settings.soundEffectsEnabled
        fontStyle = settings.fontStyle
        fontScalePercent = settings.fontScalePercent.coerceIn(80, 125)
        backgroundImageUri = settings.backgroundImageUri
        backgroundImageTransparencyPercent = (settings.backgroundImageTransparencyPercent ?: backgroundImageTransparencyPercent).coerceIn(0, 100)
        textColorOverride = settings.textColorArgb?.let { Color(it.toInt()) }
        appBackgroundColorOverride = settings.appBackgroundArgb?.let { Color(it.toInt()) }
        chromeBackgroundColorOverride = settings.chromeBackgroundArgb?.let { Color(it.toInt()) }
        cardColorOverride = settings.cardColorArgb?.let { Color(it.toInt()) }
        buttonColorOverride = settings.buttonColorArgb?.let { Color(it.toInt()) }
        journalPageColorOverride = settings.journalPageColorArgb?.let { Color(it.toInt()) }
        journalAccentColorOverride = settings.journalAccentColorArgb?.let { Color(it.toInt()) }
        journalName = settings.journalName.ifBlank { "Journal" }
    }

    fun persistAttributes(attrs: PlayerAttributes) {
        attributes = attrs
        val raw = "${attrs.str}|${attrs.int}|${attrs.vit}|${attrs.end}|${attrs.fth}"
        scope.launch { appContext.dataStore.edit { p -> p[Keys.ATTRIBUTES_RAW] = raw } }
    }

    fun persistBosses(list: List<Boss>) { // FIXED: Added this back
        bosses = list
        scope.launch { appContext.dataStore.edit { p -> p[Keys.BOSSES] = serializeBosses(list) } }
    }

    fun persistInventory(items: List<InventoryItem>) {
        inventory = items
        scope.launch { appContext.dataStore.edit { p -> p[Keys.INVENTORY] = serializeInventory(items) } }
    }

    fun persistShopItems(items: List<ShopItem>) {
        shopItems = items
        scope.launch { appContext.dataStore.edit { p -> p[Keys.SHOP_ITEMS] = serializeShopItems(items) } }
    }

    fun persistCalendarPlans(plans: Map<Long, List<String>>) {
        calendarPlans = plans
        scope.launch { appContext.dataStore.edit { p -> p[Keys.CALENDAR_PLANS] = serializeCalendarPlans(plans) } }
    }
    fun pushPlanStateToSupabase(plans: Map<Long, List<String>>) {
        if (!SupabaseApi.isConfigured || communityUserId.isBlank()) return
        scope.launch {
            SupabaseApi.upsertPlanState(
                userId = communityUserId,
                userName = communityUserName,
                plans = plans
            )
        }
    }
    fun pushHistoryStateToSupabase(day: Long, done: Int, total: Int, allDone: Boolean) {
        if (!SupabaseApi.isConfigured || communityUserId.isBlank()) return
        scope.launch {
            SupabaseApi.upsertHistoryState(
                userId = communityUserId,
                userName = communityUserName,
                day = day,
                done = done,
                total = total,
                allDone = allDone
            )
        }
    }

    fun persistCharacter(data: CharacterData) {
        characterData = data
        scope.launch {
            appContext.dataStore.edit { p ->
                p[Keys.CHAR_HEAD] = data.headColor
                p[Keys.CHAR_BODY] = data.bodyColor
                p[Keys.CHAR_LEGS] = data.legsColor
                p[Keys.CHAR_SHOES] = data.shoesColor
            }
        }
    }

    fun persistMainQuests(list: List<CustomMainQuest>) {
        mainQuests = list
        scope.launch { appContext.dataStore.edit { p -> p[Keys.MAIN_QUESTS] = serializeMainQuests(list) } }
    }

    fun persistSavedTemplates(list: List<GameTemplate>) {
        savedTemplates = list
        scope.launch { appContext.dataStore.edit { p -> p[Keys.SAVED_TEMPLATES] = serializeSavedTemplates(list) } }
    }

    fun persistCommunityPosts(list: List<CommunityPost>) {
        communityPosts = list
        scope.launch { appContext.dataStore.edit { p -> p[Keys.COMMUNITY_POSTS] = serializeCommunityPosts(list) } }
    }

    fun persistCommunityFollows(list: Set<String>) {
        followedAuthorIds = list
        scope.launch { appContext.dataStore.edit { p -> p[Keys.COMMUNITY_FOLLOWS] = serializeStringSet(list) } }
    }

    fun persistMutedAuthors(list: Set<String>) {
        mutedAuthorIds = list
        scope.launch { appContext.dataStore.edit { p -> p[Keys.COMMUNITY_MUTED_AUTHORS] = serializeStringSet(list) } }
    }

    fun persistBlockedAuthors(list: Set<String>) {
        blockedAuthorIds = list
        scope.launch { appContext.dataStore.edit { p -> p[Keys.COMMUNITY_BLOCKED_AUTHORS] = serializeStringSet(list) } }
    }

    fun persistCommunityRatings(map: Map<String, Int>) {
        myCommunityRatings = map
        scope.launch { appContext.dataStore.edit { p -> p[Keys.COMMUNITY_MY_RATINGS] = serializeRatingsMap(map) } }
    }

    fun persistCommunitySyncQueue(list: List<CommunitySyncTask>) {
        val normalized = normalizeCommunitySyncQueue(list)
        pendingCommunitySyncQueue = normalized
        scope.launch {
            appContext.dataStore.edit { p -> p[Keys.COMMUNITY_SYNC_QUEUE] = serializeCommunitySyncQueue(normalized) }
            if (normalized.isNotEmpty()) CommunitySyncWorker.enqueue(appContext)
        }
    }

    fun enqueueCommunitySyncTask(task: CommunitySyncTask) {
        persistCommunitySyncQueue(pendingCommunitySyncQueue + task.copy(attemptCount = 0))
    }

    suspend fun flushCommunitySyncQueue() {
        if (!SupabaseApi.isConfigured || communityUserId.isBlank()) return
        if (pendingCommunitySyncQueue.isEmpty()) return
        val remaining = mutableListOf<CommunitySyncTask>()
        pendingCommunitySyncQueue.forEach { original ->
            val task = original.copy(stars = original.stars?.coerceIn(1, 5))
            val ok = when (task.type) {
                CommunitySyncTaskType.PUBLISH_POST -> task.post?.let { SupabaseApi.publishPost(it) } ?: true
                CommunitySyncTaskType.FOLLOW_AUTHOR -> task.authorId?.let { SupabaseApi.upsertFollow(communityUserId, it) } ?: true
                CommunitySyncTaskType.UNFOLLOW_AUTHOR -> task.authorId?.let { SupabaseApi.deleteFollow(communityUserId, it) } ?: true
                CommunitySyncTaskType.RATE_POST -> {
                    val postId = task.postId
                    val stars = task.stars
                    if (postId != null && stars != null) SupabaseApi.upsertRatingAndRefreshAggregate(communityUserId, postId, stars) else true
                }
                CommunitySyncTaskType.INCREMENT_REMIX -> {
                    val postId = task.postId
                    val remixCount = task.currentRemixCount
                    if (postId != null && remixCount != null) SupabaseApi.incrementRemix(postId, remixCount) else true
                }
            }
            if (!ok) {
                val nextAttempt = task.attemptCount + 1
                if (nextAttempt <= 5) remaining += task.copy(attemptCount = nextAttempt)
            }
        }
        persistCommunitySyncQueue(remaining)
    }

    fun persistCommunityProfile(userId: String, userName: String) {
        val fixedName = sanitizeDisplayName(userName)
        communityUserId = userId
        communityUserName = fixedName
        scope.launch {
            appContext.dataStore.edit { p ->
                p[Keys.COMMUNITY_USER_ID] = userId
                p[Keys.COMMUNITY_USER_NAME] = fixedName
            }
        }
    }

    suspend fun syncCommunityFromRemote() {
        if (!SupabaseApi.isConfigured || communityUserId.isBlank()) return
        flushCommunitySyncQueue()
        val remotePosts = SupabaseApi.fetchPosts()
            .filterNot { blockedAuthorIds.contains(it.authorId) || mutedAuthorIds.contains(it.authorId) }
            .map { post ->
                val metadataSuspicious =
                    hasSuspiciousCommunityContent(post.title) ||
                        hasSuspiciousCommunityContent(post.description) ||
                        post.tags.any { tag -> hasSuspiciousCommunityContent(tag) }
                val (safeTemplate, templateTrust) = evaluateTemplateTrust(post.template)
                val finalTrust = when {
                    metadataSuspicious -> TemplateTrustLevel.FLAGGED
                    else -> templateTrust
                }
                post.copy(
                    title = sanitizeCommunityText(post.title, 60),
                    description = sanitizeCommunityText(post.description, 280),
                    tags = sanitizeTags(post.tags),
                    template = safeTemplate,
                    templateTrust = finalTrust
                )
            }
        if (remotePosts.isNotEmpty() || communityPosts.isEmpty()) persistCommunityPosts(remotePosts)

        val remoteFollows = SupabaseApi.fetchFollows(communityUserId)
        if (remoteFollows.isNotEmpty() || followedAuthorIds.isEmpty()) persistCommunityFollows(remoteFollows)

        val remoteRatings = SupabaseApi.fetchRatings(communityUserId)
        if (remoteRatings.isNotEmpty() || myCommunityRatings.isEmpty()) persistCommunityRatings(remoteRatings)
    }

    fun triggerCommunityRefresh() {
        if (communityRefreshInProgress) return
        scope.launch {
            val startedAt = System.currentTimeMillis()
            communityRefreshInProgress = true
            val failureMessage = runCatching { syncCommunityFromRemote() }
                .exceptionOrNull()
                ?.let { "Refresh failed. Using local data." }
            val elapsed = System.currentTimeMillis() - startedAt
            val minVisibleMs = 900L
            if (elapsed < minVisibleMs) delay(minVisibleMs - elapsed)
            communityRefreshInProgress = false
            if (failureMessage != null) {
                snackbarHostState.showSnackbar(failureMessage)
            }
        }
    }

    fun exportBackupPayload(): String {
        val dump = mutableMapOf<String, String>()
        dump["totalXp"] = totalXp.toString()
        dump["gold"] = gold.toString()
        dump["streak"] = streak.toString()
        dump["bestStreak"] = bestStreak.toString()
        dump["quests"] = serializeQuests(quests)
        dump["completed"] = quests.filter { it.completed }.joinToString(",") { it.id.toString() }
        dump["earned"] = earnedIds.joinToString(",")
        dump["lastDay"] = lastDayEpoch.toString()
        dump["refreshCount"] = refreshCount.toString()
        dump["mainQuests"] = serializeMainQuests(mainQuests)
        dump["customTemplates"] = serializeCustomTemplates(customTemplates)
        dump["inventory"] = serializeInventory(inventory)
        dump["shopItems"] = serializeShopItems(shopItems)
        dump["calendarPlans"] = serializeCalendarPlans(calendarPlans)
        dump["history"] = serializeHistory(historyMap)
        dump["achievements"] = unlockedAchievementIds.joinToString(";;") { "$it|1" }
        dump["settings_theme"] = appTheme.name
        dump["settings_accent"] = accent.toArgbCompat().toString()
        dump["settings_bg"] = backgroundImageUri.orEmpty()
        dump["settings_bgTransparency"] = backgroundImageTransparencyPercent.toString()
        dump["settings_bgColor"] = appBackgroundColorOverride?.toArgbCompat()?.toString().orEmpty()
        dump["settings_chromeColor"] = chromeBackgroundColorOverride?.toArgbCompat()?.toString().orEmpty()
        dump["settings_cardColor"] = cardColorOverride?.toArgbCompat()?.toString().orEmpty()
        dump["settings_buttonColor"] = buttonColorOverride?.toArgbCompat()?.toString().orEmpty()
        dump["settings_journalPageColor"] = journalPageColorOverride?.toArgbCompat()?.toString().orEmpty()
        dump["settings_journalAccentColor"] = journalAccentColorOverride?.toArgbCompat()?.toString().orEmpty()
        dump["settings_journalName"] = journalName
        dump["settings_fontStyle"] = fontStyle.name
        dump["settings_fontScale"] = fontScalePercent.toString()
        dump["settings_decorativeBorders"] = neonFlowEnabled.toString()
        dump["settings_neonLightBoost"] = neonLightBoost.toString()
        dump["settings_neonFlowEnabled"] = neonFlowEnabled.toString()
        dump["settings_neonFlowSpeed"] = neonFlowSpeed.toString()
        dump["settings_neonGlowPalette"] = neonGlowPalette
        dump["settings_goal"] = onboardingGoal.name
        dump["settings_diff"] = difficultyPreference.name
        dump["settings_premium"] = premiumUnlocked.toString()
        dump["settings_dailyQuestTarget"] = dailyQuestTarget.toString()
        dump["settings_cloudEnabled"] = cloudSyncEnabled.toString()
        dump["settings_cloudEmail"] = cloudAccountEmail
        dump["settings_cloudLastSync"] = cloudLastSyncAt.toString()
        dump["dailyResetHour"] = dailyResetHour.toString()
        dump["dailyRemindersEnabled"] = dailyRemindersEnabled.toString()
        dump["communityPosts"] = serializeCommunityPosts(communityPosts)
        dump["communityFollows"] = serializeStringSet(followedAuthorIds)
        dump["communityRatings"] = serializeRatingsMap(myCommunityRatings)
        dump["communityQueue"] = serializeCommunitySyncQueue(pendingCommunitySyncQueue)
        dump["communityName"] = communityUserName
        dump["communityUserId"] = communityUserId
        val payload = FullBackupPayload(dataStoreDump = dump)
        return exportFullBackupEncrypted(payload, appContext.packageName)
    }

    fun importBackupPayload(payload: String): Boolean {
        val parsed = importFullBackupEncrypted(payload.trim(), appContext.packageName) ?: return false
        val dump = parsed.dataStoreDump
        fun getInt(key: String, default: Int) = dump[key]?.toIntOrNull() ?: default
        fun getBool(key: String, default: Boolean) = dump[key]?.toBooleanStrictOrNull() ?: default
        totalXp = getInt("totalXp", totalXp)
        gold = getInt("gold", gold)
        streak = getInt("streak", streak)
        bestStreak = getInt("bestStreak", bestStreak)
        val importedCompleted = parseIds(dump["completed"])
        quests = deserializeQuests(dump["quests"].orEmpty()).map { q ->
            if (importedCompleted.contains(q.id)) q.copy(completed = true) else q
        }
        val importedEarned = parseIds(dump["earned"])
        earnedIds = if (importedEarned.isNotEmpty()) importedEarned else importedCompleted
        lastDayEpoch = dump["lastDay"]?.toLongOrNull() ?: currentEpochDay()
        refreshCount = getInt("refreshCount", refreshCount).coerceIn(0, 3)
        mainQuests = deserializeMainQuests(dump["mainQuests"].orEmpty())
        customTemplates = deserializeCustomTemplates(dump["customTemplates"].orEmpty())
        inventory = deserializeInventory(dump["inventory"].orEmpty())
        shopItems = deserializeShopItems(dump["shopItems"])
        calendarPlans = deserializeCalendarPlans(dump["calendarPlans"])
        unlockedAchievementIds = parseUnlockedAchievements(dump["achievements"].orEmpty())
        appTheme = runCatching { AppTheme.valueOf(dump["settings_theme"].orEmpty()) }.getOrDefault(appTheme)
        dump["settings_accent"]?.toIntOrNull()?.let { accent = Color(it) }
        backgroundImageUri = dump["settings_bg"].orEmpty().ifBlank { null }
        backgroundImageTransparencyPercent = getInt("settings_bgTransparency", backgroundImageTransparencyPercent).coerceIn(0, 100)
        appBackgroundColorOverride = dump["settings_bgColor"]?.toIntOrNull()?.let { Color(it) }
        chromeBackgroundColorOverride = dump["settings_chromeColor"]?.toIntOrNull()?.let { Color(it) }
        cardColorOverride = dump["settings_cardColor"]?.toIntOrNull()?.let { Color(it) }
        buttonColorOverride = dump["settings_buttonColor"]?.toIntOrNull()?.let { Color(it) }
        journalPageColorOverride = dump["settings_journalPageColor"]?.toIntOrNull()?.let { Color(it) }
        journalAccentColorOverride = dump["settings_journalAccentColor"]?.toIntOrNull()?.let { Color(it) }
        journalName = dump["settings_journalName"].orEmpty().ifBlank { journalName }
        fontStyle = runCatching { AppFontStyle.valueOf(dump["settings_fontStyle"].orEmpty()) }.getOrDefault(fontStyle)
        fontScalePercent = getInt("settings_fontScale", fontScalePercent).coerceIn(80, 125)
        val importedDecorative = getBool("settings_decorativeBorders", decorativeBorders)
        neonLightBoost = getBool("settings_neonLightBoost", neonLightBoost)
        neonFlowEnabled = getBool("settings_neonFlowEnabled", importedDecorative || neonFlowEnabled) || importedDecorative
        decorativeBorders = neonFlowEnabled
        neonFlowSpeed = getInt("settings_neonFlowSpeed", neonFlowSpeed).coerceIn(0, 2)
        neonGlowPalette = dump["settings_neonGlowPalette"].orEmpty().ifBlank { neonGlowPalette }
        onboardingGoal = runCatching { OnboardingGoal.valueOf(dump["settings_goal"].orEmpty()) }.getOrDefault(onboardingGoal)
        difficultyPreference = runCatching { DifficultyPreference.valueOf(dump["settings_diff"].orEmpty()) }.getOrDefault(difficultyPreference)
        premiumUnlocked = getBool("settings_premium", premiumUnlocked)
        dailyQuestTarget = getInt("settings_dailyQuestTarget", dailyQuestTarget).coerceIn(3, 10)
        cloudSyncEnabled = getBool("settings_cloudEnabled", cloudSyncEnabled)
        cloudAccountEmail = dump["settings_cloudEmail"].orEmpty()
        cloudLastSyncAt = dump["settings_cloudLastSync"]?.toLongOrNull() ?: cloudLastSyncAt
        dailyResetHour = getInt("dailyResetHour", dailyResetHour).coerceIn(0, 23)
        dailyRemindersEnabled = getBool("dailyRemindersEnabled", dailyRemindersEnabled)
        communityPosts = deserializeCommunityPosts(dump["communityPosts"])
        followedAuthorIds = deserializeStringSet(dump["communityFollows"])
        myCommunityRatings = deserializeRatingsMap(dump["communityRatings"])
        pendingCommunitySyncQueue = deserializeCommunitySyncQueue(dump["communityQueue"])
        communityUserName = sanitizeDisplayName(dump["communityName"].orEmpty().ifBlank { communityUserName })
        communityUserId = secureCommunityUserId(dump["communityUserId"].orEmpty().ifBlank { communityUserId })
        val importedHistory = dump["history"]?.let { serializeHistory(parseHistory(it)) }
        currentLevel = calculateLevel(totalXp).level
        scope.launch {
            appContext.dataStore.edit { p ->
                p[Keys.TOTAL_XP] = totalXp
                p[Keys.GOLD] = gold
                p[Keys.STREAK] = streak
                p[Keys.BEST_STREAK] = bestStreak
                p[Keys.LAST_DAY] = lastDayEpoch
                p[Keys.REFRESH_COUNT] = refreshCount
                p[Keys.QUESTS] = serializeQuests(quests.map { it.copy(completed = false) })
                p[Keys.COMPLETED] = quests.filter { it.completed }.joinToString(",") { it.id.toString() }
                p[Keys.EARNED] = earnedIds.joinToString(",")
                p[Keys.MAIN_QUESTS] = serializeMainQuests(mainQuests)
                p[Keys.CUSTOM_TEMPLATES] = serializeCustomTemplates(customTemplates)
                p[Keys.INVENTORY] = serializeInventory(inventory)
                p[Keys.SHOP_ITEMS] = serializeShopItems(shopItems)
                p[Keys.CALENDAR_PLANS] = serializeCalendarPlans(calendarPlans)
                p[Keys.APP_THEME] = appTheme.name
                p[Keys.ACCENT_ARGB] = accent.toArgbCompat()
                p[Keys.FONT_STYLE] = fontStyle.name
                p[Keys.FONT_SCALE_PERCENT] = fontScalePercent
                p[Keys.DECORATIVE_BORDERS] = neonFlowEnabled
                p[Keys.NEON_LIGHT_BOOST] = neonLightBoost
                p[Keys.NEON_FLOW_ENABLED] = neonFlowEnabled
                p[Keys.NEON_FLOW_SPEED] = neonFlowSpeed.coerceIn(0, 2)
                p[Keys.NEON_GLOW_PALETTE] = neonGlowPalette
                p[Keys.BACKGROUND_IMAGE_URI] = backgroundImageUri.orEmpty()
                p[Keys.BACKGROUND_IMAGE_TRANSPARENCY_PERCENT] = backgroundImageTransparencyPercent.coerceIn(0, 100)
                appBackgroundColorOverride?.let { p[Keys.APP_BACKGROUND_ARGB] = it.toArgbCompat() } ?: p.remove(Keys.APP_BACKGROUND_ARGB)
                chromeBackgroundColorOverride?.let { p[Keys.CHROME_BACKGROUND_ARGB] = it.toArgbCompat() } ?: p.remove(Keys.CHROME_BACKGROUND_ARGB)
                cardColorOverride?.let { p[Keys.CARD_COLOR_ARGB] = it.toArgbCompat() } ?: p.remove(Keys.CARD_COLOR_ARGB)
                buttonColorOverride?.let { p[Keys.BUTTON_COLOR_ARGB] = it.toArgbCompat() } ?: p.remove(Keys.BUTTON_COLOR_ARGB)
                journalPageColorOverride?.let { p[Keys.JOURNAL_PAGE_COLOR_ARGB] = it.toArgbCompat() } ?: p.remove(Keys.JOURNAL_PAGE_COLOR_ARGB)
                journalAccentColorOverride?.let { p[Keys.JOURNAL_ACCENT_COLOR_ARGB] = it.toArgbCompat() } ?: p.remove(Keys.JOURNAL_ACCENT_COLOR_ARGB)
                p[Keys.JOURNAL_NAME] = journalName
                p[Keys.ONBOARDING_GOAL] = onboardingGoal.name
                p[Keys.ONBOARDING_DIFFICULTY] = difficultyPreference.name
                p[Keys.PREMIUM_UNLOCKED] = premiumUnlocked
                p[Keys.DAILY_QUEST_TARGET] = dailyQuestTarget
                p[Keys.CLOUD_SYNC_ENABLED] = cloudSyncEnabled
                p[Keys.CLOUD_ACCOUNT_EMAIL] = cloudAccountEmail
                p[Keys.CLOUD_LAST_SYNC_AT] = cloudLastSyncAt
                p[Keys.DAILY_RESET_HOUR] = dailyResetHour
                p[Keys.DAILY_REMINDERS_ENABLED] = dailyRemindersEnabled
                p[Keys.COMMUNITY_POSTS] = serializeCommunityPosts(communityPosts)
                p[Keys.COMMUNITY_FOLLOWS] = serializeStringSet(followedAuthorIds)
                p[Keys.COMMUNITY_MY_RATINGS] = serializeRatingsMap(myCommunityRatings)
                p[Keys.COMMUNITY_SYNC_QUEUE] = serializeCommunitySyncQueue(pendingCommunitySyncQueue)
                p[Keys.COMMUNITY_USER_NAME] = communityUserName
                p[Keys.COMMUNITY_USER_ID] = communityUserId
                p[Keys.ACHIEVEMENTS] = unlockedAchievementIds.joinToString(";;") { "$it|1" }
                importedHistory?.let { p[Keys.HISTORY] = it }
            }
        }
        return true
    }

    fun triggerCloudSnapshotSync(force: Boolean = false) {
        if (!cloudSyncEnabled) return
        if (cloudConnectedAccount == null) {
            if (force) {
                scope.launch { snackbarHostState.showSnackbar("Connect Google Drive first.") }
            }
            return
        }
        val now = System.currentTimeMillis()
        if (!force && now - cloudLastAutoAttemptAt < 120_000L) return
        cloudLastAutoAttemptAt = now
        val snapshot = exportBackupPayload()
        if (snapshot.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("Cloud sync failed: backup empty.") }
            return
        }
        scope.launch {
            val ok = GoogleDriveSync.uploadBackup(appContext, snapshot)
            if (ok) {
                cloudLastSyncAt = System.currentTimeMillis()
                cloudAccountEmail = cloudConnectedAccount?.email.orEmpty()
                appContext.dataStore.edit { p ->
                    p[Keys.CLOUD_LAST_SNAPSHOT] = snapshot
                    p[Keys.CLOUD_ACCOUNT_EMAIL] = cloudAccountEmail
                    p[Keys.CLOUD_LAST_SYNC_AT] = cloudLastSyncAt
                }
                snackbarHostState.showSnackbar("Cloud backup synced.")
            } else {
                snackbarHostState.showSnackbar("Cloud sync failed. Check Google account/permission.")
            }
        }
    }

    fun restoreFromCloud() {
        if (!cloudSyncEnabled || cloudConnectedAccount == null) {
            scope.launch { snackbarHostState.showSnackbar("Connect Google Drive first.") }
            return
        }
        scope.launch {
            val payload = GoogleDriveSync.downloadBackup(appContext)
            if (payload.isNullOrBlank()) {
                snackbarHostState.showSnackbar("No cloud backup found for this account.")
                return@launch
            }
            val ok = importBackupPayload(payload)
            if (ok) {
                cloudLastSyncAt = System.currentTimeMillis()
                appContext.dataStore.edit { p -> p[Keys.CLOUD_LAST_SYNC_AT] = cloudLastSyncAt }
                snackbarHostState.showSnackbar("Cloud backup restored.")
            } else {
                snackbarHostState.showSnackbar("Cloud backup restore failed.")
            }
        }
    }

    fun disconnectCloudAccount() {
        cloudSyncEnabled = false
        cloudConnectedAccount = null
        cloudAccountEmail = ""
        GoogleDriveSync.signOut(appContext)
        scope.launch {
            appContext.dataStore.edit { p ->
                p[Keys.CLOUD_SYNC_ENABLED] = false
                p[Keys.CLOUD_ACCOUNT_EMAIL] = ""
            }
        }
    }

    fun onGoogleAccountConnected(account: GoogleSignInAccount) {
        cloudConnectedAccount = account
        cloudSyncEnabled = true
        cloudAccountEmail = account.email.orEmpty()
        scope.launch {
            appContext.dataStore.edit { p ->
                p[Keys.CLOUD_SYNC_ENABLED] = true
                p[Keys.CLOUD_ACCOUNT_EMAIL] = cloudAccountEmail
            }
            snackbarHostState.showSnackbar("Google cloud connected.")
        }
    }

    fun shareFeedbackReport(category: String = "General", message: String = "") {
        val report = buildString {
            appendLine("Questify Feedback")
            appendLine("User: $communityUserName ($communityUserId)")
            appendLine("Category: $category")
            appendLine("Level: $currentLevel")
            appendLine("Theme: ${appTheme.name}")
            appendLine("Premium: $premiumUnlocked")
            appendLine("Cloud sync: $cloudSyncEnabled")
            appendLine("Queue pending: ${pendingCommunitySyncQueue.size}")
            if (message.isNotBlank()) {
                appendLine()
                appendLine("Message:")
                appendLine(message)
            }
            appendLine()
            appendLine("Recent Logs:")
            appendLine(AppLog.exportRecentLogs().ifBlank { "No logs captured." })
        }
        scope.launch {
            val pushed = SupabaseApi.submitFeedbackInbox(
                userId = communityUserId.ifBlank { UUID.randomUUID().toString() },
                userName = communityUserName.ifBlank { "Player" },
                category = category,
                message = message.ifBlank { report },
                appTheme = appTheme.name,
                level = currentLevel
            )
            if (pushed) {
                snackbarHostState.showSnackbar("Feedback sent.")
            } else {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, report)
                    type = "text/plain"
                }
                appContext.startActivity(Intent.createChooser(intent, "Send Feedback"))
            }
        }
    }


    fun checkAchievements() {
        val currentUnlocked = unlockedAchievementIds.toMutableSet()
        var newlyUnlocked = false
        val todayEntry = historyMap[currentEpochDay()]
        val cal = java.util.Calendar.getInstance()
        val isWinter = cal.get(java.util.Calendar.MONTH) in listOf(java.util.Calendar.DECEMBER, java.util.Calendar.JANUARY, java.util.Calendar.FEBRUARY)
        if (earnedIds.isNotEmpty() && !currentUnlocked.contains("first_quest")) { currentUnlocked.add("first_quest"); newlyUnlocked = true }
        if (currentLevel >= 5 && !currentUnlocked.contains("level_5")) { currentUnlocked.add("level_5"); newlyUnlocked = true }
        if (journalPages.any { it.text.isNotBlank() } && !currentUnlocked.contains("journal_entry")) { currentUnlocked.add("journal_entry"); newlyUnlocked = true }
        if (streak >= 7 && !currentUnlocked.contains("streak_7")) { currentUnlocked.add("streak_7"); newlyUnlocked = true }
        if (streak >= 30 && !currentUnlocked.contains("streak_30")) { currentUnlocked.add("streak_30"); newlyUnlocked = true }
        if (todayEntry?.allDone == true && !currentUnlocked.contains("all_daily")) { currentUnlocked.add("all_daily"); newlyUnlocked = true }
        if (gold >= 1000 && !currentUnlocked.contains("wealthy")) { currentUnlocked.add("wealthy"); newlyUnlocked = true }
        if (appTheme == AppTheme.CYBERPUNK && !currentUnlocked.contains("dark_soul")) { currentUnlocked.add("dark_soul"); newlyUnlocked = true }
        if (currentLevel >= 10 && !currentUnlocked.contains("focus_master")) { currentUnlocked.add("focus_master"); newlyUnlocked = true }
        if (communityPosts.any { it.authorId == communityUserId } && !currentUnlocked.contains("community_builder")) { currentUnlocked.add("community_builder"); newlyUnlocked = true }
        if (isWinter && todayEntry?.allDone == true && !currentUnlocked.contains("season_winter")) { currentUnlocked.add("season_winter"); newlyUnlocked = true }
        if ((todayEntry?.done ?: 0) >= 10 && !currentUnlocked.contains("secret_speedrun")) { currentUnlocked.add("secret_speedrun"); newlyUnlocked = true }
        if (newlyUnlocked) {
            unlockedAchievementIds = currentUnlocked
            SoundManager.playSuccess()
            scope.launch { snackbarHostState.showSnackbar("New Achievement Unlocked!") }
            scope.launch { appContext.dataStore.edit { p -> p[Keys.ACHIEVEMENTS] = currentUnlocked.joinToString(";;") { "$it|1" } } }
        }
    }

    fun persistCore() {
        val info = calculateLevel(totalXp)
        if (info.level > currentLevel) { currentLevel = info.level; showLevelUpDialog = true; SoundManager.playLevelUp() }
        scope.launch {
            appContext.dataStore.edit { p ->
                p[Keys.TOTAL_XP] = totalXp
                p[Keys.GOLD] = gold
                p[Keys.STREAK] = streak
                p[Keys.BEST_STREAK] = bestStreak
            }
        }
        checkAchievements()
        triggerCloudSnapshotSync(force = false)
    }

    fun persistSettings() {
        scope.launch {
            appContext.dataStore.edit { p ->
                p[Keys.AUTO_NEW_DAY] = autoNewDay
                p[Keys.CONFIRM_COMPLETE] = confirmComplete
                p[Keys.REFRESH_INCOMPLETE_ONLY] = refreshIncompleteOnly
                p[Keys.ADMIN_MODE] = customMode
                p[Keys.APP_THEME] = appTheme.name
                p[Keys.ACCENT_ARGB] = accent.toArgbCompat()
                p[Keys.ADVANCED_OPTIONS] = advancedOptions
                p[Keys.HIGH_CONTRAST_TEXT] = highContrastText
                p[Keys.COMPACT_MODE] = compactMode
                p[Keys.LARGE_TOUCH_TARGETS] = largerTouchTargets
                p[Keys.REDUCE_ANIMATIONS] = reduceAnimations
                p[Keys.DECORATIVE_BORDERS] = neonFlowEnabled
                p[Keys.NEON_LIGHT_BOOST] = neonLightBoost
                p[Keys.NEON_FLOW_ENABLED] = neonFlowEnabled
                p[Keys.NEON_FLOW_SPEED] = neonFlowSpeed.coerceIn(0, 2)
                p[Keys.NEON_GLOW_PALETTE] = neonGlowPalette
                p[Keys.ALWAYS_SHOW_QUEST_PROGRESS] = alwaysShowQuestProgress
                p[Keys.HIDE_COMPLETED_QUESTS] = hideCompletedQuests
                p[Keys.CONFIRM_DESTRUCTIVE] = confirmDestructiveActions
                p[Keys.DAILY_RESET_HOUR] = dailyResetHour.coerceIn(0, 23)
                p[Keys.DAILY_REMINDERS_ENABLED] = dailyRemindersEnabled
                p[Keys.HAPTICS] = hapticsEnabled
                p[Keys.SOUND_EFFECTS] = soundEffectsEnabled
                p[Keys.ONBOARDING_GOAL] = onboardingGoal.name
                p[Keys.ONBOARDING_DIFFICULTY] = difficultyPreference.name
                p[Keys.PREMIUM_UNLOCKED] = premiumUnlocked
                p[Keys.DAILY_QUEST_TARGET] = dailyQuestTarget.coerceIn(3, 10)
                p[Keys.CLOUD_SYNC_ENABLED] = cloudSyncEnabled
                p[Keys.CLOUD_ACCOUNT_EMAIL] = cloudAccountEmail
                p[Keys.CLOUD_LAST_SYNC_AT] = cloudLastSyncAt
                p[Keys.FONT_STYLE] = fontStyle.name
                p[Keys.FONT_SCALE_PERCENT] = fontScalePercent.coerceIn(80, 125)
                p[Keys.BACKGROUND_IMAGE_URI] = backgroundImageUri.orEmpty()
                p[Keys.BACKGROUND_IMAGE_TRANSPARENCY_PERCENT] = backgroundImageTransparencyPercent.coerceIn(0, 100)
                appBackgroundColorOverride?.let { p[Keys.APP_BACKGROUND_ARGB] = it.toArgbCompat() } ?: p.remove(Keys.APP_BACKGROUND_ARGB)
                chromeBackgroundColorOverride?.let { p[Keys.CHROME_BACKGROUND_ARGB] = it.toArgbCompat() } ?: p.remove(Keys.CHROME_BACKGROUND_ARGB)
                cardColorOverride?.let { p[Keys.CARD_COLOR_ARGB] = it.toArgbCompat() } ?: p.remove(Keys.CARD_COLOR_ARGB)
                buttonColorOverride?.let { p[Keys.BUTTON_COLOR_ARGB] = it.toArgbCompat() } ?: p.remove(Keys.BUTTON_COLOR_ARGB)
                journalPageColorOverride?.let { p[Keys.JOURNAL_PAGE_COLOR_ARGB] = it.toArgbCompat() } ?: p.remove(Keys.JOURNAL_PAGE_COLOR_ARGB)
                journalAccentColorOverride?.let { p[Keys.JOURNAL_ACCENT_COLOR_ARGB] = it.toArgbCompat() } ?: p.remove(Keys.JOURNAL_ACCENT_COLOR_ARGB)
                p[Keys.JOURNAL_NAME] = journalName
                p[Keys.APP_LANGUAGE] = appLanguage
                if (textColorOverride == null) {
                    p.remove(Keys.TEXT_COLOR_ARGB)
                } else {
                    p[Keys.TEXT_COLOR_ARGB] = textColorOverride!!.toArgbCompat()
                }
            }
        }
    }

    fun persistAvatar(newAvatar: Avatar) {
        avatar = newAvatar
        scope.launch {
            appContext.dataStore.edit { p ->
                when (newAvatar) {
                    is Avatar.Preset -> { p[Keys.AVATAR_PRESET] = newAvatar.emoji; p[Keys.AVATAR_URI] = "" }; is Avatar.Custom -> { p[Keys.AVATAR_PRESET] = ""; p[Keys.AVATAR_URI] = newAvatar.uri.toString() }
                }
            }
        }
    }

    fun persistCustomTemplates(list: List<CustomTemplate>) {
        customTemplates = list
        scope.launch { appContext.dataStore.edit { p -> p[Keys.CUSTOM_TEMPLATES] = serializeCustomTemplates(list) } }
    }
    fun markShopTutorialSeen() {
        if (shopTutorialSeen) return
        shopTutorialSeen = true
        scope.launch { appContext.dataStore.edit { p -> p[Keys.TUTORIAL_SHOP] = true } }
    }
    fun markCalendarTutorialSeen() {
        if (calendarTutorialSeen) return
        calendarTutorialSeen = true
        scope.launch { appContext.dataStore.edit { p -> p[Keys.TUTORIAL_CALENDAR] = true } }
    }
    fun markQuestsTutorialSeen() {
        if (questsTutorialSeen) return
        questsTutorialSeen = true
        scope.launch { appContext.dataStore.edit { p -> p[Keys.TUTORIAL_QUESTS] = true } }
    }
    fun markShopHoldHintSeen() {
        if (shopHoldHintSeen) return
        shopHoldHintSeen = true
        scope.launch { appContext.dataStore.edit { p -> p[Keys.SHOP_HOLD_HINT_SEEN] = true } }
    }

    fun persistToday(day: Long, baseQuests: List<Quest>, completedIds: Set<Int>, earnedIdsNow: Set<Int>, refreshCountNow: Int) {
        lastDayEpoch = day; refreshCount = refreshCountNow; earnedIds = earnedIdsNow
        scope.launch { appContext.dataStore.edit { p -> p[Keys.LAST_DAY] = day; p[Keys.QUESTS] = serializeQuests(baseQuests); p[Keys.COMPLETED] = completedIds.joinToString(","); p[Keys.EARNED] = earnedIdsNow.joinToString(","); p[Keys.REFRESH_COUNT] = refreshCountNow } }
        QuestifyWidgetProvider.updateAll(appContext)
    }

    fun updateHistory(day: Long, baseQuests: List<Quest>, completedIds: Set<Int>) {
        val total = baseQuests.size; val done = completedIds.size.coerceAtMost(total); val allDone = (total > 0 && done == total)
        pushHistoryStateToSupabase(day = day, done = done, total = total, allDone = allDone)
        scope.launch {
            val prefs = appContext.dataStore.data.first()
            val current = prefs[Keys.HISTORY].orEmpty()
            val map = parseHistory(current).toMutableMap()
            map[day] = HistoryEntry(done = done, total = total, allDone = allDone)
            val trimmed = map.toList().sortedByDescending { it.first }.take(60).associate { it.first to it.second }
            appContext.dataStore.edit { p -> p[Keys.HISTORY] = serializeHistory(trimmed) }
        }
    }

    fun todayBaseAndCompleted(): Pair<List<Quest>, Set<Int>> {
        val base = quests.map { it.copy(completed = false) }
        val doneIds = quests.filter { it.completed }.map { it.id }.toSet()
        return base to doneIds
    }

    fun regenerateForDay(day: Long) {
        val levelNow = calculateLevel(totalXp).level
        val recentFailed = quests.filter { !it.completed }.map { it.title }.toSet()
        val base = generateDailyQuestsAdaptive(
            seed = daySeedForGeneration(day),
            playerLevel = levelNow,
            pool = customTemplatesToQuestTemplates(customTemplates.filter { it.isActive }),
            history = historyMap,
            recentFailedTitles = recentFailed,
            completedQuests = quests.filter { it.completed },
            difficultyPreference = difficultyPreference,
            desiredCount = dailyQuestTarget
        )
        quests = base
        persistToday(day, base, emptySet(), emptySet(), refreshCountNow = 0)
        updateHistory(day, base, emptySet())
    }

    fun finalizePreviousDayIfNeeded(previousDay: Long, currentDay: Long, previousQuests: List<Quest>) {
        val base = previousQuests.map { it.copy(completed = false) }
        val doneIds = previousQuests.filter { it.completed }.map { it.id }.toSet()
        val allDone = base.isNotEmpty() && doneIds.size == base.size

        val skippedDays = currentDay - previousDay
        if (skippedDays == 1L) {
            if (allDone) { streak += 1; bestStreak = maxOf(bestStreak, streak) } else { streak = 0 }
        } else {
            streak = 0
        }
        persistCore()
        updateHistory(previousDay, base, doneIds)
    }

    suspend fun load() {
        val before = appContext.dataStore.data.first()
        val beforeVersion = before[Keys.DATA_VERSION] ?: 0
        schemaDowngradeDetected = beforeVersion > CURRENT_DATA_VERSION
        runDataMigrations(appContext)
        val prefs = appContext.dataStore.data.first()
        journalPages = loadJournal(appContext)
        totalXp = prefs[Keys.TOTAL_XP] ?: 0
        gold = prefs[Keys.GOLD] ?: 0
        currentLevel = calculateLevel(totalXp).level
        streak = prefs[Keys.STREAK] ?: 0
        bestStreak = prefs[Keys.BEST_STREAK] ?: 0
// ... inside load() ...
        val savedActive = prefs[activePacksKey]
        if (!savedActive.isNullOrBlank()) {
            activePackageIds = savedActive.split(",").filter { it.isNotBlank() }.toSet()
        }
        autoNewDay = prefs[Keys.AUTO_NEW_DAY] ?: true
        confirmComplete = prefs[Keys.CONFIRM_COMPLETE] ?: true
        refreshIncompleteOnly = prefs[Keys.REFRESH_INCOMPLETE_ONLY] ?: true
        customMode = prefs[Keys.ADMIN_MODE] ?: false
        advancedOptions = prefs[Keys.ADVANCED_OPTIONS] ?: false
        highContrastText = prefs[Keys.HIGH_CONTRAST_TEXT] ?: false
        compactMode = prefs[Keys.COMPACT_MODE] ?: false
        largerTouchTargets = prefs[Keys.LARGE_TOUCH_TARGETS] ?: false
        reduceAnimations = prefs[Keys.REDUCE_ANIMATIONS] ?: false
        val savedDecorative = prefs[Keys.DECORATIVE_BORDERS] ?: false
        neonLightBoost = prefs[Keys.NEON_LIGHT_BOOST] ?: false
        neonFlowEnabled = (prefs[Keys.NEON_FLOW_ENABLED] ?: savedDecorative)
        decorativeBorders = neonFlowEnabled
        neonFlowSpeed = (prefs[Keys.NEON_FLOW_SPEED] ?: 0).coerceIn(0, 2)
        neonGlowPalette = prefs[Keys.NEON_GLOW_PALETTE].orEmpty().ifBlank { "magenta" }
        alwaysShowQuestProgress = prefs[Keys.ALWAYS_SHOW_QUEST_PROGRESS] ?: true
        hideCompletedQuests = prefs[Keys.HIDE_COMPLETED_QUESTS] ?: false
        confirmDestructiveActions = prefs[Keys.CONFIRM_DESTRUCTIVE] ?: true
        dailyResetHour = (prefs[Keys.DAILY_RESET_HOUR] ?: 0).coerceIn(0, 23)
        dailyRemindersEnabled = prefs[Keys.DAILY_REMINDERS_ENABLED] ?: true
        hapticsEnabled = prefs[Keys.HAPTICS] ?: true
        soundEffectsEnabled = prefs[Keys.SOUND_EFFECTS] ?: true
        onboardingGoal = runCatching { OnboardingGoal.valueOf(prefs[Keys.ONBOARDING_GOAL] ?: OnboardingGoal.BALANCE.name) }
            .getOrDefault(OnboardingGoal.BALANCE)
        difficultyPreference = runCatching { DifficultyPreference.valueOf(prefs[Keys.ONBOARDING_DIFFICULTY] ?: DifficultyPreference.NORMAL.name) }
            .getOrDefault(DifficultyPreference.NORMAL)
        premiumUnlocked = prefs[Keys.PREMIUM_UNLOCKED] ?: false
        dailyQuestTarget = (prefs[Keys.DAILY_QUEST_TARGET] ?: 5).coerceIn(3, 10)
        cloudSyncEnabled = prefs[Keys.CLOUD_SYNC_ENABLED] ?: true
        cloudAccountEmail = prefs[Keys.CLOUD_ACCOUNT_EMAIL].orEmpty()
        cloudLastSyncAt = prefs[Keys.CLOUD_LAST_SYNC_AT] ?: 0L
        cloudConnectedAccount = GoogleDriveSync.getLastSignedInAccount(appContext)
        if (cloudConnectedAccount != null && cloudAccountEmail.isBlank()) {
            cloudAccountEmail = cloudConnectedAccount?.email.orEmpty()
        }
        fontStyle = runCatching { AppFontStyle.valueOf(prefs[Keys.FONT_STYLE] ?: AppFontStyle.DEFAULT.name) }.getOrDefault(AppFontStyle.DEFAULT)
        fontScalePercent = (prefs[Keys.FONT_SCALE_PERCENT] ?: 100).coerceIn(80, 125)
        backgroundImageUri = prefs[Keys.BACKGROUND_IMAGE_URI].orEmpty().ifBlank { null }
        backgroundImageTransparencyPercent = (prefs[Keys.BACKGROUND_IMAGE_TRANSPARENCY_PERCENT] ?: 78).coerceIn(0, 100)
        textColorOverride = prefs[Keys.TEXT_COLOR_ARGB]?.let { Color(it) }
        appBackgroundColorOverride = prefs[Keys.APP_BACKGROUND_ARGB]?.let { Color(it) }
        chromeBackgroundColorOverride = prefs[Keys.CHROME_BACKGROUND_ARGB]?.let { Color(it) }
        cardColorOverride = prefs[Keys.CARD_COLOR_ARGB]?.let { Color(it) }
        buttonColorOverride = prefs[Keys.BUTTON_COLOR_ARGB]?.let { Color(it) }
        journalPageColorOverride = prefs[Keys.JOURNAL_PAGE_COLOR_ARGB]?.let { Color(it) }
        journalAccentColorOverride = prefs[Keys.JOURNAL_ACCENT_COLOR_ARGB]?.let { Color(it) }
        journalName = prefs[Keys.JOURNAL_NAME].orEmpty().ifBlank { "Journal" }
        val savedLang = prefs[Keys.APP_LANGUAGE].orEmpty().ifBlank { "system" }
        appLanguage = savedLang
        appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit().putString("selected_language", savedLang).apply()

        val themeName = prefs[Keys.APP_THEME]
        appTheme = if (themeName.isNullOrBlank()) {
            if (systemPrefersDark) AppTheme.DEFAULT else AppTheme.LIGHT
        } else {
            parseStoredTheme(themeName)
        }
        val argb = prefs[Keys.ACCENT_ARGB]
        accent = if (argb != null) Color(argb) else fallbackAccentForTheme(appTheme)

        val savedPreset = prefs[Keys.AVATAR_PRESET]; val savedUri = prefs[Keys.AVATAR_URI]
        avatar = when {
            !savedUri.isNullOrBlank() -> Avatar.Custom(savedUri.toUri()); !savedPreset.isNullOrBlank() -> Avatar.Preset(savedPreset); else -> Avatar.Preset("🧑‍🚀")
        }

        characterData = CharacterData(prefs[Keys.CHAR_HEAD] ?: 0xFFFACE8D, prefs[Keys.CHAR_BODY] ?: 0xFF3F51B5, prefs[Keys.CHAR_LEGS] ?: 0xFF212121, prefs[Keys.CHAR_SHOES] ?: 0xFF4E342E)

        unlockedAchievementIds = parseUnlockedAchievements(prefs[Keys.ACHIEVEMENTS].orEmpty())
        inventory = deserializeInventory(prefs[Keys.INVENTORY].orEmpty())
        val savedShop = deserializeShopItems(prefs[Keys.SHOP_ITEMS])
        shopItems = savedShop.ifEmpty {
            val defaults = getDefaultShopItems()
            persistShopItems(defaults)
            defaults
        }
        calendarPlans = deserializeCalendarPlans(prefs[Keys.CALENDAR_PLANS])

        val rawAttr = prefs[Keys.ATTRIBUTES_RAW]
        if (!rawAttr.isNullOrBlank()) {
            val s = rawAttr.split("|").map { it.toIntOrNull() ?: 1 }
            if (s.size >= 5) attributes = PlayerAttributes(s[0], s[1], s[2], s[3], s[4])
        }

        val storedCustom = prefs[Keys.CUSTOM_TEMPLATES].orEmpty()
        if (storedCustom.isBlank()) { val defaults = getInitialDefaultPool(); customTemplates = defaults; persistCustomTemplates(defaults) } else { customTemplates = deserializeCustomTemplates(storedCustom) }

// Load Main Quests
        val rawMQ = prefs[Keys.MAIN_QUESTS].orEmpty()
        mainQuests = if (rawMQ.isBlank()) {
            val defaults = getInitialMainQuests(); persistMainQuests(defaults); defaults
        } else {
            deserializeMainQuests(rawMQ)
        }

        if (savedActive.isNullOrBlank()) {
            val derivedActivePackages = (
                customTemplates.filter { it.isActive }.map { it.packageId } +
                    mainQuests.filter { it.isActive }.map { it.packageId }
                )
                .filter { it.isNotBlank() }
                .toSet()
            if (derivedActivePackages.isNotEmpty()) {
                activePackageIds = derivedActivePackages
                appContext.dataStore.edit { p -> p[activePacksKey] = activePackageIds.joinToString(",") }
            }
        }

        // NEW: Load Template Library
        savedTemplates = deserializeSavedTemplates(prefs[Keys.SAVED_TEMPLATES])
        followedAuthorIds = deserializeStringSet(prefs[Keys.COMMUNITY_FOLLOWS])
        mutedAuthorIds = deserializeStringSet(prefs[Keys.COMMUNITY_MUTED_AUTHORS])
        blockedAuthorIds = deserializeStringSet(prefs[Keys.COMMUNITY_BLOCKED_AUTHORS])
        myCommunityRatings = deserializeRatingsMap(prefs[Keys.COMMUNITY_MY_RATINGS])
        lastCommunityPublishAt = prefs[Keys.COMMUNITY_LAST_PUBLISH_AT] ?: 0L
        pendingCommunitySyncQueue = normalizeCommunitySyncQueue(deserializeCommunitySyncQueue(prefs[Keys.COMMUNITY_SYNC_QUEUE]))
        communityUserId = secureCommunityUserId(prefs[Keys.COMMUNITY_USER_ID].orEmpty().ifBlank { UUID.randomUUID().toString() })
        communityUserName = sanitizeDisplayName(
            prefs[Keys.COMMUNITY_USER_NAME].orEmpty().ifBlank { "Player-${communityUserId.take(4)}" }
        )
        val storedCommunity = deserializeCommunityPosts(prefs[Keys.COMMUNITY_POSTS]).filterNot { blockedAuthorIds.contains(it.authorId) || mutedAuthorIds.contains(it.authorId) }
        if (storedCommunity.isEmpty()) {
            val starter = getStarterCommunityPosts()
            communityPosts = starter
            persistCommunityPosts(starter)
        } else {
            communityPosts = storedCommunity.sortedByDescending { it.createdAtMillis }
        }
        persistCommunityProfile(communityUserId, communityUserName)
        showWelcomeSetup = !(prefs[Keys.ONBOARDING_DONE] ?: false)
        onboardingSkipIntroDefault = false
        shopTutorialSeen = prefs[Keys.TUTORIAL_SHOP] ?: false
        calendarTutorialSeen = prefs[Keys.TUTORIAL_CALENDAR] ?: false
        questsTutorialSeen = prefs[Keys.TUTORIAL_QUESTS] ?: false
        shopHoldHintSeen = prefs[Keys.SHOP_HOLD_HINT_SEEN] ?: false

        // FIXED: LOAD BOSSES HERE
        val savedBosses = deserializeBosses(prefs[Keys.BOSSES].orEmpty())
        if (savedBosses.isEmpty()) {
            persistBosses(listOf(Boss(UUID.randomUUID().toString(), "The Procrastinator", "Defeat him before Friday!", "👹", 500, 500, System.currentTimeMillis() + 86400000 * 3, 1000)))
        } else {
            bosses = savedBosses
        }

        // FIXED: APPLY SKILL DECAY HERE
        val lastActivity = deserializeActivityTimestamps(prefs[Keys.LAST_ACTIVITY_TIMESTAMPS])
        attributes = applySkillDecay(attributes, lastActivity)
        persistAttributes(attributes)

        val storedDay = prefs[Keys.LAST_DAY] ?: currentEpochDay(); val storedQuestsSer = prefs[Keys.QUESTS]; val storedCompletedSer = prefs[Keys.COMPLETED]; val storedEarnedSer = prefs[Keys.EARNED]; val storedRefresh = prefs[Keys.REFRESH_COUNT] ?: 0; val nowDay = currentEpochDay()

        val storedBase = if (!storedQuestsSer.isNullOrBlank()) deserializeQuests(storedQuestsSer) else emptyList()
        val storedCompleted = parseIds(storedCompletedSer); val storedEarned = parseIds(storedEarnedSer)
        val storedFull = storedBase.map { q -> if (storedCompleted.contains(q.id)) q.copy(completed = true) else q }

        if (autoNewDay && nowDay > storedDay) {
            if (storedFull.isNotEmpty()) finalizePreviousDayIfNeeded(storedDay, nowDay, storedFull)
            regenerateForDay(nowDay)
        } else {
            lastDayEpoch = storedDay; refreshCount = storedRefresh; earnedIds = storedEarned.intersect(storedBase.map { it.id }.toSet())
            quests = if (storedBase.isNotEmpty()) {
                storedFull
            } else {
                generateDailyQuestsAdaptive(
                    seed = daySeedForGeneration(storedDay),
                    playerLevel = currentLevel,
                    pool = customTemplatesToQuestTemplates(customTemplates.filter { it.isActive }),
                    history = historyMap,
                    recentFailedTitles = emptySet(),
                    completedQuests = emptyList(),
                    difficultyPreference = difficultyPreference,
                    desiredCount = dailyQuestTarget
                ).also { base ->
                    persistToday(storedDay, base, emptySet(), emptySet(), refreshCountNow = 0); updateHistory(storedDay, base, emptySet())
                }
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && dailyRemindersEnabled) {
            DailyReminderWorker.schedule(appContext, dailyResetHour)
        }
    }
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            scope.launch { snackbarHostState.showSnackbar("Google sign-in canceled.") }
            return@rememberLauncherForActivityResult
        }
        val data = result.data
        GoogleDriveSync.resolveSignInResult(data)
            .onSuccess { account ->
                if (account != null) onGoogleAccountConnected(account)
            }
            .onFailure { e ->
                AppLog.w("Google sign-in failed.", e)
                val code = GoogleDriveSync.resolveSignInStatusCode(e)
                val hint = if (code != null) " (code: $code)" else ""
                scope.launch { snackbarHostState.showSnackbar("Google sign-in failed$hint.") }
            }
    }

    LaunchedEffect(Unit) {
        load()
        if (schemaDowngradeDetected) {
            scope.launch { snackbarHostState.showSnackbar("Data schema is newer than this app build.") }
        }
        delay(700)
        showIntroSplash = false
        runCatching {
            syncCommunityFromRemote()
        }.onFailure {
            AppLog.w("Community sync failed during startup; using local cache.", it)
            scope.launch { snackbarHostState.showSnackbar("Community sync offline. Using local data.") }
        }
        if (cloudSyncEnabled && cloudConnectedAccount != null) {
            triggerCloudSnapshotSync(force = false)
        }

        // Catch incoming shared templates only from expected schemes/hosts.
        val intent = (appContext as? ComponentActivity)?.intent
        if (intent != null && intent.action == Intent.ACTION_VIEW) {
            val dataUri = intent.data
            val trustedSource = when (dataUri?.scheme?.lowercase()) {
                "https" -> dataUri.host.equals("qn8r.github.io", ignoreCase = true) &&
                    (dataUri.path ?: "").startsWith("/Questify")
                "livinglife" -> dataUri.host.equals("import", ignoreCase = true)
                else -> false
            }
            if (trustedSource) {
                val templateData = dataUri?.getQueryParameter("data")
                if (!templateData.isNullOrBlank()) {
                    if (templateData.length > 250_000) {
                        snackbarHostState.showSnackbar("Template link too large.")
                    } else {
                        try {
                            val decoded = runCatching {
                                java.net.URLDecoder.decode(templateData, StandardCharsets.UTF_8.name())
                            }.getOrDefault(templateData)
                            val template = importGameTemplate(decoded)
                            if (template != null) {
                                pendingImportTemplate = template
                            } else {
                                snackbarHostState.showSnackbar("Template link is invalid or too large.")
                            }
                        } catch (e: Exception) {
                            AppLog.e("Failed to parse incoming template link.", e)
                            snackbarHostState.showSnackbar("Failed to read template link.")
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(dailyRemindersEnabled, dailyResetHour) {
        if (!dailyRemindersEnabled) {
            DailyReminderWorker.cancel(appContext)
            return@LaunchedEffect
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                DailyReminderWorker.schedule(appContext, dailyResetHour)
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            DailyReminderWorker.schedule(appContext, dailyResetHour)
        }
    }

    LaunchedEffect(communityUserId, pendingCommunitySyncQueue.size) {
        if (communityUserId.isNotBlank() && pendingCommunitySyncQueue.isNotEmpty()) {
            CommunitySyncWorker.enqueue(appContext)
        }
    }

    LaunchedEffect(Unit) {
        appContext.dataStore.data
            .map { prefs -> deserializeCommunitySyncQueue(prefs[Keys.COMMUNITY_SYNC_QUEUE]) }
            .collect { queue -> pendingCommunitySyncQueue = normalizeCommunitySyncQueue(queue) }
    }

    fun onRefreshDay() {
        val today = currentEpochDay()
        if (today != lastDayEpoch && quests.isNotEmpty()) { finalizePreviousDayIfNeeded(lastDayEpoch, today, quests) }
        regenerateForDay(today)
        scope.launch { snackbarHostState.showSnackbar("Start of a new day!") }
    }

    fun cloneTemplateForLibrary(template: GameTemplate, newName: String): GameTemplate {
        val safeTemplate = sanitizeTemplateForCommunity(template)
        val newPkg = "community_${UUID.randomUUID()}"
        val remappedDaily = safeTemplate.dailyQuests.map { it.copy(packageId = newPkg) }

        val idMap = safeTemplate.mainQuests.associate { it.id to UUID.randomUUID().toString() }
        val remappedMain = safeTemplate.mainQuests.map { q ->
            q.copy(
                id = idMap[q.id] ?: UUID.randomUUID().toString(),
                prerequisiteId = q.prerequisiteId?.let { idMap[it] },
                packageId = newPkg
            )
        }

        return GameTemplate(
            templateName = newName,
            appTheme = safeTemplate.appTheme,
            dailyQuests = remappedDaily,
            mainQuests = remappedMain,
            shopItems = safeTemplate.shopItems,
            packageId = newPkg,
            templateSettings = safeTemplate.templateSettings,
            accentArgb = safeTemplate.accentArgb
        )
    }
    val advancedTemplateGson = Gson()
    val advancedDailyImportLimit = 500
    val advancedMainImportLimit = 200
    val advancedShopImportLimit = 120
    val advancedTemplatePromptText = """
You are editing a JSON file exported from the Questify app.

USER REQUEST:
{{USER_REQUEST}}

Goal:
- Use the USER REQUEST to generate/replace quests in this file.
- You may edit daily_quests, main_quests, shop_items, app_theme, and accent_argb.
- Keep schema-compatible JSON for Questify import.

Hard rules:
1) Return valid JSON only. No markdown. No explanation.
2) Keep required top-level keys for import: schema_version, template_name, app_theme, accent_argb, daily_quests, main_quests.
   - shop_items is optional but supported and recommended when USER REQUEST includes economy/shop.
3) ai_instructions and guide are optional in final output (you may remove them).
4) daily_quests rules:
    - category must be one of FITNESS, STUDY, HYDRATION, DISCIPLINE, MIND
    - difficulty must be 1..5
    - xp must be 10..300
    - target must be >= 1
    - title must be short and clear for mobile UI
    - avoid filler duplicates (for example repetitive numbered clones like "Quest #2/#3/#4")
    - if USER REQUEST does not specify category distribution, auto-distribute across all 5 categories: FITNESS, STUDY, HYDRATION, DISCIPLINE, MIND
    - in default auto-distribution, keep category counts near-balanced (max difference is 1)
    - if USER REQUEST specifies category focus or per-category counts, follow USER REQUEST (while respecting app limits)
    - if USER REQUEST does not specify tier distribution, distribute difficulty tiers 1..5 near-balanced (max difference is 1 whenever possible)
    - avoid concentrating most dailies into a single tier unless USER REQUEST explicitly asks for it
5) main_quests rules:
    - ref must be unique (example: mq_1, mq_2, ...)
   - title/description should match the USER REQUEST style
    - xp_reward should scale reasonably (100..5000)
    - steps should be 2..8 concise milestones
    - prerequisite_ref must be null or match an existing ref
    - optional icon is allowed (short emoji)
    - optional image_uri is allowed
    - for numbered chains in the same family (example: "Career Upgrade 1/2/3"), xp_reward must be non-decreasing by number
    - prefer clear progression in numbered chains (recommended +8% to +20% or at least +50 XP per step)
6) shop_items rules:
   - name must be clear and short
   - icon should be short emoji
   - description concise and practical
   - cost should fit app economy (recommended 20..5000)
   - stock/max_stock should be 0..99 and max_stock >= stock
   - avoid overpowered progression skips (focus utility/cosmetic/quality-of-life)
   - avoid duplicate filler items
7) App import limits:
   - daily_quests max = $advancedDailyImportLimit total
   - main_quests max = $advancedMainImportLimit total
   - shop_items max = $advancedShopImportLimit total
   - counts are TOTAL, not per category
   - if USER REQUEST exceeds limits, clamp to these maximums
8) Do not remove existing valid quests unless needed to satisfy the USER REQUEST.
9) The existing quests in daily_quests/main_quests/shop_items are examples only.
    - You may fully replace them to match the USER REQUEST.
    - Prefer replacing example content with the requested content.

Final output:
- Return the final JSON file content only.
- Do NOT add any explanation before or after JSON.
- Do NOT use markdown/code fences.
- Chat response must be JSON only.
- If your platform supports file attachments, return it as a downloadable file named questify_advanced_template.json.
""".trimIndent()

    fun buildAdvancedTemplateStarterJson(): String {
        val starter = AdvancedTemplateFile(
            template_name = "AI Generated Template",
            app_theme = appTheme.name,
            accent_argb = accent.toArgbCompat().toLong(),
            ai_instructions = listOf(
                "This JSON file is from Questify.",
                "Read USER REQUEST first, then update daily_quests/main_quests/shop_items and optionally app_theme/accent_argb.",
                "Existing quests are sample data only; you can replace or remove them.",
                "Required keys: schema_version, template_name, app_theme, accent_argb, daily_quests, main_quests.",
                "guide and ai_instructions are optional in the final returned file.",
                "Use only category values: FITNESS, STUDY, HYDRATION, DISCIPLINE, MIND.",
                "Daily rules: difficulty 1..5, xp 10..300, target >= 1.",
                "If user does not specify category distribution, auto-distribute across FITNESS, STUDY, HYDRATION, DISCIPLINE, MIND.",
                "Default auto-distribution should be near-balanced (difference <= 1).",
                "If user specifies category focus/per-category counts, follow that request within app caps.",
                "If user does not specify tier distribution, keep daily tiers 1..5 near-balanced.",
                "Do not concentrate most dailies in one tier unless user explicitly requests it.",
                "Counts are total counts, not per-category counts.",
                "Respect app caps: daily_quests <= $advancedDailyImportLimit and main_quests <= $advancedMainImportLimit.",
                "Avoid repetitive filler quests and low-quality numbered duplicates.",
                "Main rules: unique ref values, prerequisite_ref null or existing ref.",
                "Main quests can include optional icon and optional image_uri.",
                "For numbered main quest families, keep xp_reward non-decreasing by family number.",
                "Shop rules: concise name/description, icon emoji, cost fits economy, stock/max_stock in 0..99, avoid duplicate filler.",
                "Shop caps: shop_items <= $advancedShopImportLimit.",
                "Output must be JSON only, no markdown, no commentary, no extra chat text.",
                "If possible, return as downloadable file attachment named questify_advanced_template.json."
            ),
            guide = AdvancedTemplateGuide(
                summary = "Workflow: user asks AI for quests, AI edits this file directly, user uploads the returned JSON in Settings > Advanced Templates.",
                ai_prompt_example = advancedTemplatePromptText.replace("{{USER_REQUEST}}", "Generate 120 daily quests, 40 main quests, and 24 shop items for a disciplined real-life RPG. Set app_theme to DEFAULT and pick a matching accent_argb. Keep main chains progressive and shop prices balanced for this quest economy."),
                notes = listOf(
                    "Do not remove schema_version, template_name, daily_quests, main_quests.",
                    "AI may also update app_theme and accent_argb if requested.",
                    "AI may generate shop_items when requested.",
                    "Current quests are placeholders/examples and can be replaced.",
                    "daily_quests: category must be FITNESS|STUDY|HYDRATION|DISCIPLINE|MIND.",
                    "daily_quests: difficulty 1..5, xp 10..300, target >= 1.",
                    "If category distribution is not requested, auto-distribute across all 5 categories.",
                    "Default distribution should be near-balanced (difference <= 1).",
                    "If category focus/per-category counts are requested, follow that request.",
                    "If tier distribution is not requested, keep daily tiers 1..5 near-balanced.",
                    "Avoid concentrating most dailies in one tier unless user requests it.",
                    "Counts are totals (not per-category), and must respect app caps.",
                    "Caps: daily_quests <= $advancedDailyImportLimit, main_quests <= $advancedMainImportLimit.",
                    "Avoid repetitive filler quests and title spam.",
                    "main_quests: ref should be unique (e.g. mq_1).",
                    "main_quests: prerequisite_ref must point to an existing ref.",
                    "main_quests: optional icon and optional image_uri are supported.",
                    "main_quests: for numbered families, xp_reward should not decrease as the number increases.",
                    "shop_items: keep economy balanced, realistic costs, and avoid overpowered items.",
                    "Keep text concise and realistic for mobile UI."
                )
            ),
            daily_quests = listOf(
                AdvancedDailyQuestEntry(title = "Morning walk 20 min", category = QuestCategory.FITNESS.name, difficulty = 2, xp = 20, target = 1, icon = "🚶"),
                AdvancedDailyQuestEntry(title = "Deep work session", category = QuestCategory.STUDY.name, difficulty = 3, xp = 35, target = 1, icon = "🧠")
            ),
            main_quests = listOf(
                AdvancedMainQuestEntry(ref = "mq_1", title = "Build Consistency", description = "Finish 30 focused days in a row.", xp_reward = 400, steps = listOf("Week 1", "Week 2", "Week 3", "Week 4"), icon = "🏆"),
                AdvancedMainQuestEntry(ref = "mq_2", title = "Master Focus", description = "Upgrade your focus routine.", xp_reward = 600, steps = listOf("Baseline", "System", "Mastery"), prerequisite_ref = "mq_1", icon = "🧠")
            ),
            shop_items = listOf(
                AdvancedShopItemEntry(name = "Focus Timer Boost", icon = "⏱️", description = "Adds a quick focus session shortcut.", cost = 180, stock = 5, max_stock = 5, consumable = true),
                AdvancedShopItemEntry(name = "Calm Theme Pack", icon = "🎨", description = "Unlocks a calm visual preset.", cost = 420, stock = 1, max_stock = 1, consumable = false)
            )
        )
        return advancedTemplateGson.toJson(starter)
    }

    fun buildAdvancedTemplatePromptFromRequest(userRequest: String): String {
        val request = userRequest.trim().ifBlank { "Generate 100 daily quests and 30 main quests in Saitama-style progression." }
        val lower = request.lowercase(Locale.getDefault())
        val dailyCount = Regex("""(\d{1,4})\s*(daily|day|dailies)""").find(lower)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val mainCount = Regex("""(\d{1,4})\s*(main|story|boss|milestone)""").find(lower)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val shopCount = Regex("""(\d{1,4})\s*(shop|item|items|store)""").find(lower)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val finalDailyCount = dailyCount?.coerceAtMost(advancedDailyImportLimit)
        val finalMainCount = mainCount?.coerceAtMost(advancedMainImportLimit)
        val finalShopCount = shopCount?.coerceAtMost(advancedShopImportLimit)
        val countHint = buildString {
            append("\n- Counts are TOTAL, not per category.")
            append("\n- Hard cap: daily <= $advancedDailyImportLimit, main <= $advancedMainImportLimit, shop_items <= $advancedShopImportLimit.")
            append("\n- If category distribution is not specified, auto-distribute across FITNESS, STUDY, HYDRATION, DISCIPLINE, MIND.")
            append("\n- In default distribution, keep category counts near-balanced (difference <= 1).")
            append("\n- If tier distribution is not specified, keep daily tiers 1..5 near-balanced (difference <= 1 when possible).")
            append("\n- If category focus/per-category counts are specified, follow that request within caps.")
            append("\n- For numbered main quest families, keep xp_reward non-decreasing by number.")
            append("\n- For shop_items, keep economy balanced (cost and stock progression, no overpowered skips).")
            if (finalDailyCount != null) append("\n- Generate exactly $finalDailyCount daily quests.")
            if (finalMainCount != null) append("\n- Generate exactly $finalMainCount main quests.")
            if (finalShopCount != null) append("\n- Generate exactly $finalShopCount shop items.")
            if (dailyCount != null && finalDailyCount != dailyCount) append("\n- Daily request was capped from $dailyCount to $finalDailyCount.")
            if (mainCount != null && finalMainCount != mainCount) append("\n- Main request was capped from $mainCount to $finalMainCount.")
            if (shopCount != null && finalShopCount != shopCount) append("\n- Shop request was capped from $shopCount to $finalShopCount.")
            if (isNotBlank()) append("\n- If needed, replace all existing sample quests to match these counts.")
        }
        val enrichedRequest = if (countHint.isBlank()) request else request + "\n\nCOUNT TARGETS:" + countHint
        return advancedTemplatePromptText.replace("{{USER_REQUEST}}", enrichedRequest)
    }

    fun importAdvancedTemplateJson(raw: String): AdvancedTemplateImportResult {
        val payload = raw.trim()
        if (payload.isBlank()) return AdvancedTemplateImportResult(false, "Unnamed", 0, 0, errors = listOf("File is empty."))
        val parsed = runCatching { advancedTemplateGson.fromJson(payload, AdvancedTemplateFile::class.java) }.getOrNull()
            ?: return AdvancedTemplateImportResult(false, "Unnamed", 0, 0, errors = listOf("Invalid JSON format."))
        val supportedSchema = 1
        if (parsed.schema_version > supportedSchema) {
            return AdvancedTemplateImportResult(
                success = false,
                templateName = parsed.template_name.ifBlank { "Unnamed" },
                dailyAdded = 0,
                mainAdded = 0,
                errors = listOf("Unsupported schema_version=${parsed.schema_version}. Supported up to $supportedSchema.")
            )
        }

        val warnings = mutableListOf<String>()
        if (parsed.schema_version < supportedSchema) {
            warnings += "Legacy schema_version=${parsed.schema_version} detected. Applied compatibility migration."
        }
        val templateName = parsed.template_name.trim().ifBlank { "AI Template ${System.currentTimeMillis()}" }.take(60)
        val packageId = "ai_${UUID.randomUUID()}"

        val dailyRaw = parsed.daily_quests.take(advancedDailyImportLimit).mapIndexedNotNull { index, q ->
            val title = q.title.trim().take(64)
            if (title.isBlank()) { warnings += "Daily[$index] skipped: missing title."; return@mapIndexedNotNull null }
            val category = runCatching { QuestCategory.valueOf(q.category.trim().uppercase(Locale.getDefault())) }.getOrNull()
            if (category == null) { warnings += "Daily[$index] skipped: invalid category '${q.category}'."; return@mapIndexedNotNull null }
            CustomTemplate(
                id = UUID.randomUUID().toString(),
                category = category,
                difficulty = q.difficulty.coerceIn(1, 5),
                title = title,
                icon = q.icon.trim().ifBlank { "✅" }.take(3),
                xp = q.xp.coerceIn(1, 5000),
                target = q.target.coerceIn(1, 500),
                isPinned = q.pinned,
                imageUri = q.image_uri?.takeIf { it.isNotBlank() },
                packageId = packageId
            )
        }.distinctBy { it.title.lowercase(Locale.getDefault()) }
        val dailyTierMinXp = mapOf(1 to 10, 2 to 24, 3 to 48, 4 to 80, 5 to 120)
        var dailyProgressionAdjusted = 0
        val daily = dailyRaw.map { d ->
            val floor = dailyTierMinXp[d.difficulty] ?: 10
            if (d.xp < floor) {
                dailyProgressionAdjusted++
                d.copy(xp = floor)
            } else d
        }
        if (dailyProgressionAdjusted > 0) warnings += "Adjusted $dailyProgressionAdjusted daily quests to keep tier XP progression."
        if (parsed.daily_quests.size > advancedDailyImportLimit) warnings += "Daily quests capped to $advancedDailyImportLimit."

        val tempMain = parsed.main_quests.take(advancedMainImportLimit).mapIndexedNotNull { index, q ->
            val title = q.title.trim().take(64)
            if (title.isBlank()) { warnings += "Main[$index] skipped: missing title."; return@mapIndexedNotNull null }
            val steps = q.steps.map { it.trim().take(48) }.filter { it.isNotBlank() }.take(8)
            if (steps.isEmpty()) { warnings += "Main[$index] skipped: at least one step required."; return@mapIndexedNotNull null }
            Triple(q, title, steps)
        }
        if (parsed.main_quests.size > advancedMainImportLimit) warnings += "Main quests capped to $advancedMainImportLimit."
        val refToId = mutableMapOf<String, String>()
        val mainRawToQuest = tempMain.map { (q, title, steps) ->
            val id = UUID.randomUUID().toString()
            q.ref.trim().takeIf { it.isNotBlank() }?.let { refToId[it] = id }
            q to CustomMainQuest(
                id = id,
                title = title,
                description = q.description.trim().take(220),
                xpReward = q.xp_reward.coerceIn(20, 20000),
                steps = steps,
                packageId = packageId,
                icon = q.icon.trim().ifBlank { "🏆" }.take(3),
                imageUri = q.image_uri?.takeIf { it.isNotBlank() }
            )
        }
        val mainBase = mainRawToQuest.map { (rawMain, quest) ->
            val pre = rawMain.prerequisite_ref?.trim()?.let { refToId[it] }
            if (rawMain.prerequisite_ref != null && pre == null) warnings += "Main '${quest.title}': prerequisite_ref '${rawMain.prerequisite_ref}' not found."
            quest.copy(prerequisiteId = pre)
        }.distinctBy { it.title.lowercase(Locale.getDefault()) }
        var mainProgressionAdjusted = 0
        val indexedMain = mainBase.mapIndexed { idx, q -> idx to q }
        fun parseNumberedFamily(title: String): Pair<String, Int?> {
            val m = Regex("""^(.*?)(?:\s+(\d+))$""").find(title.trim())
            val base = (m?.groupValues?.getOrNull(1)?.trim().takeUnless { it.isNullOrBlank() } ?: title.trim()).lowercase(Locale.getDefault())
            val num = m?.groupValues?.getOrNull(2)?.toIntOrNull()
            return base to num
        }
        val mainAdjustedById = mutableMapOf<String, CustomMainQuest>()
        indexedMain
            .groupBy { parseNumberedFamily(it.second.title).first }
            .values
            .forEach { family ->
                val numbered = family.map { it.second to parseNumberedFamily(it.second.title).second }.filter { it.second != null }
                if (numbered.size < 2) return@forEach
                var runningXp = Int.MIN_VALUE
                family.sortedWith(compareBy<Pair<Int, CustomMainQuest>>({ parseNumberedFamily(it.second.title).second ?: Int.MAX_VALUE }, { it.first })).forEach { (_, quest) ->
                    val targetXp = if (runningXp == Int.MIN_VALUE) quest.xpReward else maxOf(quest.xpReward, runningXp)
                    if (targetXp != quest.xpReward) mainProgressionAdjusted++
                    runningXp = targetXp
                    mainAdjustedById[quest.id] = quest.copy(xpReward = targetXp)
                }
            }
        val main = mainBase.map { q -> mainAdjustedById[q.id] ?: q }
        if (mainProgressionAdjusted > 0) warnings += "Adjusted $mainProgressionAdjusted main quests to keep numbered-chain XP non-decreasing."
        val shop = parsed.shop_items.take(advancedShopImportLimit).mapIndexedNotNull { index, s ->
            val name = s.name.trim().take(48)
            if (name.isBlank()) {
                warnings += "Shop[$index] skipped: missing name."
                return@mapIndexedNotNull null
            }
            val safeIdBase = s.id?.trim().orEmpty().ifBlank { name.lowercase(Locale.getDefault()).replace(Regex("[^a-z0-9]+"), "_").trim('_') }
            val safeId = safeIdBase.ifBlank { "shop_${index + 1}" }.take(40)
            ShopItem(
                id = "${packageId}_$safeId",
                name = name,
                icon = s.icon.trim().ifBlank { "🧩" }.take(3),
                description = s.description.trim().take(140),
                cost = s.cost.coerceIn(20, 20000),
                stock = s.stock.coerceIn(0, 99),
                maxStock = s.max_stock.coerceIn(0, 99).coerceAtLeast(s.stock.coerceIn(0, 99)),
                isConsumable = s.consumable,
                imageUri = s.image_uri?.takeIf { it.isNotBlank() }
            )
        }.distinctBy { it.id.lowercase(Locale.getDefault()) }
        if (parsed.shop_items.size > advancedShopImportLimit) warnings += "Shop items capped to $advancedShopImportLimit."

        if (daily.isEmpty() && main.isEmpty() && shop.isEmpty()) {
            return AdvancedTemplateImportResult(false, templateName, 0, 0, warnings = warnings, errors = listOf("No valid quests found in JSON."))
        }

        val theme = runCatching { parseStoredTheme(parsed.app_theme) }.getOrDefault(appTheme)
        val importedTemplate = GameTemplate(
            templateName = templateName,
            appTheme = theme,
            dailyQuests = customTemplatesToQuestTemplates(daily),
            mainQuests = main,
            shopItems = shop,
            packageId = packageId,
            accentArgb = parsed.accent_argb
        )
        persistSavedTemplates((savedTemplates + importedTemplate).distinctBy { "${it.packageId}|${it.templateName}" })
        return AdvancedTemplateImportResult(true, templateName, daily.size, main.size, packageId = packageId, warnings = warnings)
    }

    fun applyAdvancedImportedTemplate(packageId: String): Boolean {
        val t = savedTemplates.firstOrNull { it.packageId == packageId } ?: return false
        appTheme = normalizeTheme(t.appTheme)
        accent = t.accentArgb?.let { Color(it.toInt()) } ?: fallbackAccentForTheme(appTheme)
        applyTemplateSettings(t.templateSettings)
        persistSettings()
        val mappedDailies = t.dailyQuests.map { qt ->
            CustomTemplate(
                id = UUID.randomUUID().toString(),
                category = qt.category,
                difficulty = qt.difficulty,
                title = qt.title,
                icon = qt.icon,
                xp = qt.xp,
                target = qt.target,
                isPinned = qt.isPinned,
                imageUri = qt.imageUri,
                packageId = t.packageId
            )
        }
        persistCustomTemplates(mappedDailies)
        persistMainQuests(t.mainQuests)
        if (t.shopItems.isNotEmpty()) persistShopItems(t.shopItems)
        activePackageIds = setOf(t.packageId)
        scope.launch { appContext.dataStore.edit { p -> p[activePacksKey] = activePackageIds.joinToString(",") } }
        regenerateForDay(currentEpochDay())
        return true
    }

    fun applyStarterTemplate(template: GameTemplate) {
        appTheme = normalizeTheme(template.appTheme)
        accent = template.accentArgb?.let { Color(it.toInt()) } ?: fallbackAccentForTheme(appTheme)
        applyTemplateSettings(template.templateSettings)
        applyTemplateDailyQuestDefaults(template.packageId, clearExisting = true)
        persistSettings()
        val mappedDailies = template.dailyQuests.map { qt ->
            CustomTemplate(
                id = UUID.randomUUID().toString(),
                category = qt.category,
                difficulty = qt.difficulty,
                title = qt.title,
                icon = qt.icon,
                xp = qt.xp,
                target = qt.target,
                isPinned = qt.isPinned,
                imageUri = qt.imageUri,
                packageId = template.packageId
            )
        }
        persistCustomTemplates(mappedDailies)
        persistMainQuests(template.mainQuests)
        persistShopItems(template.shopItems.ifEmpty { getDefaultShopItems() })
        activePackageIds = setOf(template.packageId)
        scope.launch { appContext.dataStore.edit { p -> p[activePacksKey] = activePackageIds.joinToString(",") } }
        regenerateForDay(currentEpochDay())
    }

    fun publishCurrentTemplateToCommunity(title: String, description: String, tagsRaw: String) {
        val now = System.currentTimeMillis()
        val cooldownMs = 1000L * 60L * 3L
        if (now - lastCommunityPublishAt < cooldownMs) {
            scope.launch { snackbarHostState.showSnackbar("Publishing cooldown active. Try again in a few minutes.") }
            return
        }
        val recentBurstCount = communityPosts.count { it.authorId == communityUserId && now - it.createdAtMillis < (60L * 60L * 1000L) }
        if (recentBurstCount >= 5) {
            scope.launch { snackbarHostState.showSnackbar("Publishing temporarily locked due to spam protection.") }
            return
        }
        val rawTitle = title.trim()
        if (rawTitle.length < 3) {
            scope.launch { snackbarHostState.showSnackbar("Title is too short.") }
            return
        }
        val cleanTitle = sanitizeCommunityText(rawTitle, 60)
        val cleanDescription = sanitizeCommunityText(
            description.ifBlank { "Community challenge by $communityUserName." },
            280
        )
        val tags = sanitizeTags(tagsRaw.split(","))
        if (cleanDescription.length < 10) {
            scope.launch { snackbarHostState.showSnackbar("Description is too short.") }
            return
        }
        if (tags.size > 8) {
            scope.launch { snackbarHostState.showSnackbar("Use up to 8 tags.") }
            return
        }
        val tagRegex = Regex("^[a-zA-Z0-9 _-]{2,24}$")
        if (tags.any { !tagRegex.matches(it) }) {
            scope.launch { snackbarHostState.showSnackbar("Tags contain unsupported characters.") }
            return
        }
        if (hasSuspiciousCommunityContent(cleanTitle) || hasSuspiciousCommunityContent(cleanDescription) || tags.any { hasSuspiciousCommunityContent(it) }) {
            scope.launch { snackbarHostState.showSnackbar("Publish blocked: unsafe content detected.") }
            return
        }
        val duplicateExists = communityPosts.any {
            it.title.equals(cleanTitle, ignoreCase = true)
        }
        if (duplicateExists) {
            scope.launch { snackbarHostState.showSnackbar("Challenge title already exists in community.") }
            return
        }
        val baseTemplate = GameTemplate(
            templateName = cleanTitle,
            appTheme = appTheme,
            dailyQuests = customTemplatesToQuestTemplates(customTemplates),
            mainQuests = mainQuests,
            shopItems = shopItems,
            templateSettings = currentTemplateSettings(),
            accentArgb = accent.toArgbCompat().toLong()
        )
        val (safeBaseTemplate, baseTrust) = evaluateTemplateTrust(baseTemplate)
        val postTemplate = cloneTemplateForLibrary(safeBaseTemplate, cleanTitle)
        if (postTemplate.dailyQuests.isEmpty() && postTemplate.mainQuests.isEmpty() && postTemplate.shopItems.isEmpty()) {
            scope.launch { snackbarHostState.showSnackbar("Nothing safe to publish in this template.") }
            return
        }
        val post = CommunityPost(
            authorId = communityUserId,
            authorName = communityUserName,
            title = cleanTitle,
            description = cleanDescription,
            tags = tags,
            template = postTemplate,
            templateTrust = baseTrust
        )
        persistCommunityPosts((listOf(post) + communityPosts).distinctBy { it.id })
        AppLog.event("publish", "title=${cleanTitle.take(24)}")
        lastCommunityPublishAt = now
        scope.launch { snackbarHostState.showSnackbar("Challenge published!") }
        scope.launch { appContext.dataStore.edit { p -> p[Keys.COMMUNITY_LAST_PUBLISH_AT] = now } }
        scope.launch {
            val ok = SupabaseApi.publishPost(post)
            if (!ok) enqueueCommunitySyncTask(CommunitySyncTask(type = CommunitySyncTaskType.PUBLISH_POST, post = post))
            syncCommunityFromRemote()
        }
    }

    fun onToggleFollowAuthor(authorId: String) {
        if (authorId == communityUserId) return
        val wasFollowing = followedAuthorIds.contains(authorId)
        val next = if (wasFollowing) followedAuthorIds - authorId else followedAuthorIds + authorId
        persistCommunityFollows(next)
        scope.launch {
            val ok = if (wasFollowing) {
                SupabaseApi.deleteFollow(communityUserId, authorId)
            } else {
                SupabaseApi.upsertFollow(communityUserId, authorId)
            }
            if (!ok) {
                enqueueCommunitySyncTask(
                    CommunitySyncTask(
                        type = if (wasFollowing) CommunitySyncTaskType.UNFOLLOW_AUTHOR else CommunitySyncTaskType.FOLLOW_AUTHOR,
                        authorId = authorId
                    )
                )
            }
            syncCommunityFromRemote()
        }
    }

    fun onToggleMuteAuthor(authorId: String) {
        if (authorId == communityUserId) return
        val next = if (mutedAuthorIds.contains(authorId)) mutedAuthorIds - authorId else mutedAuthorIds + authorId
        persistMutedAuthors(next)
        persistCommunityPosts(communityPosts.filterNot { next.contains(it.authorId) || blockedAuthorIds.contains(it.authorId) })
    }

    fun onToggleBlockAuthor(authorId: String) {
        if (authorId == communityUserId) return
        val next = if (blockedAuthorIds.contains(authorId)) blockedAuthorIds - authorId else blockedAuthorIds + authorId
        persistBlockedAuthors(next)
        persistCommunityPosts(communityPosts.filterNot { next.contains(it.authorId) || mutedAuthorIds.contains(it.authorId) })
        if (next.contains(authorId)) persistCommunityFollows(followedAuthorIds - authorId)
    }

    fun onReportAuthor(authorId: String) {
        AppLog.w("Community report submitted for author=$authorId by user=$communityUserId")
        scope.launch { snackbarHostState.showSnackbar("Report submitted.") }
    }

    fun onRateCommunityPost(postId: String, starsRaw: Int) {
        val stars = starsRaw.coerceIn(1, 5)
        val previous = myCommunityRatings[postId]
        val updatedPosts = communityPosts.map { post ->
            if (post.id != postId) return@map post
            val count = post.ratingCount.coerceAtLeast(0)
            val sum = post.ratingAverage * count
            if (previous == null) {
                val newCount = count + 1
                post.copy(ratingCount = newCount, ratingAverage = (sum + stars) / newCount)
            } else {
                val safeCount = count.coerceAtLeast(1)
                post.copy(ratingCount = safeCount, ratingAverage = (sum - previous + stars) / safeCount)
            }
        }
        persistCommunityPosts(updatedPosts)
        persistCommunityRatings(myCommunityRatings + (postId to stars))
        scope.launch {
            val ok = SupabaseApi.upsertRatingAndRefreshAggregate(communityUserId, postId, stars)
            if (!ok) {
                enqueueCommunitySyncTask(
                    CommunitySyncTask(
                        type = CommunitySyncTaskType.RATE_POST,
                        postId = postId,
                        stars = stars
                    )
                )
            }
            syncCommunityFromRemote()
        }
    }

    fun onRemixCommunityPost(post: CommunityPost) {
        if (post.template.isPremium && !premiumUnlocked) {
            scope.launch { snackbarHostState.showSnackbar("This is a premium template. Enable Creator Pass (beta) in Settings.") }
            return
        }
        remixPostPending = post
    }

    fun finalizeRemix(post: CommunityPost, applyNow: Boolean) {
        val remixName = "Template: ${post.title}"
        val remixed = cloneTemplateForLibrary(post.template, remixName)
        persistSavedTemplates((savedTemplates + remixed).distinctBy { it.packageId })
        if (applyNow) {
            applyStarterTemplate(remixed)
        }
        val boosted = communityPosts.map { if (it.id == post.id) it.copy(remixCount = it.remixCount + 1) else it }
        persistCommunityPosts(boosted)
        scope.launch {
            val ok = SupabaseApi.incrementRemix(post.id, post.remixCount)
            if (!ok) {
                enqueueCommunitySyncTask(
                    CommunitySyncTask(
                        type = CommunitySyncTaskType.INCREMENT_REMIX,
                        postId = post.id,
                        currentRemixCount = post.remixCount
                    )
                )
            }
            syncCommunityFromRemote()
            snackbarHostState.showSnackbar(if (applyNow) "Template saved and applied." else "Template saved to library.")
        }
    }

    fun awardBonusXp(uniqueId : Int, xp : Int) {
        if (earnedIds.contains(uniqueId)) return
        totalXp += xp; earnedIds = earnedIds + uniqueId; SoundManager.playSuccess(); persistCore()
        val (base, completedIds) = todayBaseAndCompleted(); persistToday(lastDayEpoch, base, completedIds, earnedIds, refreshCount); updateHistory(lastDayEpoch, base, completedIds)
        scope.launch { snackbarHostState.showSnackbar("+$xp XP") }
    }

    fun awardFocusXp(xp: Int) {
        totalXp += xp; SoundManager.playSuccess(); persistCore()
        scope.launch { snackbarHostState.showSnackbar("Focus session: +$xp XP") }
    }

    fun onUpdateQuestProgress(id: Int, newProgress: Int) {
        val updated = quests.map { q ->
            if (q.id == id) q.copy(currentProgress = newProgress.coerceIn(0, q.target + 1)) else q
        }
        quests = updated

        // Save immediately so it survives Theme Changes
        val (base, completedIds) = todayBaseAndCompleted()
        persistToday(lastDayEpoch, base, completedIds, earnedIds, refreshCount)
    }

    fun onUpdateQuestProgressWithUndo(id: Int, newProgress: Int) {
        val before = quests.firstOrNull { it.id == id } ?: return
        val clamped = newProgress.coerceIn(0, before.target + 1)
        if (before.currentProgress == clamped) return

        onUpdateQuestProgress(id, clamped)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val message = when {
                clamped > before.target -> "Ready to claim"
                clamped == before.target -> "Marked done"
                clamped <= 0 -> "Progress reset"
                clamped == 1 -> "Quest started"
                else -> "Progress $clamped/${before.target}"
            }
            val undoResult = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "UNDO",
                duration = SnackbarDuration.Short
            )
            if (undoResult == SnackbarResult.ActionPerformed) {
                val latest = quests.firstOrNull { it.id == id } ?: return@launch
                if (!latest.completed) {
                    onUpdateQuestProgress(id, before.currentProgress)
                }
            }
        }
    }

    fun onResetQuestProgressWithUndo(id: Int) {
        val before = quests.firstOrNull { it.id == id } ?: return
        if (!before.completed && before.currentProgress <= 0) return

        val hadReward = earnedIds.contains(id)
        var newTotalXp = totalXp
        var newGold = gold
        var newAttrs = attributes
        var newEarned = earnedIds

        if (hadReward) {
            newTotalXp = (newTotalXp - before.xpReward).coerceAtLeast(0)
            val rewardGold = calculateGoldReward(before.difficulty, streak)
            newGold = (newGold - rewardGold).coerceAtLeast(0)
            newAttrs = when (before.category) {
                QuestCategory.FITNESS -> newAttrs.copy(str = (newAttrs.str - 1).coerceAtLeast(1))
                QuestCategory.STUDY -> newAttrs.copy(int = (newAttrs.int - 1).coerceAtLeast(1))
                QuestCategory.HYDRATION -> newAttrs.copy(vit = (newAttrs.vit - 1).coerceAtLeast(1))
                QuestCategory.DISCIPLINE -> newAttrs.copy(end = (newAttrs.end - 1).coerceAtLeast(1))
                QuestCategory.MIND -> newAttrs.copy(fth = (newAttrs.fth - 1).coerceAtLeast(1))
            }
            newEarned = newEarned - id
        }

        quests = quests.map { q ->
            if (q.id == id) q.copy(currentProgress = 0, completed = false) else q
        }
        totalXp = newTotalXp
        gold = newGold
        attributes = newAttrs
        earnedIds = newEarned
        persistCore()
        persistAttributes(newAttrs)

        val (base, completedIds) = todayBaseAndCompleted()
        persistToday(lastDayEpoch, base, completedIds, earnedIds, refreshCount)
        updateHistory(lastDayEpoch, base, completedIds)

        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val undoResult = snackbarHostState.showSnackbar(
                message = "Quest progress reset",
                actionLabel = "UNDO",
                duration = SnackbarDuration.Short
            )
            if (undoResult == SnackbarResult.ActionPerformed) {
                val latest = quests.firstOrNull { it.id == id } ?: return@launch
                if (latest.completed || latest.currentProgress != 0) return@launch

                var undoTotalXp = totalXp
                var undoGold = gold
                var undoAttrs = attributes
                var undoEarned = earnedIds

                if (hadReward && !undoEarned.contains(id)) {
                    undoTotalXp += before.xpReward
                    val rewardGold = calculateGoldReward(before.difficulty, streak)
                    undoGold += rewardGold
                    undoAttrs = when (before.category) {
                        QuestCategory.FITNESS -> undoAttrs.copy(str = undoAttrs.str + 1)
                        QuestCategory.STUDY -> undoAttrs.copy(int = undoAttrs.int + 1)
                        QuestCategory.HYDRATION -> undoAttrs.copy(vit = undoAttrs.vit + 1)
                        QuestCategory.DISCIPLINE -> undoAttrs.copy(end = undoAttrs.end + 1)
                        QuestCategory.MIND -> undoAttrs.copy(fth = undoAttrs.fth + 1)
                    }
                    undoEarned = undoEarned + id
                }

                quests = quests.map { q -> if (q.id == id) before else q }
                totalXp = undoTotalXp
                gold = undoGold
                attributes = undoAttrs
                earnedIds = undoEarned
                persistCore()
                persistAttributes(undoAttrs)

                val (undoBase, undoCompletedIds) = todayBaseAndCompleted()
                persistToday(lastDayEpoch, undoBase, undoCompletedIds, earnedIds, refreshCount)
                updateHistory(lastDayEpoch, undoBase, undoCompletedIds)
            }
        }
    }

    fun onToggleQuest(id: Int, force: Boolean = false) {
        val target = quests.firstOrNull { it.id == id } ?: return
        if (!force && target.completed && confirmComplete) { pendingUncheckQuestId = id; return }

        var newEarned = earnedIds
        var newTotalXp = totalXp
        var newGold = gold
        var newAttrs = attributes

        val updated = quests.map { q ->
            if (q.id != id) return@map q
            val newCompleted = !q.completed

            if (newCompleted) {
                // === CHECKING: Award Stuff ===
                if (!newEarned.contains(q.id)) {
                    newTotalXp += q.xpReward
                    val g = calculateGoldReward(q.difficulty, streak)
                    newGold += g
                    newAttrs = when (q.category) {
                        QuestCategory.FITNESS -> newAttrs.copy(str = newAttrs.str + 1)
                        QuestCategory.STUDY -> newAttrs.copy(int = newAttrs.int + 1)
                        QuestCategory.HYDRATION -> newAttrs.copy(vit = newAttrs.vit + 1)
                        QuestCategory.DISCIPLINE -> newAttrs.copy(end = newAttrs.end + 1)
                        QuestCategory.MIND -> newAttrs.copy(fth = newAttrs.fth + 1)
                    }
                    newEarned = newEarned + q.id
                    SoundManager.playSuccess()

                    // BOSS DAMAGE LOGIC REMOVED
                }
            } else {
                // === UNCHECKING: Remove Stuff ===
                if (newEarned.contains(q.id)) {
                    newTotalXp = (newTotalXp - q.xpReward).coerceAtLeast(0)
                    val g = calculateGoldReward(q.difficulty, streak)
                    newGold = (newGold - g).coerceAtLeast(0)
                    newAttrs = when (q.category) {
                        QuestCategory.FITNESS -> newAttrs.copy(str = (newAttrs.str - 1).coerceAtLeast(1))
                        QuestCategory.STUDY -> newAttrs.copy(int = (newAttrs.int - 1).coerceAtLeast(1))
                        QuestCategory.HYDRATION -> newAttrs.copy(vit = (newAttrs.vit - 1).coerceAtLeast(1))
                        QuestCategory.DISCIPLINE -> newAttrs.copy(end = (newAttrs.end - 1).coerceAtLeast(1))
                        QuestCategory.MIND -> newAttrs.copy(fth = (newAttrs.fth - 1).coerceAtLeast(1))
                    }
                    newEarned = newEarned - q.id

                    // BOSS HEAL LOGIC REMOVED
                }
            }
            q.copy(completed = newCompleted)
        }

        totalXp = newTotalXp; gold = newGold; quests = updated; earnedIds = newEarned; attributes = newAttrs
        persistCore()
        persistAttributes(newAttrs)

        val (base, completedIds) = todayBaseAndCompleted()
        persistToday(lastDayEpoch, base, completedIds, earnedIds, refreshCount)
        updateHistory(lastDayEpoch, base, completedIds)
        val completedNow = updated.firstOrNull { it.id == id }?.completed == true
        if (completedNow) {
            AppLog.event("quest_done", "quest=$id")
        }
    }

    fun onClaimQuestWithUndo(id: Int) {
        val questBefore = quests.firstOrNull { it.id == id } ?: return
        if (questBefore.completed || questBefore.currentProgress <= questBefore.target) return

        onToggleQuest(id)
        val claimed = quests.firstOrNull { it.id == id }?.completed == true
        if (!claimed) return

        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val undoResult = snackbarHostState.showSnackbar(
                message = "+${questBefore.xpReward} XP earned",
                actionLabel = "UNDO",
                duration = SnackbarDuration.Short
            )
            if (undoResult == SnackbarResult.ActionPerformed) {
                val latestQuest = quests.firstOrNull { it.id == id } ?: return@launch
                if (latestQuest.completed) {
                    onToggleQuest(id, force = true)
                }
            }
        }
    }

    fun onBuyShopItem(item: ShopItem) {
        if (item.cost <= 0) {
            scope.launch { snackbarHostState.showSnackbar("Invalid item price") }
            return
        }
        if (item.stock <= 0) {
            scope.launch { snackbarHostState.showSnackbar("${item.name} is out of stock") }
            return
        }
        if (gold < item.cost) {
            scope.launch { snackbarHostState.showSnackbar("Not enough Gold") }
            return
        }
        gold -= item.cost
        persistCore()

        val updatedShop = shopItems.map {
            if (it.id == item.id) it.copy(stock = (it.stock - 1).coerceAtLeast(0)) else it
        }
        persistShopItems(updatedShop)
        SoundManager.playSuccess()
        AppLog.event("shop_buy", "item=${item.id},cost=${item.cost}")
        scope.launch { snackbarHostState.showSnackbar("Used ${item.name}") }
    }

    fun onUpsertShopItem(item: ShopItem) {
        if (!customMode) {
            scope.launch { snackbarHostState.showSnackbar("Enable Custom Mode in Settings.") }
            return
        }
        val cappedMax = item.maxStock.coerceAtLeast(1)
        val cappedStock = item.stock.coerceIn(0, cappedMax)
        val sanitized = item.copy(maxStock = cappedMax, stock = cappedStock, cost = item.cost.coerceAtLeast(1))
        val list = shopItems.toMutableList()
        val idx = list.indexOfFirst { it.id == sanitized.id }
        if (idx >= 0) list[idx] = sanitized else list.add(sanitized)
        persistShopItems(list)
        scope.launch { snackbarHostState.showSnackbar("Shop item saved") }
    }

    fun onDeleteShopItem(id: String) {
        if (!customMode) {
            scope.launch { snackbarHostState.showSnackbar("Enable Custom Mode in Settings.") }
            return
        }
        val removed = shopItems.firstOrNull { it.id == id } ?: return
        persistShopItems(shopItems.filterNot { it.id == id })
        scope.launch {
            val res = snackbarHostState.showSnackbar("Shop item removed", actionLabel = "UNDO", duration = SnackbarDuration.Short)
            if (res == SnackbarResult.ActionPerformed) {
                persistShopItems((shopItems + removed).distinctBy { it.id })
            }
        }
    }

    fun onConsumeItem(item: InventoryItem) {
        if (item.ownedCount > 0) {
            val newInv = if (item.ownedCount > 1) {
                inventory.map { if (it.id == item.id) it.copy(ownedCount = it.ownedCount - 1) else it }
            } else {
                inventory.filter { it.id != item.id }
            }
            persistInventory(newInv); scope.launch { snackbarHostState.showSnackbar("Used ${item.name}") }
        }
    }

    fun onAddPlan(day: Long, text: String) {
        val clean = text.trim()
        if (clean.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("Plan title required.") }
            return
        }
        val next = calendarPlans.toMutableMap()
        val current = next[day].orEmpty()
        next[day] = current + clean
        persistCalendarPlans(next)
        pushPlanStateToSupabase(next)
        AppLog.event("plan_add", "day=$day")
        scope.launch { snackbarHostState.showSnackbar("Plan added") }
    }

    fun onDeletePlan(day: Long, index: Int) {
        val next = calendarPlans.toMutableMap()
        val current = next[day].orEmpty().toMutableList()
        if (index !in current.indices) return
        val removed = current.removeAt(index)
        if (current.isEmpty()) next.remove(day) else next[day] = current
        persistCalendarPlans(next)
        pushPlanStateToSupabase(next)
        scope.launch {
            val res = snackbarHostState.showSnackbar("Plan removed", actionLabel = "UNDO", duration = SnackbarDuration.Short)
            if (res == SnackbarResult.ActionPerformed) {
                val restored = calendarPlans.toMutableMap()
                val list = restored[day].orEmpty().toMutableList()
                val insertAt = index.coerceIn(0, list.size)
                list.add(insertAt, removed)
                restored[day] = list
                persistCalendarPlans(restored)
                pushPlanStateToSupabase(restored)
            }
        }
    }

    fun restoreDefaultQuests() {
        val defaults = getInitialDefaultPool()

        // Merge logic: If we have a default quest, update its target/definition to the new one
        val currentMap = customTemplates.associateBy { it.title }.toMutableMap()
        var added = 0

        defaults.forEach { def ->
            if (!currentMap.containsKey(def.title)) {
                currentMap[def.title] = def
                added++
            } else {
                // Update existing default to new target (e.g. Water 1 -> Water 2)
                val existing = currentMap[def.title]!!
                if (existing.target != def.target) {
                    currentMap[def.title] = existing.copy(target = def.target)
                }
            }
        }

        persistCustomTemplates(currentMap.values.toList())
        scope.launch { snackbarHostState.showSnackbar("Quest pool updated. Tap 'Start New Day' to apply.") }
    }
    fun onRefreshTodayQuests() {
        if (homeRefreshInProgress) return
        homeRefreshInProgress = true
        try {
            if (refreshCount >= 3) {
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        message = "Refresh limit reached",
                        duration = SnackbarDuration.Short,
                        withDismissAction = true // Adds "X" button
                    )
                }
                return
            }

            val seed = System.currentTimeMillis()
            val refreshed = if (refreshIncompleteOnly) {
                refreshKeepingCompleted(
                    current = quests,
                    playerLevel = currentLevel,
                    seed = seed,
                    pool = customTemplatesToQuestTemplates(customTemplates.filter { it.isActive }),
                    desiredCount = dailyQuestTarget
                )
            } else {
                generateDailyQuestsAdaptive(
                    seed = secondSeedForGeneration(seed / 1000L),
                    playerLevel = currentLevel,
                    pool = customTemplatesToQuestTemplates(customTemplates.filter { it.isActive }),
                    history = historyMap,
                    recentFailedTitles = quests.filter { !it.completed }.map { it.title }.toSet(),
                    completedQuests = quests.filter { it.completed },
                    difficultyPreference = difficultyPreference,
                    desiredCount = dailyQuestTarget
                )
            }

            quests = refreshed
            refreshCount += 1

            val base = refreshed.map { it.copy(completed = false) }
            val completedIds = refreshed.filter { it.completed }.map { it.id }.toSet()

            persistToday(lastDayEpoch, base, completedIds, earnedIds, refreshCount)
            updateHistory(lastDayEpoch, base, completedIds)

            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    message = "Quests rerolled",
                    duration = SnackbarDuration.Short,
                    withDismissAction = true // Adds "X" button
                )
            }
        } finally {
            scope.launch {
                delay(450)
                homeRefreshInProgress = false
            }
        }
    }

    fun onDeleteMainQuest(id: String) {
        persistMainQuests(mainQuests.filterNot { it.id == id }); scope.launch { snackbarHostState.showSnackbar("Deleted") }
    }

    fun onUpdateMainQuest(updated: CustomMainQuest) {
        // FIXED: Capture the previous state BEFORE we update the list
        val previous = mainQuests.find { it.id == updated.id }

        // Save the new state
        persistMainQuests(mainQuests.map { if (it.id == updated.id) updated else it })

        // Check: If it WAS NOT claimed before, and IS claimed now -> Give XP
        if (previous != null && !previous.isClaimed && updated.isClaimed) {
            awardBonusXp(updated.id.hashCode(), updated.xpReward)
            SoundManager.playSuccess() // Play the Congratz sound
        }
    }

    fun onResetMainQuestWithUndo(id: String) {
        val before = mainQuests.find { it.id == id } ?: return
        val reset = before.copy(currentStep = 0, hasStarted = false, isClaimed = false)
        if (before == reset) return

        val bonusUniqueId = before.id.hashCode()
        val hadAwardedXp = earnedIds.contains(bonusUniqueId)
        var newTotalXp = totalXp
        var newEarned = earnedIds
        if (hadAwardedXp) {
            newTotalXp = (newTotalXp - before.xpReward).coerceAtLeast(0)
            newEarned = newEarned - bonusUniqueId
        }

        persistMainQuests(mainQuests.map { if (it.id == id) reset else it })
        totalXp = newTotalXp
        earnedIds = newEarned
        persistCore()

        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val undoResult = snackbarHostState.showSnackbar(
                message = "Main quest reset",
                actionLabel = "UNDO",
                duration = SnackbarDuration.Short
            )
            if (undoResult == SnackbarResult.ActionPerformed) {
                val latest = mainQuests.find { it.id == id } ?: return@launch
                if (latest != reset) return@launch

                var undoTotalXp = totalXp
                var undoEarned = earnedIds
                if (hadAwardedXp && !undoEarned.contains(bonusUniqueId)) {
                    undoTotalXp += before.xpReward
                    undoEarned = undoEarned + bonusUniqueId
                }

                persistMainQuests(mainQuests.map { if (it.id == id) before else it })
                totalXp = undoTotalXp
                earnedIds = undoEarned
                persistCore()
            }
        }
    }

    fun resetAll(saveBackup: Boolean) {
        scope.launch {
            val backupTemplate = if (saveBackup) {
                GameTemplate(
                    templateName = resetBackupName.ifBlank { "Pre-reset backup" },
                    appTheme = appTheme,
                    dailyQuests = customTemplatesToQuestTemplates(customTemplates),
                    mainQuests = mainQuests,
                    shopItems = shopItems,
                    templateSettings = currentTemplateSettings(),
                    accentArgb = accent.toArgbCompat().toLong()
                )
            } else null
            appContext.dataStore.edit { it.clear() }
            screen = Screen.HOME
            totalXp = 0; gold = 0; currentLevel = 1; streak = 0; bestStreak = 0; refreshCount = 0
            lastDayEpoch = currentEpochDay()
            earnedIds = emptySet()
            quests = emptyList()
            journalPages = emptyList()
            grimoirePageIndex = 0
            avatar = Avatar.Preset("🧑‍🚀")
            characterData = CharacterData()

            autoNewDay = true; confirmComplete = true; refreshIncompleteOnly = true
            customMode = false
            advancedOptions = false
            highContrastText = false
            compactMode = false
            largerTouchTargets = false
            reduceAnimations = false
            decorativeBorders = false
            neonLightBoost = false
            neonFlowEnabled = false
            neonFlowSpeed = 0
            neonGlowPalette = "magenta"
            alwaysShowQuestProgress = true
            hideCompletedQuests = false
            confirmDestructiveActions = true
            dailyResetHour = 0
            dailyRemindersEnabled = true
            hapticsEnabled = true
            soundEffectsEnabled = true
            onboardingGoal = OnboardingGoal.BALANCE
            difficultyPreference = DifficultyPreference.NORMAL
            premiumUnlocked = false
            dailyQuestTarget = 5
            settingsExpandedSection = "gameplay"
            questsPreferredTab = 0
            cloudSyncEnabled = true
            cloudAccountEmail = ""
            cloudConnectedAccount = null
            cloudLastSyncAt = 0L
            fontStyle = AppFontStyle.DEFAULT
            fontScalePercent = 100
            backgroundImageUri = null
            backgroundImageTransparencyPercent = 78
            textColorOverride = null
            appBackgroundColorOverride = null
            chromeBackgroundColorOverride = null
            cardColorOverride = null
            buttonColorOverride = null
            journalPageColorOverride = null
            journalAccentColorOverride = null
            journalName = "Journal"
            val defaultTemplate = getDefaultGameTemplate()
            appTheme = if (systemPrefersDark) AppTheme.DEFAULT else AppTheme.LIGHT
            accent = defaultTemplate.accentArgb?.let { Color(it.toInt()) } ?: fallbackAccentForTheme(appTheme)

            unlockedAchievementIds = emptySet()
            inventory = emptyList()
            shopItems = defaultTemplate.shopItems.ifEmpty { getDefaultShopItems() }
            calendarPlans = emptyMap()
            attributes = PlayerAttributes(1, 1, 1, 1, 1)
            activePackageIds = setOf(defaultTemplate.packageId)
            communityPosts = emptyList()
            followedAuthorIds = emptySet()
            mutedAuthorIds = emptySet()
            blockedAuthorIds = emptySet()
            myCommunityRatings = emptyMap()
            pendingCommunitySyncQueue = emptyList()
            lastCommunityPublishAt = 0L
            shopTutorialSeen = false
            calendarTutorialSeen = false
            questsTutorialSeen = false
            shopHoldHintSeen = false

            val defaults = defaultTemplate.dailyQuests.map { qt ->
                CustomTemplate(
                    id = UUID.randomUUID().toString(),
                    category = qt.category,
                    difficulty = qt.difficulty,
                    title = qt.title,
                    icon = qt.icon,
                    xp = qt.xp,
                    target = qt.target,
                    isPinned = qt.isPinned,
                    imageUri = qt.imageUri,
                    packageId = defaultTemplate.packageId
                )
            }
            customTemplates = defaults
            val mqDefaults = defaultTemplate.mainQuests
            val templatesAfterReset = listOfNotNull(backupTemplate)
            savedTemplates = templatesAfterReset

            persistCore(); persistSettings(); persistAvatar(avatar); persistCustomTemplates(defaults)
            persistCharacter(characterData); persistMainQuests(mqDefaults)
            persistInventory(emptyList())
            persistShopItems(shopItems)
            persistCalendarPlans(emptyMap())
            persistSavedTemplates(templatesAfterReset)
            persistCommunityPosts(emptyList())
            persistCommunityFollows(emptySet())
            persistMutedAuthors(emptySet())
            persistBlockedAuthors(emptySet())
            persistCommunityRatings(emptyMap())
            persistCommunitySyncQueue(emptyList())
            appContext.dataStore.edit { p -> p[Keys.ONBOARDING_DONE] = false }
            appContext.dataStore.edit { p -> p[activePacksKey] = activePackageIds.joinToString(",") }
            customTemplatesToQuestTemplates(customTemplates.filter { it.isActive })
            showWelcomeSetup = true
            onboardingSkipIntroDefault = false
            snackbarHostState.showSnackbar("Reset complete. Default pack enabled.")
        }
    }

    LaunchedEffect(soundEffectsEnabled) {
        SoundManager.setEnabled(soundEffectsEnabled)
    }
    LaunchedEffect(hapticsEnabled) {
        SoundManager.setHapticsEnabled(hapticsEnabled)
    }

    LivingLifeMMOTheme(
        theme = runtimeTheme,
        accentOverride = accentStrong,
        backgroundOverride = themeBg,
        cardColorOverride = cardColorOverride,
        highContrastTextEnabled = highContrastText,
        reduceAnimationsEnabled = reduceAnimations,
        compactModeEnabled = compactMode,
        largerTouchTargetsEnabled = largerTouchTargets,
        decorativeBordersEnabled = neonFlowEnabled,
        neonLightBoostEnabled = neonLightBoost,
        neonFlowEnabled = neonFlowEnabled,
        neonFlowSpeed = neonFlowSpeed,
        neonGlowPalette = neonGlowPalette,
        fontStyle = fontStyle,
        fontScalePercent = fontScalePercent,
        textColorOverride = textColorOverride
    ) {
        if (showRefreshDayConfirm) { AlertDialog(onDismissRequest = { showRefreshDayConfirm = false }, title = { Text("Start New Day?") }, text = { Text("This forces a new day calculation.") }, confirmButton = { TextButton(onClick = { onRefreshDay(); showRefreshDayConfirm = false }) { Text("Start Day") } }, dismissButton = { TextButton(onClick = { showRefreshDayConfirm = false }) { Text("Cancel") } }) }
        if (showLevelUpDialog) { AlertDialog(onDismissRequest = { showLevelUpDialog = false }, title = { Text("LEVEL UP!", fontWeight = FontWeight.Bold, color = accentStrong) }, text = { Text("Congratulations! You've reached Level $currentLevel. Your legend grows!") }, confirmButton = { TextButton(onClick = { showLevelUpDialog = false }) { Text("AWESOME") } }) }
        if (showBackupImport) {
            AlertDialog(
                onDismissRequest = { showBackupImport = false },
                title = { Text("Import Encrypted Backup") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Paste your encrypted backup. Import applies immediately.", color = OnCardText.copy(alpha = 0.75f), fontSize = 12.sp)
                        OutlinedTextField(
                            value = backupImportPayload,
                            onValueChange = { backupImportPayload = it },
                            label = { Text("Backup payload") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val ok = importBackupPayload(backupImportPayload)
                        showBackupImport = false
                        backupImportPayload = ""
                        scope.launch { snackbarHostState.showSnackbar(if (ok) "Backup imported." else "Backup import failed.") }
                    }) { Text("Import") }
                },
                dismissButton = {
                    TextButton(onClick = { showBackupImport = false }) { Text("Cancel") }
                }
            )
        }
        if (showResetAll) {
            AlertDialog(
                onDismissRequest = { showResetAll = false },
                title = { Text("Reset everything?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("This will erase progress and active data.")
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { resetBackupBefore = !resetBackupBefore },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = resetBackupBefore, onCheckedChange = { resetBackupBefore = it })
                        Text("Save current setup to Template before reset")
                        }
                        if (resetBackupBefore) {
                            OutlinedTextField(
                                value = resetBackupName,
                                onValueChange = { resetBackupName = it },
                                label = { Text("Backup name") },
                                singleLine = true
                            )
                        }
                        Text("Default package will be enabled automatically after reset.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        resetAll(resetBackupBefore)
                        showResetAll = false
                    }) { Text("Reset") }
                },
                dismissButton = { TextButton(onClick = { showResetAll = false }) { Text("Cancel") } }
            )
        }
        if (remixPostPending != null) {
            AlertDialog(
                onDismissRequest = { remixPostPending = null },
                containerColor = CardDarkBlue,
                title = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Add Template From Community?",
                            color = OnCardText,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(end = 40.dp)
                        )
                        IconButton(
                            onClick = { remixPostPending = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = OnCardText.copy(alpha = 0.82f)
                            )
                        }
                    }
                },
                text = {
                    Text(
                        "You're adding '${remixPostPending!!.title}' as a template from community content.",
                        color = OnCardText.copy(alpha = 0.86f)
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val post = remixPostPending
                        remixPostPending = null
                        if (post != null) finalizeRemix(post, applyNow = true)
                    }) { Text("Save & Apply", color = accentStrong, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        val post = remixPostPending
                        remixPostPending = null
                        if (post != null) finalizeRemix(post, applyNow = false)
                    }) { Text("Save", color = OnCardText) }
                }
            )
        }
        if (pendingUncheckQuestId != null) { AlertDialog(onDismissRequest = { pendingUncheckQuestId = null }, title = { Text("Uncheck quest?") }, text = { Text("This will remove earned XP, but keep earned Gold/Stats.") }, confirmButton = { TextButton(onClick = { val id = pendingUncheckQuestId; pendingUncheckQuestId = null; if (id != null) onToggleQuest(id, force = true) }) { Text("Uncheck") } }, dismissButton = { TextButton(onClick = { pendingUncheckQuestId = null }) { Text("Cancel") } }) }
        if (showFocusTimer) { FocusTimerDialog(accentStrong = accentStrong, accentSoft = accentSoft, onDismiss = { showFocusTimer = false }, onComplete = { minutes -> awardFocusXp(minutes); showFocusTimer = false }) }

// 1. Initial Import Dialog
        if (pendingImportTemplate != null) {
            AlertDialog(
                onDismissRequest = { pendingImportTemplate = null },
                containerColor = CardDarkBlue,
                title = { Text("Save Template?", color = accentStrong, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("You are about to save:", color = OnCardText)
                        Text("• ${pendingImportTemplate!!.templateName}", fontWeight = FontWeight.Bold, color = accentStrong)
                        Text("• ${pendingImportTemplate!!.dailyQuests.size} Daily Quests", color = OnCardText)
                        Text("• ${pendingImportTemplate!!.mainQuests.size} Main Quests", color = OnCardText)
                        Text("• ${pendingImportTemplate!!.shopItems.size} Shop Items", color = OnCardText)
                        Text("• Theme: ${pendingImportTemplate!!.appTheme.name}", color = accentStrong)
                        Text("• Includes advanced options + background if present", color = OnCardText.copy(alpha = 0.85f))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val t = pendingImportTemplate!!
                        persistSavedTemplates(savedTemplates + t)
                        promptApplyTemplate = t // NEW: Trigger the follow-up dialog
                        pendingImportTemplate = null
                    }) { Text("Save to Template", color = accentStrong) }
                },
                dismissButton = { TextButton(onClick = { pendingImportTemplate = null }) { Text("Cancel", color = OnCardText) } }
            )
        }

// 2. Follow-Up Apply Prompt (With Backup & Clear Checkboxes)
        if (promptApplyTemplate != null) {
            AlertDialog(
                onDismissRequest = { promptApplyTemplate = null },
                containerColor = CardDarkBlue,
                title = { Text("Equip Template?", color = accentStrong, fontWeight = FontWeight.Bold) },
                text = {
                    val t = promptApplyTemplate!!
                    val dailyDelta = t.dailyQuests.size - customTemplates.size
                    val mainDelta = t.mainQuests.size - mainQuests.size
                    val shopDelta = t.shopItems.size - shopItems.size
                    val minCost = t.shopItems.minOfOrNull { it.cost.coerceAtLeast(1) } ?: 0
                    val affordableCount = t.shopItems.count { gold >= it.cost.coerceAtLeast(1) }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Template saved! Are you sure you want to apply it now?", color = OnCardText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("This will change theme, background, advanced settings, and quests.", color = OnCardText.copy(alpha = 0.85f), fontSize = 13.sp)
                        Text(
                            "Preview: Daily ${if (dailyDelta >= 0) "+" else ""}$dailyDelta, Main ${if (mainDelta >= 0) "+" else ""}$mainDelta, Shop ${if (shopDelta >= 0) "+" else ""}$shopDelta",
                            color = OnCardText.copy(alpha = 0.84f),
                            fontSize = 12.sp
                        )
                        Text(
                            "Economy: ${if (minCost > 0) "min item $minCost gold" else "no shop items"} • affordable now $affordableCount/${t.shopItems.size}",
                            color = OnCardText.copy(alpha = 0.72f),
                            fontSize = 12.sp
                        )
                        HorizontalDivider(color = OnCardText.copy(alpha=0.1f))

                        // NEW: Clear Existing Checkbox
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { importClearExisting = !importClearExisting }) {
                            Checkbox(checked = importClearExisting, onCheckedChange = { importClearExisting = it }, colors = CheckboxDefaults.colors(checkedColor = accentStrong))
                            Text("Clear my current quests first", color = OnCardText, fontSize = 14.sp)
                        }

                        // Backup Checkbox
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { importBackupBeforeApply = !importBackupBeforeApply }) {
                            Checkbox(checked = importBackupBeforeApply, onCheckedChange = { importBackupBeforeApply = it }, colors = CheckboxDefaults.colors(checkedColor = accentStrong))
                            Text("Backup current setup to Template", color = OnCardText, fontSize = 14.sp)
                        }
                        if (importBackupBeforeApply) {
                            OutlinedTextField(value = importBackupName, onValueChange = { importBackupName = it }, label = { Text("Backup Name", color = OnCardText.copy(alpha=0.5f)) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OnCardText, unfocusedTextColor = OnCardText, cursorColor = accentStrong))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        // 1. Handle Backup First!
                        if (importBackupBeforeApply) {
                            val backupNameFinal = importBackupName.ifBlank { "My Backup" }
                            val backup = GameTemplate(
                                backupNameFinal,
                                appTheme,
                                customTemplatesToQuestTemplates(customTemplates),
                                mainQuests,
                                shopItems,
                                templateSettings = currentTemplateSettings(),
                                accentArgb = accent.toArgbCompat().toLong()
                            )
                            persistSavedTemplates(savedTemplates + backup)
                        }

                        // 2. Apply the Imported Template
                        val t = promptApplyTemplate!!
                        appTheme = normalizeTheme(t.appTheme)
                        accent = t.accentArgb?.let { Color(it.toInt()) } ?: fallbackAccentForTheme(appTheme)
                        applyTemplateSettings(t.templateSettings)
                        persistSettings()

                        val mappedDailies = t.dailyQuests.map { qt ->
                            CustomTemplate(
                                id = UUID.randomUUID().toString(),
                                category = qt.category,
                                difficulty = qt.difficulty,
                                title = qt.title,
                                icon = qt.icon,
                                xp = qt.xp,
                                target = qt.target,
                                isPinned = qt.isPinned,
                                imageUri = qt.imageUri,
                                packageId = t.packageId
                            )
                        }

                        if (importClearExisting) {
                            persistCustomTemplates(mappedDailies)
                            persistMainQuests(t.mainQuests)
                            persistShopItems(t.shopItems.ifEmpty { getDefaultShopItems() })
                            activePackageIds = setOf(t.packageId)
                            applyTemplateDailyQuestDefaults(t.packageId, clearExisting = true)
                            persistSettings()
                        } else {
                            val newCustoms = customTemplates + mappedDailies
                            persistCustomTemplates(newCustoms.distinctBy { it.title })
                            val newMqs = mainQuests + t.mainQuests
                            persistMainQuests(newMqs.distinctBy { it.title })
                            if (t.shopItems.isNotEmpty()) {
                                val mergedShop = (shopItems + t.shopItems).distinctBy { it.id }
                                persistShopItems(mergedShop)
                            }
                            activePackageIds = activePackageIds + t.packageId
                        }
                        scope.launch { appContext.dataStore.edit { p -> p[activePacksKey] = activePackageIds.joinToString(",") } }

                        scope.launch { snackbarHostState.showSnackbar("Theme & Quests Applied!") }
                        promptApplyTemplate = null
                    }) { Text("Yes, Equip Now", color = accentStrong) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        scope.launch { snackbarHostState.showSnackbar("Template saved.") }
                        promptApplyTemplate = null
                    }) { Text("No, Later", color = OnCardText) }
                }
            )
        }

        fun onTogglePackage(template: GameTemplate, isActive: Boolean) {
            val pid = template.packageId

            if (isActive) {
                // ENABLE: Add ID and Quests
                activePackageIds = activePackageIds + pid

                // 1. Add Dailies (Avoid duplicates)
                val newDailies = template.dailyQuests.map { qt ->
                    CustomTemplate(UUID.randomUUID().toString(), qt.category, qt.difficulty, qt.title, qt.icon, qt.xp, qt.target, qt.isPinned, qt.imageUri, pid)
                }
                val currentTitles = customTemplates.map { it.title }
                val toAdd = newDailies.filter { !currentTitles.contains(it.title) }
                persistCustomTemplates(customTemplates + toAdd)

                // 2. Add Main Quests
                val newMains = template.mainQuests.map { mq ->
                    // Ensure the ID matches what's in the template so prerequisites work
                    mq.copy(packageId = pid)
                }
                // Filter out if we somehow already have them to prevent ID collisions
                val currentMqIds = mainQuests.map { it.id }
                val mainsToAdd = newMains.filter { !currentMqIds.contains(it.id) }
                persistMainQuests(mainQuests + mainsToAdd)

                SoundManager.playAccept() // Nice feedback

            } else {
                // DISABLE: Remove ID and Quests
                activePackageIds = activePackageIds - pid

                // 1. Remove Dailies belonging to this pack
                persistCustomTemplates(customTemplates.filter { it.packageId != pid })

                // 2. Remove Main Quests belonging to this pack
                persistMainQuests(mainQuests.filter { it.packageId != pid })

                SoundManager.playClick()
            }

            // Save the active list
            scope.launch { appContext.dataStore.edit { p -> p[activePacksKey] = activePackageIds.joinToString(",") } }
        }
        fun swipeNavIndex(target: Screen): Float = when (target) {
            Screen.HOME -> 0f
            Screen.MAINQUEST -> 1f
            Screen.GRIMOIRE -> 2f
            else -> 0f
        }
        fun swipeNavEmphasis(target: Screen): Float {
            val usingSwipeScreens = screen == Screen.HOME || screen == Screen.MAINQUEST || screen == Screen.GRIMOIRE
            val visual = if (usingSwipeScreens) swipeVisualProgress.coerceIn(0f, 2f) else swipeNavIndex(screen)
            return (1f - abs(visual - swipeNavIndex(target))).coerceIn(0f, 1f)
        }
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                val drawerShape = RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp)
                ModalDrawerSheet(
                    modifier = Modifier
                        .clip(drawerShape)
                        .background(drawerBg),
                    drawerContainerColor = drawerBg
                ) {
                    Spacer(Modifier.height(16.dp))
                    Text("Questify", modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = drawerContentColor)
                    DrawerItem(stringResource(R.string.nav_home), Icons.Default.Home, screen == Screen.HOME, accentStrong, drawerBg, drawerContentColor) { SoundManager.playClick(); screen = Screen.HOME; scope.launch { drawerState.close() } }
                    DrawerItem(stringResource(R.string.nav_dashboard), Icons.Default.QueryStats, screen == Screen.STATS, accentStrong, drawerBg, drawerContentColor) { SoundManager.playClick(); screen = Screen.STATS; scope.launch { drawerState.close() } }
                    DrawerItem(stringResource(R.string.title_shop), Icons.Default.Backpack, screen == Screen.INVENTORY, accentStrong, drawerBg, drawerContentColor) { SoundManager.playClick(); screen = Screen.INVENTORY; scope.launch { drawerState.close() } }
                    DrawerItem(stringResource(R.string.title_calendar), Icons.Default.Today, screen == Screen.CALENDAR, accentStrong, drawerBg, drawerContentColor) { SoundManager.playClick(); screen = Screen.CALENDAR; scope.launch { drawerState.close() } }
                    DrawerItem(stringResource(R.string.title_quests_templates), Icons.Default.Checklist, screen == Screen.QUESTS, accentStrong, drawerBg, drawerContentColor) { SoundManager.playClick(); questsPreferredTab = 0; screen = Screen.QUESTS; scope.launch { drawerState.close() } }
                    DrawerItem("${stringResource(R.string.title_community)} | Experimental", Icons.Default.People, screen == Screen.COMMUNITY, accentStrong, drawerBg, drawerContentColor) { SoundManager.playClick(); screen = Screen.COMMUNITY; scope.launch { drawerState.close() } }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = drawerContentColor.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    DrawerItem(stringResource(R.string.title_settings), Icons.Default.Settings, screen == Screen.SETTINGS, accentStrong, drawerBg, drawerContentColor) { SoundManager.playClick(); screen = Screen.SETTINGS; scope.launch { drawerState.close() } }
                    DrawerItem(stringResource(R.string.title_about), Icons.Default.Info, screen == Screen.ABOUT, accentStrong, drawerBg, drawerContentColor) { SoundManager.playClick(); screen = Screen.ABOUT; scope.launch { drawerState.close() } }
                }
            }
        ) {
            Scaffold(
                containerColor = themeBg,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState) { data ->
                        // FIXED: Detect swipe and dismiss IMMEDIATELY to free up the FAB
                        @Suppress("DEPRECATION")
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value != SwipeToDismissBoxValue.Settled) {
                                    data.dismiss() // Kill the timer and the box instantly
                                    true
                                } else {
                                    false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {}, // No background needed
                            content = {
                                Snackbar(
                                    snackbarData = data,
                                    containerColor = Color(0xFF333333), // Dark Grey
                                    contentColor = Color.White,
                                    actionColor = accentStrong,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        )
                    }
                },
                bottomBar = {
                    if (screen == Screen.HOME || screen == Screen.MAINQUEST || screen == Screen.GRIMOIRE) {
                        Surface(color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
                            val navDockShape = RoundedCornerShape(18.dp)
                            val navDockBg = Color.Transparent
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .clip(navDockShape)
                                    .background(navDockBg)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TikTokNavButton(stringResource(R.string.title_daily_quests), Icons.Default.Checklist, screen == Screen.HOME, accentStrong, navContentColor, navBarBg, emphasis = swipeNavEmphasis(Screen.HOME)) { SoundManager.playClick(); screen = Screen.HOME }
                                    TikTokNavButton(stringResource(R.string.title_main_quests), Icons.Default.Star, screen == Screen.MAINQUEST, accentStrong, navContentColor, navBarBg, emphasis = swipeNavEmphasis(Screen.MAINQUEST)) { SoundManager.playClick(); screen = Screen.MAINQUEST }
                                    TikTokNavButton(stringResource(R.string.title_journal), Icons.AutoMirrored.Filled.MenuBook, screen == Screen.GRIMOIRE, accentStrong, navContentColor, navBarBg, emphasis = swipeNavEmphasis(Screen.GRIMOIRE)) { SoundManager.playClick(); screen = Screen.GRIMOIRE }
                                }
                            }
                        }
                    }
                }) { padding ->
                val drawerClosed = drawerState.currentValue == DrawerValue.Closed && !drawerState.isAnimationRunning
                val renderScreen: @Composable (Screen) -> Unit = { target ->
                    CompositionLocalProvider(LocalHeaderThemeToggle provides {
                        appTheme = if (appTheme == AppTheme.LIGHT) AppTheme.DEFAULT else AppTheme.LIGHT
                        if (buttonColorOverride == null) {
                            accent = fallbackAccentForTheme(appTheme)
                        }
                        persistSettings()
                    }) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (!backgroundImageUri.isNullOrBlank()) {
                            AsyncImage(
                                model = backgroundImageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (isThemeBgLight) {
                                            Color.White.copy(alpha = 0.70f * (1f - (backgroundImageTransparencyPercent.coerceIn(0, 100) / 100f)))
                                        } else {
                                            Color.Black.copy(alpha = 0.45f * (1f - (backgroundImageTransparencyPercent.coerceIn(0, 100) / 100f)))
                                        }
                                    )
                            )
                        }
                        Surface(modifier = Modifier.fillMaxSize(), color = if (backgroundImageUri.isNullOrBlank()) themeBg else themeBg.copy(alpha = 1f - (backgroundImageTransparencyPercent.coerceIn(0, 100) / 100f))) {
                        when (target) {
                            // FIXED: Screen.HOME now passes the bosses list
                            Screen.HOME -> HomeScreen(
                                Modifier.fillMaxSize().padding(padding), appContext, quests.filter { !hideCompletedQuests || !it.completed }, bosses, avatar, calculateLevel(totalXp), attributes, streak, accentStrong, accentSoft, refreshCount, gold,
                                alwaysShowQuestProgress,
                                { onRefreshTodayQuests() },
                                homeRefreshInProgress,
                                { onClaimQuestWithUndo(it) },
                                { id, prog -> onUpdateQuestProgressWithUndo(id, prog) }, // NEW: Pass the progress handler
                                { id -> onResetQuestProgressWithUndo(id) },
                                communityUserName,
                                { name ->
                                    val fixed = name.trim().ifBlank { communityUserName }
                                    persistCommunityProfile(communityUserId, fixed)
                                },
                                { persistAvatar(it) },
                                {
                                    SoundManager.playClick()
                                    if (it == Screen.QUESTS) questsPreferredTab = 0
                                    screen = it
                                },
                                { scope.launch { drawerState.open() } },
                                { showFocusTimer = true }
                            )



                            Screen.MAINQUEST -> MainQuestsScreen(
                                modifier = Modifier.fillMaxSize().padding(padding),
                                quests = mainQuests.filter { it.isActive && (!hideCompletedQuests || !it.isClaimed) },
                                accentStrong = accentStrong,
                                accentSoft = accentSoft,
                                onUpdate = { onUpdateMainQuest(it) },
                                onResetProgress = { onResetMainQuestWithUndo(it) },
                                onDelete = { onDeleteMainQuest(it) },
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onOpenSettings = { screen = Screen.SETTINGS },
                                onOpenCustomMainQuests = {
                                    questsPreferredTab = 1
                                    screen = Screen.QUESTS
                                }

                            )

                            Screen.GRIMOIRE -> JournalScreen(
                                modifier = Modifier.fillMaxSize().padding(padding),
                                pages = journalPages,
                                accentSoft = accentSoft,
                                journalPageColorOverride = journalPageColorOverride,
                                journalAccentColorOverride = journalAccentColorOverride,
                                journalName = journalName,
                                onJournalNameChanged = {
                                    journalName = it.take(24).ifBlank { "Journal" }
                                    persistSettings()
                                },
                                onUpdatePages = {
                                    journalPages = it
                                    scope.launch { persistJournal(appContext, it) }
                                    checkAchievements()
                                },
                                onBack = { SoundManager.playClick(); screen = Screen.HOME },
                                pageIndexExternal = grimoirePageIndex,
                                onPageIndexExternalChange = { grimoirePageIndex = it },
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onOpenSettings = { screen = Screen.SETTINGS },
                                onBookOpenStateChanged = { isOpen -> journalBookOpen = isOpen }
                            )
                            Screen.INVENTORY -> InventoryScreen(
                                modifier = Modifier.fillMaxSize().padding(padding),
                                inventory = inventory,
                                shopItems = shopItems,
                                customMode = customMode,
                                gold = gold,
                                accentStrong = accentStrong,
                                accentSoft = accentSoft,
                                showTutorial = !shopTutorialSeen,
                                onTutorialDismiss = { markShopTutorialSeen() },
                                showHoldHint = customMode && !shopHoldHintSeen,
                                onHoldHintShown = { markShopHoldHintSeen() },
                                onBuyShopItem = { onBuyShopItem(it) },
                                onUpsertShopItem = { onUpsertShopItem(it) },
                                onDeleteShopItem = { onDeleteShopItem(it) },
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onOpenSettings = { screen = Screen.SETTINGS }
                            )
                            Screen.CALENDAR -> CalendarScreen(
                                modifier = Modifier.fillMaxSize().padding(padding),
                                appContext = appContext,
                                plans = calendarPlans,
                                accentStrong = accentStrong,
                                accentSoft = accentSoft,
                                showTutorial = !calendarTutorialSeen,
                                onTutorialDismiss = { markCalendarTutorialSeen() },
                                onAddPlan = { day, text -> onAddPlan(day, text) },
                                onDeletePlan = { day, idx -> onDeletePlan(day, idx) },
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onOpenSettings = { screen = Screen.SETTINGS }
                            )
                            Screen.COMMUNITY -> CommunityScreen(
                                modifier = Modifier.fillMaxSize().padding(padding),
                                accentStrong = accentStrong,
                                accentSoft = accentSoft,
                                currentUserId = communityUserId,
                                currentUserName = communityUserName,
                                communityPosts = communityPosts,
                                followedAuthorIds = followedAuthorIds,
                                mutedAuthorIds = mutedAuthorIds,
                                blockedAuthorIds = blockedAuthorIds,
                                myRatings = myCommunityRatings,
                                pendingSyncCount = pendingCommunitySyncQueue.size,
                                isRefreshing = communityRefreshInProgress,
                                onRefresh = { },
                                onChangeUserName = { },
                                onPublish = { _, _, _ -> },
                                onToggleFollow = { _ -> },
                                onToggleMute = { _ -> },
                                onToggleBlock = { _ -> },
                                onReport = { _ -> },
                                onRate = { _, _ -> },
                                onRemix = { _ -> },
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onOpenSettings = { screen = Screen.SETTINGS }
                            )

                            Screen.QUESTS -> QuestsScreen(
                                modifier = Modifier.fillMaxSize().padding(padding),
                                accentStrong = accentStrong,
                                accentSoft = accentSoft,
                                customMode = customMode,
                                dailyTemplates = customTemplates,
                                mainQuests = mainQuests,
                                savedTemplates = savedTemplates,
                                activePackageIds = activePackageIds, // NEW
                                onTogglePackage = { t, b -> onTogglePackage(t, b) }, // NEW
                                onUpsertDaily = { t ->
                                    if (!customMode) {
                                        scope.launch { snackbarHostState.showSnackbar("Enable Custom Mode in Settings.") }
                                        return@QuestsScreen
                                    }
                                    val list = customTemplates.toMutableList()
                                    val idx = list.indexOfFirst { it.id == t.id }
                                    val isNewTemplate = idx < 0
                                    if (idx >= 0) list[idx] = t else list.add(t)
                                    persistCustomTemplates(list)
                                    if (isNewTemplate) {
                                        regenerateForDay(currentEpochDay())
                                    }
                                    scope.launch { snackbarHostState.showSnackbar("Daily quest saved.") }
                                },
                                onDeleteDaily = { id ->
                                    if (!customMode) {
                                        scope.launch { snackbarHostState.showSnackbar("Enable Custom Mode in Settings.") }
                                        return@QuestsScreen
                                    }
                                    val list = customTemplates.filterNot { it.id == id }
                                    persistCustomTemplates(list)
                                    scope.launch { snackbarHostState.showSnackbar("Daily quest deleted.") }
                                },
                                onUpsertMain = { mq ->
                                    if (!customMode) {
                                        scope.launch { snackbarHostState.showSnackbar("Enable Custom Mode in Settings.") }
                                        return@QuestsScreen
                                    }
                                    val list = mainQuests.toMutableList()
                                    val idx = list.indexOfFirst { it.id == mq.id }
                                    if (idx >= 0) list[idx] = mq else list.add(mq)
                                    persistMainQuests(list)
                                    scope.launch { snackbarHostState.showSnackbar("Main quest saved.") }
                                },
                                onDeleteMain = { id ->
                                    if (!customMode) {
                                        scope.launch { snackbarHostState.showSnackbar("Enable Custom Mode in Settings.") }
                                        return@QuestsScreen
                                    }
                                    val list = mainQuests.filterNot { it.id == id }
                                    persistMainQuests(list)
                                    scope.launch { snackbarHostState.showSnackbar("Main quest deleted.") }
                                },
                                onRestoreDefaults = { restoreDefaultQuests() },
                                onExportTemplate = { templateName ->
                                    val template = GameTemplate(
                                        templateName,
                                        appTheme,
                                        customTemplatesToQuestTemplates(customTemplates),
                                        mainQuests,
                                        shopItems,
                                        templateSettings = currentTemplateSettings(),
                                        accentArgb = accent.toArgbCompat().toLong()
                                    )
                                    val compressedPayload = exportGameTemplate(template)
                                    val link = "https://qn8r.github.io/Questify/?data=$compressedPayload"
                                    val sendIntent = Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, "Check out my Questify Template: $templateName!\n\n$link"); type = "text/plain" }
                                    appContext.startActivity(Intent.createChooser(sendIntent, "Share Template"))
                                },
                                onSaveCurrentToLibrary = { templateName ->
                                    val template = GameTemplate(
                                        templateName,
                                        appTheme,
                                        customTemplatesToQuestTemplates(customTemplates),
                                        mainQuests,
                                        shopItems,
                                        templateSettings = currentTemplateSettings(),
                                        accentArgb = accent.toArgbCompat().toLong()
                                    )
                                    persistSavedTemplates(savedTemplates + template)
                                    scope.launch { snackbarHostState.showSnackbar("Template saved.") }
                                },
                                onApplySavedTemplate = { t, backupName, clearExisting -> // UPDATED: Added clearExisting
                                    if (!backupName.isNullOrBlank()) {
                                        val backup = GameTemplate(
                                            backupName,
                                            appTheme,
                                            customTemplatesToQuestTemplates(customTemplates),
                                            mainQuests,
                                            shopItems,
                                            templateSettings = currentTemplateSettings(),
                                            accentArgb = accent.toArgbCompat().toLong()
                                        )
                                        persistSavedTemplates(savedTemplates + backup)
                                    }
                                    appTheme = normalizeTheme(t.appTheme)
                                    accent = t.accentArgb?.let { Color(it.toInt()) } ?: fallbackAccentForTheme(appTheme)
                                    applyTemplateSettings(t.templateSettings)
                                    persistSettings()

                                    val mappedDailies = t.dailyQuests.map { qt ->
                                        CustomTemplate(
                                            id = UUID.randomUUID().toString(),
                                            category = qt.category,
                                            difficulty = qt.difficulty,
                                            title = qt.title,
                                            icon = qt.icon,
                                            xp = qt.xp,
                                            target = qt.target,
                                            isPinned = qt.isPinned,
                                            imageUri = qt.imageUri,
                                            packageId = t.packageId
                                        )
                                    }

                                    if (clearExisting) {
                                        persistCustomTemplates(mappedDailies)
                                        persistMainQuests(t.mainQuests)
                                        persistShopItems(t.shopItems.ifEmpty { getDefaultShopItems() })
                                        activePackageIds = setOf(t.packageId)
                                        applyTemplateDailyQuestDefaults(t.packageId, clearExisting = true)
                                        persistSettings()
                                    } else {
                                        val newCustoms = customTemplates + mappedDailies
                                        persistCustomTemplates(newCustoms.distinctBy { it.title })
                                        val newMqs = mainQuests + t.mainQuests
                                        persistMainQuests(newMqs.distinctBy { it.title })
                                        if (t.shopItems.isNotEmpty()) {
                                            val mergedShop = (shopItems + t.shopItems).distinctBy { it.id }
                                            persistShopItems(mergedShop)
                                        }
                                        activePackageIds = activePackageIds + t.packageId
                                    }
                                    scope.launch { appContext.dataStore.edit { p -> p[activePacksKey] = activePackageIds.joinToString(",") } }
                                    scope.launch { snackbarHostState.showSnackbar("Theme & Quests Applied!") }
                                },
                                onDeleteSavedTemplate = { t ->
                                    persistSavedTemplates(savedTemplates.filterNot { it == t })
                                    scope.launch {
                                        val res = snackbarHostState.showSnackbar("Template deleted.", actionLabel = "UNDO", duration = SnackbarDuration.Short)
                                        if (res == SnackbarResult.ActionPerformed) {
                                            persistSavedTemplates((savedTemplates + t).distinctBy { "${it.packageId}|${it.templateName}" })
                                        }
                                    }
                                },
                                onRequireCustomMode = {
                                    scope.launch { snackbarHostState.showSnackbar("Enable Custom Mode in Settings to add quests.") }
                                },
                                onOpenCommunityTemplates = { screen = Screen.COMMUNITY },
                                onOpenAdvancedTemplates = {
                                    settingsExpandedSection = "advanced_templates"
                                    screen = Screen.SETTINGS
                                },
                                showTutorial = !questsTutorialSeen,
                                onTutorialDismiss = { markQuestsTutorialSeen() },
                                initialTab = questsPreferredTab,
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onOpenSettings = { screen = Screen.SETTINGS }
                            )
                            Screen.STATS -> DashboardScreen( // Pointing to the new screen!
                                modifier = Modifier.fillMaxSize().padding(padding),
                                levelInfo = calculateLevel(totalXp),
                                attributes = attributes,
                                gold = gold,
                                streak = streak,
                                history = historyMap,
                                unlockedAchievementIds = unlockedAchievementIds,
                                accentStrong = accentStrong,
                                accentSoft = accentSoft,
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onOpenSettings = { screen = Screen.SETTINGS }
                            )
                            Screen.SETTINGS -> SettingsScreen(
                                modifier = Modifier.fillMaxSize().padding(padding),
                                autoNewDay = autoNewDay,
                                confirmComplete = confirmComplete,
                                refreshIncompleteOnly = refreshIncompleteOnly,
                                customMode = customMode,
                                advancedOptions = advancedOptions,
                                highContrastText = highContrastText,
                                compactMode = compactMode,
                                largerTouchTargets = largerTouchTargets,
                                reduceAnimations = reduceAnimations,
                                decorativeBorders = neonFlowEnabled,
                                neonLightBoost = neonLightBoost,
                                neonFlowEnabled = neonFlowEnabled,
                                neonFlowSpeed = neonFlowSpeed,
                                neonGlowPalette = neonGlowPalette,
                                alwaysShowQuestProgress = alwaysShowQuestProgress,
                                hideCompletedQuests = hideCompletedQuests,
                                confirmDestructiveActions = confirmDestructiveActions,
                                dailyResetHour = dailyResetHour,
                                dailyQuestTarget = dailyQuestTarget,
                                expandedSection = settingsExpandedSection,
                                premiumUnlocked = premiumUnlocked,
                                cloudSyncEnabled = cloudSyncEnabled,
                                cloudConnected = cloudConnectedAccount != null,
                                cloudAccountEmail = cloudAccountEmail,
                                cloudLastSyncAt = cloudLastSyncAt,
                                dailyRemindersEnabled = dailyRemindersEnabled,
                                hapticsEnabled = hapticsEnabled,
                                soundEffectsEnabled = soundEffectsEnabled,
                                fontStyle = fontStyle,
                                fontScalePercent = fontScalePercent,
                                appLanguage = appLanguage,
                                backgroundImageTransparencyPercent = backgroundImageTransparencyPercent,
                                journalName = journalName,
                                textColorOverride = textColorOverride,
                                appBackgroundColorOverride = appBackgroundColorOverride,
                                chromeBackgroundColorOverride = chromeBackgroundColorOverride,
                                cardColorOverride = cardColorOverride,
                                buttonColorOverride = buttonColorOverride,
                                journalPageColorOverride = journalPageColorOverride,
                                journalAccentColorOverride = journalAccentColorOverride,
                                appTheme = appTheme,
                                accentStrong = accentStrong,
                                accentSoft = accentSoft,
                                onAutoNewDayChanged = { autoNewDay = it; persistSettings() },
                                onConfirmCompleteChanged = { confirmComplete = it; persistSettings() },
                                onRefreshIncompleteOnlyChanged = { refreshIncompleteOnly = it; persistSettings() },
                                onCustomModeChanged = { customMode = it; persistSettings() },
                                onAdvancedOptionsChanged = { advancedOptions = it; persistSettings() },
                                onHighContrastTextChanged = { highContrastText = it; persistSettings() },
                                onCompactModeChanged = { compactMode = it; persistSettings() },
                                onLargeTouchTargetsChanged = { largerTouchTargets = it; persistSettings() },
                                onReduceAnimationsChanged = { reduceAnimations = it; persistSettings() },
                                onDecorativeBordersChanged = {
                                    decorativeBorders = it
                                    neonFlowEnabled = it
                                    persistSettings()
                                },
                                onNeonLightBoostChanged = { neonLightBoost = it; persistSettings() },
                                onNeonFlowEnabledChanged = {
                                    neonFlowEnabled = it
                                    decorativeBorders = it
                                    persistSettings()
                                },
                                onNeonFlowSpeedChanged = { neonFlowSpeed = it.coerceIn(0, 2); persistSettings() },
                                onNeonGlowPaletteChanged = { neonGlowPalette = it.ifBlank { "magenta" }; persistSettings() },
                                onAlwaysShowQuestProgressChanged = { alwaysShowQuestProgress = it; persistSettings() },
                                onHideCompletedQuestsChanged = { hideCompletedQuests = it; persistSettings() },
                                onConfirmDestructiveChanged = { confirmDestructiveActions = it; persistSettings() },
                                onDailyResetHourChanged = { dailyResetHour = it.coerceIn(0, 23); persistSettings() },
                                onDailyQuestTargetChanged = { dailyQuestTarget = it.coerceIn(3, 10); persistSettings(); regenerateForDay(currentEpochDay()) },
                                onExpandedSectionChanged = { settingsExpandedSection = it },
                                onPremiumUnlockedChanged = { premiumUnlocked = it; persistSettings() },
                                onCloudSyncEnabledChanged = { cloudSyncEnabled = it; persistSettings() },
                                onCloudEmailChanged = { cloudAccountEmail = it.take(60); persistSettings() },
                                onDailyRemindersEnabledChanged = { dailyRemindersEnabled = it; persistSettings() },
                                onHapticsChanged = { hapticsEnabled = it; persistSettings() },
                                onSoundEffectsChanged = { soundEffectsEnabled = it; persistSettings() },
                                onFontStyleChanged = { fontStyle = it; persistSettings() },
                                onFontScalePercentChanged = { fontScalePercent = it.coerceIn(80, 125); persistSettings() },
                                onAppLanguageChanged = { lang ->
                                    appLanguage = lang
                                    appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                                        .edit().putString("selected_language", lang).commit()
                                    scope.launch {
                                        appContext.dataStore.edit { p -> p[Keys.APP_LANGUAGE] = lang }
                                        (appContext as android.app.Activity).recreate()
                                    }
                                },
                                onJournalNameChanged = { journalName = it.take(24).ifBlank { "Journal" }; persistSettings() },
                                onTextColorChanged = { textColorOverride = it; persistSettings() },
                                backgroundImageUri = backgroundImageUri,
                                onBackgroundImageChanged = {
                                    backgroundImageUri = it
                                    persistSettings()
                                },
                                onBackgroundImageTransparencyPercentChanged = {
                                    backgroundImageTransparencyPercent = it.coerceIn(0, 100)
                                    persistSettings()
                                },
                                onAppBackgroundColorChanged = { appBackgroundColorOverride = it; persistSettings() },
                                onChromeBackgroundColorChanged = { chromeBackgroundColorOverride = it; persistSettings() },
                                onCardColorChanged = { cardColorOverride = it; persistSettings() },
                                onButtonColorChanged = { buttonColorOverride = it; persistSettings() },
                                onJournalPageColorChanged = { journalPageColorOverride = it; persistSettings() },
                                onJournalAccentColorChanged = { journalAccentColorOverride = it; persistSettings() },
                                onThemeChanged = {
                                    appTheme = normalizeTheme(it)
                                    if (buttonColorOverride == null) {
                                        accent = fallbackAccentForTheme(appTheme)
                                    }
                                    persistSettings()
                                },
                                onAccentChanged = {
                                    accent = it
                                    buttonColorOverride = null
                                    persistSettings()
                                },
                                onExportBackup = {
                                    val blob = exportBackupPayload()
                                    if (blob.isBlank()) {
                                        scope.launch { snackbarHostState.showSnackbar("Backup export failed.") }
                                    } else {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, blob)
                                            type = "text/plain"
                                        }
                                        appContext.startActivity(Intent.createChooser(sendIntent, "Export Encrypted Backup"))
                                    }
                                },
                                onImportBackup = {
                                    showBackupImport = true
                                },
                                onCloudSyncNow = { triggerCloudSnapshotSync(force = true) },
                                onCloudRestore = { restoreFromCloud() },
                                onCloudConnectRequest = {
                                    googleSignInLauncher.launch(GoogleDriveSync.signInClient(appContext).signInIntent)
                                },
                                onCloudDisconnect = { disconnectCloudAccount() },
                                onSendFeedback = { category, text -> shareFeedbackReport(category, text) },
                                onExportLogs = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, AppLog.exportRecentLogs().ifBlank { "No logs captured." })
                                        type = "text/plain"
                                    }
                                    appContext.startActivity(Intent.createChooser(sendIntent, "Export Logs"))
                                },
                                onBuildAdvancedTemplateStarterJson = { buildAdvancedTemplateStarterJson() },
                                onBuildAdvancedTemplatePromptFromRequest = { request ->
                                    buildAdvancedTemplatePromptFromRequest(request)
                                },
                                onImportAdvancedTemplateJson = { json ->
                                    val result = importAdvancedTemplateJson(json)
                                    if (result.success) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Template imported: ${result.dailyAdded} daily, ${result.mainAdded} main.")
                                        }
                                    }
                                    result
                                },
                                onApplyAdvancedTemplateByPackage = { pkg ->
                                    val ok = applyAdvancedImportedTemplate(pkg)
                                    if (ok) {
                                        scope.launch { snackbarHostState.showSnackbar("Advanced template applied.") }
                                    }
                                    ok
                                },
                                onRequestResetAll = {
                                    if (confirmDestructiveActions) {
                                        resetBackupBefore = false
                                        resetBackupName = "Pre-reset backup"
                                        showResetAll = true
                                    } else {
                                        resetAll(saveBackup = false)
                                    }
                                },
                                onRequestForceNewDay = {
                                    if (confirmDestructiveActions) {
                                        showRefreshDayConfirm = true
                                    } else {
                                        onRefreshDay()
                                    }
                                },
                                onOpenDrawer = { scope.launch { drawerState.open() } }
                            )
                            Screen.ABOUT -> AboutScreen(
                                Modifier.fillMaxSize().padding(padding),
                                accentStrong,
                                accentSoft
                            ) { scope.launch { drawerState.open() } }
                        }
                        }
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    SwipePagerHost(
                        modifier = Modifier.fillMaxSize(),
                        enabled = screen == Screen.HOME || screen == Screen.MAINQUEST || screen == Screen.GRIMOIRE,
                        drawerClosed = drawerClosed,
                        current = screen,
                        onRequestScreen = { screen = it },
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onSwipeProgress = { swipeVisualProgress = it },
                        content = renderScreen,
                        grimoireBookOpen = journalBookOpen
                    )
                }
            }
        }
    }
    if (showIntroSplash) {
        IntroSplash(backgroundImageUri = backgroundImageUri)
    }
    if (showWelcomeSetup) {
        WelcomeSetupScreen(
            defaultSkipIntro = onboardingSkipIntroDefault,
            onLanguageChanged = { lang ->
                appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .edit().putString("selected_language", lang).commit()
                scope.launch {
                    appContext.dataStore.edit { p -> p[Keys.APP_LANGUAGE] = lang }
                    (appContext as android.app.Activity).recreate()
                }
            },
            onDone = { setup ->
                val finalName = sanitizeDisplayName(setup.name.ifBlank { "Player" })
                val chosenAvatar = setup.avatarImageUri
                    ?.takeIf { it.isNotBlank() }
                    ?.let { Avatar.Custom(it.toUri()) }
                    ?: Avatar.Preset(setup.avatar)
                persistAvatar(chosenAvatar)
                persistCommunityProfile(communityUserId, finalName)
                onboardingGoal = setup.goal
                difficultyPreference = setup.difficultyPreference
                dailyResetHour = setup.reminderHour.coerceIn(0, 23)
                val starter = when (setup.templateId) {
                    "saitama_v1" -> getLimitBreakerTemplate()
                    REAL_WORLD_MOMENTUM_PACKAGE_ID -> getRealWorldMomentumTemplate()
                    "empty_pack" -> getEmptyStarterTemplate()
                    else -> getDefaultGameTemplate()
                }
                applyStarterTemplate(starter)
                if (setup.templateId == "empty_pack") {
                    customMode = true
                }
                appTheme = normalizeTheme(setup.theme)
                accent = setup.accentArgb?.let { Color(it.toInt()) } ?: fallbackAccentForTheme(appTheme)
                persistSettings()
                scope.launch { appContext.dataStore.edit { p -> p[Keys.ONBOARDING_DONE] = true } }
                onboardingSkipIntroDefault = false
                screen = Screen.HOME
                showWelcomeSetup = false
            }
        )
    }
}

@Composable
private fun SwipePagerHost(
    modifier: Modifier,
    enabled: Boolean,
    drawerClosed: Boolean,
    current: Screen,
    onRequestScreen: (Screen) -> Unit,
    onOpenDrawer: () -> Unit,
    onSwipeProgress: (Float) -> Unit,
    content: @Composable (Screen) -> Unit,
    grimoireBookOpen: Boolean
) {
    val scope = rememberCoroutineScope(); val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val threshold = widthPx * 0.28f
        var dragOffset by remember { mutableFloatStateOf(0f) }
        var settleJob by remember { mutableStateOf<Job?>(null) }
        val touchSlop = 24f
        LaunchedEffect(current) { settleJob?.cancel(); settleJob = null; dragOffset = 0f }

        val leftTarget: Screen? = when (current) {
            Screen.HOME -> Screen.MAINQUEST
            Screen.MAINQUEST -> Screen.GRIMOIRE
            else -> null
        }
        val rightTargetScreen: Screen? = when (current) {
            Screen.MAINQUEST -> Screen.HOME
            Screen.GRIMOIRE -> Screen.MAINQUEST
            else -> null
        }
        val rightOpensDrawer = current == Screen.HOME
        // In RTL, swiping right = next (higher index), left = prev (lower index) — opposite of LTR
        val effectiveRightTarget: Screen? = if (isRtl) leftTarget else rightTargetScreen
        val effectiveLeftTarget: Screen? = if (isRtl) rightTargetScreen else leftTarget
        fun screenSwipeIndex(target: Screen): Float = when (target) {
            Screen.HOME -> 0f
            Screen.MAINQUEST -> 1f
            Screen.GRIMOIRE -> 2f
            else -> 0f
        }
        val visualProgress = run {
            val base = screenSwipeIndex(current)
            val hasDrag = (dragOffset > 0f && effectiveRightTarget != null) || (dragOffset < 0f && effectiveLeftTarget != null)
            if (!hasDrag) base
            else {
                val sign = if (isRtl) 1f else -1f
                (base + sign * dragOffset / widthPx).coerceIn(0f, 2f)
            }
        }
        SideEffect { onSwipeProgress(visualProgress) }

        fun clampOffset(v: Float): Float {
            val canSwipeGrimoire = current != Screen.GRIMOIRE || !grimoireBookOpen
            val maxRight = if (effectiveRightTarget != null && canSwipeGrimoire) widthPx else 0f
            val maxLeft = if (effectiveLeftTarget != null && canSwipeGrimoire) -widthPx else 0f
            return v.coerceIn(maxLeft, maxRight)
        }

        Box(modifier = Modifier.fillMaxSize().pointerInput(enabled, drawerClosed, current, rightTargetScreen, leftTarget, rightOpensDrawer, grimoireBookOpen, isRtl) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val pointerId = down.id
                if (!drawerClosed) return@awaitEachGesture
                settleJob?.cancel(); settleJob = null
                var totalDx = 0f
                var totalDy = 0f
                var locked = false
                var openDrawerFromEdge = false
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                    if (!change.pressed) break
                    val delta = change.positionChangeIgnoreConsumed()
                    val dx = delta.x
                    val dy = delta.y
                    totalDx += dx
                    totalDy += dy
                    if (!locked && abs(totalDx) > touchSlop) {
                        val absDx = abs(totalDx)
                        val absDy = abs(totalDy)
                        if (absDx > absDy * 2.0f) {
                            locked = true
                        }
                    }
                    if (!locked) continue
                    if (rightTargetScreen == null && rightOpensDrawer && (if (isRtl) totalDx < 0f else totalDx > 0f)) {
                        openDrawerFromEdge = true
                        break
                    }
                    dragOffset = clampOffset(dragOffset + dx)
                    change.consume()
                }
                if (openDrawerFromEdge) {
                    onOpenDrawer()
                    dragOffset = 0f
                    return@awaitEachGesture
                }
                val v = dragOffset
                if (v == 0f && totalDx <= threshold) return@awaitEachGesture
                val canSwipeGrimoire = current != Screen.GRIMOIRE || !grimoireBookOpen
                val goRight = v > threshold && effectiveRightTarget != null && canSwipeGrimoire
                val goLeft = v < -threshold && effectiveLeftTarget != null && canSwipeGrimoire

                val target = when {
                    goRight -> widthPx
                    goLeft -> -widthPx
                    else -> 0f
                }
                settleJob = scope.launch {
                    animate(
                        initialValue = dragOffset,
                        targetValue = target,
                        animationSpec = tween(durationMillis = if (target == 0f) 230 else 320)
                    ) { value, _ -> dragOffset = value }
                    when {
                        goRight -> onRequestScreen(effectiveRightTarget!!)
                        goLeft -> onRequestScreen(effectiveLeftTarget!!)
                    }
                    dragOffset = 0f
                }
            }
        }) {
            val off = dragOffset
            if (off > 0f) {
                effectiveRightTarget?.let { screen ->
                    Box(Modifier.fillMaxSize().graphicsLayer { translationX = off - widthPx }) { content(screen) }
                }
            } else if (off < 0f) {
                effectiveLeftTarget?.let { screen ->
                    Box(Modifier.fillMaxSize().graphicsLayer { translationX = off + widthPx }) { content(screen) }
                }
            }
            Box(Modifier.fillMaxSize().graphicsLayer { translationX = off }) { content(current) }
        }
    }
}
