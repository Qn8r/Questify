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
    fun getString(resId: Int, vararg formatArgs: Any): String = appContext.getString(resId, *formatArgs)
    fun categoryLabel(cat: QuestCategory): String = when (cat) {
        QuestCategory.FITNESS -> getString(R.string.cat_fitness)
        QuestCategory.STUDY -> getString(R.string.cat_study)
        QuestCategory.HYDRATION -> getString(R.string.cat_hydration)
        QuestCategory.DISCIPLINE -> getString(R.string.cat_discipline)
        QuestCategory.MIND -> getString(R.string.cat_mind)
    }

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
    var backgroundImageTintEnabled by remember { mutableStateOf(true) }
    var backgroundImageTransparencyPercent by remember { mutableIntStateOf(78) }
    var accentTransparencyPercent by remember { mutableIntStateOf(0) }
    var textTransparencyPercent by remember { mutableIntStateOf(0) }
    var appBgTransparencyPercent by remember { mutableIntStateOf(0) }
    var chromeBgTransparencyPercent by remember { mutableIntStateOf(0) }
    var cardBgTransparencyPercent by remember { mutableIntStateOf(0) }
    var journalPageTransparencyPercent by remember { mutableIntStateOf(0) }
    var journalAccentTransparencyPercent by remember { mutableIntStateOf(0) }
    var buttonTransparencyPercent by remember { mutableIntStateOf(0) }
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
    var isLoggedIn by remember { mutableStateOf(false) }
    var authAccessToken by remember { mutableStateOf("") }
    var authRefreshToken by remember { mutableStateOf("") }
    var authUserEmail by remember { mutableStateOf("") }
    var authUserId by remember { mutableStateOf("") }
    var showLoginRequiredDialog by remember { mutableStateOf(false) }
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
    val appBgAlpha = (1f - (appBgTransparencyPercent.coerceIn(0, 100) / 100f)).coerceIn(0f, 1f)
    val chromeBgAlpha = (1f - (chromeBgTransparencyPercent.coerceIn(0, 100) / 100f)).coerceIn(0f, 1f)
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
    val drawerBg = remember(defaultChromeBg, chromeBackgroundColorOverride, chromeBgAlpha) {
        (chromeBackgroundColorOverride ?: defaultChromeBg).copy(alpha = chromeBgAlpha)
    }
    val navBarBg = remember(drawerBg, chromeBackgroundColorOverride, isThemeBgLight, backgroundImageUri, backgroundImageTransparencyPercent, chromeBgAlpha) {
        val imageBlend = if (backgroundImageUri.isNullOrBlank()) 0f else (backgroundImageTransparencyPercent.coerceIn(0, 100) / 100f)
        val targetAlpha = (0.96f - (imageBlend * 0.18f)).coerceIn(0.74f, 0.96f)
        chromeBackgroundColorOverride ?: if (isThemeBgLight) {
            mixForBackground(Color(0xFFBDCCE3), drawerBg).copy(alpha = targetAlpha * chromeBgAlpha)
        } else {
            mixForBackground(Color(0xFF03060B), drawerBg).copy(alpha = targetAlpha * chromeBgAlpha)
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
    var timerPersistJob by remember { mutableStateOf<Job?>(null) }
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
    var pendingHomeEditDailyTemplateId by rememberSaveable { mutableStateOf<String?>(null) }
    var cloudSyncEnabled by remember { mutableStateOf(true) }
    var cloudAccountEmail by remember { mutableStateOf("") }
    var cloudConnectedAccount by remember { mutableStateOf<GoogleSignInAccount?>(null) }
    var cloudLastSyncAt by remember { mutableLongStateOf(0L) }
    var cloudLastAutoAttemptAt by remember { mutableLongStateOf(0L) }
    var communityPosts by remember { mutableStateOf<List<CommunityPost>>(emptyList()) }
    var communityCommentsByPost by remember { mutableStateOf<Map<String, List<CommunityComment>>>(emptyMap()) }
    var myCommunityCommentVotes by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
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
    var healthDailySnapshot by remember { mutableStateOf<HealthDailySnapshot?>(null) }
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
            backgroundImageTintEnabled = backgroundImageTintEnabled,
            backgroundImageTransparencyPercent = backgroundImageTransparencyPercent.coerceIn(0, 100),
            accentTransparencyPercent = accentTransparencyPercent.coerceIn(0, 100),
            textTransparencyPercent = textTransparencyPercent.coerceIn(0, 100),
            appBgTransparencyPercent = appBgTransparencyPercent.coerceIn(0, 100),
            chromeBgTransparencyPercent = chromeBgTransparencyPercent.coerceIn(0, 100),
            cardBgTransparencyPercent = cardBgTransparencyPercent.coerceIn(0, 100),
            journalPageTransparencyPercent = journalPageTransparencyPercent.coerceIn(0, 100),
            journalAccentTransparencyPercent = journalAccentTransparencyPercent.coerceIn(0, 100),
            buttonTransparencyPercent = buttonTransparencyPercent.coerceIn(0, 100),
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
        } else if (packageId == QUEST_FEATURE_TEST_PACKAGE_ID) {
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
        neonGlowPalette = runCatching { settings.neonGlowPalette }.getOrDefault("magenta").ifBlank { "magenta" }
        alwaysShowQuestProgress = settings.alwaysShowQuestProgress
        hideCompletedQuests = settings.hideCompletedQuests
        confirmDestructiveActions = true
        dailyResetHour = settings.dailyResetHour.coerceIn(0, 23)
        dailyRemindersEnabled = settings.dailyRemindersEnabled
        hapticsEnabled = settings.hapticsEnabled
        soundEffectsEnabled = settings.soundEffectsEnabled
        fontStyle = runCatching { settings.fontStyle }.getOrDefault(AppFontStyle.DEFAULT)
        fontScalePercent = settings.fontScalePercent.coerceIn(80, 125)
        backgroundImageUri = runCatching { settings.backgroundImageUri }.getOrNull()
        backgroundImageTintEnabled = runCatching { settings.backgroundImageTintEnabled }.getOrDefault(true)
        backgroundImageTransparencyPercent = (settings.backgroundImageTransparencyPercent ?: backgroundImageTransparencyPercent).coerceIn(0, 100)
        accentTransparencyPercent = (settings.accentTransparencyPercent ?: accentTransparencyPercent).coerceIn(0, 100)
        textTransparencyPercent = (settings.textTransparencyPercent ?: textTransparencyPercent).coerceIn(0, 100)
        appBgTransparencyPercent = (settings.appBgTransparencyPercent ?: appBgTransparencyPercent).coerceIn(0, 100)
        chromeBgTransparencyPercent = (settings.chromeBgTransparencyPercent ?: chromeBgTransparencyPercent).coerceIn(0, 100)
        cardBgTransparencyPercent = (settings.cardBgTransparencyPercent ?: cardBgTransparencyPercent).coerceIn(0, 100)
        journalPageTransparencyPercent = (settings.journalPageTransparencyPercent ?: journalPageTransparencyPercent).coerceIn(0, 100)
        journalAccentTransparencyPercent = (settings.journalAccentTransparencyPercent ?: journalAccentTransparencyPercent).coerceIn(0, 100)
        buttonTransparencyPercent = (settings.buttonTransparencyPercent ?: buttonTransparencyPercent).coerceIn(0, 100)
        textColorOverride = settings.textColorArgb?.let { Color(it.toInt()) }
        appBackgroundColorOverride = settings.appBackgroundArgb?.let { Color(it.toInt()) }
        chromeBackgroundColorOverride = settings.chromeBackgroundArgb?.let { Color(it.toInt()) }
        cardColorOverride = settings.cardColorArgb?.let { Color(it.toInt()) }
        buttonColorOverride = settings.buttonColorArgb?.let { Color(it.toInt()) }
        journalPageColorOverride = settings.journalPageColorArgb?.let { Color(it.toInt()) }
        journalAccentColorOverride = settings.journalAccentColorArgb?.let { Color(it.toInt()) }
        journalName = runCatching { settings.journalName }.getOrDefault("Journal").ifBlank { "Journal" }
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

    fun sanitizeHealthSnapshot(snapshot: HealthDailySnapshot): HealthDailySnapshot {
        val safeDistance = if (snapshot.distanceMeters.isFinite()) snapshot.distanceMeters else 0f
        val safeCalories = if (snapshot.caloriesKcal.isFinite()) snapshot.caloriesKcal else 0f
        return snapshot.copy(
            steps = snapshot.steps.coerceAtLeast(0),
            avgHeartRate = snapshot.avgHeartRate?.coerceIn(0, 260),
            distanceMeters = safeDistance.coerceIn(0f, 1_000_000f),
            caloriesKcal = safeCalories.coerceIn(0f, 100_000f)
        )
    }

    fun persistHealthDailySnapshot(snapshot: HealthDailySnapshot?) {
        val safeSnapshot = snapshot?.let(::sanitizeHealthSnapshot)
        healthDailySnapshot = safeSnapshot
        scope.launch {
            runCatching {
                appContext.dataStore.edit { p ->
                    if (safeSnapshot == null) {
                        p.remove(Keys.HEALTH_DAILY_SNAPSHOT)
                    } else {
                        p[Keys.HEALTH_DAILY_SNAPSHOT] = Gson().toJson(safeSnapshot)
                    }
                }
            }.onFailure { err ->
                AppLog.w("persist_health_snapshot_failed", err)
            }
        }
    }

    fun metricValueForQuest(snapshot: HealthDailySnapshot, metric: String?): Int {
        return when (metric?.trim()?.lowercase(Locale.getDefault())) {
            "steps" -> snapshot.steps
            "heart_rate" -> snapshot.avgHeartRate ?: 0
            "distance_m" -> snapshot.distanceMeters.toInt()
            "calories_kcal" -> snapshot.caloriesKcal.toInt()
            else -> 0
        }
    }

    fun syncHealthObjectiveQuestProgress(
        snapshot: HealthDailySnapshot?,
        startedQuestId: Int? = null
    ) {
        if (snapshot == null) return
        val updated = quests.map { q ->
            if (q.completed || q.objectiveType != QuestObjectiveType.HEALTH) return@map q
            val metric = q.healthMetric?.trim()?.lowercase(Locale.getDefault())
            val value = metricValueForQuest(snapshot, q.healthMetric).coerceAtLeast(0)
            val target = q.target.coerceAtLeast(1)
            val mapped = when (metric) {
                // Heart-rate goals are threshold goals: lower/equal target is success.
                "heart_rate" -> if (value > 0 && value <= target) target + 1 else 0
                else -> if (value >= target) target + 1 else value
            }
            val effective = when {
                mapped > 0 -> mapped
                q.id == startedQuestId -> 1
                q.currentProgress > 0 -> 1
                else -> 0
            }
            if (effective == q.currentProgress) q else q.copy(currentProgress = effective)
        }
        if (updated != quests) {
            quests = updated
            val completedIds = updated.filter { it.completed }.map { it.id }.toSet()
            val base = updated.map { it.copy(completed = false) }
            scope.launch {
                runCatching {
                    appContext.dataStore.edit { p ->
                        p[Keys.LAST_DAY] = lastDayEpoch
                        p[Keys.QUESTS] = serializeQuests(base)
                        p[Keys.COMPLETED] = completedIds.joinToString(",")
                        p[Keys.EARNED] = earnedIds.joinToString(",")
                        p[Keys.REFRESH_COUNT] = refreshCount
                    }
                }.onFailure { err ->
                    AppLog.w("persist_health_progress_failed", err)
                }
            }
        }
    }

    fun clampHealthTarget(metric: String?, value: Int): Int {
        return when (metric?.trim()?.lowercase(Locale.getDefault())) {
            "steps" -> value.coerceIn(100, 50000)
            "heart_rate" -> value.coerceIn(40, 220)
            "distance_m" -> value.coerceIn(100, 50000)
            "calories_kcal" -> value.coerceIn(50, 5000)
            else -> value.coerceAtLeast(1)
        }
    }

    fun sanitizeHealthTemplateOrNull(template: CustomTemplate): CustomTemplate? {
        if (template.objectiveType != QuestObjectiveType.HEALTH) return template
        val metric = template.healthMetric?.trim()?.lowercase(Locale.getDefault())
        if (metric !in setOf("steps", "heart_rate", "distance_m", "calories_kcal")) {
            AppLog.w("Dropped invalid HEALTH template id=${template.id} title=${template.title} metric=${template.healthMetric}")
            return null
        }
        val safeTarget = clampHealthTarget(metric, template.target)
        return template.copy(
            target = safeTarget,
            healthMetric = metric,
            healthAggregation = template.healthAggregation ?: if (metric == "heart_rate") "daily_avg" else "daily_total"
        )
    }

    fun sanitizeHealthQuestOrNull(quest: Quest): Quest? {
        if (quest.objectiveType != QuestObjectiveType.HEALTH) return quest
        val metric = quest.healthMetric?.trim()?.lowercase(Locale.getDefault())
        if (metric !in setOf("steps", "heart_rate", "distance_m", "calories_kcal")) {
            AppLog.w("Dropped invalid HEALTH quest id=${quest.id} title=${quest.title} metric=${quest.healthMetric}")
            return null
        }
        val safeTarget = clampHealthTarget(metric, quest.target)
        return quest.copy(
            target = safeTarget,
            currentProgress = quest.currentProgress.coerceIn(0, safeTarget + 1),
            healthMetric = metric,
            healthAggregation = quest.healthAggregation ?: if (metric == "heart_rate") "daily_avg" else "daily_total"
        )
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
        val remotePosts = runCatching { SupabaseApi.fetchPosts() }.getOrDefault(emptyList())
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

        val remoteFollows = runCatching { SupabaseApi.fetchFollows(communityUserId) }.getOrDefault(emptySet())
        if (remoteFollows.isNotEmpty() || followedAuthorIds.isEmpty()) persistCommunityFollows(remoteFollows)

        val remoteRatings = runCatching { SupabaseApi.fetchRatings(communityUserId) }.getOrDefault(emptyMap())
        if (remoteRatings.isNotEmpty() || myCommunityRatings.isEmpty()) persistCommunityRatings(remoteRatings)

        val postIds = remotePosts.take(40).map { it.id }
        if (postIds.isEmpty()) {
            communityCommentsByPost = emptyMap()
            myCommunityCommentVotes = emptyMap()
        } else {
            val myVotes = runCatching { SupabaseApi.fetchMyCommentVotes(communityUserId) }.getOrDefault(emptyMap())
            myCommunityCommentVotes = myVotes
            val commentMap = mutableMapOf<String, List<CommunityComment>>()
            postIds.distinct().forEach { postId ->
                commentMap[postId] = runCatching { SupabaseApi.fetchComments(postId) }.getOrDefault(emptyList()).map { c ->
                    c.copy(myVote = myVotes[c.id] ?: 0)
                }
            }
            communityCommentsByPost = commentMap
        }
    }


    suspend fun refreshCommunityComments(postIds: List<String>) {
        if (!SupabaseApi.isConfigured || communityUserId.isBlank()) return
        if (postIds.isEmpty()) {
            communityCommentsByPost = emptyMap()
            myCommunityCommentVotes = emptyMap()
            return
        }
        val myVotes = runCatching { SupabaseApi.fetchMyCommentVotes(communityUserId) }.getOrDefault(emptyMap())
        myCommunityCommentVotes = myVotes
        val map = mutableMapOf<String, List<CommunityComment>>()
        postIds.distinct().forEach { postId ->
            val comments = runCatching { SupabaseApi.fetchComments(postId) }.getOrDefault(emptyList()).map { c ->
                c.copy(myVote = myVotes[c.id] ?: 0)
            }
            map[postId] = comments
        }
        communityCommentsByPost = map
    }

    fun submitCommunityComment(postId: String, body: String) {
        if (!isLoggedIn) { showLoginRequiredDialog = true; return }
        val clean = body.trim().take(500)
        if (clean.length < 2) return
        scope.launch {
            val ok = runCatching { SupabaseApi.postComment(postId, communityUserId, communityUserName, clean) }.getOrDefault(false)
            if (ok) {
                refreshCommunityComments(listOf(postId))
            } else {
                snackbarHostState.showSnackbar(getString(R.string.snackbar_comment_failed))
            }
        }
    }

    fun voteCommunityComment(postId: String, commentId: String, vote: Int) {
        if (!isLoggedIn) { showLoginRequiredDialog = true; return }
        scope.launch {
            val ok = runCatching { SupabaseApi.voteComment(commentId, communityUserId, vote) }.getOrDefault(false)
            if (ok) {
                refreshCommunityComments(listOf(postId))
            } else {
                snackbarHostState.showSnackbar(getString(R.string.snackbar_vote_failed))
            }
        }
    }

    fun triggerCommunityRefresh() {
        if (communityRefreshInProgress) return
        scope.launch {
            val startedAt = System.currentTimeMillis()
            communityRefreshInProgress = true
            val failureMessage = runCatching { syncCommunityFromRemote() }
                .exceptionOrNull()
                ?.let { getString(R.string.snackbar_community_offline) }
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
        val importedDecoded = deserializeQuests(dump["quests"].orEmpty())
            .mapNotNull(::sanitizeHealthQuestOrNull)
        quests = ensureUniqueQuestIds(importedDecoded).map { q ->
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
                p[Keys.BACKGROUND_IMAGE_TINT_ENABLED] = backgroundImageTintEnabled
                p[Keys.BACKGROUND_IMAGE_TRANSPARENCY_PERCENT] = backgroundImageTransparencyPercent.coerceIn(0, 100)
                p[Keys.TRANSPARENCY_ACCENT] = accentTransparencyPercent.coerceIn(0, 100)
                p[Keys.TRANSPARENCY_TEXT] = textTransparencyPercent.coerceIn(0, 100)
                p[Keys.TRANSPARENCY_APP_BG] = appBgTransparencyPercent.coerceIn(0, 100)
                p[Keys.TRANSPARENCY_CHROME_BG] = chromeBgTransparencyPercent.coerceIn(0, 100)
                p[Keys.TRANSPARENCY_CARD_BG] = cardBgTransparencyPercent.coerceIn(0, 100)
                p[Keys.TRANSPARENCY_JOURNAL_PAGE] = journalPageTransparencyPercent.coerceIn(0, 100)
                p[Keys.TRANSPARENCY_JOURNAL_ACCENT] = journalAccentTransparencyPercent.coerceIn(0, 100)
                p[Keys.TRANSPARENCY_BUTTON] = buttonTransparencyPercent.coerceIn(0, 100)
                appBackgroundColorOverride?.let { p[Keys.APP_BACKGROUND_ARGB] = it.toArgbCompat() } ?: p.remove(Keys.APP_BACKGROUND_ARGB)
                chromeBackgroundColorOverride?.let { p[Keys.CHROME_BACKGROUND_ARGB] = it.toArgbCompat() } ?: p.remove(Keys.CHROME_BACKGROUND_ARGB)
                cardColorOverride?.let { p[Keys.CARD_COLOR_ARGB] = it.toArgbCompat() } ?: p.remove(Keys.CARD_COLOR_ARGB)
                buttonColorOverride?.let { p[Keys.BUTTON_COLOR_ARGB] = it.toArgbCompat() } ?: p.remove(Keys.BUTTON_COLOR_ARGB)
                journalPageColorOverride?.let { p[Keys.JOURNAL_PAGE_COLOR_ARGB] = it.toArgbCompat() } ?: p.remove(Keys.JOURNAL_PAGE_COLOR_ARGB)
                journalAccentColorOverride?.let { p[Keys.JOURNAL_ACCENT_COLOR_ARGB] = it.toArgbCompat() } ?: p.remove(Keys.JOURNAL_ACCENT_COLOR_ARGB)
                p[Keys.JOURNAL_NAME] = journalName
                p[Keys.AUTH_ACCESS_TOKEN] = authAccessToken
                p[Keys.AUTH_REFRESH_TOKEN] = authRefreshToken
                p[Keys.AUTH_USER_EMAIL] = authUserEmail
                p[Keys.AUTH_USER_ID] = authUserId
                p[Keys.AUTH_PROVIDER] = if (isLoggedIn) "google" else ""
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
                scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_google_drive_first)) }
            }
            return
        }
        val now = System.currentTimeMillis()
        if (!force && now - cloudLastAutoAttemptAt < 120_000L) return
        cloudLastAutoAttemptAt = now
        val snapshot = exportBackupPayload()
        if (snapshot.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_cloud_sync_empty)) }
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
                snackbarHostState.showSnackbar(getString(R.string.snackbar_cloud_backup_synced))
            } else {
                snackbarHostState.showSnackbar(getString(R.string.snackbar_cloud_sync_failed))
            }
        }
    }

    fun restoreFromCloud() {
        if (!cloudSyncEnabled || cloudConnectedAccount == null) {
            scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_google_drive_first)) }
            return
        }
        scope.launch {
            val payload = GoogleDriveSync.downloadBackup(appContext)
            if (payload.isNullOrBlank()) {
                snackbarHostState.showSnackbar(getString(R.string.snackbar_no_cloud_backup))
                return@launch
            }
            val ok = importBackupPayload(payload)
            if (ok) {
                cloudLastSyncAt = System.currentTimeMillis()
                appContext.dataStore.edit { p -> p[Keys.CLOUD_LAST_SYNC_AT] = cloudLastSyncAt }
                snackbarHostState.showSnackbar(getString(R.string.snackbar_cloud_restored))
            } else {
                snackbarHostState.showSnackbar(getString(R.string.snackbar_cloud_restore_failed))
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
            snackbarHostState.showSnackbar(getString(R.string.snackbar_google_cloud_connected))
        }
    }

    fun shareFeedbackReport(category: String = "", message: String = "") {
        val resolvedCategory = category.ifBlank { getString(R.string.feedback_general) }
        val premiumLabel = if (premiumUnlocked) getString(R.string.on_label) else getString(R.string.off_label)
        val cloudSyncLabel = if (cloudSyncEnabled) getString(R.string.on_label) else getString(R.string.off_label)
        val report = buildString {
            appendLine(getString(R.string.feedback_report_title))
            appendLine(getString(R.string.feedback_report_user, communityUserName, communityUserId))
            appendLine(getString(R.string.feedback_report_category, resolvedCategory))
            appendLine(getString(R.string.feedback_report_level, currentLevel))
            appendLine(getString(R.string.feedback_report_theme, appTheme.name))
            appendLine(getString(R.string.feedback_report_premium, premiumLabel))
            appendLine(getString(R.string.feedback_report_cloud_sync, cloudSyncLabel))
            appendLine(getString(R.string.feedback_report_queue_pending, pendingCommunitySyncQueue.size))
            if (message.isNotBlank()) {
                appendLine()
                appendLine(getString(R.string.feedback_report_message_label))
                appendLine(message)
            }
            appendLine()
            appendLine(getString(R.string.feedback_report_logs_label))
            appendLine(AppLog.exportRecentLogs().ifBlank { getString(R.string.feedback_report_no_logs) })
        }
        scope.launch {
            val pushed = SupabaseApi.submitFeedbackInbox(
                userId = communityUserId.ifBlank { UUID.randomUUID().toString() },
                userName = communityUserName.ifBlank { "Player" },
                category = resolvedCategory,
                message = message.ifBlank { report },
                appTheme = appTheme.name,
                level = currentLevel
            )
            if (pushed) {
                snackbarHostState.showSnackbar(getString(R.string.snackbar_feedback_sent))
            } else {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, report)
                    type = "text/plain"
                }
                appContext.startActivity(Intent.createChooser(intent, getString(R.string.settings_send_feedback)))
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
            scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_achievement_unlocked)) }
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
                p[Keys.BACKGROUND_IMAGE_TINT_ENABLED] = backgroundImageTintEnabled
                p[Keys.BACKGROUND_IMAGE_TRANSPARENCY_PERCENT] = backgroundImageTransparencyPercent.coerceIn(0, 100)
                p[Keys.TRANSPARENCY_ACCENT] = accentTransparencyPercent.coerceIn(0, 100)
                p[Keys.TRANSPARENCY_TEXT] = textTransparencyPercent.coerceIn(0, 100)
                p[Keys.TRANSPARENCY_APP_BG] = appBgTransparencyPercent.coerceIn(0, 100)
                p[Keys.TRANSPARENCY_CHROME_BG] = chromeBgTransparencyPercent.coerceIn(0, 100)
                p[Keys.TRANSPARENCY_CARD_BG] = cardBgTransparencyPercent.coerceIn(0, 100)
                p[Keys.TRANSPARENCY_JOURNAL_PAGE] = journalPageTransparencyPercent.coerceIn(0, 100)
                p[Keys.TRANSPARENCY_JOURNAL_ACCENT] = journalAccentTransparencyPercent.coerceIn(0, 100)
                p[Keys.TRANSPARENCY_BUTTON] = buttonTransparencyPercent.coerceIn(0, 100)
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
        backgroundImageTintEnabled = prefs[Keys.BACKGROUND_IMAGE_TINT_ENABLED] ?: true
        backgroundImageTransparencyPercent = (prefs[Keys.BACKGROUND_IMAGE_TRANSPARENCY_PERCENT] ?: 78).coerceIn(0, 100)
        accentTransparencyPercent = (prefs[Keys.TRANSPARENCY_ACCENT] ?: 0).coerceIn(0, 100)
        textTransparencyPercent = (prefs[Keys.TRANSPARENCY_TEXT] ?: 0).coerceIn(0, 100)
        appBgTransparencyPercent = (prefs[Keys.TRANSPARENCY_APP_BG] ?: 0).coerceIn(0, 100)
        chromeBgTransparencyPercent = (prefs[Keys.TRANSPARENCY_CHROME_BG] ?: 0).coerceIn(0, 100)
        cardBgTransparencyPercent = (prefs[Keys.TRANSPARENCY_CARD_BG] ?: 0).coerceIn(0, 100)
        journalPageTransparencyPercent = (prefs[Keys.TRANSPARENCY_JOURNAL_PAGE] ?: 0).coerceIn(0, 100)
        journalAccentTransparencyPercent = (prefs[Keys.TRANSPARENCY_JOURNAL_ACCENT] ?: 0).coerceIn(0, 100)
        buttonTransparencyPercent = (prefs[Keys.TRANSPARENCY_BUTTON] ?: 0).coerceIn(0, 100)
        textColorOverride = prefs[Keys.TEXT_COLOR_ARGB]?.let { Color(it) }
        appBackgroundColorOverride = prefs[Keys.APP_BACKGROUND_ARGB]?.let { Color(it) }
        chromeBackgroundColorOverride = prefs[Keys.CHROME_BACKGROUND_ARGB]?.let { Color(it) }
        cardColorOverride = prefs[Keys.CARD_COLOR_ARGB]?.let { Color(it) }
        buttonColorOverride = prefs[Keys.BUTTON_COLOR_ARGB]?.let { Color(it) }
        journalPageColorOverride = prefs[Keys.JOURNAL_PAGE_COLOR_ARGB]?.let { Color(it) }
        journalAccentColorOverride = prefs[Keys.JOURNAL_ACCENT_COLOR_ARGB]?.let { Color(it) }
        journalName = prefs[Keys.JOURNAL_NAME].orEmpty().ifBlank { "Journal" }
        authAccessToken = prefs[Keys.AUTH_ACCESS_TOKEN].orEmpty()
        authRefreshToken = prefs[Keys.AUTH_REFRESH_TOKEN].orEmpty()
        authUserEmail = prefs[Keys.AUTH_USER_EMAIL].orEmpty()
        authUserId = prefs[Keys.AUTH_USER_ID].orEmpty()
        isLoggedIn = prefs[Keys.AUTH_PROVIDER].orEmpty().isNotBlank() && authAccessToken.isNotBlank()
        healthDailySnapshot = runCatching { Gson().fromJson(prefs[Keys.HEALTH_DAILY_SNAPSHOT].orEmpty(), HealthDailySnapshot::class.java) }
            .getOrNull()
            ?.let(::sanitizeHealthSnapshot)
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
        shopItems = savedShop
        calendarPlans = deserializeCalendarPlans(prefs[Keys.CALENDAR_PLANS])

        val rawAttr = prefs[Keys.ATTRIBUTES_RAW]
        if (!rawAttr.isNullOrBlank()) {
            val s = rawAttr.split("|").map { it.toIntOrNull() ?: 1 }
            if (s.size >= 5) attributes = PlayerAttributes(s[0], s[1], s[2], s[3], s[4])
        }

        val storedCustom = prefs[Keys.CUSTOM_TEMPLATES].orEmpty()
        if (storedCustom.isBlank()) {
            val defaults = getInitialDefaultPool()
            customTemplates = defaults
            persistCustomTemplates(defaults)
        } else {
            val decoded = deserializeCustomTemplates(storedCustom)
            val safe = decoded.mapNotNull(::sanitizeHealthTemplateOrNull)
            val droppedCount = decoded.size - safe.size
            customTemplates = safe
            if (droppedCount > 0) {
                AppLog.w("Sanitized $droppedCount invalid health template(s) on load")
                persistCustomTemplates(safe)
            }
        }

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
        if (!activePackageIds.contains(REAL_DAILY_LIFE_PACKAGE_ID)) {
            val filteredStarterShop = shopItems.filterNot { it.id == "shop_apple" || it.id == "shop_coffee" }
            if (filteredStarterShop.size != shopItems.size) {
                shopItems = filteredStarterShop
                appContext.dataStore.edit { p -> p[Keys.SHOP_ITEMS] = serializeShopItems(filteredStarterShop) }
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
        if (isLoggedIn && authUserId.isNotBlank()) {
            communityUserId = secureCommunityUserId(authUserId)
            if (communityUserName.isBlank() || communityUserName.equals("Player", ignoreCase = true)) {
                communityUserName = sanitizeDisplayName(
                    authUserEmail.substringBefore("@").replace(Regex("[^A-Za-z0-9 _-]"), "").ifBlank { "Player-${communityUserId.take(4)}" }
                )
            }
        }
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

        val storedBase = if (!storedQuestsSer.isNullOrBlank()) {
            val decoded = deserializeQuests(storedQuestsSer)
            val safe = decoded.mapNotNull(::sanitizeHealthQuestOrNull)
            val unique = ensureUniqueQuestIds(safe)
            val droppedCount = decoded.size - safe.size
            if (droppedCount > 0) {
                AppLog.w("Sanitized $droppedCount invalid health quest(s) on load")
            }
            unique
        } else emptyList()
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
            scope.launch { snackbarHostState.showSnackbar(appContext.getString(R.string.google_sign_in_canceled)) }
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
                scope.launch { snackbarHostState.showSnackbar(appContext.getString(R.string.google_sign_in_failed_hint, hint)) }
            }
    }

    val authGoogleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_google_signin_canceled)) }
            return@rememberLauncherForActivityResult
        }
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            val idToken = account?.idToken
            if (idToken.isNullOrBlank()) {
                scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_google_id_token_failed)) }
                return@rememberLauncherForActivityResult
            }
            scope.launch {
                val session = SupabaseApi.signInWithGoogleIdToken(idToken)
                if (session != null && !session.accessToken.isNullOrBlank()) {
                    authAccessToken = session.accessToken.orEmpty()
                    authRefreshToken = session.refreshToken.orEmpty()
                    authUserEmail = session.user?.email ?: account.email.orEmpty()
                    authUserId = session.user?.id.orEmpty()
                    if (authUserId.isNotBlank()) {
                        val safeUid = secureCommunityUserId(authUserId)
                        val suggestedName = communityUserName.takeIf { it.isNotBlank() && !it.equals("Player", ignoreCase = true) }
                            ?: authUserEmail.substringBefore("@").replace(Regex("[^A-Za-z0-9 _-]"), "").take(24).ifBlank { "Player" }
                        persistCommunityProfile(safeUid, suggestedName)
                    }
                    isLoggedIn = true
                    persistSettings()
                    snackbarHostState.showSnackbar(appContext.getString(R.string.account_signed_in_as_dot, authUserEmail))
                } else {
                    snackbarHostState.showSnackbar(appContext.getString(R.string.google_sign_in_failed_check_config))
                }
            }
        } catch (e: Exception) {
            AppLog.w("Auth Google sign-in failed.", e)
            scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_google_signin_failed)) }
        }
    }

    fun performGoogleLogin() {
        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (webClientId.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_google_web_client_missing)) }
            return
        }
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
        )
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(appContext, gso)
        authGoogleSignInLauncher.launch(client.signInIntent)
    }

    fun performLogout() {
        scope.launch {
            if (authAccessToken.isNotBlank()) {
                SupabaseApi.signOut(authAccessToken)
            }
            authAccessToken = ""
            authRefreshToken = ""
            authUserEmail = ""
            authUserId = ""
            isLoggedIn = false
            persistSettings()
            snackbarHostState.showSnackbar(getString(R.string.snackbar_signed_out))
        }
    }

    LaunchedEffect(Unit) {
        load()
        if (schemaDowngradeDetected) {
            scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_data_schema_newer)) }
        }
        delay(700)
        showIntroSplash = false
        runCatching {
            syncCommunityFromRemote()
        }.onFailure {
            AppLog.w("Community sync failed during startup; using local cache.", it)
            scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_community_offline)) }
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
                        snackbarHostState.showSnackbar(getString(R.string.snackbar_template_link_too_large))
                    } else {
                        try {
                            val decoded = runCatching {
                                java.net.URLDecoder.decode(templateData, StandardCharsets.UTF_8.name())
                            }.getOrDefault(templateData)
                            val template = importGameTemplate(decoded)
                            if (template != null) {
                                pendingImportTemplate = template
                            } else {
                                snackbarHostState.showSnackbar(getString(R.string.snackbar_template_link_invalid))
                            }
                        } catch (e: Exception) {
                            AppLog.e("Failed to parse incoming template link.", e)
                            snackbarHostState.showSnackbar(getString(R.string.snackbar_read_template_failed))
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
        scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_new_day_started)) }
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
You are editing a Questify template JSON file.

USER REQUEST (highest priority):
==========
{{USER_REQUEST}}
==========

TASK:
- Generate/update daily_quests, main_quests, and optional shop_items to satisfy USER REQUEST.
- {{THEME_POLICY_GOAL}}
- Keep the file import-compatible for Questify.

STRICT OUTPUT CONTRACT:
1) Output valid JSON only (no markdown, no commentary).
2) Return ONE full JSON object (not partial patches).
3) Keep required top-level keys: schema_version, template_name, app_theme, accent_argb, daily_quests, main_quests.

SCHEMA RULES:
4) Categories allowed: FITNESS, STUDY, HYDRATION, DISCIPLINE, MIND.
5) daily_quests[] fields: title, category, difficulty, xp, target, icon, objective_type, optional pinned, image_uri.
6) objective_type must be COUNT|TIMER|HEALTH.
   - COUNT: target >= 1.
   - TIMER: target_seconds required (30..86400). Set target = target_seconds for compatibility.
   - HEALTH: health_metric required in steps|heart_rate|distance_m|calories_kcal.
     health_aggregation optional (prefer daily_total, use daily_avg for heart_rate when requested).
7) main_quests[] fields: ref, title, description, xp_reward, steps[], prerequisite_ref, optional icon/image_uri.
   - ref must be unique.
   - prerequisite_ref must be null or an existing ref.
   - steps count: 2..8 concise items.
   - xp_reward range: 100..5000.
8) shop_items[] is optional: id, name, icon, description, cost, stock, max_stock, consumable, optional image_uri.
   - stock/max_stock range 0..99 and max_stock >= stock.
9) If distribution is not specified, keep daily_quests near-balanced across all 5 categories and tiers 1..5.
10) Hard caps (TOTAL counts): daily_quests <= $advancedDailyImportLimit, main_quests <= $advancedMainImportLimit, shop_items <= $advancedShopImportLimit.
11) {{THEME_POLICY_RULE}}
12) ai_instructions and guide are optional in final output.

REFERENCE SHAPE (keep names exactly):
{
  "schema_version": 2,
  "template_name": "Name",
  "app_theme": "DEFAULT",
  "accent_argb": 4283215696,
  "daily_quests": [
    {
      "title": "Run 20 minutes",
      "category": "FITNESS",
      "difficulty": 2,
      "xp": 28,
      "target": 1200,
      "icon": "🏃",
      "objective_type": "TIMER",
      "target_seconds": 1200
    },
    {
      "title": "Hit 7000 steps",
      "category": "FITNESS",
      "difficulty": 2,
      "xp": 24,
      "target": 7000,
      "icon": "👟",
      "objective_type": "HEALTH",
      "health_metric": "steps",
      "health_aggregation": "daily_total"
    }
  ],
  "main_quests": [],
  "shop_items": []
}

FINAL OUTPUT:
- Return the full updated JSON file only.
- If attachments are supported, use filename questify_advanced_template.json.
""".trimIndent()

    fun buildAdvancedTemplateStarterJson(): String {
        val starterSettings = currentTemplateSettings().copy(backgroundImageUri = null)
        val starter = AdvancedTemplateFile(
            template_name = "AI Generated Template",
            app_theme = appTheme.name,
            accent_argb = accent.toArgbCompat().toLong(),
            ai_instructions = listOf(
                "This JSON file is from Questify.",
                "Read USER REQUEST first, then update daily_quests/main_quests/shop_items.",
                "Return ONE valid JSON object only.",
                "Keep required keys: schema_version, template_name, app_theme, accent_argb, daily_quests, main_quests.",
                "For TIMER quests set target_seconds and set target to the same value.",
                "For HEALTH quests set health_metric and optional health_aggregation.",
                "Follow prompt limits/rules; return JSON only."
            ),
            guide = AdvancedTemplateGuide(
                summary = "Workflow: user asks AI for quests, AI edits this file directly, user uploads the returned JSON in Settings > Advanced Templates.",
                ai_prompt_example = "",
                notes = listOf(
                    "daily_quests[]: title, category, difficulty, xp, target, icon, objective_type.",
                    "objective_type TIMER can include target_seconds (30..86400).",
                    "objective_type HEALTH can include health_metric and health_aggregation.",
                    "main_quests[]: ref, title, description, xp_reward, steps[], prerequisite_ref, optional icon/image_uri.",
                    "shop_items[] is optional: id, name, icon, description, cost, stock, max_stock, consumable, optional image_uri.",
                    "template_settings is optional. Transparency values must be 0..100.",
                    "Counts are totals and capped by the app.",
                    "guide and ai_instructions may be removed from final output."
                )
            ),
            template_settings = starterSettings,
            daily_quests = listOf(
                // FITNESS
                AdvancedDailyQuestEntry(title = "Run 20 minutes", category = QuestCategory.FITNESS.name, difficulty = 2, xp = 28, target = 1200, icon = "🏃", objective_type = "TIMER", target_seconds = 1200),
                AdvancedDailyQuestEntry(title = "Hit 7000 steps", category = QuestCategory.FITNESS.name, difficulty = 2, xp = 24, target = 7000, icon = "👟", objective_type = "HEALTH", health_metric = "steps", health_aggregation = "daily_total"),
                // STUDY
                AdvancedDailyQuestEntry(title = "Deep work 45 min", category = QuestCategory.STUDY.name, difficulty = 3, xp = 40, target = 2700, icon = "🧠", objective_type = "TIMER", target_seconds = 2700),
                AdvancedDailyQuestEntry(title = "Review 30 flashcards", category = QuestCategory.STUDY.name, difficulty = 2, xp = 24, target = 30, icon = "🃏", objective_type = "COUNT"),
                // HYDRATION
                AdvancedDailyQuestEntry(title = "Drink 8 cups water", category = QuestCategory.HYDRATION.name, difficulty = 2, xp = 20, target = 8, icon = "💧", objective_type = "COUNT"),
                AdvancedDailyQuestEntry(title = "Walk 2500 meters", category = QuestCategory.HYDRATION.name, difficulty = 2, xp = 24, target = 2500, icon = "🛣️", objective_type = "HEALTH", health_metric = "distance_m", health_aggregation = "daily_total"),
                // DISCIPLINE
                AdvancedDailyQuestEntry(title = "Focused cleanup 15 min", category = QuestCategory.DISCIPLINE.name, difficulty = 2, xp = 24, target = 900, icon = "🧹", objective_type = "TIMER", target_seconds = 900),
                AdvancedDailyQuestEntry(title = "Declutter one zone", category = QuestCategory.DISCIPLINE.name, difficulty = 2, xp = 20, target = 1, icon = "📦", objective_type = "COUNT"),
                // MIND
                AdvancedDailyQuestEntry(title = "Meditation 12 min", category = QuestCategory.MIND.name, difficulty = 2, xp = 24, target = 720, icon = "🧘", objective_type = "TIMER", target_seconds = 720),
                AdvancedDailyQuestEntry(title = "Calm average heart rate", category = QuestCategory.MIND.name, difficulty = 3, xp = 30, target = 92, icon = "❤️", objective_type = "HEALTH", health_metric = "heart_rate", health_aggregation = "daily_avg")
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

    fun buildAdvancedTemplatePromptFromRequest(userRequest: String, allowThemeChanges: Boolean): String {
        val request = userRequest.trim().ifBlank { "Generate 100 daily quests and 30 main quests in Saitama-style progression." }
        val lower = request.lowercase(Locale.getDefault())
        val dailyCount = Regex("""(\d{1,4})\s*(daily|day|dailies)""").find(lower)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val mainCount = Regex("""(\d{1,4})\s*(main|story|boss|milestone)""").find(lower)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val shopCount = Regex("""(\d{1,4})\s*(shop|item|items|store)""").find(lower)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val finalDailyCount = dailyCount?.coerceAtMost(advancedDailyImportLimit)
        val finalMainCount = mainCount?.coerceAtMost(advancedMainImportLimit)
        val finalShopCount = shopCount?.coerceAtMost(advancedShopImportLimit)
        val countHint = buildString {
            append("- Hard caps: daily <= $advancedDailyImportLimit, main <= $advancedMainImportLimit, shop_items <= $advancedShopImportLimit.")
            if (allowThemeChanges) {
                append("\n- Theme generation is enabled for this run.")
            } else {
                append("\n- Theme generation is disabled for this run.")
            }
            if (finalDailyCount != null) append("\n- Daily quests target: $finalDailyCount.")
            if (finalMainCount != null) append("\n- Main quests target: $finalMainCount.")
            if (finalShopCount != null) append("\n- Shop items target: $finalShopCount.")
            if (dailyCount != null && finalDailyCount != dailyCount) append("\n- Daily request was capped from $dailyCount to $finalDailyCount.")
            if (mainCount != null && finalMainCount != mainCount) append("\n- Main request was capped from $mainCount to $finalMainCount.")
            if (shopCount != null && finalShopCount != shopCount) append("\n- Shop request was capped from $shopCount to $finalShopCount.")
        }
        val enrichedRequest = if (countHint.isBlank()) request else request + "\n\nCOUNT TARGETS:\n" + countHint
        val themePolicyGoal = if (allowThemeChanges) {
            "You may edit daily_quests, main_quests, shop_items, app_theme, accent_argb, and template_settings."
        } else {
            "You may edit daily_quests, main_quests, and shop_items only. Keep app_theme, accent_argb, and template_settings unchanged."
        }
        val themePolicyRule = if (allowThemeChanges) {
            "Theme changes are allowed when they help satisfy USER REQUEST."
        } else {
            "Theme changes are NOT allowed for this run. Do not modify app_theme, accent_argb, or template_settings."
        }
        return advancedTemplatePromptText
            .replace("{{USER_REQUEST}}", enrichedRequest)
            .replace("{{THEME_POLICY_GOAL}}", themePolicyGoal)
            .replace("{{THEME_POLICY_RULE}}", themePolicyRule)
    }

    fun importAdvancedTemplateJson(raw: String): AdvancedTemplateImportResult {
        val payload = raw.trim()
        if (payload.isBlank()) return AdvancedTemplateImportResult(false, "Unnamed", 0, 0, errors = listOf("File is empty."))
        val parsed = runCatching { advancedTemplateGson.fromJson(payload, AdvancedTemplateFile::class.java) }.getOrNull()
            ?: return AdvancedTemplateImportResult(false, "Unnamed", 0, 0, errors = listOf("Invalid JSON format."))
        val supportedSchema = 2
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
            val objectiveType = runCatching { QuestObjectiveType.valueOf(q.objective_type.trim().uppercase(Locale.getDefault())) }.getOrDefault(QuestObjectiveType.COUNT)
            val safeHealthMetric = q.health_metric?.trim()?.lowercase(Locale.getDefault())?.takeIf { it in setOf("steps", "heart_rate", "distance_m", "calories_kcal") }
            val safeTargetSeconds = q.target_seconds?.coerceIn(30, 24 * 60 * 60)
            CustomTemplate(
                id = UUID.randomUUID().toString(),
                category = category,
                difficulty = q.difficulty.coerceIn(1, 5),
                title = title,
                icon = q.icon.trim().ifBlank { "✅" }.take(3),
                xp = q.xp.coerceIn(1, 5000),
                target = if (objectiveType == QuestObjectiveType.TIMER) (safeTargetSeconds ?: q.target).coerceIn(30, 24 * 60 * 60) else q.target.coerceIn(1, 500),
                isPinned = q.pinned,
                imageUri = q.image_uri?.takeIf { it.isNotBlank() },
                packageId = packageId,
                objectiveType = objectiveType,
                targetSeconds = if (objectiveType == QuestObjectiveType.TIMER) safeTargetSeconds else null,
                healthMetric = if (objectiveType == QuestObjectiveType.HEALTH) safeHealthMetric else null,
                healthAggregation = if (objectiveType == QuestObjectiveType.HEALTH) q.health_aggregation?.trim()?.take(32) else null
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
        val safeTemplateSettings = parsed.template_settings?.copy(
            dailyResetHour = parsed.template_settings.dailyResetHour.coerceIn(0, 23),
            fontScalePercent = parsed.template_settings.fontScalePercent.coerceIn(80, 125),
            neonFlowSpeed = parsed.template_settings.neonFlowSpeed.coerceIn(0, 2),
            backgroundImageTransparencyPercent = parsed.template_settings.backgroundImageTransparencyPercent?.coerceIn(0, 100),
            accentTransparencyPercent = parsed.template_settings.accentTransparencyPercent?.coerceIn(0, 100),
            textTransparencyPercent = parsed.template_settings.textTransparencyPercent?.coerceIn(0, 100),
            appBgTransparencyPercent = parsed.template_settings.appBgTransparencyPercent?.coerceIn(0, 100),
            chromeBgTransparencyPercent = parsed.template_settings.chromeBgTransparencyPercent?.coerceIn(0, 100),
            cardBgTransparencyPercent = parsed.template_settings.cardBgTransparencyPercent?.coerceIn(0, 100),
            journalPageTransparencyPercent = parsed.template_settings.journalPageTransparencyPercent?.coerceIn(0, 100),
            journalAccentTransparencyPercent = parsed.template_settings.journalAccentTransparencyPercent?.coerceIn(0, 100),
            buttonTransparencyPercent = parsed.template_settings.buttonTransparencyPercent?.coerceIn(0, 100)
        )
        val importedTemplate = GameTemplate(
            templateName = templateName,
            appTheme = theme,
            dailyQuests = customTemplatesToQuestTemplates(daily),
            mainQuests = main,
            shopItems = shop,
            packageId = packageId,
            templateSettings = safeTemplateSettings,
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
                packageId = t.packageId,
                objectiveType = qt.objectiveType,
                targetSeconds = qt.targetSeconds,
                healthMetric = qt.healthMetric,
                healthAggregation = qt.healthAggregation
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
                packageId = template.packageId,
                objectiveType = qt.objectiveType,
                targetSeconds = qt.targetSeconds,
                healthMetric = qt.healthMetric,
                healthAggregation = qt.healthAggregation
            )
        }
        persistCustomTemplates(mappedDailies)
        persistMainQuests(template.mainQuests)
        persistShopItems(template.shopItems)
        activePackageIds = setOf(template.packageId)
        scope.launch { appContext.dataStore.edit { p -> p[activePacksKey] = activePackageIds.joinToString(",") } }
        regenerateForDay(currentEpochDay())
    }

    fun publishCurrentTemplateToCommunity(title: String, description: String, tagsRaw: String) {
        if (!isLoggedIn) { showLoginRequiredDialog = true; return }
        val now = System.currentTimeMillis()
        val cooldownMs = 1000L * 60L * 3L
        if (now - lastCommunityPublishAt < cooldownMs) {
            scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_publishing_cooldown)) }
            return
        }
        val recentBurstCount = communityPosts.count { it.authorId == communityUserId && now - it.createdAtMillis < (60L * 60L * 1000L) }
        if (recentBurstCount >= 5) {
            scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_publishing_locked)) }
            return
        }
        val rawTitle = title.trim()
        if (rawTitle.length < 3) {
            scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_title_too_short)) }
            return
        }
        val cleanTitle = sanitizeCommunityText(rawTitle, 60)
        val cleanDescription = sanitizeCommunityText(
            description.ifBlank { "Community challenge by $communityUserName." },
            280
        )
        val tags = sanitizeTags(tagsRaw.split(","))
        if (cleanDescription.length < 10) {
            scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_description_too_short)) }
            return
        }
        if (tags.size > 8) {
            scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_tags_limit)) }
            return
        }
        val tagRegex = Regex("^[a-zA-Z0-9 _-]{2,24}$")
        if (tags.any { !tagRegex.matches(it) }) {
            scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_tags_invalid)) }
            return
        }
        if (hasSuspiciousCommunityContent(cleanTitle) || hasSuspiciousCommunityContent(cleanDescription) || tags.any { hasSuspiciousCommunityContent(it) }) {
            scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_unsafe_content)) }
            return
        }
        val duplicateExists = communityPosts.any {
            it.title.equals(cleanTitle, ignoreCase = true)
        }
        if (duplicateExists) {
            scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_title_duplicate)) }
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
            scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_nothing_safe_to_publish)) }
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
        scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_challenge_published)) }
        scope.launch { appContext.dataStore.edit { p -> p[Keys.COMMUNITY_LAST_PUBLISH_AT] = now } }
        scope.launch {
            val ok = SupabaseApi.publishPost(post)
            if (!ok) enqueueCommunitySyncTask(CommunitySyncTask(type = CommunitySyncTaskType.PUBLISH_POST, post = post))
            syncCommunityFromRemote()
        }
    }

    fun onToggleFollowAuthor(authorId: String) {
        if (!isLoggedIn) { showLoginRequiredDialog = true; return }
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
        if (!isLoggedIn) { showLoginRequiredDialog = true; return }
        AppLog.w("Community report submitted for author=$authorId by user=$communityUserId")
        scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_report_submitted)) }
    }

    fun onRateCommunityPost(postId: String, starsRaw: Int) {
        if (!isLoggedIn) { showLoginRequiredDialog = true; return }
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
            scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_premium_template)) }
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
        scope.launch { snackbarHostState.showSnackbar(appContext.getString(R.string.xp_bonus_snackbar, xp)) }
    }

    fun awardFocusXp(xp: Int) {
        totalXp += xp; SoundManager.playSuccess(); persistCore()
        scope.launch { snackbarHostState.showSnackbar(appContext.getString(R.string.focus_session_xp_snackbar, xp)) }
    }

    fun onUpdateQuestProgress(id: Int, newProgress: Int) {
        timerPersistJob?.cancel()
        timerPersistJob = null
        val updated = quests.map { q ->
            if (q.id == id) q.copy(currentProgress = newProgress.coerceIn(0, q.target + 1)) else q
        }
        quests = updated

        // Save immediately so it survives Theme Changes
        val (base, completedIds) = todayBaseAndCompleted()
        persistToday(lastDayEpoch, base, completedIds, earnedIds, refreshCount)
    }

    fun onTimerTickProgress(id: Int, newProgress: Int) {
        val before = quests.firstOrNull { it.id == id } ?: return
        val clamped = newProgress.coerceIn(0, before.target + 1)
        if (before.currentProgress == clamped) return

        quests = quests.map { q ->
            if (q.id == id) q.copy(currentProgress = clamped) else q
        }

        timerPersistJob?.cancel()
        timerPersistJob = scope.launch {
            delay(5000)
            val (base, completedIds) = todayBaseAndCompleted()
            persistToday(lastDayEpoch, base, completedIds, earnedIds, refreshCount)
            timerPersistJob = null
        }
    }

    fun onTimerComplete(id: Int, newProgress: Int) {
        timerPersistJob?.cancel()
        timerPersistJob = null

        val before = quests.firstOrNull { it.id == id } ?: return
        val clamped = newProgress.coerceIn(0, before.target + 1)
        if (before.currentProgress != clamped) {
            quests = quests.map { q ->
                if (q.id == id) q.copy(currentProgress = clamped) else q
            }
        }

        val (base, completedIds) = todayBaseAndCompleted()
        persistToday(lastDayEpoch, base, completedIds, earnedIds, refreshCount)
    }

    fun flushTimerPersist() {
        val pending = timerPersistJob
        timerPersistJob = null
        pending?.cancel()
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
                clamped > before.target -> appContext.getString(R.string.progress_ready_to_claim)
                clamped == before.target -> appContext.getString(R.string.progress_marked_done)
                clamped <= 0 -> appContext.getString(R.string.progress_reset)
                clamped == 1 -> appContext.getString(R.string.quest_started_msg)
                else -> appContext.getString(R.string.progress_value_of_target, clamped, before.target)
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

    fun onRemoveQuestFromToday(id: Int) {
        val target = quests.firstOrNull { it.id == id } ?: return
        var newTotalXp = totalXp
        var newGold = gold
        var newAttrs = attributes
        var newEarned = earnedIds

        if (newEarned.contains(id)) {
            newTotalXp = (newTotalXp - target.xpReward).coerceAtLeast(0)
            val rewardGold = calculateGoldReward(target.difficulty, streak)
            newGold = (newGold - rewardGold).coerceAtLeast(0)
            newAttrs = when (target.category) {
                QuestCategory.FITNESS -> newAttrs.copy(str = (newAttrs.str - 1).coerceAtLeast(1))
                QuestCategory.STUDY -> newAttrs.copy(int = (newAttrs.int - 1).coerceAtLeast(1))
                QuestCategory.HYDRATION -> newAttrs.copy(vit = (newAttrs.vit - 1).coerceAtLeast(1))
                QuestCategory.DISCIPLINE -> newAttrs.copy(end = (newAttrs.end - 1).coerceAtLeast(1))
                QuestCategory.MIND -> newAttrs.copy(fth = (newAttrs.fth - 1).coerceAtLeast(1))
            }
            newEarned = newEarned - id
        }

        quests = quests.filterNot { it.id == id }
        totalXp = newTotalXp
        gold = newGold
        attributes = newAttrs
        earnedIds = newEarned
        persistCore()
        persistAttributes(newAttrs)

        val (base, completedIds) = todayBaseAndCompleted()
        persistToday(lastDayEpoch, base, completedIds, earnedIds, refreshCount)
        updateHistory(lastDayEpoch, base, completedIds)
        scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_quest_removed)) }
    }

    fun onOpenQuestEditorFromHome(id: Int) {
        val targetQuest = quests.firstOrNull { it.id == id } ?: return
        fun stableMatches(template: CustomTemplate): Boolean {
            val q = QuestTemplate(
                category = template.category,
                difficulty = template.difficulty,
                title = template.title,
                icon = template.icon,
                xp = template.xp,
                target = template.target,
                isPinned = template.isPinned,
                imageUri = template.imageUri,
                packageId = template.packageId,
                objectiveType = template.objectiveType,
                targetSeconds = template.targetSeconds,
                healthMetric = template.healthMetric,
                healthAggregation = template.healthAggregation
            )
            return stableQuestId(template.category, q) == targetQuest.id
        }
        var matchedTemplate = customTemplates.firstOrNull(::stableMatches)
            ?: customTemplates.firstOrNull {
                it.title.equals(targetQuest.title, ignoreCase = true) &&
                    it.category == targetQuest.category &&
                    it.packageId == targetQuest.packageId
            }
            ?: customTemplates.firstOrNull {
                it.title.equals(targetQuest.title, ignoreCase = true) &&
                    it.category == targetQuest.category
            }
            ?: customTemplates.firstOrNull { it.title.equals(targetQuest.title, ignoreCase = true) }

        if (matchedTemplate == null) {
            val created = CustomTemplate(
                id = UUID.randomUUID().toString(),
                category = targetQuest.category,
                difficulty = targetQuest.difficulty.coerceIn(1, 5),
                title = targetQuest.title,
                icon = targetQuest.icon.ifBlank { "⭐" },
                xp = targetQuest.xpReward.coerceIn(5, 500),
                target = when (targetQuest.objectiveType) {
                    QuestObjectiveType.HEALTH -> targetQuest.target.coerceAtLeast(100)
                    QuestObjectiveType.TIMER -> (targetQuest.targetSeconds ?: targetQuest.target).coerceAtLeast(60)
                    else -> targetQuest.target.coerceAtLeast(1)
                },
                isPinned = false,
                imageUri = targetQuest.imageUri,
                packageId = targetQuest.packageId,
                objectiveType = targetQuest.objectiveType,
                targetSeconds = if (targetQuest.objectiveType == QuestObjectiveType.TIMER) {
                    (targetQuest.targetSeconds ?: targetQuest.target).coerceAtLeast(60)
                } else null,
                healthMetric = if (targetQuest.objectiveType == QuestObjectiveType.HEALTH) targetQuest.healthMetric else null,
                healthAggregation = if (targetQuest.objectiveType == QuestObjectiveType.HEALTH) {
                    targetQuest.healthAggregation ?: "daily_total"
                } else null
            )
            persistCustomTemplates(customTemplates + created)
            matchedTemplate = created
        }
        questsPreferredTab = 0
        pendingHomeEditDailyTemplateId = matchedTemplate?.id
        screen = Screen.QUESTS
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
                message = appContext.getString(R.string.xp_earned_msg, questBefore.xpReward),
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
            scope.launch { snackbarHostState.showSnackbar(appContext.getString(R.string.invalid_item_price)) }
            return
        }
        if (item.stock <= 0) {
            scope.launch { snackbarHostState.showSnackbar(appContext.getString(R.string.item_out_of_stock, item.name)) }
            return
        }
        if (gold < item.cost) {
            scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_not_enough_gold)) }
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
        scope.launch { snackbarHostState.showSnackbar(appContext.getString(R.string.used_item_name, item.name)) }
    }

    fun onUpsertShopItem(item: ShopItem) {
        if (!customMode) {
            scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_custom_mode_required)) }
            return
        }
        val cappedMax = item.maxStock.coerceAtLeast(1)
        val cappedStock = item.stock.coerceIn(0, cappedMax)
        val sanitized = item.copy(maxStock = cappedMax, stock = cappedStock, cost = item.cost.coerceAtLeast(1))
        val list = shopItems.toMutableList()
        val idx = list.indexOfFirst { it.id == sanitized.id }
        if (idx >= 0) list[idx] = sanitized else list.add(sanitized)
        persistShopItems(list)
        scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_shop_item_saved)) }
    }

    fun onDeleteShopItem(id: String) {
        if (!customMode) {
            scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_custom_mode_required)) }
            return
        }
        val removed = shopItems.firstOrNull { it.id == id } ?: return
        persistShopItems(shopItems.filterNot { it.id == id })
        scope.launch {
            val res = snackbarHostState.showSnackbar(appContext.getString(R.string.shop_item_removed), actionLabel = "UNDO", duration = SnackbarDuration.Short)
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
            persistInventory(newInv); scope.launch { snackbarHostState.showSnackbar(appContext.getString(R.string.used_item_name, item.name)) }
        }
    }

    fun onAddPlan(day: Long, text: String) {
        val clean = text.trim()
        if (clean.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_plan_title_required)) }
            return
        }
        val next = calendarPlans.toMutableMap()
        val current = next[day].orEmpty()
        next[day] = current + clean
        persistCalendarPlans(next)
        pushPlanStateToSupabase(next)
        AppLog.event("plan_add", "day=$day")
        scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_plan_added)) }
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
            val res = snackbarHostState.showSnackbar(getString(R.string.snackbar_plan_removed), actionLabel = getString(R.string.undo), duration = SnackbarDuration.Short)
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
        scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_quest_pool_updated)) }
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
        persistMainQuests(mainQuests.filterNot { it.id == id }); scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_deleted)) }
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
            shopItems = defaultTemplate.shopItems
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
            snackbarHostState.showSnackbar(getString(R.string.snackbar_reset_complete))
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
        ThemeRuntime.accentTransparencyPercent = accentTransparencyPercent
        ThemeRuntime.textTransparencyPercent = textTransparencyPercent
        ThemeRuntime.appBgTransparencyPercent = appBgTransparencyPercent
        ThemeRuntime.chromeBgTransparencyPercent = chromeBgTransparencyPercent
        ThemeRuntime.cardBgTransparencyPercent = cardBgTransparencyPercent
        ThemeRuntime.journalPageTransparencyPercent = journalPageTransparencyPercent
        ThemeRuntime.journalAccentTransparencyPercent = journalAccentTransparencyPercent
        ThemeRuntime.buttonTransparencyPercent = buttonTransparencyPercent
        if (showLoginRequiredDialog) {
            AlertDialog(
                onDismissRequest = { showLoginRequiredDialog = false },
                title = { Text(stringResource(R.string.l10n_sign_in_required), color = OnCardText) },
                text = { Text(stringResource(R.string.l10n_you_need_to_sign_in_with_google_to_use_thi), color = OnCardText.copy(alpha = 0.8f)) },
                confirmButton = {
                    TextButton(onClick = {
                        showLoginRequiredDialog = false
                        settingsExpandedSection = "hub"
                        screen = Screen.SETTINGS
                    }) { Text(stringResource(R.string.l10n_go_to_settings)) }
                },
                dismissButton = {
                    TextButton(onClick = { showLoginRequiredDialog = false }) {
                        Text(stringResource(R.string.cancel), color = OnCardText.copy(alpha = 0.6f))
                    }
                },
                containerColor = CardDarkBlue
            )
        }
        if (showRefreshDayConfirm) { AlertDialog(onDismissRequest = { showRefreshDayConfirm = false }, title = { Text(stringResource(R.string.l10n_start_new_day)) }, text = { Text(stringResource(R.string.l10n_this_forces_a_new_day_calculation)) }, confirmButton = { TextButton(onClick = { onRefreshDay(); showRefreshDayConfirm = false }) { Text(stringResource(R.string.l10n_start_day)) } }, dismissButton = { TextButton(onClick = { showRefreshDayConfirm = false }) { Text(stringResource(R.string.cancel)) } }) }
        if (showLevelUpDialog) { AlertDialog(onDismissRequest = { showLevelUpDialog = false }, title = { Text(stringResource(R.string.l10n_level_up), fontWeight = FontWeight.Bold, color = accentStrong) }, text = { Text(stringResource(R.string.level_up_congrats, currentLevel)) }, confirmButton = { TextButton(onClick = { showLevelUpDialog = false }) { Text(stringResource(R.string.l10n_awesome)) } }) }
        if (showBackupImport) {
            AlertDialog(
                onDismissRequest = { showBackupImport = false },
                title = { Text(stringResource(R.string.l10n_import_encrypted_backup)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.l10n_paste_your_encrypted_backup_import_applies), color = OnCardText.copy(alpha = 0.75f), fontSize = 12.sp)
                        OutlinedTextField(
                            value = backupImportPayload,
                            onValueChange = { backupImportPayload = it },
                            label = { Text(stringResource(R.string.l10n_backup_payload)) },
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
                    }) { Text(stringResource(R.string.l10n_import)) }
                },
                dismissButton = {
                    TextButton(onClick = { showBackupImport = false }) { Text(stringResource(R.string.cancel)) }
                }
            )
        }
        if (showResetAll) {
            AlertDialog(
                onDismissRequest = { showResetAll = false },
                title = { Text(stringResource(R.string.l10n_reset_everything)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.l10n_this_will_erase_progress_and_active_data))
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { resetBackupBefore = !resetBackupBefore },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = resetBackupBefore, onCheckedChange = { resetBackupBefore = it })
                        Text(stringResource(R.string.l10n_save_current_setup_to_template_before_rese))
                        }
                        if (resetBackupBefore) {
                            OutlinedTextField(
                                value = resetBackupName,
                                onValueChange = { resetBackupName = it },
                                label = { Text(stringResource(R.string.l10n_backup_name)) },
                                singleLine = true
                            )
                        }
                        Text(stringResource(R.string.l10n_default_package_will_be_enabled_automatica))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        resetAll(resetBackupBefore)
                        showResetAll = false
                    }) { Text(stringResource(R.string.l10n_reset)) }
                },
                dismissButton = { TextButton(onClick = { showResetAll = false }) { Text(stringResource(R.string.cancel)) } }
            )
        }
        if (remixPostPending != null) {
            AlertDialog(
                onDismissRequest = { remixPostPending = null },
                containerColor = CardDarkBlue,
                title = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.l10n_add_template_from_community),
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
                                contentDescription = stringResource(R.string.close),
                                tint = OnCardText.copy(alpha = 0.82f)
                            )
                        }
                    }
                },
                text = {
                    Text(
                        stringResource(R.string.remix_add_template_body, remixPostPending!!.title),
                        color = OnCardText.copy(alpha = 0.86f)
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val post = remixPostPending
                        remixPostPending = null
                        if (post != null) finalizeRemix(post, applyNow = true)
                    }) { Text(stringResource(R.string.l10n_save_apply), color = accentStrong, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        val post = remixPostPending
                        remixPostPending = null
                        if (post != null) finalizeRemix(post, applyNow = false)
                    }) { Text(stringResource(R.string.save), color = OnCardText) }
                }
            )
        }
        if (pendingUncheckQuestId != null) { AlertDialog(onDismissRequest = { pendingUncheckQuestId = null }, title = { Text(stringResource(R.string.l10n_uncheck_quest)) }, text = { Text(stringResource(R.string.l10n_this_will_remove_earned_xp_but_keep_earned)) }, confirmButton = { TextButton(onClick = { val id = pendingUncheckQuestId; pendingUncheckQuestId = null; if (id != null) onToggleQuest(id, force = true) }) { Text(stringResource(R.string.l10n_uncheck)) } }, dismissButton = { TextButton(onClick = { pendingUncheckQuestId = null }) { Text(stringResource(R.string.cancel)) } }) }
        if (showFocusTimer) { FocusTimerDialog(accentStrong = accentStrong, accentSoft = accentSoft, onDismiss = { showFocusTimer = false }, onComplete = { minutes -> awardFocusXp(minutes); showFocusTimer = false }) }

// 1. Initial Import Dialog
        if (pendingImportTemplate != null) {
            AlertDialog(
                onDismissRequest = { pendingImportTemplate = null },
                containerColor = CardDarkBlue,
                title = { Text(stringResource(R.string.l10n_save_template), color = accentStrong, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(stringResource(R.string.l10n_you_are_about_to_save), color = OnCardText)
                        Text(stringResource(R.string.template_bullet_name, pendingImportTemplate!!.templateName), fontWeight = FontWeight.Bold, color = accentStrong)
                        Text(stringResource(R.string.template_bullet_daily_count, pendingImportTemplate!!.dailyQuests.size), color = OnCardText)
                        Text(stringResource(R.string.template_bullet_main_count, pendingImportTemplate!!.mainQuests.size), color = OnCardText)
                        Text(stringResource(R.string.template_bullet_shop_count, pendingImportTemplate!!.shopItems.size), color = OnCardText)
                        Text(stringResource(R.string.template_bullet_theme_name, pendingImportTemplate!!.appTheme.name), color = accentStrong)
                        Text(stringResource(R.string.l10n_includes_advanced_options_background_if_pr), color = OnCardText.copy(alpha = 0.85f))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val t = pendingImportTemplate!!
                        persistSavedTemplates(savedTemplates + t)
                        promptApplyTemplate = t // NEW: Trigger the follow-up dialog
                        pendingImportTemplate = null
                    }) { Text(stringResource(R.string.save_template_lib), color = accentStrong) }
                },
                dismissButton = { TextButton(onClick = { pendingImportTemplate = null }) { Text(stringResource(R.string.cancel), color = OnCardText) } }
            )
        }

// 2. Follow-Up Apply Prompt (With Backup & Clear Checkboxes)
        if (promptApplyTemplate != null) {
            AlertDialog(
                onDismissRequest = { promptApplyTemplate = null },
                containerColor = CardDarkBlue,
                title = { Text(stringResource(R.string.equip_template_title), color = accentStrong, fontWeight = FontWeight.Bold) },
                text = {
                    val t = promptApplyTemplate!!
                    val dailyDelta = t.dailyQuests.size - customTemplates.size
                    val mainDelta = t.mainQuests.size - mainQuests.size
                    val shopDelta = t.shopItems.size - shopItems.size
                    val minCost = t.shopItems.minOfOrNull { it.cost.coerceAtLeast(1) } ?: 0
                    val affordableCount = t.shopItems.count { gold >= it.cost.coerceAtLeast(1) }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.l10n_template_saved_are_you_sure_you_want_to_ap), color = OnCardText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.l10n_this_will_change_theme_background_advanced), color = OnCardText.copy(alpha = 0.85f), fontSize = 13.sp)
                        Text(
                            stringResource(R.string.template_preview_delta_line, if (dailyDelta >= 0) "+$dailyDelta" else "$dailyDelta", if (mainDelta >= 0) "+$mainDelta" else "$mainDelta", if (shopDelta >= 0) "+$shopDelta" else "$shopDelta"),
                            color = OnCardText.copy(alpha = 0.84f),
                            fontSize = 12.sp
                        )
                        Text(
                            stringResource(R.string.template_economy_line, if (minCost > 0) appContext.getString(R.string.template_economy_min_item_gold, minCost) else appContext.getString(R.string.template_economy_no_shop_items), affordableCount, t.shopItems.size),
                            color = OnCardText.copy(alpha = 0.72f),
                            fontSize = 12.sp
                        )
                        HorizontalDivider(color = OnCardText.copy(alpha=0.1f))

                        // NEW: Clear Existing Checkbox
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { importClearExisting = !importClearExisting }) {
                            Checkbox(checked = importClearExisting, onCheckedChange = { importClearExisting = it }, colors = CheckboxDefaults.colors(checkedColor = accentStrong))
                            Text(stringResource(R.string.clear_existing_quests), color = OnCardText, fontSize = 14.sp)
                        }

                        // Backup Checkbox
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { importBackupBeforeApply = !importBackupBeforeApply }) {
                            Checkbox(checked = importBackupBeforeApply, onCheckedChange = { importBackupBeforeApply = it }, colors = CheckboxDefaults.colors(checkedColor = accentStrong))
                            Text(stringResource(R.string.backup_current_setup), color = OnCardText, fontSize = 14.sp)
                        }
                        if (importBackupBeforeApply) {
                            OutlinedTextField(value = importBackupName, onValueChange = { importBackupName = it }, label = { Text(stringResource(R.string.backup_name_label), color = OnCardText.copy(alpha=0.5f)) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OnCardText, unfocusedTextColor = OnCardText, cursorColor = accentStrong))
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
                            persistShopItems(t.shopItems)
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

                        scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_theme_applied)) }
                        promptApplyTemplate = null
                    }) { Text(stringResource(R.string.l10n_yes_equip_now), color = accentStrong) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_template_saved)) }
                        promptApplyTemplate = null
                    }) { Text(stringResource(R.string.l10n_no_later), color = OnCardText) }
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
                    CustomTemplate(UUID.randomUUID().toString(), qt.category, qt.difficulty, qt.title, qt.icon, qt.xp, qt.target, qt.isPinned, qt.imageUri, pid, true, qt.objectiveType, qt.targetSeconds, qt.healthMetric, qt.healthAggregation)
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

                // 3. Add Shop Items
                val prefixedShop = template.shopItems.map { it.copy(id = "${pid}_${it.id}".take(64)) }
                val currentShopIds = shopItems.map { it.id }.toSet()
                val shopToAdd = prefixedShop.filterNot { currentShopIds.contains(it.id) }
                persistShopItems(shopItems + shopToAdd)

                SoundManager.playAccept() // Nice feedback

            } else {
                // DISABLE: Remove ID and Quests
                activePackageIds = activePackageIds - pid

                // 1. Remove Dailies belonging to this pack
                persistCustomTemplates(customTemplates.filter { it.packageId != pid })

                // 2. Remove Main Quests belonging to this pack
                persistMainQuests(mainQuests.filter { it.packageId != pid })

                // 3. Remove Shop Items belonging to this pack
                val afterPackageRemoval = shopItems.filterNot { it.id.startsWith("${pid}_") }
                val finalShop = if (pid == REAL_DAILY_LIFE_PACKAGE_ID) {
                    afterPackageRemoval.filterNot { it.id == "shop_apple" || it.id == "shop_coffee" }
                } else {
                    afterPackageRemoval
                }
                persistShopItems(finalShop)

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
                    Text(stringResource(R.string.app_name), modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = drawerContentColor)
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
                val questsScreenContent: @Composable () -> Unit = {
                    QuestsScreen(
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
                                                            scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_custom_mode_required)) }
                                                            return@QuestsScreen
                                                        }
                                                        val safeTemplate = sanitizeHealthTemplateOrNull(t)
                                                        if (safeTemplate == null) {
                                                            scope.launch { snackbarHostState.showSnackbar("Invalid health quest metric. Use steps, heart_rate, distance_m, or calories_kcal.") }
                                                            return@QuestsScreen
                                                        }
                                                        val list = customTemplates.toMutableList()
                                                        val idx = list.indexOfFirst { it.id == safeTemplate.id }
                                                        val oldTemplate = if (idx >= 0) list[idx] else null
                                                        val isNewTemplate = idx < 0
                                                        if (idx >= 0) list[idx] = safeTemplate else list.add(safeTemplate)
                                                        persistCustomTemplates(list)
                                                        if (isNewTemplate) {
                                                            regenerateForDay(currentEpochDay())
                                                        } else if (oldTemplate != null) {
                                                           fun stableId(template: CustomTemplate): Int {
                                                               val qt = QuestTemplate(
                                                                   category = template.category,
                                                                   difficulty = template.difficulty,
                                                                   title = template.title,
                                                                   icon = template.icon,
                                                                   xp = template.xp,
                                                                   target = template.target,
                                                                   isPinned = template.isPinned,
                                                                   imageUri = template.imageUri,
                                                                   packageId = template.packageId,
                                                                   objectiveType = template.objectiveType,
                                                                   targetSeconds = template.targetSeconds,
                                                                   healthMetric = template.healthMetric,
                                                                   healthAggregation = template.healthAggregation
                                                               )
                                                               return stableQuestId(template.category, qt)
                                                           }
                                                           val oldQuestId = stableId(oldTemplate)
                                                           val matchIndex = quests.indexOfFirst { q ->
                                                               q.id == oldQuestId ||
                                                                   (q.title.equals(oldTemplate.title, ignoreCase = true) &&
                                                                       q.category == oldTemplate.category &&
                                                                       q.packageId == oldTemplate.packageId)
                                                           }
                                                           if (matchIndex >= 0) {
                                                                val newQuestId = stableId(safeTemplate)
                                                                val existing = quests[matchIndex]
                                                                val nextTarget = when (safeTemplate.objectiveType) {
                                                                    QuestObjectiveType.TIMER -> (safeTemplate.targetSeconds ?: safeTemplate.target).coerceAtLeast(60)
                                                                    QuestObjectiveType.HEALTH -> safeTemplate.target.coerceAtLeast(100)
                                                                    QuestObjectiveType.COUNT -> safeTemplate.target.coerceAtLeast(1)
                                                                }
                                                                val updatedQuest = existing.copy(
                                                                    id = newQuestId,
                                                                    title = safeTemplate.title,
                                                                    xpReward = safeTemplate.xp,
                                                                    icon = safeTemplate.icon,
                                                                    category = safeTemplate.category,
                                                                    difficulty = safeTemplate.difficulty,
                                                                    target = nextTarget,
                                                                    currentProgress = if (existing.completed) nextTarget else existing.currentProgress.coerceAtMost(nextTarget),
                                                                    imageUri = safeTemplate.imageUri,
                                                                    packageId = safeTemplate.packageId,
                                                                    objectiveType = safeTemplate.objectiveType,
                                                                    targetSeconds = if (safeTemplate.objectiveType == QuestObjectiveType.TIMER) nextTarget else null,
                                                                    healthMetric = if (safeTemplate.objectiveType == QuestObjectiveType.HEALTH) safeTemplate.healthMetric else null,
                                                                    healthAggregation = if (safeTemplate.objectiveType == QuestObjectiveType.HEALTH) (safeTemplate.healthAggregation ?: "daily_total") else null
                                                                )
                                                               val nextQuests = quests.toMutableList()
                                                               nextQuests[matchIndex] = updatedQuest
                                                               quests = nextQuests
                                                               if (oldQuestId != newQuestId && earnedIds.contains(oldQuestId)) {
                                                                   earnedIds = (earnedIds - oldQuestId) + newQuestId
                                                               }
                                                               val (base, completedIds) = todayBaseAndCompleted()
                                                               persistToday(lastDayEpoch, base, completedIds, earnedIds, refreshCount)
                                                               updateHistory(lastDayEpoch, base, completedIds)
                                                           }
                                                       }
                                                        scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_daily_quest_saved)) }
                                                   },
                                                   onDeleteDaily = { id ->
                                                       if (!customMode) {
                                                           scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_custom_mode_required)) }
                                                           return@QuestsScreen
                                                       }
                                                       val list = customTemplates.filterNot { it.id == id }
                                                       persistCustomTemplates(list)
                                                            scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_daily_quest_deleted)) }
                                                   },
                                                   onUpsertMain = { mq ->
                                                       if (!customMode) {
                                                           scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_custom_mode_required)) }
                                                           return@QuestsScreen
                                                       }
                                                       val list = mainQuests.toMutableList()
                                                       val idx = list.indexOfFirst { it.id == mq.id }
                                                       if (idx >= 0) list[idx] = mq else list.add(mq)
                                                       persistMainQuests(list)
                                                        scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_main_quest_saved)) }
                                                   },
                                                   onDeleteMain = { id ->
                                                       if (!customMode) {
                                                           scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_custom_mode_required)) }
                                                           return@QuestsScreen
                                                       }
                                                       val list = mainQuests.filterNot { it.id == id }
                                                       persistMainQuests(list)
                                                            scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_main_quest_deleted)) }
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
                                                       appContext.startActivity(Intent.createChooser(sendIntent, getString(R.string.share_template)))
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
                                                       scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_template_saved)) }
                                                   },
                                                   onApplySavedTemplate = { t, backupName, clearExisting -> // UPDATED: Added clearExisting
                                                       val safeTemplate = runCatching { normalizeGameTemplateSafe(t) }.getOrElse {
                                                            scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_incompatible_template)) }
                                                           return@QuestsScreen
                                                       }
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
                                                       appTheme = normalizeTheme(safeTemplate.appTheme)
                                                       accent = safeTemplate.accentArgb?.let { Color(it.toInt()) } ?: fallbackAccentForTheme(appTheme)
                                                       applyTemplateSettings(safeTemplate.templateSettings)
                                                       persistSettings()

                                                       val mappedDailies = safeTemplate.dailyQuests.map { qt ->
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
                                                               packageId = safeTemplate.packageId,
                                                               objectiveType = qt.objectiveType,
                                                               targetSeconds = qt.targetSeconds,
                                                               healthMetric = qt.healthMetric,
                                                               healthAggregation = qt.healthAggregation
                                                           )
                                                       }

                                                       if (clearExisting) {
                                                           persistCustomTemplates(mappedDailies)
                                                           persistMainQuests(safeTemplate.mainQuests)
                                                           persistShopItems(safeTemplate.shopItems)
                                                           activePackageIds = setOf(safeTemplate.packageId)
                                                           applyTemplateDailyQuestDefaults(safeTemplate.packageId, clearExisting = true)
                                                           persistSettings()
                                                       } else {
                                                           val newCustoms = customTemplates + mappedDailies
                                                           persistCustomTemplates(newCustoms.distinctBy { it.title })
                                                           val newMqs = mainQuests + safeTemplate.mainQuests
                                                           persistMainQuests(newMqs.distinctBy { it.title })
                                                           if (safeTemplate.shopItems.isNotEmpty()) {
                                                               val mergedShop = (shopItems + safeTemplate.shopItems).distinctBy { it.id }
                                                               persistShopItems(mergedShop)
                                                           }
                                                           activePackageIds = activePackageIds + safeTemplate.packageId
                                                       }
                                                       scope.launch { appContext.dataStore.edit { p -> p[activePacksKey] = activePackageIds.joinToString(",") } }
                                                       scope.launch { snackbarHostState.showSnackbar(appContext.getString(R.string.theme_quests_applied)) }
                                                   },
                                                   onDeleteSavedTemplate = { t ->
                                                        persistSavedTemplates(savedTemplates.filterNot { it == t })
                                                        scope.launch {
                                                            val res = snackbarHostState.showSnackbar(appContext.getString(R.string.template_deleted), actionLabel = "UNDO", duration = SnackbarDuration.Short)
                                                            if (res == SnackbarResult.ActionPerformed) {
                                                                persistSavedTemplates((savedTemplates + t).distinctBy { "${it.packageId}|${it.templateName}" })
                                                            }
                                                        }
                                                    },
                                                    onRequireCustomMode = {
                                                        scope.launch { snackbarHostState.showSnackbar(appContext.getString(R.string.enable_custom_mode_add_quests)) }
                                                    },
                                                    onDeleteCategory = { cat ->
                                                        val list = customTemplates.filterNot { it.category == cat }
                                                        persistCustomTemplates(list)
                                                        scope.launch { snackbarHostState.showSnackbar(getString(R.string.all_category_quests_deleted, categoryLabel(cat))) }
                                                    },
                                                   onDeleteChain = { family ->
                                                       fun parseFamily(title: String): String {
                                                           val m = Regex("""^(.*?)(?:\s+(\d+))$""").find(title.trim())
                                                           return (m?.groupValues?.getOrNull(1)?.trim().takeUnless { it.isNullOrBlank() } ?: title.trim())
                                                       }
                                                       val familyKey = parseFamily(family).lowercase(java.util.Locale.getDefault())
                                                       val list = mainQuests.filterNot { parseFamily(it.title).lowercase(java.util.Locale.getDefault()) == familyKey }
                                                       persistMainQuests(list)
                                                        scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_family_quests_deleted, family)) }
                                                   },
                                                   onOpenCommunityTemplates = { screen = Screen.COMMUNITY },
                                                   onOpenAdvancedTemplates = {
                                                       settingsExpandedSection = "advanced_templates"
                                                       screen = Screen.SETTINGS
                                                   },
                                                   showTutorial = !questsTutorialSeen,
                                                   onTutorialDismiss = { markQuestsTutorialSeen() },
                                                   initialTab = questsPreferredTab,
                                                   openDailyEditorForId = pendingHomeEditDailyTemplateId,
                                                   onOpenDailyEditorHandled = { pendingHomeEditDailyTemplateId = null },
                                                   onOpenDrawer = { scope.launch { drawerState.open() } },
                                                   onOpenSettings = { screen = Screen.SETTINGS }
                                               )
                }
                val settingsScreenContent: @Composable () -> Unit = {
                    SettingsScreen(
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
                                                   cloudConnected = isLoggedIn,
                                                   cloudAccountEmail = if (isLoggedIn) authUserEmail else "",
                                                   cloudLastSyncAt = cloudLastSyncAt,
                                                   dailyRemindersEnabled = dailyRemindersEnabled,
                                                   hapticsEnabled = hapticsEnabled,
                                                   soundEffectsEnabled = soundEffectsEnabled,
                                                   fontStyle = fontStyle,
                                                   fontScalePercent = fontScalePercent,
                                                   appLanguage = appLanguage,
                                                   backgroundImageTransparencyPercent = backgroundImageTransparencyPercent,
                                                   accentTransparencyPercent = accentTransparencyPercent,
                                                   textTransparencyPercent = textTransparencyPercent,
                                                   appBgTransparencyPercent = appBgTransparencyPercent,
                                                   chromeBgTransparencyPercent = chromeBgTransparencyPercent,
                                                   cardBgTransparencyPercent = cardBgTransparencyPercent,
                                                   journalPageTransparencyPercent = journalPageTransparencyPercent,
                                                   journalAccentTransparencyPercent = journalAccentTransparencyPercent,
                                                   buttonTransparencyPercent = buttonTransparencyPercent,
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
                                                   onAccentTransparencyChanged = { accentTransparencyPercent = it.coerceIn(0, 100); persistSettings() },
                                                   onTextTransparencyChanged = { textTransparencyPercent = it.coerceIn(0, 100); persistSettings() },
                                                   onAppBgTransparencyChanged = { appBgTransparencyPercent = it.coerceIn(0, 100); persistSettings() },
                                                   onChromeBgTransparencyChanged = { chromeBgTransparencyPercent = it.coerceIn(0, 100); persistSettings() },
                                                   onCardBgTransparencyChanged = { cardBgTransparencyPercent = it.coerceIn(0, 100); persistSettings() },
                                                   onJournalPageTransparencyChanged = { journalPageTransparencyPercent = it.coerceIn(0, 100); persistSettings() },
                                                   onJournalAccentTransparencyChanged = { journalAccentTransparencyPercent = it.coerceIn(0, 100); persistSettings() },
                                                   onButtonTransparencyChanged = { buttonTransparencyPercent = it.coerceIn(0, 100); persistSettings() },
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
                                                            scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_backup_export_failed)) }
                                                       } else {
                                                           val sendIntent = Intent().apply {
                                                               action = Intent.ACTION_SEND
                                                               putExtra(Intent.EXTRA_TEXT, blob)
                                                               type = "text/plain"
                                                           }
                                                           appContext.startActivity(Intent.createChooser(sendIntent, getString(R.string.settings_export_backup)))
                                                       }
                                                   },
                                                   onImportBackup = {
                                                       showBackupImport = true
                                                   },
                                                   onCloudSyncNow = { triggerCloudSnapshotSync(force = true) },
                                                   onCloudRestore = { restoreFromCloud() },
                                                   onCloudConnectRequest = {
                                                       performGoogleLogin()
                                                   },
                                                   onCloudDisconnect = { performLogout() },
                                                   onSendFeedback = { category, text -> shareFeedbackReport(category, text) },
                                                   onExportLogs = {
                                                       val sendIntent = Intent().apply {
                                                           action = Intent.ACTION_SEND
                                                           putExtra(Intent.EXTRA_TEXT, AppLog.exportRecentLogs().ifBlank { "No logs captured." })
                                                           type = "text/plain"
                                                       }
                                                       appContext.startActivity(Intent.createChooser(sendIntent, getString(R.string.settings_export_logs)))
                                                   },
                                                   onBuildAdvancedTemplateStarterJson = { buildAdvancedTemplateStarterJson() },
                                                   onBuildAdvancedTemplatePromptFromRequest = { request, allowThemeChanges ->
                                                       buildAdvancedTemplatePromptFromRequest(request, allowThemeChanges)
                                                   },
                                                   onImportAdvancedTemplateJson = { json ->
                                                       val result = importAdvancedTemplateJson(json)
                                                        if (result.success) {
                                                            scope.launch {
                                                                snackbarHostState.showSnackbar(appContext.getString(R.string.template_imported_counts, result.dailyAdded, result.mainAdded))
                                                            }
                                                        }
                                                       result
                                                   },
                                                   onApplyAdvancedTemplateByPackage = { pkg ->
                                                       val ok = applyAdvancedImportedTemplate(pkg)
                                                       if (ok) {
                                                            scope.launch { snackbarHostState.showSnackbar(getString(R.string.snackbar_advanced_template_applied)) }
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
                }
                val renderScreen: @Composable (Screen) -> Unit = { target ->
                    CompositionLocalProvider(LocalHeaderThemeToggle provides {
                        settingsExpandedSection = "appearance"
                        screen = Screen.SETTINGS
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
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = if (backgroundImageUri.isNullOrBlank()) {
                                themeBg.copy(alpha = appBgAlpha)
                            } else {
                                themeBg.copy(alpha = appBgAlpha * (1f - (backgroundImageTransparencyPercent.coerceIn(0, 100) / 100f)))
                            }
                        ) {
                        when (target) {
                            // FIXED: Screen.HOME now passes the bosses list
                            Screen.HOME -> HomeScreen(
                                Modifier.fillMaxSize().padding(padding), appContext, quests.filter { !hideCompletedQuests || !it.completed }, bosses, avatar, calculateLevel(totalXp), attributes, streak, accentStrong, accentSoft, refreshCount, gold,
                                alwaysShowQuestProgress,
                                { onRefreshTodayQuests() },
                                homeRefreshInProgress,
                                { onClaimQuestWithUndo(it) },
                                { id, prog -> onUpdateQuestProgressWithUndo(id, prog) },
                                { id, prog -> onTimerTickProgress(id, prog) },
                                { id, prog -> onTimerComplete(id, prog) },
                                { flushTimerPersist() },
                                { snapshot, startedQuestId ->
                                    persistHealthDailySnapshot(snapshot)
                                    syncHealthObjectiveQuestProgress(snapshot, startedQuestId)
                                },
                                { id -> onResetQuestProgressWithUndo(id) },
                                { id -> onRemoveQuestFromToday(id) },
                                { id -> onOpenQuestEditorFromHome(id) },
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
                                commentsByPost = communityCommentsByPost,
                                pendingSyncCount = pendingCommunitySyncQueue.size,
                                isRefreshing = communityRefreshInProgress,
                                onRefresh = { triggerCommunityRefresh() },
                                onChangeUserName = { persistCommunityProfile(communityUserId, it) },
                                onPublish = { title, desc, tags -> publishCurrentTemplateToCommunity(title, desc, tags) },
                                onToggleFollow = { onToggleFollowAuthor(it) },
                                onToggleMute = { onToggleMuteAuthor(it) },
                                onToggleBlock = { onToggleBlockAuthor(it) },
                                onReport = { onReportAuthor(it) },
                                onRate = { id, stars -> onRateCommunityPost(id, stars) },
                                onRemix = { onRemixCommunityPost(it) },
                                onSubmitComment = { postId, body -> submitCommunityComment(postId, body) },
                                onVoteComment = { postId, commentId, vote -> voteCommunityComment(postId, commentId, vote) },
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onOpenSettings = { screen = Screen.SETTINGS }
                            )

                            Screen.QUESTS -> questsScreenContent()
                            Screen.STATS -> DashboardScreen( // Pointing to the new screen!
                                modifier = Modifier.fillMaxSize().padding(padding),
                                levelInfo = calculateLevel(totalXp),
                                attributes = attributes,
                                gold = gold,
                                streak = streak,
                                history = historyMap,
                                unlockedAchievementIds = unlockedAchievementIds,
                                healthSnapshot = healthDailySnapshot,
                                onSaveHealthSnapshot = {
                                    persistHealthDailySnapshot(it)
                                    syncHealthObjectiveQuestProgress(it)
                                },
                                accentStrong = accentStrong,
                                accentSoft = accentSoft,
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onOpenSettings = { screen = Screen.SETTINGS }
                            )
                            Screen.SETTINGS -> settingsScreenContent()
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
