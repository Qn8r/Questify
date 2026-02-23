@file:Suppress(
    "ALL",
    "UNUSED",
    "UNUSED_PARAMETER",
    "UNUSED_VARIABLE",
    "UNUSED_IMPORT",
    "UNUSED_VALUE",
    "UNUSED_CHANGED_VALUE",
    "NON_UI_COMPOSABLE_CALL",
    "KotlinConstantConditions",
    "RedundantQualifierName",
    "RemoveRedundantQualifierName"
)

package com.example.livinglifemmo
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.rotate
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.content.Context
import android.content.Intent
import android.Manifest
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.UUID
import java.io.File
import java.util.Locale
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.roundToInt
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
// === HELPER: SCALABLE WRAPPER & HEADER ===

@Composable
fun ScalableScreen(modifier: Modifier = Modifier, content: @Composable (Float) -> Unit) {
    val config = LocalConfiguration.current
    Box(modifier = modifier) {
        val screenHeight = config.screenHeightDp.dp
        val baseHeight = 840.dp
        val rawScale = (screenHeight / baseHeight).coerceIn(0.70f, 1.1f)
        val finalScale = if (ThemeRuntime.compactModeEnabled) (rawScale * 0.92f).coerceIn(0.65f, 1f) else rawScale
        content(finalScale)
    }
}

val LocalHeaderThemeToggle = staticCompositionLocalOf<(() -> Unit)?> { null }

@Composable
fun ScalableHeader(
    title: String,
    uiScale: Float,
    onOpenDrawer: () -> Unit,
    showMenu: Boolean = true,
    titleEndContent: @Composable RowScope.() -> Unit = {},
    endContent: @Composable RowScope.() -> Unit = {}
) {
    val onToggleTheme = LocalHeaderThemeToggle.current
    val isLight = ThemeRuntime.currentTheme.isLightCategory()
    Row(modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (showMenu) {
            IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, "Open navigation menu", tint = OnCardText, modifier = Modifier.size(24.dp * uiScale)) }
        }
        Text(text = title, color = OnCardText, fontSize = (maxOf(20f * uiScale, 16f)).sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = if (showMenu) 12.dp else 4.dp))
        titleEndContent()
        Spacer(Modifier.weight(1f))
        if (onToggleTheme != null) {
            IconButton(onClick = onToggleTheme) {
                Icon(
                    if (isLight) Icons.Default.DarkMode else Icons.Default.LightMode,
                    contentDescription = "Toggle theme",
                    tint = OnCardText.copy(alpha = 0.86f),
                    modifier = Modifier.size(22.dp * uiScale)
                )
            }
        }
        endContent()
    }
}

data class CoachStep(
    val title: String,
    val body: String,
    val panelAlignment: Alignment,
    val pointerAlignment: Alignment,
    val panelOffsetX: Dp = 0.dp,
    val panelOffsetY: Dp = 0.dp,
    val pointerPosition: Offset? = null
)

@Composable
fun CoachmarkOverlay(
    steps: List<CoachStep>,
    accent: Color,
    onDone: () -> Unit
) {
    if (steps.isEmpty()) {
        onDone()
        return
    }
    val config = LocalConfiguration.current
    val cardWidth = (config.screenWidthDp.dp * 0.62f).coerceIn(220.dp, 300.dp)
    val cardHeight = (config.screenHeightDp.dp * 0.20f).coerceIn(132.dp, 180.dp)
    var stepIndex by remember { mutableIntStateOf(0) }
    val step = steps[stepIndex]
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(0.dp, 24.dp)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardDarkBlue),
            border = BorderStroke(1.2.dp, accent.copy(alpha = 0.8f))
        ) {
            Column(modifier = Modifier.width(cardWidth).height(cardHeight).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(step.title, color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(step.body, color = OnCardText.copy(alpha = 0.88f), fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDone) { Text("Skip", color = OnCardText.copy(alpha = 0.8f)) }
                    Button(
                        onClick = {
                            if (stepIndex >= steps.lastIndex) onDone() else stepIndex++
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
                    ) {
                        Text(if (stepIndex >= steps.lastIndex) "Done" else "Next", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    containerColor: Color = CardDarkBlue,
    contentColor: Color = OnCardText,
    onClick: () -> Unit
) {
    val selectedContainer = mixForBackground(accent, containerColor).copy(
        alpha = if (containerColor.luminance() >= 0.5f) 0.62f else 0.78f
    )
    val unselectedColor = contentColor.copy(alpha = 0.85f)
    val selectedColor = if (containerColor.luminance() >= 0.5f) accent else accent.copy(alpha = 0.95f)
    val itemColor = if (selected) selectedColor else unselectedColor
    val itemShape = RoundedCornerShape(14.dp)
    val experimentalSuffix = " | Experimental"
    val hasExperimental = label.endsWith(experimentalSuffix)
    val mainLabel = if (hasExperimental) label.removeSuffix(experimentalSuffix) else label
    val experimentalColor = OnCardText.copy(alpha = 0.36f)
    NavigationDrawerItem(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(itemShape),
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(mainLabel, color = itemColor)
                if (hasExperimental) {
                    Text(experimentalSuffix, color = experimentalColor, fontWeight = FontWeight.SemiBold)
                }
            }
        }, selected = selected, onClick = onClick,
        icon = { Icon(icon, contentDescription = label, tint = itemColor) },
        shape = itemShape,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = selectedContainer,
            unselectedContainerColor = Color.Transparent,
            selectedTextColor = selectedColor,
            unselectedTextColor = unselectedColor,
            selectedIconColor = selectedColor,
            unselectedIconColor = unselectedColor
        )
    )
}

@Composable
fun RowScope.TikTokNavButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    baseColor: Color = OnCardText,
    surfaceColor: Color = CardDarkBlue,
    emphasis: Float = if (selected) 1f else 0f,
    onClick: () -> Unit
) {
    val normalizedEmphasis = emphasis.coerceIn(0f, 1f)
    val color = lerp(baseColor.copy(alpha = 0.5f), accent, normalizedEmphasis)
    val pillShape = RoundedCornerShape(14.dp)
    val selectedBg = if (selected) mixForBackground(accent, surfaceColor).copy(alpha = 0.20f) else Color.Transparent
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 2.dp)
            .clip(pillShape)
            .background(selectedBg)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(3.dp))
        Text(text = label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AvatarPickerDialog(accentStrong: Color, onPreset: (String) -> Unit, onPick: () -> Unit, onDismiss: () -> Unit) {
    val presets = listOf("🧙‍♂️", "🧝‍♂️", "🥷", "🧛‍♂️", "🧟‍♂️", "🧑‍🚀", "👑", "🐺")
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Choose avatar", fontWeight = FontWeight.Bold, color = OnCardText) },
        text = { @OptIn(ExperimentalLayoutApi::class) FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { presets.forEach { emoji -> Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(CardDarkBlue).clickable { SoundManager.playClick(); onPreset(emoji) }, contentAlignment = Alignment.Center) { Text(emoji, fontSize = 22.sp) } } } },
        confirmButton = { TextButton(onClick = { SoundManager.playClick(); onPick() }) { Text("Pick image", color = accentStrong) } }, dismissButton = { TextButton(onClick = { SoundManager.playClick(); onDismiss() }) { Text("Close", color = OnCardText) } }
    )
}

// === HOME SCREEN (Updated with Boss UI) ===
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier, appContext: Context, quests: List<Quest>, bosses: List<Boss>, avatar: Avatar, levelInfo: LevelInfo, attributes: PlayerAttributes,
    streak: Int, accentStrong: Color, accentSoft: Color, refreshCount: Int, gold: Int, alwaysShowQuestProgress: Boolean,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    onClaimQuest: (Int) -> Unit,
    onProgress: (Int, Int) -> Unit, // NEW PARAMETER
    onResetQuestProgress: (Int) -> Unit,
    playerName: String,
    onSavePlayerName: (String) -> Unit,
    onChangeAvatar: (Avatar) -> Unit, onNavigate: (Screen) -> Unit,
    onOpenDrawer: () -> Unit, onOpenFocus: () -> Unit
) {
    var showAvatarPicker by rememberSaveable { mutableStateOf(false) }
    var showNameEditor by rememberSaveable { mutableStateOf(false) }
    var questOptionsQuestId by rememberSaveable { mutableStateOf<Int?>(null) }
    var nameDraft by remember(playerName) { mutableStateOf(playerName) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked ->
        if (picked != null) {
            runCatching { appContext.contentResolver.takePersistableUriPermission(picked, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            onChangeAvatar(Avatar.Custom(picked))
        }
    }

    ScalableScreen(modifier) { uiScale ->
        val isLightTheme = ThemeRuntime.currentTheme.isLightCategory()
        val avatarInteraction = remember { MutableInteractionSource() }
        val avatarPressed by avatarInteraction.collectIsPressedAsState()
        val nameInteraction = remember { MutableInteractionSource() }
        val namePressed by nameInteraction.collectIsPressedAsState()
        val neonBordersEnabled = ThemeRuntime.neonFlowEnabled || ThemeRuntime.decorativeBordersEnabled
        val boostedNeon = ThemeRuntime.neonLightBoostEnabled
        val neonSecondary = neonPaletteColor(ThemeRuntime.neonGlowPalette, boostedNeon)
        val neonBorderActive = neonBordersEnabled
        val neonBorderBrush = if (neonBorderActive) rememberNeonBorderBrush(accentStrong, neonSecondary) else null
        val heroFrameBrush = Brush.verticalGradient(
            listOf(
                CardDarkBlue.copy(alpha = 0.95f),
                CardDarkBlue.copy(alpha = 0.78f)
            )
        )
        val heroBorderColor = when {
            !neonBordersEnabled -> Color.Transparent
            boostedNeon -> accentStrong.copy(alpha = if (isLightTheme) 0.58f else 0.82f)
            isLightTheme -> OnCardText.copy(alpha = 0.20f)
            else -> OnCardText.copy(alpha = 0.30f)
        }
        val heroBorderWidth = if (neonBordersEnabled) {
            if (neonBorderActive) {
                (if (boostedNeon) {
                    if (isLightTheme) 1.8.dp else 2.4.dp
                } else {
                    if (isLightTheme) 1.3.dp else 1.8.dp
                }) * uiScale
            } else {
                (1.3.dp * uiScale).coerceAtLeast(1.dp)
            }
        } else 0.dp
        val avatarBorderColor = when {
            neonBordersEnabled && boostedNeon -> accentStrong.copy(alpha = 0.72f)
            neonBordersEnabled -> OnCardText.copy(alpha = if (isLightTheme) 0.34f else 0.30f)
            else -> OnCardText.copy(alpha = if (isLightTheme) 0.28f else 0.22f)
        }
        val avatarPinBg = if (isLightTheme) Color(0xFFE9EEF4) else Color(0xFF17212C)
        val avatarPinBorder = if (neonBordersEnabled) {
            if (boostedNeon) accentStrong.copy(alpha = 0.62f) else OnCardText.copy(alpha = if (isLightTheme) 0.32f else 0.24f)
        } else {
            OnCardText.copy(alpha = if (isLightTheme) 0.22f else 0.18f)
        }
        val questsStripBrush = if (isLightTheme) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFF5F8FC)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    CardDarkBlue.copy(alpha = 0.95f),
                    CardDarkBlue.copy(alpha = 0.75f)
                )
            )
        }
        val questsStripBorderColor = when {
            !neonBordersEnabled -> Color.Transparent
            boostedNeon -> accentStrong.copy(alpha = if (isLightTheme) 0.54f else 0.78f)
            isLightTheme -> OnCardText.copy(alpha = 0.18f)
            else -> OnCardText.copy(alpha = 0.24f)
        }
        val questsStripBorderWidth = if (neonBordersEnabled) {
            if (neonBorderActive) {
                (if (boostedNeon) {
                    if (isLightTheme) 1.6.dp else 2.1.dp
                } else {
                    if (isLightTheme) 1.15.dp else 1.6.dp
                }) * uiScale
            } else {
                (1.2.dp * uiScale).coerceAtLeast(1.dp)
            }
        } else 0.dp
        val timerChipBg = accentStrong.copy(alpha = if (isLightTheme) 0.18f else 0.15f)
        val timerChipTint = accentStrong.copy(alpha = if (isLightTheme) 0.92f else 1f)
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy((6.dp * uiScale))) {
            // 1. Header
            ScalableHeader(title = stringResource(R.string.title_daily_quests), uiScale = uiScale, onOpenDrawer = onOpenDrawer, showMenu = true) {
                if (streak > 0) { Text("🔥 $streak", color = Color(0xFFFF9800), fontSize = (16.sp * uiScale), fontWeight = FontWeight.Bold); Spacer(Modifier.width(12.dp)) }
                IconButton(onClick = { onNavigate(Screen.SETTINGS) }) { Icon(Icons.Default.Settings, "Open settings", tint = OnCardText, modifier = Modifier.size(24.dp * uiScale)) }
            }

            // 2. Avatar & Stats Row
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                val lowerPanelHeight = (42.dp * uiScale).coerceAtLeast(38.dp)
                val heroFrameShape = RoundedCornerShape((16.dp * uiScale).coerceAtLeast(12.dp))
                val avatarCircle = (74.dp * uiScale).coerceAtLeast(58.dp)
                val avatarPin = (22.dp * uiScale).coerceAtLeast(18.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(heroFrameShape)
                        .background(brush = heroFrameBrush)
                        .then(
                            if (neonBorderActive && heroBorderWidth > 0.dp) {
                                Modifier.border(heroBorderWidth, neonBorderBrush!!, heroFrameShape)
                            } else {
                                Modifier.border(heroBorderWidth, heroBorderColor, heroFrameShape)
                            }
                        )
                        .padding((10.dp * uiScale).coerceAtLeast(8.dp))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy((12.dp * uiScale)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(avatarCircle + avatarPin * 0.35f)
                                .clickable(
                                    interactionSource = avatarInteraction,
                                    indication = null
                                ) { showAvatarPicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(avatarCircle)
                                    .clip(CircleShape)
                                    .background(AvatarBackground)
                                    .border(1.5.dp, if (avatarPressed) accentStrong.copy(alpha = 0.8f) else avatarBorderColor, CircleShape)
                                    .padding(2.dp)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                when (avatar) {
                                    is Avatar.Preset -> Text(avatar.emoji, fontSize = maxOf(36f * uiScale, 26f).sp)
                                    is Avatar.Custom -> AsyncImage(model = avatar.uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (2.dp * uiScale), y = (2.dp * uiScale))
                                    .size(avatarPin)
                                    .clip(CircleShape)
                                    .background(avatarPinBg)
                                    .border(1.dp, avatarPinBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = OnCardText.copy(alpha = 0.92f), modifier = Modifier.size((14.dp * uiScale).coerceAtLeast(12.dp)))
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy((6.dp * uiScale))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable(
                                            interactionSource = nameInteraction,
                                            indication = null
                                        ) { showNameEditor = true }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = playerName,
                                            color = if (namePressed) accentStrong.copy(alpha = 1f) else accentStrong,
                                            fontSize = (22.sp * uiScale),
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Icon(Icons.Default.Edit, null, tint = if (namePressed) accentStrong.copy(alpha = 1f) else accentStrong.copy(alpha = 0.9f), modifier = Modifier.size((16.dp * uiScale).coerceAtLeast(14.dp)))
                                    }
                                    Text(text = "Lv ${levelInfo.level}", color = OnCardText.copy(alpha = 0.9f), fontSize = (16.sp * uiScale), fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "$gold", color = accentStrong, fontSize = (24.sp * uiScale), fontWeight = FontWeight.ExtraBold)
                                    Text(text = "GOLD", color = OnCardText.copy(alpha = 0.65f), fontSize = (11.sp * uiScale), fontWeight = FontWeight.Bold)
                                }
                            }
                            XpBar(levelInfo = levelInfo, accentStrong = accentStrong, showValue = true, compact = true, showContainer = false)
                        }
                    }
                }

                Spacer(Modifier.height((8.dp * uiScale).coerceAtLeast(6.dp)))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(lowerPanelHeight)
                        .clip(RoundedCornerShape((12.dp * uiScale)))
                        .background(questsStripBrush)
                        .then(
                            if (neonBorderActive && questsStripBorderWidth > 0.dp) {
                                Modifier.border(questsStripBorderWidth, neonBorderBrush!!, RoundedCornerShape((12.dp * uiScale)))
                            } else {
                                Modifier.border(questsStripBorderWidth, questsStripBorderColor, RoundedCornerShape((12.dp * uiScale)))
                            }
                        )
                        .padding(horizontal = (12.dp * uiScale)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "DAILY QUESTS", color = OnCardText.copy(alpha = 0.9f), fontWeight = FontWeight.ExtraBold, fontSize = (12.sp * uiScale), letterSpacing = 1.sp)
                        Spacer(Modifier.width(6.dp))
                        Box(modifier = Modifier.size(24.dp * uiScale).clip(CircleShape).background(timerChipBg).clickable { onOpenFocus() }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Timer, null, tint = timerChipTint, modifier = Modifier.size(14.dp * uiScale))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val refreshEnabled = refreshCount < 3
                        val refreshInteraction = remember { MutableInteractionSource() }
                        val refreshPressed by refreshInteraction.collectIsPressedAsState()
                        val refreshContent = when {
                            !refreshEnabled -> OnCardText.copy(alpha = 0.55f)
                            refreshPressed -> accentStrong.copy(alpha = 0.95f)
                            else -> OnCardText.copy(alpha = 0.95f)
                        }
                        Row(
                            modifier = Modifier
                                .clickable(
                                    enabled = refreshEnabled,
                                    interactionSource = refreshInteraction,
                                    indication = null
                                ) { onRefresh() }
                                .padding(horizontal = (6.dp * uiScale).coerceAtLeast(4.dp), vertical = (4.dp * uiScale).coerceAtLeast(3.dp)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Refresh $refreshCount/3",
                                color = refreshContent,
                                fontSize = (11.sp * uiScale),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Refresh,
                                null,
                                tint = refreshContent,
                                modifier = Modifier.size((13.dp * uiScale).coerceAtLeast(11.dp))
                            )
                        }
                    }
                }
            }

            // 3. BOSS SECTION REMOVED HERE

// 5. Quest List
            val questListState = rememberLazyListState()
            key(refreshCount, quests) {
                LazyColumn(
                    state = questListState,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy((8.dp * uiScale)),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    if (quests.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().height(220.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No quests.", color = OnCardText.copy(alpha = 0.5f))
                                Spacer(Modifier.height(10.dp))
                                TextButton(onClick = { onNavigate(Screen.QUESTS) }) {
                                    Text("Open Quests & Templates", color = accentStrong, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        items(quests) { q ->
                            // FIX: Use QuestCard here, not MainQuestItem
                            QuestCard(
                                quest = q,
                                accentStrong = accentStrong,
                                accentSoft = accentSoft,
                                modifier = Modifier.pointerInput(q.id) {
                                    detectTapGestures(
                                        onLongPress = {
                                            SoundManager.playClick()
                                            questOptionsQuestId = q.id
                                        },
                                        onTap = {
                                            if (!q.completed) {
                                                val target = q.target
                                                val current = q.currentProgress
                                                when {
                                                    current > target -> onClaimQuest(q.id)
                                                    current == target && target > 0 -> onProgress(q.id, target + 1)
                                                    else -> onProgress(q.id, (current + 1).coerceAtMost(target))
                                                }
                                            }
                                        }
                                    )
                                },
                                uiScale = uiScale,
                                alwaysShowProgress = alwaysShowQuestProgress,
                                onClaimQuest = { onClaimQuest(q.id) },
                                onProgress = { p -> onProgress(q.id, p) }
                            )
                        }
                    }
                }
            }
        }
        }
    }
    if (showAvatarPicker) { AvatarPickerDialog(accentStrong = accentStrong, onPreset = { onChangeAvatar(Avatar.Preset(it)); showAvatarPicker = false }, onPick = { imagePicker.launch(arrayOf("image/*")); showAvatarPicker = false }, onDismiss = { showAvatarPicker = false }) }
    val selectedQuest = quests.firstOrNull { it.id == questOptionsQuestId }
    if (selectedQuest != null) {
        AlertDialog(
            onDismissRequest = { questOptionsQuestId = null },
            containerColor = CardDarkBlue,
            title = { Text("Quest Options", color = OnCardText, fontWeight = FontWeight.Bold) },
            text = { Text(selectedQuest.title, color = OnCardText.copy(alpha = 0.8f)) },
            confirmButton = {
                TextButton(onClick = {
                    onResetQuestProgress(selectedQuest.id)
                    questOptionsQuestId = null
                }) {
                    Text("Reset Progress", color = accentStrong, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { questOptionsQuestId = null }) {
                    Text("Cancel", color = OnCardText)
                }
            }
        )
    }
    if (showNameEditor) {
        AlertDialog(
            onDismissRequest = { showNameEditor = false },
            containerColor = CardDarkBlue,
            title = { Text("Set Player Name", color = accentStrong, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    singleLine = true,
                    label = { Text("Player name", color = OnCardText.copy(alpha = 0.6f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OnCardText,
                        unfocusedTextColor = OnCardText,
                        cursorColor = accentStrong
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSavePlayerName(nameDraft.ifBlank { playerName })
                    showNameEditor = false
                }) { Text("Save", color = accentStrong) }
            },
            dismissButton = { TextButton(onClick = { showNameEditor = false }) { Text("Cancel", color = OnCardText) } }
        )
    }
}

@Composable
fun QuickActionButton(icon: ImageVector, label: String, uiScale: Float, color: Color, onClick: () -> Unit) {
    val sizeMul = if (ThemeRuntime.largerTouchTargetsEnabled) 1.15f else 1f
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { SoundManager.playClick(); onClick() }) {
        Box(modifier = Modifier.size((32.dp * uiScale) * sizeMul).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size((18.dp * uiScale) * sizeMul))
        }
        Text(label, fontSize = (9.sp * uiScale), color = OnCardText.copy(alpha = 0.7f), lineHeight = 10.sp, modifier = Modifier.padding(top = 2.dp))
    }
}
@Composable
fun AttributeQuickChip(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(label, color = color, fontWeight = FontWeight.Black, fontSize = 10.sp)
        Spacer(Modifier.width(4.dp))
        Text(value.toString(), color = OnCardText, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

// === INVENTORY SCREEN (Updated with Grid & Pixel Hero) ===
@Composable
fun InventoryScreen(
    modifier: Modifier,
    inventory: List<InventoryItem>,
    shopItems: List<ShopItem>,
    customMode: Boolean,
    gold: Int,
    accentStrong: Color,
    accentSoft: Color,
    showTutorial: Boolean,
    onTutorialDismiss: () -> Unit,
    showHoldHint: Boolean,
    onHoldHintShown: () -> Unit,
    onBuyShopItem: (ShopItem) -> Unit,
    onUpsertShopItem: (ShopItem) -> Unit,
    onDeleteShopItem: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var editingShopItem by remember { mutableStateOf<ShopItem?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var pendingDeleteShopItem by remember { mutableStateOf<ShopItem?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val ownedCounts = remember(inventory) { inventory.associate { it.id to it.ownedCount } }

    ScalableScreen(modifier) { uiScale ->
        Column(verticalArrangement = Arrangement.spacedBy((6.dp * uiScale))) {
            ScalableHeader(stringResource(R.string.title_shop), uiScale, onOpenDrawer) {
                Text("$gold G", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, "Open settings", tint = OnCardText, modifier = Modifier.size(22.dp * uiScale))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CardBlock {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFD700).copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MonetizationOn, null, tint = Color(0xFFFFD700), modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("YOUR GOLD", color = OnCardText.copy(alpha = 0.55f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text("$gold G", color = Color(0xFFFFD700), fontSize = 20.sp, fontWeight = FontWeight.Black)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${shopItems.size}", color = accentStrong, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            Text("ITEMS", color = OnCardText.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Shop Catalog", color = accentStrong, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    IconButton(
                        onClick = {
                            if (customMode) {
                                showCreateDialog = true
                            } else {
                                Toast.makeText(context, "Enable Custom Mode in Settings to create or edit shop items.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.AddCircle,
                            null,
                            tint = if (customMode) accentStrong else OnCardText.copy(alpha = 0.35f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                if (!customMode) {
                    Text(
                        "Enable Custom Mode in Settings to add/edit shop items.",
                        color = OnCardText.copy(alpha = 0.65f),
                        fontSize = 12.sp
                    )
                }

                if (shopItems.isEmpty()) {
                    CardBlock {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                            Text(
                                if (customMode) "No shop items yet.\nTap + to add your first item." else "No shop items available right now.",
                                color = OnCardText.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    if (showHoldHint && customMode) {
                        Text(
                            "Tip: hold item icon to edit or delete.",
                            color = OnCardText.copy(alpha = 0.68f),
                            fontSize = 11.sp
                        )
                    }
                    ShopItemsGrid(
                        items = shopItems,
                        ownedCounts = ownedCounts,
                        gold = gold,
                        accentStrong = accentStrong,
                        accentSoft = accentSoft,
                        customMode = customMode,
                        onHoldHintShown = onHoldHintShown,
                        onBuy = onBuyShopItem,
                        onEdit = { if (customMode) editingShopItem = it },
                        onDelete = { if (customMode) pendingDeleteShopItem = it }
                    )
                }
            }
        }
    }

    if (showCreateDialog && customMode) {
        ShopItemEditorDialog(
            accentStrong = accentStrong,
            initial = null,
            onSave = {
                onUpsertShopItem(it)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
    if (editingShopItem != null && customMode) {
        ShopItemEditorDialog(
            accentStrong = accentStrong,
            initial = editingShopItem,
            onSave = {
                onUpsertShopItem(it)
                editingShopItem = null
            },
            onDismiss = { editingShopItem = null }
        )
    }
    if (pendingDeleteShopItem != null && customMode) {
        AlertDialog(
            onDismissRequest = { pendingDeleteShopItem = null },
            containerColor = CardDarkBlue,
            title = { Text("Delete Shop Item?", color = OnCardText, fontWeight = FontWeight.Bold) },
            text = { Text("Remove '${pendingDeleteShopItem!!.name}' from shop catalog?", color = OnCardText.copy(alpha = 0.8f)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteShopItem(pendingDeleteShopItem!!.id)
                    pendingDeleteShopItem = null
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteShopItem = null }) { Text("Cancel", color = OnCardText) } }
        )
    }
    if (showTutorial) {
        CoachmarkOverlay(
            steps = listOf(
                CoachStep(
                    title = "Shop Catalog",
                    body = "Tap an item icon to buy and auto-use it instantly.",
                    panelAlignment = Alignment.TopStart,
                    pointerAlignment = Alignment.Center,
                    panelOffsetY = 38.dp,
                    pointerPosition = Offset(0.24f, 0.44f)
                ),
                CoachStep(
                    title = "Edit Items",
                    body = "In Custom Mode, hold an item icon to open edit/delete.",
                    panelAlignment = Alignment.BottomStart,
                    pointerAlignment = Alignment.Center,
                    pointerPosition = Offset(0.42f, 0.44f)
                )
            ),
            accent = accentStrong,
            onDone = onTutorialDismiss
        )
    }
}

@Composable
fun ShopItemsGrid(
    items: List<ShopItem>,
    ownedCounts: Map<String, Int>,
    gold: Int,
    accentStrong: Color,
    accentSoft: Color,
    customMode: Boolean,
    onHoldHintShown: () -> Unit,
    onBuy: (ShopItem) -> Unit,
    onEdit: (ShopItem) -> Unit,
    onDelete: (ShopItem) -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val columns = remember(screenWidth) {
        when {
            screenWidth < 360.dp -> 4
            screenWidth < 760.dp -> 6
            else -> 6
        }
    }
    val totalSlots = remember(items.size, columns) { maxOf(items.size, columns * 6) }
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Fixed(columns),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(bottom = 84.dp)
    ) {
        items(totalSlots, key = { idx -> items.getOrNull(idx)?.id ?: "empty_slot_$idx" }) { idx ->
            val item = items.getOrNull(idx)
            if (item == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.10f))
                        .border(1.dp, OnCardText.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                )
            } else {
                ShopGridItemCard(
                    item = item,
                    ownedCount = ownedCounts[item.id] ?: 0,
                    canAfford = gold >= item.cost,
                    hasStock = item.stock > 0,
                    customMode = customMode,
                    accentStrong = accentStrong,
                    accentSoft = accentSoft,
                    onHoldHintShown = onHoldHintShown,
                    onBuy = { onBuy(item) },
                    onEdit = { onEdit(item) },
                    onDelete = { onDelete(item) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShopGridItemCard(
    item: ShopItem,
    @Suppress("UNUSED_PARAMETER") ownedCount: Int,
    canAfford: Boolean,
    hasStock: Boolean,
    customMode: Boolean,
    accentStrong: Color,
    accentSoft: Color,
    onHoldHintShown: () -> Unit,
    onBuy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val neonEnabled = ThemeRuntime.neonFlowEnabled || ThemeRuntime.decorativeBordersEnabled
    val boostedNeon = ThemeRuntime.neonLightBoostEnabled
    val neonSecondary = neonPaletteColor(ThemeRuntime.neonGlowPalette, boostedNeon)
    val neonBrush = if (neonEnabled) rememberNeonBorderBrush(accentStrong, neonSecondary) else null
    val shopCardShape = RoundedCornerShape(14.dp)
    var showQuickActions by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val iconInteraction = remember { MutableInteractionSource() }
    val iconPressed by iconInteraction.collectIsPressedAsState()
    val holdPulse by rememberInfiniteTransition(label = "shop_hold_pulse").animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(animation = tween(650), repeatMode = RepeatMode.Reverse),
        label = "shop_hold_pulse_value"
    )
    val iconScale = if (iconPressed) holdPulse else 1f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shopCardShape)
                .background(CardDarkBlue)
                .then(if (neonEnabled && neonBrush != null) Modifier.border(if (boostedNeon) 2.dp else 1.5.dp, neonBrush, shopCardShape) else Modifier.border(1.dp, OnCardText.copy(alpha = 0.2f), shopCardShape))
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
                .combinedClickable(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onBuy()
                    },
                    onLongClick = {
                        if (customMode) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onHoldHintShown()
                            showQuickActions = true
                        }
                    },
                    interactionSource = iconInteraction,
                    indication = null
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!item.imageUri.isNullOrBlank()) {
                AsyncImage(
                    model = item.imageUri,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(item.icon, fontSize = 24.sp, lineHeight = 24.sp)
            }
            DropdownMenu(
                expanded = showQuickActions,
                onDismissRequest = { showQuickActions = false },
                modifier = Modifier.background(CardDarkBlue)
            ) {
                DropdownMenuItem(
                    text = { Text("Edit", color = OnCardText) },
                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = accentStrong) },
                    onClick = {
                        showQuickActions = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = OnCardText) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        showQuickActions = false
                        onDelete()
                    }
                )
            }
        }
        Text(
            text = item.name,
            color = accentStrong,
            fontWeight = FontWeight.Black,
            fontSize = 9.5.sp,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 2.dp, y = (-8).dp)
                .fillMaxWidth(0.58f)
                .rotate(-14f),
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-2).dp, y = (-5).dp)
                .rotate(8f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Icon(Icons.Default.MonetizationOn, null, tint = Color(0xFFFFD54F), modifier = Modifier.size(9.dp))
            Text("${item.cost}", color = Color(0xFFFFD54F), fontSize = 8.sp, fontWeight = FontWeight.Black)
        }
    }
}

// === MAIN QUESTS SCREEN (Player View Only) ===
// === MAIN QUESTS SCREEN (Player View Only) ===
@Composable
fun MainQuestsScreen(
    modifier: Modifier,
    quests: List<CustomMainQuest>,
    accentStrong: Color,
    accentSoft: Color,
    onUpdate: (CustomMainQuest) -> Unit,
    onResetProgress: (String) -> Unit,
    onDelete: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCustomMainQuests: () -> Unit
) {
    var questForWizard by remember { mutableStateOf<CustomMainQuest?>(null) }
    data class MainQuestFamily(val key: String, val header: CustomMainQuest, val chain: List<CustomMainQuest>)
    val (familyGroups, displayPrereqById, chainDiagnostics) = remember(quests) {
        data class ParsedMainQuest(val quest: CustomMainQuest, val index: Int, val familyKey: String, val familyTier: Int?)
        fun parseFamily(title: String): Pair<String, Int?> {
            val m = Regex("""^(.*?)(?:\s+(\d+))$""").find(title.trim())
            val base = (m?.groupValues?.getOrNull(1)?.trim().takeUnless { it.isNullOrBlank() } ?: title.trim())
            val tier = m?.groupValues?.getOrNull(2)?.toIntOrNull()
            return base.lowercase() to tier
        }
        val parsed = quests.mapIndexed { idx, q ->
            val (familyKey, familyTier) = parseFamily(q.title)
            ParsedMainQuest(q, idx, familyKey, familyTier)
        }
        val indexById = parsed.associate { it.quest.id to it.index }
        val groups = parsed.groupBy { it.familyKey }.entries.sortedBy { (_, g) -> g.minOf { it.index } }
        val families = mutableListOf<MainQuestFamily>()
        val prereqMap = mutableMapOf<String, String?>()
        var brokenPrereqCount = 0
        var crossFamilyPrereqCount = 0
        groups.forEach { (familyKey, group) ->
            val tiered = group.filter { it.familyTier != null }
            val sorted = if (tiered.size >= 2) {
                group.sortedWith(compareBy<ParsedMainQuest> { it.familyTier ?: Int.MAX_VALUE }.thenBy { it.index })
            } else {
                group.sortedBy { it.index }
            }
            sorted.forEachIndexed { i, p ->
                val explicitPrereq = p.quest.prerequisiteId
                val explicitPrereqInFamily = explicitPrereq != null && sorted.any { it.quest.id == explicitPrereq }
                if (explicitPrereq != null && !indexById.containsKey(explicitPrereq)) brokenPrereqCount++
                if (explicitPrereq != null && indexById.containsKey(explicitPrereq) && !explicitPrereqInFamily && tiered.size >= 2) crossFamilyPrereqCount++
                prereqMap[p.quest.id] = if (tiered.size >= 2) {
                    if (i == 0) null
                    else if (explicitPrereqInFamily) explicitPrereq
                    else sorted[i - 1].quest.id
                } else {
                    explicitPrereq
                }
            }
            val orderedFamily = sorted.map { it.quest }
            val header = orderedFamily.firstOrNull { !it.isClaimed } ?: orderedFamily.first()
            val chain = orderedFamily
                .filterNot { it.id == header.id }
                .sortedWith(compareBy<CustomMainQuest> { it.isClaimed }.thenBy { indexById[it.id] ?: Int.MAX_VALUE })
            families += MainQuestFamily(key = familyKey, header = header, chain = chain)
        }
        Triple(families, prereqMap, brokenPrereqCount to crossFamilyPrereqCount)
    }

    ScalableScreen(modifier) { uiScale ->
        Column(verticalArrangement = Arrangement.spacedBy((6.dp * uiScale))) {
            ScalableHeader(title = stringResource(R.string.title_main_quests), uiScale = uiScale, onOpenDrawer = onOpenDrawer, showMenu = true) {
                IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, null, tint = OnCardText, modifier = Modifier.size(24.dp * uiScale)) }
            }

            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                if (quests.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("No active main quests.\nGo to Quests & Templates to add one.", color = OnCardText.copy(alpha = 0.5f), textAlign = TextAlign.Center)
                            TextButton(onClick = onOpenCustomMainQuests) {
                                Text("Open Custom Main Quests", color = accentStrong, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    if (chainDiagnostics.first > 0 || chainDiagnostics.second > 0) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black.copy(alpha = 0.20f))
                                    .border(1.dp, OnCardText.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    "Chain auto-fix active: repaired ${chainDiagnostics.first} broken links and ignored ${chainDiagnostics.second} cross-family locks.",
                                    color = OnCardText.copy(alpha = 0.78f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    items(familyGroups, key = { it.key }) { family ->
                        val q = family.header
                        val prerequisite = quests.find { it.id == displayPrereqById[q.id] }
                        val isLocked = prerequisite != null && !prerequisite.isClaimed
                        MainQuestItem(
                            quest = q,
                            isLocked = isLocked,
                            lockedByTitle = prerequisite?.title,
                            accentStrong = accentStrong,
                            accentSoft = accentSoft,
                            onOpenWizard = { questForWizard = q },
                            onUpdate = onUpdate,
                            onResetProgress = { onResetProgress(q.id) },
                            onDelete = onDelete
                        )
                        if (family.chain.isNotEmpty()) {
                            var expanded by rememberSaveable(family.key) { mutableStateOf(false) }
                            val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "main_family_chain")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CardDarkBlue.copy(alpha = 0.45f))
                                    .clickable { expanded = !expanded }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (expanded) "Hide chain (${family.chain.size})" else "Show chain (${family.chain.size})",
                                    color = OnCardText.copy(alpha = 0.78f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.KeyboardArrowDown, null, tint = OnCardText.copy(alpha = 0.72f), modifier = Modifier.rotate(rotation))
                            }
                            if (expanded) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    family.chain.forEach { child ->
                                        val childPrereq = quests.find { it.id == displayPrereqById[child.id] }
                                        val childLocked = childPrereq != null && !childPrereq.isClaimed
                                        MainQuestItem(
                                            quest = child,
                                            isLocked = childLocked,
                                            lockedByTitle = childPrereq?.title,
                                            accentStrong = accentStrong,
                                            accentSoft = accentSoft,
                                            onOpenWizard = { questForWizard = child },
                                            onUpdate = onUpdate,
                                            onResetProgress = { onResetProgress(child.id) },
                                            onDelete = onDelete
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Wizard only appears for the Final Claim
    if (questForWizard != null) {
        NpcQuestDialog(
            quest = questForWizard!!,
            accentStrong = accentStrong,
            onUpdate = { updated ->
                onUpdate(updated)
                questForWizard = updated
            },
            onDismiss = { questForWizard = null }
        )
    }
}
// === JOURNAL ===
@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun JournalScreen(
    modifier: Modifier = Modifier,
    pages: List<JournalPage>,
    accentSoft: Color,
    journalPageColorOverride: Color?,
    journalAccentColorOverride: Color?,
    journalName: String,
    onJournalNameChanged: (String) -> Unit,
    onUpdatePages: (List<JournalPage>) -> Unit,
    onBack: () -> Unit,
    pageIndexExternal: Int,
    onPageIndexExternalChange: (Int) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onBookOpenStateChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val pageIndex = rememberSaveable { mutableIntStateOf(pageIndexExternal) }
    val isSaving = remember { mutableStateOf(false) }
    var showCover by rememberSaveable { mutableStateOf(true) }
    var recordingPage by remember { mutableStateOf<Int?>(null) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var pendingRecordPage by remember { mutableStateOf<Int?>(null) }
    var recordingFilePath by remember { mutableStateOf<String?>(null) }
    var showVoiceNameEditor by remember { mutableStateOf(false) }
    var voiceNameTargetPage by remember { mutableStateOf<Int?>(null) }
    var voiceNameTargetUri by remember { mutableStateOf<String?>(null) }
    var voiceNameDraft by remember { mutableStateOf("") }
    var voiceToDeletePage by remember { mutableStateOf<Int?>(null) }
    var voiceToDeleteUri by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingUri by remember { mutableStateOf<String?>(null) }
    var showJournalNameEditor by rememberSaveable { mutableStateOf(false) }
    var journalNameDraft by rememberSaveable { mutableStateOf(journalName) }
    val pagerState = rememberPagerState(initialPage = pageIndex.intValue, pageCount = { 365 })
    LaunchedEffect(Unit) { showCover = true }
    LaunchedEffect(showCover) { onBookOpenStateChanged(!showCover) }
    LaunchedEffect(pageIndexExternal) { if (pageIndexExternal != pageIndex.intValue) pageIndex.intValue = pageIndexExternal }
    LaunchedEffect(pageIndex.intValue) {
        onPageIndexExternalChange(pageIndex.intValue)
        if (pagerState.currentPage != pageIndex.intValue) {
            pagerState.animateScrollToPage(pageIndex.intValue)
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        if (pageIndex.intValue != pagerState.currentPage) {
            pageIndex.intValue = pagerState.currentPage
        }
    }
    LaunchedEffect(journalName) {
        if (!showJournalNameEditor) {
            journalNameDraft = journalName
        }
    }
    val safePages = remember(pages) { pages.ifEmpty { listOf(JournalPage(epochDayNow(), title = "", text = "")) } }
    val lightTheme = ThemeRuntime.currentTheme.isLightCategory()
    val defaultPageBg = if (lightTheme) Color.White else CardDarkBlue
    val pageBg = journalPageColorOverride ?: defaultPageBg
    val pageIsLight = pageBg.luminance() >= 0.52f
    val journalAccent = journalAccentColorOverride ?: accentSoft
    val pageBorderColor = mixForBackground(journalAccent, pageBg).copy(alpha = if (pageIsLight) 0.58f else 0.42f)
    val journalNeonBrush = rememberNeonBorderBrush(journalAccent, neonPaletteColor(ThemeRuntime.neonGlowPalette, ThemeRuntime.neonLightBoostEnabled))
    val journalBorderWidth = if (ThemeRuntime.neonLightBoostEnabled) (2.4.dp) else (2.dp)
    val fieldTint = journalAccent
    val fieldText = OnCardText
    val fieldPlaceholder = OnCardText.copy(alpha = if (pageIsLight) 0.6f else 0.48f)
    val ribbonBrush = Brush.verticalGradient(
        listOf(
            fieldTint.copy(alpha = 0.96f),
            mixForBackground(fieldTint, pageBg).copy(alpha = 0.94f),
            mixForBackground(fieldTint, CardDarkBlue).copy(alpha = 0.90f)
        )
    )
    val ribbonIconColor = readableTextColor(fieldTint)
    fun setPage(i: Int, page: JournalPage) { isSaving.value = true; val list = safePages.toMutableList(); while (list.size <= i) list.add(JournalPage(epochDayNow(), title = "", text = "")); list[i] = page; onUpdatePages(list.take(365)) }
    fun pageVoiceUris(page: JournalPage): List<String> {
        return if (page.voiceNoteUris.isNotEmpty()) page.voiceNoteUris else listOfNotNull(page.voiceNoteUri)
    }
    fun pageVoiceNames(page: JournalPage): Map<String, String> {
        val notes = pageVoiceUris(page)
        return page.voiceNoteNames
            .mapNotNull { (k, v) ->
                val key = k.trim()
                val value = v.trim()
                if (key.isBlank() || value.isBlank()) null else key to value
            }
            .toMap()
            .filterKeys { notes.contains(it) }
    }
    fun parseSubmittedAtFromUri(uri: String): Long? {
        val fileName = runCatching { uri.toUri().lastPathSegment }.getOrNull().orEmpty()
        val match = Regex(""".*_(\d{11,})\.m4a$""").find(fileName) ?: return null
        return match.groupValues.getOrNull(1)?.toLongOrNull()
    }
    fun pageVoiceSubmittedAt(page: JournalPage): Map<String, Long> {
        val notes = pageVoiceUris(page)
        val clean = page.voiceNoteSubmittedAt.filterKeys { notes.contains(it) && (page.voiceNoteSubmittedAt[it] ?: 0L) > 0L }
        if (clean.size == notes.size) return clean
        val enriched = clean.toMutableMap()
        notes.forEach { uri ->
            if ((enriched[uri] ?: 0L) <= 0L) {
                parseSubmittedAtFromUri(uri)?.let { enriched[uri] = it }
            }
        }
        return enriched
    }
    val shortDateTimeFormatter = remember {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val target = pendingRecordPage
        pendingRecordPage = null
        if (!granted || target == null) return@rememberLauncherForActivityResult
        runCatching {
            val audioDir = File(context.filesDir, "journal_voice").apply { mkdirs() }
            val voiceFile = File(audioDir, "page_${target}_${System.currentTimeMillis()}.m4a")
            @Suppress("DEPRECATION")
            val nextRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
            nextRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            nextRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            nextRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            nextRecorder.setAudioEncodingBitRate(128000)
            nextRecorder.setAudioSamplingRate(44100)
            nextRecorder.setOutputFile(voiceFile.absolutePath)
            nextRecorder.prepare()
            nextRecorder.start()
            recorder = nextRecorder
            recordingPage = target
            recordingFilePath = voiceFile.absolutePath
        }
    }
    fun stopRecording(page: Int) {
        runCatching {
            recorder?.stop()
            recorder?.release()
        }
        val filePath = recordingFilePath
        recorder = null
        recordingPage = null
        recordingFilePath = null
        if (!filePath.isNullOrBlank()) {
            val existing = safePages.getOrElse(page) { JournalPage(epochDayNow(), title = "", text = "") }
            val newUri = filePath.toUri().toString()
            val nextUris = (pageVoiceUris(existing) + newUri).distinct()
            val recordedAtMillis = System.currentTimeMillis()
            val nextSubmittedAt = pageVoiceSubmittedAt(existing).toMutableMap().apply {
                this[newUri] = recordedAtMillis
            }
            setPage(
                page,
                existing.copy(
                    voiceNoteUri = null,
                    voiceNoteUris = nextUris,
                    voiceNoteSubmittedAt = nextSubmittedAt,
                    editedAtMillis = recordedAtMillis
                )
            )
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                recorder?.stop()
                recorder?.release()
            }
            runCatching {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            }
        }
    }
    LaunchedEffect(isSaving.value) { if (isSaving.value) { delay(1000); isSaving.value = false } }
    val canPrev = !showCover
    val canNext = !showCover && pageIndex.intValue < 364
    val coverProgress by animateFloatAsState(
        targetValue = if (showCover) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (ThemeRuntime.reduceAnimationsEnabled) 120 else 460,
            easing = FastOutSlowInEasing
        ),
        label = "journal_cover_progress"
    )

    ScalableScreen(modifier) { uiScale ->
        val firstPageSwipeCloseThresholdPx = with(LocalDensity.current) { 64.dp.toPx() }
        val firstPageSwipeTouchSlopPx = with(LocalDensity.current) { 18.dp.toPx() }
        val pagerRevealShiftPx = with(LocalDensity.current) { 58.dp.toPx() }
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val coverCameraDistancePx = with(LocalDensity.current) { 96.dp.toPx() }
        var coverCardSize by remember { mutableStateOf(IntSize.Zero) }
        val coverToPagesProgress = (1f - coverProgress).coerceIn(0f, 1f)
        val showingCoverLabel = coverProgress > 0.5f
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy((6.dp * uiScale))) {
            ScalableHeader(
                title = journalName,
                uiScale = uiScale,
                onOpenDrawer = onOpenDrawer,
                showMenu = true
            ) {
                Text(text = if (showingCoverLabel) "Cover" else "${pageIndex.intValue + 1}/365", color = OnCardText.copy(alpha = 0.8f), fontSize = (12.sp * uiScale), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                IconButton(onClick = { showJournalNameEditor = true }) { Icon(Icons.Default.Edit, null, tint = OnCardText, modifier = Modifier.size(20.dp * uiScale)) }
                IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, null, tint = OnCardText, modifier = Modifier.size(24.dp * uiScale)) }
            }
            Box(Modifier.weight(1f)) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = coverToPagesProgress
                            translationX = pagerRevealShiftPx * coverProgress * (if (isRtl) -1f else 1f)
                            scaleX = 0.97f + (0.03f * coverToPagesProgress)
                            scaleY = 0.985f + (0.015f * coverToPagesProgress)
                        }
                        .padding(start = 16.dp, end = 16.dp, bottom = (10.dp * uiScale).coerceAtLeast(8.dp))
                ) {
                    if (isSaving.value) { Text("Saving...", color = fieldTint, fontSize = 10.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp)) }
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .weight(1f)
                                    .pointerInput(showCover, pagerState.currentPage) {
                                        if (showCover) return@pointerInput
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            val startedOnFirstPage = pagerState.currentPage == 0
                                            var totalDx = 0f
                                            var totalDy = 0f
                                            var horizontalLocked = false
                                            while (true) {
                                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                                if (!change.pressed) break
                                                val delta = change.positionChange()
                                                totalDx += delta.x
                                                totalDy += delta.y
                                                if (!horizontalLocked) {
                                                    val absDx = kotlin.math.abs(totalDx)
                                                    val absDy = kotlin.math.abs(totalDy)
                                                    if (absDx >= firstPageSwipeTouchSlopPx && absDx > absDy * 1.2f) {
                                                        horizontalLocked = true
                                                    } else if (absDy >= firstPageSwipeTouchSlopPx && absDy > absDx) {
                                                        break
                                                    }
                                                }
                                                if (!horizontalLocked) continue
                                                val closeGesture = if (isRtl) totalDx <= -firstPageSwipeCloseThresholdPx else totalDx >= firstPageSwipeCloseThresholdPx
                                                val pageNavBreak = if (isRtl) totalDx > firstPageSwipeTouchSlopPx else totalDx < -firstPageSwipeTouchSlopPx
                                                if (startedOnFirstPage && closeGesture) {
                                                    showCover = true
                                                    break
                                                }
                                                if (pageNavBreak) {
                                                    // Forward-direction swipe should stay as page navigation.
                                                    break
                                                }
                                            }
                                        }
                                    },
                                userScrollEnabled = !showCover,
                                beyondViewportPageCount = 1,
                                flingBehavior = PagerDefaults.flingBehavior(
                                    state = pagerState,
                                    pagerSnapDistance = PagerSnapDistance.atMost(1),
                                    snapAnimationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    snapPositionalThreshold = 0.40f
                                )
                            ) { index ->
                                val page = safePages.getOrElse(index) { JournalPage(epochDayNow(), title = "", text = "") }
                                val voiceUris = pageVoiceUris(page)
                                val voiceNames = pageVoiceNames(page)
                                val voiceSubmittedAt = pageVoiceSubmittedAt(page)
                                Card(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(journalBorderWidth, journalNeonBrush, RoundedCornerShape(18.dp * uiScale))
                                        .border((1.dp * uiScale).coerceAtLeast(1.dp), pageBorderColor.copy(alpha = 0.45f), RoundedCornerShape(18.dp * uiScale)),
                                    colors = CardDefaults.cardColors(containerColor = pageBg),
                                    shape = RoundedCornerShape(18.dp * uiScale)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Column(modifier = Modifier.fillMaxSize().padding(16.dp * uiScale)) {
                                            OutlinedTextField(
                                                value = page.title,
                                                onValueChange = { setPage(index, page.copy(title = it.take(32), editedAtMillis = System.currentTimeMillis())) },
                                                singleLine = true,
                                                placeholder = { Text("Journal Title", color = fieldPlaceholder, fontSize = (18.sp * uiScale)) },
                                                textStyle = LocalTextStyle.current.copy(color = fieldText, fontSize = (18.sp * uiScale), fontWeight = FontWeight.Bold),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = fieldTint.copy(alpha = 0.35f),
                                                    unfocusedBorderColor = fieldTint.copy(alpha = 0.18f),
                                                    cursorColor = fieldTint
                                                ),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            Spacer(Modifier.height(12.dp * uiScale))
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                                Button(
                                                    onClick = {
                                                        if (recordingPage == index) {
                                                            stopRecording(index)
                                                        } else {
                                                            pendingRecordPage = index
                                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = if (recordingPage == index) MaterialTheme.colorScheme.error else fieldTint)
                                                ) {
                                                    val recordButtonColor = if (recordingPage == index) MaterialTheme.colorScheme.error else fieldTint
                                                    val recordContent = readableTextColor(recordButtonColor)
                                                    Icon(if (recordingPage == index) Icons.Default.Stop else Icons.Default.Mic, null, tint = recordContent)
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(if (recordingPage == index) "Stop Voice" else "Record Voice", color = recordContent, fontWeight = FontWeight.Bold)
                                                }
                                                if (voiceUris.isNotEmpty()) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            val latest = voiceUris.last()
                                                            if (playingUri == latest) {
                                                                runCatching { mediaPlayer?.stop(); mediaPlayer?.release() }
                                                                mediaPlayer = null
                                                                playingUri = null
                                                            } else {
                                                                runCatching { mediaPlayer?.stop(); mediaPlayer?.release() }
                                                                mediaPlayer = MediaPlayer().apply {
                                                                    setDataSource(context, latest.toUri())
                                                                    prepare()
                                                                    start()
                                                                    setOnCompletionListener {
                                                                        runCatching { it.release() }
                                                                        mediaPlayer = null
                                                                        playingUri = null
                                                                    }
                                                                }
                                                                playingUri = latest
                                                            }
                                                        }
                                                    ) { Text(if (playingUri == voiceUris.last()) "Stop Latest" else "Play Latest") }
                                                }
                                            }
                                            if (voiceUris.isNotEmpty()) {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    voiceUris.forEachIndexed { voiceIndex, uri ->
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SubtlePanel).padding(horizontal = 8.dp, vertical = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            val submittedLabel = voiceSubmittedAt[uri]?.let { runCatching { shortDateTimeFormatter.format(java.util.Date(it)) }.getOrNull() }.orEmpty()
                                                            val defaultName = "Voice ${voiceIndex + 1}"
                                                            val displayName = voiceNames[uri]?.takeIf { it.isNotBlank() } ?: defaultName
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(displayName, color = fieldText, fontSize = (12.sp * uiScale))
                                                                if (submittedLabel.isNotBlank()) {
                                                                    Text(submittedLabel, color = fieldText.copy(alpha = 0.62f), fontSize = (10.sp * uiScale))
                                                                }
                                                            }
                                                            TextButton(onClick = {
                                                                if (playingUri == uri) {
                                                                    runCatching { mediaPlayer?.stop(); mediaPlayer?.release() }
                                                                    mediaPlayer = null
                                                                    playingUri = null
                                                                } else {
                                                                    runCatching { mediaPlayer?.stop(); mediaPlayer?.release() }
                                                                    mediaPlayer = MediaPlayer().apply {
                                                                        setDataSource(context, uri.toUri())
                                                                        prepare()
                                                                        start()
                                                                        setOnCompletionListener {
                                                                            runCatching { it.release() }
                                                                            mediaPlayer = null
                                                                            playingUri = null
                                                                        }
                                                                    }
                                                                    playingUri = uri
                                                                }
                                                            }) { Text(if (playingUri == uri) "Stop" else "Play") }
                                                            IconButton(onClick = {
                                                                voiceNameTargetPage = index
                                                                voiceNameTargetUri = uri
                                                                voiceNameDraft = voiceNames[uri].orEmpty()
                                                                showVoiceNameEditor = true
                                                            }) {
                                                                Icon(Icons.Default.Edit, "Rename voice", tint = fieldText.copy(alpha = 0.8f))
                                                            }
                                                            IconButton(onClick = { voiceToDeletePage = index; voiceToDeleteUri = uri }) {
                                                                Icon(Icons.Default.Delete, "Remove voice", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.9f))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            OutlinedTextField(value = page.text, onValueChange = { setPage(index, page.copy(text = it.take(900))) }, modifier = Modifier.fillMaxSize(), placeholder = { Text("Write your notes...", color = fieldPlaceholder.copy(alpha = 0.85f), fontSize = (14.sp * uiScale)) }, textStyle = LocalTextStyle.current.copy(color = fieldText, fontSize = (14.sp * uiScale), lineHeight = (20.sp * uiScale)), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = fieldTint.copy(alpha = 0.35f), unfocusedBorderColor = fieldTint.copy(alpha = 0.18f), cursorColor = fieldTint), shape = RoundedCornerShape(14.dp))
                                        }
                                        if (!showCover) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(end = 30.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(30.dp)
                                                        .height(44.dp)
                                                        .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                                                        .background(ribbonBrush)
                                                        .clickable { onBack() },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Home, null, tint = ribbonIconColor, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(start = 30.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(30.dp)
                                                        .height(44.dp)
                                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                                        .background(
                                                            ribbonBrush
                                                        )
                                                        .clickable { showCover = true },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Close, null, tint = ribbonIconColor, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(pageBg.copy(alpha = if (pageIsLight) 0.90f else 0.84f))
                                    .border(2.dp, pageBorderColor, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = {
                                    if (pageIndex.intValue <= 0) {
                                        showCover = true
                                    } else {
                                        pageIndex.intValue--
                                    }
                                }, enabled = canPrev) {
                                    Text("< Prev", color = if (canPrev) OnCardText else OnCardText.copy(alpha = 0.35f), fontWeight = FontWeight.Bold)
                                }
                                Text("${pageIndex.intValue + 1}/365", color = OnCardText.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    TextButton(onClick = { if (canNext) pageIndex.intValue++ }, enabled = canNext) {
                                    Text("Next >", color = if (canNext) OnCardText else OnCardText.copy(alpha = 0.35f), fontWeight = FontWeight.Bold)
                                }
                            }
                }
                if (coverProgress > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp, end = 16.dp, bottom = (10.dp * uiScale).coerceAtLeast(8.dp))
                            .graphicsLayer {
                                alpha = coverProgress
                                cameraDistance = coverCameraDistancePx
                                if (isRtl) {
                                    transformOrigin = TransformOrigin(1f, 0.5f)
                                    rotationY = 64f * coverToPagesProgress
                                    translationX = coverCardSize.width.toFloat() * 0.17f * coverToPagesProgress
                                } else {
                                    transformOrigin = TransformOrigin(0f, 0.5f)
                                    rotationY = -64f * coverToPagesProgress
                                    translationX = -(coverCardSize.width.toFloat() * 0.17f * coverToPagesProgress)
                                }
                            }
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxSize()
                                .onSizeChanged { coverCardSize = it }
                                .border((2.2.dp * uiScale).coerceAtLeast(2.dp), pageBorderColor, RoundedCornerShape(18.dp * uiScale))
                                .clickable { showCover = false },
                            colors = CardDefaults.cardColors(containerColor = pageBg),
                            shape = RoundedCornerShape(18.dp * uiScale)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(24.dp * uiScale),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(journalName, color = fieldText, fontSize = (28.sp * uiScale), fontWeight = FontWeight.Black)
                                Spacer(Modifier.height(10.dp))
                                Text("Tap to open last page", color = fieldText.copy(alpha = 0.7f), fontSize = (14.sp * uiScale))
                                Text("Page ${pageIndex.intValue + 1}", color = fieldTint, fontSize = (13.sp * uiScale), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
                }
            }
        }
    if (showJournalNameEditor) {
        AlertDialog(
            onDismissRequest = { showJournalNameEditor = false },
            title = { Text("Rename Journal", color = OnCardText, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = journalNameDraft,
                    onValueChange = { journalNameDraft = it.take(24) },
                    singleLine = true,
                    label = { Text("Journal name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OnCardText,
                        unfocusedTextColor = OnCardText,
                        cursorColor = accentSoft
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onJournalNameChanged(journalNameDraft.trim().ifBlank { "Journal" })
                    showJournalNameEditor = false
                }) { Text("Save", color = accentSoft) }
            },
            dismissButton = {
                TextButton(onClick = { showJournalNameEditor = false }) { Text("Cancel", color = OnCardText) }
            }
        )
    }
    if (showVoiceNameEditor && voiceNameTargetPage != null && !voiceNameTargetUri.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showVoiceNameEditor = false; voiceNameTargetPage = null; voiceNameTargetUri = null; voiceNameDraft = "" },
            title = { Text("Rename Voice") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Set a custom name for this voice note.")
                    OutlinedTextField(
                        value = voiceNameDraft,
                        onValueChange = { voiceNameDraft = it.take(32) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Voice name") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = voiceNameTargetPage
                    val noteUri = voiceNameTargetUri
                    if (target != null && !noteUri.isNullOrBlank()) {
                        val page = safePages.getOrElse(target) { JournalPage(epochDayNow(), title = "", text = "") }
                        val nextMap = pageVoiceNames(page).toMutableMap()
                        val trimmed = voiceNameDraft.trim()
                        if (trimmed.isBlank()) nextMap.remove(noteUri) else nextMap[noteUri] = trimmed
                        setPage(
                            target,
                            page.copy(
                                voiceNoteNames = nextMap,
                                editedAtMillis = System.currentTimeMillis()
                            )
                        )
                    }
                    showVoiceNameEditor = false
                    voiceNameTargetPage = null
                    voiceNameTargetUri = null
                    voiceNameDraft = ""
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showVoiceNameEditor = false; voiceNameTargetPage = null; voiceNameTargetUri = null; voiceNameDraft = "" }) { Text("Cancel") } }
        )
    }
    if (voiceToDeletePage != null && !voiceToDeleteUri.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { voiceToDeletePage = null; voiceToDeleteUri = null },
            title = { Text("Remove Voice Attachment?") },
            text = { Text("This deletes this voice note from the page.") },
            confirmButton = {
                TextButton(onClick = {
                    val target = voiceToDeletePage
                    val uri = voiceToDeleteUri
                    voiceToDeletePage = null
                    voiceToDeleteUri = null
                    if (target != null && !uri.isNullOrBlank()) {
                        val page = safePages.getOrElse(target) { JournalPage(epochDayNow(), title = "", text = "") }
                        val nextUris = pageVoiceUris(page).filterNot { it == uri }
                        val nextNames = pageVoiceNames(page).toMutableMap().apply { remove(uri) }
                        runCatching {
                            val filePath = uri.toUri().path
                            if (!filePath.isNullOrBlank()) File(filePath).delete()
                        }
                        setPage(
                            target,
                            page.copy(
                                voiceNoteUri = null,
                                voiceNoteUris = nextUris,
                                voiceNoteSubmittedAt = pageVoiceSubmittedAt(page).filterKeys { nextUris.contains(it) },
                                voiceNoteNames = nextNames,
                                editedAtMillis = System.currentTimeMillis()
                            )
                        )
                    }
                }) { Text("Remove", color = Color(0xFFE57373)) }
            },
            dismissButton = { TextButton(onClick = { voiceToDeletePage = null; voiceToDeleteUri = null }) { Text("Cancel") } }
        )
    }
}

// === STATS & CALENDAR & SETTINGS & OTHER SCREENS (Preserved Verbatim) ===

@Composable
fun StatsScreen(modifier: Modifier, levelInfo: LevelInfo, attributes: PlayerAttributes, accentStrong: Color, accentSoft: Color, appContext: Context, onOpenDrawer: () -> Unit) {
    val historyFlow = remember(appContext) { appContext.dataStore.data.map { prefs -> parseHistory(prefs[Keys.HISTORY].orEmpty()) } }
    val historyMap: Map<Long, HistoryEntry> by historyFlow.collectAsState(initial = emptyMap())
    val last7 = remember(historyMap) { historyMap.toList().sortedByDescending { it.first }.take(7) }
    val completionRate = remember(last7) { if (last7.isEmpty()) 0 else { val sumDone = last7.sumOf { it.second.done }; val sumTotal = last7.sumOf { it.second.total.coerceAtLeast(1) }; ((sumDone.toFloat() / sumTotal.toFloat()) * 100f).toInt() } }
    ScalableScreen(modifier) { uiScale ->
        Column(verticalArrangement = Arrangement.spacedBy((6.dp * uiScale))) {
            ScalableHeader(title = stringResource(R.string.title_stats), uiScale = uiScale, onOpenDrawer = onOpenDrawer)
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp * uiScale)) {
                CardBlock { Text("Level", color = OnCardText.copy(alpha = 0.85f), fontSize = 12.sp); Text("Lv ${levelInfo.level}", color = accentStrong, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold); Spacer(Modifier.height(8.dp)); XpBar(levelInfo = levelInfo, accentStrong = accentStrong) }
                CardBlock { Text("Attributes", color = OnCardText.copy(alpha = 0.85f), fontSize = 12.sp); Spacer(Modifier.height(8.dp)); AttributeRow("Strength", attributes.str); AttributeRow("Intellect", attributes.int); AttributeRow("Vitality", attributes.vit); AttributeRow("Endurance", attributes.end); AttributeRow("Faith", attributes.fth) }
                CardBlock { Text("Last 7 days completion", color = OnCardText.copy(alpha = 0.85f), fontSize = 12.sp); Spacer(Modifier.height(8.dp)); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { repeat(7) { idx -> val item = last7.getOrNull(idx); val ok = item?.second?.allDone == true; Box(modifier = Modifier.size(18.dp).clip(RoundedCornerShape(6.dp)).background(if (ok) accentSoft else ProgressTrack)) }; Spacer(Modifier.width(8.dp)); Text("$completionRate%", color = accentStrong, fontWeight = FontWeight.Bold) } }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    modifier: Modifier,
    appContext: Context,
    plans: Map<Long, List<String>>,
    accentStrong: Color,
    accentSoft: Color,
    showTutorial: Boolean,
    onTutorialDismiss: () -> Unit,
    onAddPlan: (Long, String) -> Unit,
    onDeletePlan: (Long, Int) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val historyFlow = remember(appContext) { appContext.dataStore.data.map { prefs -> parseHistory(prefs[Keys.HISTORY].orEmpty()) } }
    val historyMap: Map<Long, HistoryEntry> by historyFlow.collectAsState(initial = emptyMap())
    var year by rememberSaveable { mutableIntStateOf(todayYmd().first) }
    var month by rememberSaveable { mutableIntStateOf(todayYmd().second) }
    val today = remember { epochDayNow() }
    var selectedDay by rememberSaveable { mutableLongStateOf(today) }
    var showAddPlanDialog by rememberSaveable { mutableStateOf(false) }
    var planTitleDraft by rememberSaveable { mutableStateOf("") }
    var planTimeDraft by rememberSaveable { mutableStateOf("9:00 AM") }
    var planNoteDraft by rememberSaveable { mutableStateOf("") }
    var planTypeExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedPlanType by rememberSaveable { mutableStateOf("General") }
    val planTypes = remember { listOf("General", "Workout", "Deep Work", "Hydrate", "Sleep", "Study") }
    val monthTitleText = remember(year, month) { monthTitle(year, month) }
    val grid = remember(year, month) { buildMonthGrid(year, month) }
    val visibleRows = remember(grid) {
        val lastFilled = grid.indexOfLast { it != null }
        if (lastFilled < 0) 0 else (lastFilled / 7) + 1
    }
    val selectedDayPlans = remember(plans, selectedDay) { plans[selectedDay].orEmpty() }
    ScalableScreen(modifier) { uiScale ->
        Column(verticalArrangement = Arrangement.spacedBy((6.dp * uiScale))) {
            ScalableHeader(
                title = stringResource(R.string.title_calendar),
                uiScale = uiScale,
                onOpenDrawer = onOpenDrawer,
                endContent = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, "Open settings", tint = OnCardText, modifier = Modifier.size(24.dp * uiScale))
                    }
                }
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp * uiScale),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(monthTitleText, color = accentStrong, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { val (y, m) = prevMonth(year, month); year = y; month = m }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month", tint = OnCardText) }
                        TextButton(onClick = {
                            val (y, m) = todayYmd()
                            year = y
                            month = m
                            selectedDay = today
                        }) { Text("Today", color = OnCardText, fontWeight = FontWeight.Bold) }
                        IconButton(onClick = { val (y, m) = nextMonth(year, month); year = y; month = m }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month", tint = OnCardText) }
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { dayLabel ->
                            Text(
                                text = dayLabel,
                                color = OnCardText.copy(alpha = 0.72f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(44.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                items(visibleRows) { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        for (col in 0 until 7) {
                            val dayEpoch = grid[row * 7 + col]
                            val isSelected = dayEpoch == selectedDay
                            val hasPlan = dayEpoch != null && plans[dayEpoch].orEmpty().isNotEmpty()
                            val history = if (dayEpoch != null) historyMap[dayEpoch] else null
                            val baseBg = when {
                                dayEpoch == null -> Color.Transparent
                                isSelected -> accentStrong.copy(alpha = 0.32f)
                                history?.allDone == true -> accentSoft.copy(alpha = 0.92f)
                                history != null -> ProgressTrack
                                else -> CardDarkBlue
                            }
                            val borderColor = when {
                                dayEpoch == today -> accentStrong.copy(alpha = 0.85f)
                                isSelected -> accentStrong.copy(alpha = 0.6f)
                                else -> Color.Transparent
                            }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(baseBg)
                                    .border(1.25.dp, borderColor, RoundedCornerShape(12.dp))
                                    .clickable(enabled = dayEpoch != null) {
                                        if (dayEpoch != null) selectedDay = dayEpoch
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (dayEpoch != null) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Text(dayOfMonthFromEpoch(dayEpoch).toString(), color = OnCardText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        if (hasPlan) {
                                            Box(modifier = Modifier.padding(top = 2.dp).size(4.dp).clip(CircleShape).background(accentStrong))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CardDarkBlue)
                            .border(1.35.dp, accentStrong.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Plans for ${formatEpochDayFull(selectedDay)}", color = accentStrong, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            FilledTonalIconButton(
                                onClick = {
                                    selectedDay -= 1L
                                    val (y, m, _) = ymdFromEpoch(selectedDay)
                                    year = y
                                    month = m
                                },
                                modifier = Modifier.size(30.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = accentStrong.copy(alpha = 0.18f),
                                    contentColor = OnCardText
                                )
                            ) {
                                Icon(Icons.Default.Remove, null, tint = OnCardText, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            FilledTonalIconButton(
                                onClick = { showAddPlanDialog = true },
                                modifier = Modifier.size(30.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = accentStrong.copy(alpha = 0.18f),
                                    contentColor = OnCardText
                                )
                            ) {
                                Icon(Icons.Default.Add, null, tint = OnCardText, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text("Tap - for previous day, + to add a plan for this day.", color = OnCardText.copy(alpha = 0.64f), fontSize = 12.sp)
                    }
                }
                item {
                    Text("Plan", color = accentStrong, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                if (selectedDayPlans.isEmpty()) {
                    item { Text("No saved plans yet.", color = OnCardText.copy(alpha = 0.7f), fontSize = 12.sp) }
                } else {
                    items(selectedDayPlans.size) { idx ->
                        val plan = selectedDayPlans[idx]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardDarkBlue)
                                .border(1.dp, accentStrong.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(plan, color = OnCardText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            IconButton(onClick = { onDeletePlan(selectedDay, idx) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.DeleteOutline, null, tint = OnCardText.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }
    }
    if (showAddPlanDialog) {
        AlertDialog(
            onDismissRequest = { showAddPlanDialog = false },
            containerColor = CardDarkBlue,
            title = { Text("Add Plan", color = OnCardText, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("For ${formatEpochDayFull(selectedDay)}", color = accentSoft, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = planTitleDraft,
                        onValueChange = { planTitleDraft = it.take(64) },
                        label = { Text("Title") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = planTimeDraft,
                        onValueChange = { planTimeDraft = it.take(20) },
                        label = { Text("Time") },
                        singleLine = true
                    )
                    ExposedDropdownMenuBox(expanded = planTypeExpanded, onExpandedChange = { planTypeExpanded = !planTypeExpanded }) {
                        OutlinedTextField(
                            value = selectedPlanType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = planTypeExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = planTypeExpanded, onDismissRequest = { planTypeExpanded = false }) {
                            planTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        selectedPlanType = type
                                        planTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = planNoteDraft,
                        onValueChange = { planNoteDraft = it.take(120) },
                        label = { Text("Note") },
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val title = planTitleDraft.trim()
                    if (title.isNotBlank()) {
                        val time = planTimeDraft.trim().ifBlank { "Any time" }
                        val note = planNoteDraft.trim()
                        val packed = buildString {
                            append(time)
                            append(" • ")
                            append(selectedPlanType)
                            append(": ")
                            append(title)
                            if (note.isNotBlank()) {
                                append(" • ")
                                append(note)
                            }
                        }
                        onAddPlan(selectedDay, packed)
                        planTitleDraft = ""
                        planNoteDraft = ""
                        showAddPlanDialog = false
                    }
                }) { Text("Save", color = accentStrong, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showAddPlanDialog = false }) { Text("Cancel", color = OnCardText) }
            }
        )
    }
    if (showTutorial) {
        CoachmarkOverlay(
            steps = listOf(
                CoachStep(
                    title = "Pick Date",
                    body = "Tap any day cell to set the active date.",
                    panelAlignment = Alignment.TopEnd,
                    pointerAlignment = Alignment.TopCenter,
                    panelOffsetY = 32.dp,
                    pointerPosition = Offset(0.52f, 0.34f)
                ),
                CoachStep(
                    title = "Add Plan",
                    body = "Use + in the Plans card to create a plan for selected date.",
                    panelAlignment = Alignment.CenterEnd,
                    pointerAlignment = Alignment.Center,
                    pointerPosition = Offset(0.90f, 0.58f)
                ),
                CoachStep(
                    title = "Manage",
                    body = "Your saved plans appear below. Tap trash to remove.",
                    panelAlignment = Alignment.BottomEnd,
                    pointerAlignment = Alignment.BottomCenter,
                    pointerPosition = Offset(0.52f, 0.78f)
                )
            ),
            accent = accentStrong,
            onDone = onTutorialDismiss
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier,
    autoNewDay: Boolean,
    confirmComplete: Boolean,
    refreshIncompleteOnly: Boolean,
    customMode: Boolean,
    advancedOptions: Boolean,
    highContrastText: Boolean,
    compactMode: Boolean,
    largerTouchTargets: Boolean,
    reduceAnimations: Boolean,
    decorativeBorders: Boolean,
    neonLightBoost: Boolean,
    neonFlowEnabled: Boolean,
    neonFlowSpeed: Int,
    neonGlowPalette: String,
    alwaysShowQuestProgress: Boolean,
    hideCompletedQuests: Boolean,
    confirmDestructiveActions: Boolean,
    dailyResetHour: Int,
    dailyQuestTarget: Int,
    expandedSection: String,
    premiumUnlocked: Boolean,
    cloudSyncEnabled: Boolean,
    cloudConnected: Boolean,
    cloudAccountEmail: String,
    cloudLastSyncAt: Long,
    dailyRemindersEnabled: Boolean,
    hapticsEnabled: Boolean,
    soundEffectsEnabled: Boolean,
    fontStyle: AppFontStyle,
    fontScalePercent: Int,
    appLanguage: String,
    backgroundImageUri: String?,
    backgroundImageTransparencyPercent: Int,
    journalName: String,
    textColorOverride: Color?,
    appBackgroundColorOverride: Color?,
    chromeBackgroundColorOverride: Color?,
    cardColorOverride: Color?,
    buttonColorOverride: Color?,
    journalPageColorOverride: Color?,
    journalAccentColorOverride: Color?,
    appTheme: AppTheme,
    accentStrong: Color,
    accentSoft: Color,
    onAutoNewDayChanged: (Boolean) -> Unit,
    onConfirmCompleteChanged: (Boolean) -> Unit,
    onRefreshIncompleteOnlyChanged: (Boolean) -> Unit,
    onCustomModeChanged: (Boolean) -> Unit,
    onAdvancedOptionsChanged: (Boolean) -> Unit,
    onHighContrastTextChanged: (Boolean) -> Unit,
    onCompactModeChanged: (Boolean) -> Unit,
    onLargeTouchTargetsChanged: (Boolean) -> Unit,
    onReduceAnimationsChanged: (Boolean) -> Unit,
    onDecorativeBordersChanged: (Boolean) -> Unit,
    onNeonLightBoostChanged: (Boolean) -> Unit,
    onNeonFlowEnabledChanged: (Boolean) -> Unit,
    onNeonFlowSpeedChanged: (Int) -> Unit,
    onNeonGlowPaletteChanged: (String) -> Unit,
    onAlwaysShowQuestProgressChanged: (Boolean) -> Unit,
    onHideCompletedQuestsChanged: (Boolean) -> Unit,
    onConfirmDestructiveChanged: (Boolean) -> Unit,
    onDailyResetHourChanged: (Int) -> Unit,
    onDailyQuestTargetChanged: (Int) -> Unit,
    onExpandedSectionChanged: (String) -> Unit,
    onPremiumUnlockedChanged: (Boolean) -> Unit,
    onCloudSyncEnabledChanged: (Boolean) -> Unit,
    onCloudEmailChanged: (String) -> Unit,
    onDailyRemindersEnabledChanged: (Boolean) -> Unit,
    onHapticsChanged: (Boolean) -> Unit,
    onSoundEffectsChanged: (Boolean) -> Unit,
    onFontStyleChanged: (AppFontStyle) -> Unit,
    onFontScalePercentChanged: (Int) -> Unit,
    onAppLanguageChanged: (String) -> Unit,
    onJournalNameChanged: (String) -> Unit,
    onTextColorChanged: (Color?) -> Unit,
    onBackgroundImageChanged: (String?) -> Unit,
    onBackgroundImageTransparencyPercentChanged: (Int) -> Unit,
    onAppBackgroundColorChanged: (Color?) -> Unit,
    onChromeBackgroundColorChanged: (Color?) -> Unit,
    onCardColorChanged: (Color?) -> Unit,
    onButtonColorChanged: (Color?) -> Unit,
    onJournalPageColorChanged: (Color?) -> Unit,
    onJournalAccentColorChanged: (Color?) -> Unit,
    onThemeChanged: (AppTheme) -> Unit,
    onAccentChanged: (Color) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onCloudSyncNow: () -> Unit,
    onCloudRestore: () -> Unit,
    onCloudConnectRequest: () -> Unit,
    onCloudDisconnect: () -> Unit,
    onSendFeedback: (String, String) -> Unit,
    onExportLogs: () -> Unit,
    onBuildAdvancedTemplateStarterJson: () -> String,
    onBuildAdvancedTemplatePromptFromRequest: (String) -> String,
    onImportAdvancedTemplateJson: (String) -> AdvancedTemplateImportResult,
    onApplyAdvancedTemplateByPackage: (String) -> Boolean,
    onRequestResetAll: () -> Unit,
    onRequestForceNewDay: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    ScalableScreen(modifier) { uiScale ->
        val context = androidx.compose.ui.platform.LocalContext.current
        val clipboard = LocalClipboardManager.current
        val advancedTemplateScope = rememberCoroutineScope()
        var advancedTemplateImportResult by remember { mutableStateOf<AdvancedTemplateImportResult?>(null) }
        var advancedTemplateImportBusy by remember { mutableStateOf(false) }
        val bgPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                onBackgroundImageChanged(uri.toString())
            }
        }
        val advancedTemplateExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                        writer.write(onBuildAdvancedTemplateStarterJson())
                    }
                }
            }
        }
        val advancedTemplateImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                val importText = runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                        val limit = 1_000_000
                        val sb = StringBuilder()
                        val buf = CharArray(8192)
                        while (true) {
                            val n = reader.read(buf)
                            if (n <= 0) break
                            if (sb.length + n > limit) {
                                throw IllegalArgumentException("JSON too large. Keep file under ~1MB.")
                            }
                            sb.append(buf, 0, n)
                        }
                        sb.toString()
                    }.orEmpty()
                }
                importText.exceptionOrNull()?.let { e ->
                    advancedTemplateImportResult = AdvancedTemplateImportResult(
                        success = false,
                        templateName = "Unnamed",
                        dailyAdded = 0,
                        mainAdded = 0,
                        errors = listOf(e.message ?: "Failed to read file.")
                    )
                }
                importText.getOrNull()?.let { json ->
                    if (json.length > 1_000_000) {
                        advancedTemplateImportResult = AdvancedTemplateImportResult(
                            success = false,
                            templateName = "Unnamed",
                            dailyAdded = 0,
                            mainAdded = 0,
                            errors = listOf("JSON too large. Keep file under ~1MB.")
                        )
                    } else {
                        advancedTemplateImportBusy = true
                        advancedTemplateScope.launch {
                            advancedTemplateImportResult = withContext(Dispatchers.Default) {
                                onImportAdvancedTemplateJson(json)
                            }
                            advancedTemplateImportBusy = false
                        }
                    }
                }
            }
        }
        var pendingTheme by rememberSaveable { mutableStateOf<AppTheme?>(null) }
        var pendingThemeAccentArgb by rememberSaveable { mutableLongStateOf(0L) }
        var colorPickerTarget by rememberSaveable { mutableStateOf<String?>(null) }
        var showThemePresetDialog by rememberSaveable { mutableStateOf(false) }
        var themeLabExpanded by rememberSaveable { mutableStateOf(false) }
        var showFeedbackDialog by rememberSaveable { mutableStateOf(false) }
        var feedbackCategoryExpanded by rememberSaveable { mutableStateOf(false) }
        var feedbackCategory by rememberSaveable { mutableStateOf("General") }
        var feedbackMessage by rememberSaveable { mutableStateOf("") }
        var advancedTemplateJsonDraft by rememberSaveable { mutableStateOf("") }
        var advancedTemplateRequestDraft by rememberSaveable { mutableStateOf("") }
        var advancedTemplateGeneratedPrompt by rememberSaveable { mutableStateOf("") }
        var backgroundMode by rememberSaveable(backgroundImageUri) { mutableStateOf(if (backgroundImageUri.isNullOrBlank()) "color" else "image") }
        val feedbackCategories = remember { listOf("Bug", "UI/UX", "Feature Request", "Performance", "General") }
        val tabGameplay = stringResource(R.string.tab_gameplay)
        val tabAppearance = stringResource(R.string.tab_appearance)
        val tabAlerts = stringResource(R.string.tab_alerts)
        val tabCloud = stringResource(R.string.tab_cloud)
        val tabCreator = stringResource(R.string.tab_creator)
        val tabSafety = stringResource(R.string.tab_safety)
        val tabAdvancedTemplates = "Templates"
        val settingsTabs = remember(tabGameplay, tabAppearance, tabAlerts, tabCloud, tabCreator, tabSafety, tabAdvancedTemplates) {
            listOf(
                "gameplay" to tabGameplay,
                "appearance" to tabAppearance,
                "alerts" to tabAlerts,
                "cloud" to tabCloud,
                "advanced_templates" to tabAdvancedTemplates,
                "feedback" to tabCreator,
                "data" to tabSafety
            )
        }
        val initialSettingsPage = remember(settingsTabs, expandedSection) {
            settingsTabs.indexOfFirst { it.first == expandedSection }.takeIf { it >= 0 } ?: 0
        }
        val settingsPagerState = rememberPagerState(initialPage = initialSettingsPage, pageCount = { settingsTabs.size })
        val settingsTabListState = rememberLazyListState()
        val settingsPagerScope = rememberCoroutineScope()
        val settingsTabProgress = (settingsPagerState.currentPage + settingsPagerState.currentPageOffsetFraction)
            .coerceIn(0f, settingsTabs.lastIndex.toFloat())
        LaunchedEffect(expandedSection) {
            val page = settingsTabs.indexOfFirst { it.first == expandedSection }
            if (page >= 0 && page != settingsPagerState.currentPage) {
                settingsPagerState.scrollToPage(page)
            }
        }
        LaunchedEffect(settingsPagerState.currentPage) {
            val key = settingsTabs[settingsPagerState.currentPage].first
            if (expandedSection != key) onExpandedSectionChanged(key)
            val tabTarget = (settingsPagerState.currentPage - 1).coerceAtLeast(0)
            settingsTabListState.animateScrollToItem(tabTarget)
        }
        val themeCyberAccent = remember(neonLightBoost) { ThemeEngine.getColors(AppTheme.CYBERPUNK).first }
        val themeDarkBg = remember { ThemeEngine.getColors(AppTheme.DEFAULT).second }
        val themeLightBg = remember { ThemeEngine.getColors(AppTheme.LIGHT).second }
        val themeCyberBg = remember(neonLightBoost) { ThemeEngine.getColors(AppTheme.CYBERPUNK).second }
        val themeDarkText = Color(0xFFE8EAF0)
        val themeLightText = Color(0xFF1B2430)
        val themeCyberText = Color(0xFFEAF8FF)
        fun baseTextForTheme(theme: AppTheme): Color = when (theme) {
            AppTheme.LIGHT -> themeLightText
            AppTheme.CYBERPUNK -> themeCyberText
            else -> themeDarkText
        }
        fun themeDisplayName(theme: AppTheme): String = when (theme) {
            AppTheme.LIGHT -> "Light"
            AppTheme.CYBERPUNK -> "Cyberpunk"
            else -> "Classic Dark"
        }
        val themeDefaultAccent = remember(appTheme, neonLightBoost) { ThemeEngine.getColors(appTheme).first }
        val themeDefaultBg = remember(appTheme, neonLightBoost) { ThemeEngine.getColors(appTheme).second }
        val neonPaletteOptions = remember {
            listOf(
                "magenta" to "Magenta",
                "cyan" to "Cyan",
                "violet" to "Violet",
                "sunset" to "Sunset",
                "lime" to "Lime",
                "ice" to "Ice"
            )
        }
        val introStyleThemePresets = remember {
            listOf(
                IntroStyleThemePreset(
                    id = "midnight_blue",
                    name = "Midnight Blue",
                    description = "Balanced dark with clean blue focus",
                    theme = AppTheme.DEFAULT,
                    accent = Color(0xFF5C9DFF),
                    previewBg = Color(0xFF0C1118),
                    previewCard = Color(0xFF141D2C),
                    previewText = Color(0xFFEAF0FA)
                ),
                IntroStyleThemePreset(
                    id = "forest_steel",
                    name = "Forest Steel",
                    description = "Calm dark with teal highlights",
                    theme = AppTheme.DEFAULT,
                    accent = Color(0xFF45B7A8),
                    previewBg = Color(0xFF0D1518),
                    previewCard = Color(0xFF162126),
                    previewText = Color(0xFFE6F2F0)
                ),
                IntroStyleThemePreset(
                    id = "ashen_ember",
                    name = "Ashen Ember",
                    description = "Dark Souls-inspired ash with ember glow",
                    theme = AppTheme.DEFAULT,
                    accent = Color(0xFFB57A3E),
                    previewBg = Color(0xFF120F10),
                    previewCard = Color(0xFF1B1719),
                    previewText = Color(0xFFE9E1D6)
                ),
                IntroStyleThemePreset(
                    id = "moonlit_steel",
                    name = "Moonlit Steel",
                    description = "Dark Souls-inspired steel blue night",
                    theme = AppTheme.DEFAULT,
                    accent = Color(0xFF6D86B3),
                    previewBg = Color(0xFF10141D),
                    previewCard = Color(0xFF171E2A),
                    previewText = Color(0xFFE3EAF6)
                ),
                IntroStyleThemePreset(
                    id = "mist_blue",
                    name = "Mist Blue",
                    description = "Soft light with clear contrast",
                    theme = AppTheme.LIGHT,
                    accent = Color(0xFF3F7FC2),
                    previewBg = Color(0xFFF6F8FC),
                    previewCard = Color(0xFFFFFFFF),
                    previewText = Color(0xFF1B2430)
                ),
                IntroStyleThemePreset(
                    id = "mint_light",
                    name = "Mint Light",
                    description = "Fresh light with green clarity",
                    theme = AppTheme.LIGHT,
                    accent = Color(0xFF2E9A86),
                    previewBg = Color(0xFFF4F9F6),
                    previewCard = Color(0xFFFFFFFF),
                    previewText = Color(0xFF18252A)
                ),
                IntroStyleThemePreset(
                    id = "cyber_aqua",
                    name = "Cyber Aqua",
                    description = "Neon cyan on deep futuristic dark",
                    theme = AppTheme.CYBERPUNK,
                    accent = Color(0xFF00F5D4),
                    previewBg = Color(0xFF090B1A),
                    previewCard = Color(0xFF141938),
                    previewText = Color(0xFFEAF8FF)
                ),
                IntroStyleThemePreset(
                    id = "cyber_rose",
                    name = "Cyber Rose",
                    description = "Neon rose with bold contrast",
                    theme = AppTheme.CYBERPUNK,
                    accent = Color(0xFFFF4FAE),
                    previewBg = Color(0xFF090B1A),
                    previewCard = Color(0xFF171E43),
                    previewText = Color(0xFFEAF8FF)
                )
            )
        }
        fun colorDistance(a: Color, b: Color): Float {
            val dr = a.red - b.red
            val dg = a.green - b.green
            val db = a.blue - b.blue
            return dr * dr + dg * dg + db * db
        }
        val selectedIntroStylePreset = remember(appTheme, accentStrong, introStyleThemePresets) {
            introStyleThemePresets
                .filter { it.theme == appTheme }
                .minByOrNull { colorDistance(it.accent, accentStrong) }
                ?: introStyleThemePresets.first()
        }
        val advancedThemePresets = remember {
            listOf(
                AdvancedThemePreset("Classic Dark", "Balanced dark with warm amber actions", Color(0xFFE8EAF0), Color(0xFF0E0F12), Color(0xFF111418), Color(0xFF111418), Color(0xFFB36A2E), Color(0xFF0C1015), Color(0xFFB36A2E)),
                AdvancedThemePreset("Ocean Night", "Deep navy + aqua clarity", Color(0xFFDCEAF8), Color(0xFF102A43), Color(0xFF0F2238), Color(0xFF132A3E), Color(0xFF2B7A78), Color(0xFF0D1A2A), Color(0xFF4FC3F7)),
                AdvancedThemePreset("Soft Light", "Clean light with subtle contrast", Color(0xFF1B2430), Color(0xFFF3F5F8), Color(0xFFEAF0F7), Color(0xFFFFFFFF), Color(0xFF1976D2), Color(0xFFF0E9DD), Color(0xFF7B5E3B)),
                AdvancedThemePreset("Amber Forge", "Warm metal + ember accents", Color(0xFFECEFF4), Color(0xFF1F1310), Color(0xFF251814), Color(0xFF2A1B15), Color(0xFFEF6C00), Color(0xFF1B120E), Color(0xFFC77F37)),
                AdvancedThemePreset("Neo Violet", "Cool dark with violet highlights", Color(0xFFDCEAF8), Color(0xFF0F1726), Color(0xFF121D30), Color(0xFF172335), Color(0xFF7E57C2), Color(0xFF10192A), Color(0xFF7E57C2)),
                AdvancedThemePreset("Mint Day", "Fresh bright mint palette", Color(0xFF0E1A2B), Color(0xFFEAF5EC), Color(0xFFDFEEE3), Color(0xFFF7FCF8), Color(0xFF2E7D32), Color(0xFFF1F8F2), Color(0xFF2E7D32)),
                AdvancedThemePreset("Sky Glass", "Light sky tones with crisp blue", Color(0xFF102A43), Color(0xFFE8F1FB), Color(0xFFDDEAF8), Color(0xFFFFFFFF), Color(0xFF1E88E5), Color(0xFFF3F8FE), Color(0xFF1976D2)),
                AdvancedThemePreset("Aqua Tech", "Tech dark with aqua energy", Color(0xFFF4F7F9), Color(0xFF12202A), Color(0xFF152633), Color(0xFF192B3A), Color(0xFF00BFA5), Color(0xFF13222E), Color(0xFF00BFA5)),
                AdvancedThemePreset("Cyberpunk Neon", "Neon pink + cyan on deep dark", Color(0xFFEAF8FF), Color(0xFF090B1A), Color(0xFF141938), Color(0xFF1A2050), Color(0xFF00F5D4), Color(0xFF101635), Color(0xFFFF2E97)),
                AdvancedThemePreset("Neon Hyperlight", "Brighter neon glow with higher contrast", Color(0xFFF4FEFF), Color(0xFF0E1230), Color(0xFF171F48), Color(0xFF202A5E), Color(0xFF3FFFE8), Color(0xFF141C42), Color(0xFFFF5EB8)),
                AdvancedThemePreset("Dark Souls Ember", "Ash dark with ember fire", Color(0xFFE6DED1), Color(0xFF14110F), Color(0xFF1A1513), Color(0xFF201915), Color(0xFFB36A2E), Color(0xFF171210), Color(0xFFC77F37)),
                AdvancedThemePreset("Blood Moon", "Crimson night with high intensity", Color(0xFFF3E9EA), Color(0xFF170E12), Color(0xFF1D1117), Color(0xFF24141C), Color(0xFFE53935), Color(0xFF1A0F14), Color(0xFFFF6B6B)),
                AdvancedThemePreset("Emerald Ruins", "Ancient green stone vibe", Color(0xFFE9F3EE), Color(0xFF101712), Color(0xFF142019), Color(0xFF1A2A22), Color(0xFF2E7D32), Color(0xFF111A14), Color(0xFF66BB6A))
            )
        }
        val advancedColoringEnabled = textColorOverride != null ||
            appBackgroundColorOverride != null ||
            chromeBackgroundColorOverride != null ||
            cardColorOverride != null ||
            buttonColorOverride != null ||
            journalPageColorOverride != null ||
            journalAccentColorOverride != null
        val previewTextColor = textColorOverride ?: baseTextForTheme(appTheme)
        val previewAppBg = appBackgroundColorOverride ?: themeDefaultBg
        val previewChromeBg = chromeBackgroundColorOverride ?: previewAppBg
        val previewCardBg = cardColorOverride ?: if (appTheme.isLightCategory()) Color.White else CardDarkBlue
        val previewButton = buttonColorOverride ?: accentStrong
        val previewJournalPage = journalPageColorOverride ?: if (appTheme.isLightCategory()) Color.White else CardDarkBlue
        val previewJournalAccent = journalAccentColorOverride ?: previewButton
        fun applyAdvancedExample(
            text: Color?,
            appBg: Color?,
            chromeBg: Color?,
            cardBg: Color?,
            button: Color?,
            journalPage: Color?,
            journalAccent: Color?
        ) {
            onTextColorChanged(text)
            onAppBackgroundColorChanged(appBg)
            onChromeBackgroundColorChanged(chromeBg)
            onCardColorChanged(cardBg)
            onButtonColorChanged(button)
            onJournalPageColorChanged(journalPage)
            onJournalAccentColorChanged(journalAccent)
        }
        val cardSpacing = (10.dp * uiScale).coerceIn(8.dp, 14.dp)
        val settingsDrawerSwipeThresholdPx = with(LocalDensity.current) { 22.dp.toPx() }
        val settingsDrawerSwipeTouchSlopPx = with(LocalDensity.current) { 10.dp.toPx() }
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val settingsTabsShape = RoundedCornerShape(16.dp)
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy((6.dp * uiScale))) {
            ScalableHeader(
                stringResource(R.string.title_settings),
                uiScale,
                onOpenDrawer,
                endContent = {
                    IconButton(onClick = {
                        settingsPagerScope.launch {
                            val page = settingsTabs.indexOfFirst { it.first == "advanced_templates" }
                            if (page >= 0) settingsPagerState.animateScrollToPage(page)
                        }
                    }) {
                        Icon(Icons.Default.AutoAwesome, "Open Advanced Template", tint = accentStrong, modifier = Modifier.size(22.dp * uiScale))
                    }
                }
            )
            Box(
                modifier = Modifier
                    .weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(settingsTabsShape)
                            .background(CardDarkBlue)
                            .border(1.4.dp, rememberNeonBorderBrush(accentStrong, neonPaletteColor(ThemeRuntime.neonGlowPalette, ThemeRuntime.neonLightBoostEnabled)), settingsTabsShape)
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val gap = 6.dp
                            val edgePad = 2.dp
                            val tabWidth = ((maxWidth - (edgePad * 2) - (gap * 2)) / 3f).coerceAtLeast(108.dp)
                            LazyRow(
                                state = settingsTabListState,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(gap),
                                contentPadding = PaddingValues(horizontal = edgePad)
                            ) {
                                itemsIndexed(settingsTabs, key = { _, tab -> tab.first }) { index, tab ->
                                    SettingsTabChip(
                                        text = tab.second,
                                        selected = settingsPagerState.currentPage == index,
                                        accent = accentStrong,
                                        modifier = Modifier.width(tabWidth),
                                        emphasis = (1f - kotlin.math.abs(settingsTabProgress - index.toFloat())).coerceIn(0f, 1f)
                                    ) {
                                        SoundManager.playClick()
                                        settingsPagerScope.launch {
                                            settingsPagerState.animateScrollToPage(
                                                page = index,
                                                animationSpec = tween(durationMillis = if (reduceAnimations) 140 else 300)
                                            )
                                            settingsTabListState.animateScrollToItem((index - 1).coerceAtLeast(0))
                                        }
                                    }
                                }
                            } 
                        }
                    }

                    HorizontalPager(
                        state = settingsPagerState,
                        modifier = Modifier
                            .pointerInput(settingsPagerState.currentPage) {
                                if (settingsPagerState.currentPage != 0) return@pointerInput
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    var totalDx = 0f
                                    var totalDy = 0f
                                    var horizontalLocked = false
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!change.pressed) break
                                        val delta = change.positionChange()
                                        totalDx += delta.x
                                        totalDy += delta.y
                                        if (!horizontalLocked) {
                                            val absDx = kotlin.math.abs(totalDx)
                                            val absDy = kotlin.math.abs(totalDy)
                                            if (absDx >= settingsDrawerSwipeTouchSlopPx && absDx > absDy * 1.05f) {
                                                horizontalLocked = true
                                            } else if (absDy >= settingsDrawerSwipeTouchSlopPx && absDy > absDx) {
                                                break
                                            }
                                        }
                                        if (!horizontalLocked) continue
                                        val drawerSwipe = if (isRtl) totalDx < -settingsDrawerSwipeThresholdPx else totalDx > settingsDrawerSwipeThresholdPx
                                        if (drawerSwipe) {
                                            onOpenDrawer()
                                            break
                                        }
                                    }
                                }
                            }
                            .weight(1f)
                            .fillMaxWidth(),
                        beyondViewportPageCount = 1
                    ) { page ->
                    val key = settingsTabs[page].first
                    val pageScroll = rememberSaveable(page, saver = ScrollState.Saver) { ScrollState(0) }
                    val absPageOffset = kotlin.math.abs(settingsPagerState.currentPage - page + settingsPagerState.currentPageOffsetFraction)
                    val pageAlpha = (1f - absPageOffset * 0.55f).coerceIn(0f, 1f)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = pageAlpha }
                            .verticalScroll(pageScroll)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(cardSpacing)
                    ) {
                        when (key) {
                            "gameplay" -> {
                                CardBlock {
                                    SettingsSubheading(stringResource(R.string.settings_daily_cycle))
                                    SettingsHint(stringResource(R.string.settings_daily_cycle_hint))
                                    SettingRow(stringResource(R.string.settings_auto_new_day), autoNewDay, onAutoNewDayChanged)
                                    SettingsStepperRow(
                                        title = stringResource(R.string.settings_reset_hour),
                                        valueLabel = "%02d:00".format(dailyResetHour),
                                        onDecrement = { onDailyResetHourChanged((dailyResetHour - 1).coerceIn(0, 23)) },
                                        onIncrement = { onDailyResetHourChanged((dailyResetHour + 1).coerceIn(0, 23)) }
                                    )
                                    SettingsStepperRow(
                                        title = stringResource(R.string.settings_quest_slots),
                                        valueLabel = "$dailyQuestTarget",
                                        onDecrement = { onDailyQuestTargetChanged((dailyQuestTarget - 1).coerceIn(3, 10)) },
                                        onIncrement = { onDailyQuestTargetChanged((dailyQuestTarget + 1).coerceIn(3, 10)) }
                                    )
                                }
                                CardBlock {
                                    SettingsSubheading(stringResource(R.string.settings_quest_flow))
                                    SettingsHint(stringResource(R.string.settings_quest_flow_hint))
                                    SettingRow(stringResource(R.string.settings_confirm_uncheck), confirmComplete, onConfirmCompleteChanged)
                                    SettingRow(stringResource(R.string.settings_reroll_incomplete), refreshIncompleteOnly, onRefreshIncompleteOnlyChanged)
                                    SettingRow(stringResource(R.string.settings_always_show_progress), alwaysShowQuestProgress, onAlwaysShowQuestProgressChanged)
                                    SettingRow(stringResource(R.string.settings_hide_completed), hideCompletedQuests, onHideCompletedQuestsChanged)
                                }
                                CardBlock {
                                    SettingsSubheading(stringResource(R.string.settings_play_modes))
                                    SettingsHint(stringResource(R.string.settings_play_modes_hint))
                                    SettingRow(stringResource(R.string.settings_custom_mode), customMode, onCustomModeChanged)
                                }
                            }
                            "appearance" -> {
                                CardBlock {
                                    OutlinedTextField(
                                        value = journalName,
                                        onValueChange = { onJournalNameChanged(it.take(24)) },
                                        singleLine = true,
                                        label = { Text(stringResource(R.string.journal_name_label)) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = OnCardText,
                                            unfocusedTextColor = OnCardText,
                                            cursorColor = accentStrong
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                CardBlock {
                                    SettingsSubheading(stringResource(R.string.settings_theme))
                                    SettingsHint(stringResource(R.string.settings_theme_hint))
                                    val introCount = introStyleThemePresets.size
                                    val appliedPageIndex = introStyleThemePresets.indexOfFirst { it.id == selectedIntroStylePreset.id }.coerceAtLeast(0)
                                    val allCount = introCount + advancedThemePresets.size
                                    val themeLoopStart = (Int.MAX_VALUE / 2 / allCount) * allCount
                                    val presetPagerState = rememberPagerState(initialPage = themeLoopStart + appliedPageIndex) { Int.MAX_VALUE }
                                    var themeSkipFirst by remember { mutableStateOf(true) }
                                    LaunchedEffect(presetPagerState.settledPage) {
                                        if (themeSkipFirst) { themeSkipFirst = false; return@LaunchedEffect }
                                        val ap = presetPagerState.settledPage % allCount
                                        if (ap < introCount) {
                                            val p = introStyleThemePresets[ap]
                                            onThemeChanged(p.theme); onAccentChanged(p.accent)
                                            applyAdvancedExample(p.previewText, p.previewBg, p.previewBg, p.previewCard, p.accent, p.previewCard, p.accent)
                                        } else {
                                            val p = advancedThemePresets[ap - introCount]
                                            applyAdvancedExample(p.text, p.appBg, p.chromeBg, p.cardBg, p.button, p.journalPage, p.journalAccent)
                                        }
                                    }
                                    val themeFling = PagerDefaults.flingBehavior(state = presetPagerState, snapAnimationSpec = spring(stiffness = Spring.StiffnessHigh))
                                    HorizontalPager(state = presetPagerState, modifier = Modifier.fillMaxWidth(), flingBehavior = themeFling) { page ->
                                        val ap = page % allCount
                                        val tName: String; val tSubtitle: String
                                        val tTextColor: Color; val tAppBg: Color; val tChromeBg: Color
                                        val tCardBg: Color; val tButton: Color
                                        val tJournalPage: Color; val tJournalAccent: Color; val tIsActive: Boolean
                                        if (ap < introCount) {
                                            val p = introStyleThemePresets[ap]
                                            tName = p.name; tSubtitle = p.description
                                            tTextColor = p.previewText; tAppBg = p.previewBg; tChromeBg = p.previewBg
                                            tCardBg = p.previewCard; tButton = p.accent
                                            tJournalPage = p.previewCard; tJournalAccent = p.accent
                                            tIsActive = ap == appliedPageIndex
                                        } else {
                                            val p = advancedThemePresets[ap - introCount]
                                            tName = p.name; tSubtitle = p.subtitle
                                            tTextColor = p.text; tAppBg = p.appBg; tChromeBg = p.chromeBg
                                            tCardBg = p.cardBg; tButton = p.button
                                            tJournalPage = p.journalPage; tJournalAccent = p.journalAccent
                                            tIsActive = false
                                        }
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(tCardBg)
                                                .border(1.5.dp, if (tIsActive) tButton else tTextColor.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            AdvancedColorMiniPreview(
                                                textColor = tTextColor,
                                                appBackground = tAppBg,
                                                chromeBackground = tChromeBg,
                                                cardBackground = tCardBg,
                                                buttonColor = tButton,
                                                journalPageBackground = tJournalPage,
                                                journalAccent = tJournalAccent
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(tName, color = tTextColor, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                                    if (tSubtitle.isNotBlank()) Text(tSubtitle, color = tTextColor.copy(alpha = 0.72f), fontSize = 11.sp, maxLines = 1)
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                    listOf(tAppBg, tCardBg, tButton, tTextColor).forEach { swatch ->
                                                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(swatch).border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape))
                                                    }
                                                }
                                                if (tIsActive) {
                                                    Box(modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(tButton.copy(alpha = 0.22f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                                        Text("Active", color = tButton, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    SettingsDivider()
                                    SettingsSubheading(stringResource(R.string.settings_accent))
                                    ColorPickerControlRow(
                                        label = stringResource(R.string.settings_accent_color),
                                        value = accentStrong,
                                        onAuto = { onAccentChanged(themeDefaultAccent) },
                                        onPick = { colorPickerTarget = "accent" },
                                        autoLabel = stringResource(R.string.settings_theme_default)
                                    )
                                }
                                CardBlock {
                                    SettingsSubheading(stringResource(R.string.settings_personalization))
                                    SettingsHint(stringResource(R.string.settings_personalization_hint))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        FilterChip(
                                            selected = backgroundMode == "image",
                                            onClick = {
                                                backgroundMode = "image"
                                                onAppBackgroundColorChanged(null)
                                            },
                                            label = { Text("Image") }
                                        )
                                        FilterChip(
                                            selected = backgroundMode == "color",
                                            onClick = {
                                                backgroundMode = "color"
                                                onBackgroundImageChanged(null)
                                            },
                                            label = { Text("Color") }
                                        )
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    if (backgroundMode == "image") {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(SubtlePanel)
                                                    .border(1.dp, OnCardText.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                                                    .clickable { bgPicker.launch(arrayOf("image/*")) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (!backgroundImageUri.isNullOrBlank()) {
                                                    AsyncImage(model = backgroundImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                                } else {
                                                    Icon(Icons.Default.AddPhotoAlternate, null, tint = OnCardText.copy(alpha = 0.7f))
                                                }
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(if (backgroundImageUri.isNullOrBlank()) stringResource(R.string.settings_no_bg) else stringResource(R.string.settings_bg_set), color = OnCardText, fontSize = 12.sp)
                                                Text(if (backgroundImageUri.isNullOrBlank()) stringResource(R.string.settings_tap_to_pick) else stringResource(R.string.settings_tap_to_replace), color = OnCardText.copy(alpha = 0.6f), fontSize = 11.sp)
                                            }
                                            if (!backgroundImageUri.isNullOrBlank()) {
                                                TextButton(onClick = { SoundManager.playClick(); onBackgroundImageChanged(null) }) { Text(stringResource(R.string.clear), color = Color(0xFFE57373)) }
                                            }
                                        }
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(appBackgroundColorOverride ?: themeDefaultBg)
                                                    .border(1.dp, OnCardText.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                                                    .clickable { colorPickerTarget = "app_bg" }
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Background color", color = OnCardText, fontSize = 12.sp)
                                                Text("Tap swatch to pick", color = OnCardText.copy(alpha = 0.6f), fontSize = 11.sp)
                                            }
                                            if (appBackgroundColorOverride != null) {
                                                TextButton(onClick = { SoundManager.playClick(); onAppBackgroundColorChanged(null) }) { Text(stringResource(R.string.clear), color = Color(0xFFE57373)) }
                                            }
                                        }
                                    }
                                    if (backgroundMode == "image") {
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Background transparency", color = OnCardText, fontSize = 12.sp)
                                            Text("${backgroundImageTransparencyPercent.coerceIn(0, 100)}%", color = OnCardText.copy(alpha = 0.75f), fontSize = 11.sp)
                                        }
                                        Slider(
                                            value = backgroundImageTransparencyPercent.coerceIn(0, 100) / 100f,
                                            onValueChange = { onBackgroundImageTransparencyPercentChanged((it * 100f).roundToInt().coerceIn(0, 100)) },
                                            valueRange = 0f..1f
                                        )
                                    }
                                }
                                CardBlock {
                                    SettingsSubheading(stringResource(R.string.settings_typography))
                                    SettingsHint(stringResource(R.string.settings_typography_hint))
                                    FontStyleSelector(selected = fontStyle, onSelect = onFontStyleChanged)
                                    SettingsStepperRow(
                                        title = stringResource(R.string.settings_font_size),
                                        valueLabel = "$fontScalePercent%",
                                        onDecrement = { onFontScalePercentChanged((fontScalePercent - 5).coerceIn(80, 125)) },
                                        onIncrement = { onFontScalePercentChanged((fontScalePercent + 5).coerceIn(80, 125)) }
                                    )
                                    val fontPreviewFamily = when (fontStyle) {
                                        AppFontStyle.SERIF, AppFontStyle.ELEGANT -> FontFamily.Serif
                                        AppFontStyle.MONO, AppFontStyle.TERMINAL -> FontFamily.Monospace
                                        AppFontStyle.HANDWRITTEN -> FontFamily.Cursive
                                        else -> FontFamily.SansSerif
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(OnCardText.copy(alpha = 0.07f))
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = "The quick brown fox jumps over the lazy dog",
                                            fontFamily = fontPreviewFamily,
                                            color = OnCardText.copy(alpha = 0.78f),
                                            fontSize = (13f * fontScalePercent / 100f).sp,
                                            lineHeight = (19f * fontScalePercent / 100f).sp
                                        )
                                    }
                                }
                                CardBlock {
                                    SettingsSubheading(stringResource(R.string.settings_accessibility))
                                    SettingsHint(stringResource(R.string.settings_accessibility_hint))
                                    SettingRow(stringResource(R.string.settings_high_contrast), highContrastText, onHighContrastTextChanged)
                                    SettingRow(stringResource(R.string.settings_compact_mode), compactMode, onCompactModeChanged)
                                    SettingRow(stringResource(R.string.settings_larger_touch), largerTouchTargets, onLargeTouchTargetsChanged)
                                }
                                CardBlock {
                                    SettingsSubheading(stringResource(R.string.settings_motion))
                                    SettingsHint(stringResource(R.string.settings_motion_hint))
                                    SettingRow(
                                        title = stringResource(R.string.settings_neon_borders),
                                        value = neonFlowEnabled,
                                        onChange = { enabled ->
                                            onNeonFlowEnabledChanged(enabled)
                                            onDecorativeBordersChanged(enabled)
                                        }
                                    )
                                    val paletteIdx = neonPaletteOptions.indexOfFirst { it.first.equals(neonGlowPalette, ignoreCase = true) }.coerceAtLeast(0)
                                    SettingsSelectorRow(
                                        title = "Neon Style",
                                        valueLabel = neonPaletteOptions[paletteIdx].second,
                                        onPrev = { onNeonGlowPaletteChanged(neonPaletteOptions[(paletteIdx - 1 + neonPaletteOptions.size) % neonPaletteOptions.size].first) },
                                        onNext = { onNeonGlowPaletteChanged(neonPaletteOptions[(paletteIdx + 1) % neonPaletteOptions.size].first) }
                                    )
                                    SettingRow(stringResource(R.string.settings_neon_boost), neonLightBoost, onNeonLightBoostChanged)
                                    SettingsSubheading(stringResource(R.string.settings_neon_speed))
                                    SettingsHint(stringResource(R.string.settings_neon_speed_hint))
                                    val speedSlow = stringResource(R.string.settings_speed_slow)
                                    val speedFast = stringResource(R.string.settings_speed_fast)
                                    val speedSmooth = stringResource(R.string.settings_speed_smooth)
                                    SettingsStepperRow(
                                        title = stringResource(R.string.settings_speed_label),
                                        valueLabel = when (neonFlowSpeed.coerceIn(0, 2)) {
                                            0 -> speedSlow
                                            2 -> speedFast
                                            else -> speedSmooth
                                        },
                                        onDecrement = { onNeonFlowSpeedChanged((neonFlowSpeed - 1).coerceIn(0, 2)) },
                                        onIncrement = { onNeonFlowSpeedChanged((neonFlowSpeed + 1).coerceIn(0, 2)) }
                                    )
                                    Text(
                                        stringResource(R.string.settings_neon_boost_tip),
                                        color = OnCardText.copy(alpha = 0.62f),
                                        fontSize = 11.sp
                                    )
                                    if (reduceAnimations && neonFlowEnabled) {
                                        Text(
                                            stringResource(R.string.settings_neon_anim_note),
                                            color = OnCardText.copy(alpha = 0.62f),
                                            fontSize = 11.sp
                                        )
                                    }
                                    SettingsDivider()
                                    SettingRow(stringResource(R.string.settings_reduce_animations), reduceAnimations, onReduceAnimationsChanged)
                                }
                                CardBlock {
                                    SettingsSubheading(stringResource(R.string.settings_language))
                                    SettingsHint(stringResource(R.string.settings_language_hint))
                                    LanguageSelector(selected = appLanguage, onSelect = onAppLanguageChanged)
                                }
                                CardBlock {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { themeLabExpanded = !themeLabExpanded }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Theme Lab", color = OnCardText.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        Icon(
                                            if (themeLabExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            null,
                                            tint = OnCardText.copy(alpha = 0.75f)
                                        )
                                    }
                                    AnimatedVisibility(visible = themeLabExpanded) {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            SettingsHint(stringResource(R.string.settings_advanced_color_hint))
                                            SettingsSubheading(stringResource(R.string.settings_typography_colors))
                                            SettingsHint(stringResource(R.string.settings_typography_colors_hint))
                                            ColorPickerControlRow(
                                                label = stringResource(R.string.settings_color_primary_text),
                                                value = textColorOverride,
                                                onAuto = { onTextColorChanged(null) },
                                                onPick = { colorPickerTarget = "text" }
                                            )
                                            Spacer(Modifier.height(6.dp))
                                            SettingsSubheading(stringResource(R.string.settings_surface_colors))
                                            SettingsHint(stringResource(R.string.settings_surface_colors_hint))
                                            ColorPickerControlRow(
                                                label = stringResource(R.string.settings_color_app_bg),
                                                value = appBackgroundColorOverride,
                                                onAuto = { onAppBackgroundColorChanged(null) },
                                                onPick = { colorPickerTarget = "app_bg" }
                                            )
                                            ColorPickerControlRow(
                                                label = stringResource(R.string.settings_color_drawer_nav),
                                                value = chromeBackgroundColorOverride,
                                                onAuto = { onChromeBackgroundColorChanged(null) },
                                                onPick = { colorPickerTarget = "chrome_bg" }
                                            )
                                            ColorPickerControlRow(
                                                label = stringResource(R.string.settings_color_cards),
                                                value = cardColorOverride,
                                                onAuto = { onCardColorChanged(null) },
                                                onPick = { colorPickerTarget = "card_bg" }
                                            )
                                            Spacer(Modifier.height(6.dp))
                                            SettingsSubheading(stringResource(R.string.settings_journal_colors))
                                            SettingsHint(stringResource(R.string.settings_journal_colors_hint))
                                            ColorPickerControlRow(
                                                label = stringResource(R.string.settings_color_journal_page),
                                                value = journalPageColorOverride,
                                                onAuto = { onJournalPageColorChanged(null) },
                                                onPick = { colorPickerTarget = "journal_page" }
                                            )
                                            ColorPickerControlRow(
                                                label = stringResource(R.string.settings_color_journal_accents),
                                                value = journalAccentColorOverride,
                                                onAuto = { onJournalAccentColorChanged(null) },
                                                onPick = { colorPickerTarget = "journal_accent" }
                                            )
                                            Spacer(Modifier.height(6.dp))
                                            SettingsSubheading(stringResource(R.string.settings_action_colors))
                                            SettingsHint(stringResource(R.string.settings_action_colors_hint))
                                            ColorPickerControlRow(
                                                label = stringResource(R.string.settings_color_buttons),
                                                value = buttonColorOverride,
                                                onAuto = { onButtonColorChanged(null) },
                                                onPick = { colorPickerTarget = "button" }
                                            )
                                        }
                                    }
                                }
                            }
                            "alerts" -> {
                                CardBlock {
                                    SettingsHint(stringResource(R.string.settings_alerts_hint))
                                    SettingRow(stringResource(R.string.settings_daily_reminders), dailyRemindersEnabled, onDailyRemindersEnabledChanged)
                                    SettingRow(stringResource(R.string.settings_haptics), hapticsEnabled, onHapticsChanged)
                                    SettingRow(stringResource(R.string.settings_sound_effects), soundEffectsEnabled, onSoundEffectsChanged)
                                }
                            }
                            "cloud" -> {
                                CardBlock {
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .alpha(0.42f)
                                        ) {
                                            SettingsHint(stringResource(R.string.settings_cloud_hint))
                                            SettingRow(stringResource(R.string.settings_cloud_auto), true, {}, enabled = false)
                                            Text(
                                                if (cloudAccountEmail.isBlank()) stringResource(R.string.settings_cloud_not_connected) else stringResource(R.string.settings_cloud_account, cloudAccountEmail),
                                                color = OnCardText.copy(alpha = 0.7f),
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                if (cloudLastSyncAt <= 0L) stringResource(R.string.settings_cloud_never_sync) else stringResource(R.string.settings_cloud_last_sync, java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(cloudLastSyncAt))),
                                                color = OnCardText.copy(alpha = 0.6f),
                                                fontSize = 11.sp
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = { },
                                                    enabled = false,
                                                    colors = ButtonDefaults.buttonColors(containerColor = if (cloudConnected) Color(0xFF8E24AA) else Color(0xFF1565C0)),
                                                    modifier = Modifier.heightIn(min = 46.dp)
                                                ) { Text(if (cloudConnected) stringResource(R.string.settings_cloud_disconnect) else stringResource(R.string.settings_cloud_connect), color = Color.White) }
                                                if (cloudConnected) {
                                                    Button(onClick = { }, enabled = false, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B)), modifier = Modifier.heightIn(min = 46.dp)) {
                                                        Text(stringResource(R.string.settings_sync_now), color = Color.White)
                                                    }
                                                    Button(onClick = { }, enabled = false, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF455A64)), modifier = Modifier.heightIn(min = 46.dp)) {
                                                        Text(stringResource(R.string.settings_restore), color = Color.White)
                                                    }
                                                }
                                            }
                                        }
                                        Text(
                                            "COMING SOON",
                                            color = OnCardText.copy(alpha = 0.28f),
                                            fontSize = 34.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .graphicsLayer { rotationZ = -16f }
                                        )
                                    }
                                }
                            }
                            "advanced_templates" -> {
                                CardBlock {
                                    SettingsSubheading("Advanced Templates")
                                    SettingsHint("Option 1: Write your goal, generate/copy prompt, send it to AI, then paste the returned JSON and analyze.\nOption 2: Download starter JSON, send it to AI with your request, then upload/paste the returned JSON and analyze.\nLimits: daily up to 500 total, main up to 200 total, shop items up to 120 total.")
                                    val sectionShape = RoundedCornerShape(14.dp)
                                    val sectionBorder = OnCardText.copy(alpha = 0.18f)
                                    val sectionBg = OnCardText.copy(alpha = 0.03f)

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(sectionShape)
                                            .background(sectionBg)
                                            .border(1.dp, sectionBorder, sectionShape)
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        SettingsSubheading("Option 1) Build Prompt")
                                        OutlinedTextField(
                                            value = advancedTemplateRequestDraft,
                                            onValueChange = { advancedTemplateRequestDraft = it.take(700) },
                                            modifier = Modifier.fillMaxWidth(),
                                            minLines = 2,
                                            maxLines = 4,
                                            label = { Text("Tell AI your quest goal") },
                                            placeholder = {
                                                Text(
                                                    "Example: Generate 120 daily quests, 40 main quests, and 24 shop items in Saitama-style anime progression with a colorful neon vibe. Use a bright aqua accent and matching UI colors.",
                                                    fontSize = 14.sp,
                                                    color = OnCardText.copy(alpha = 0.42f)
                                                )
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = OnCardText,
                                                unfocusedTextColor = OnCardText,
                                                focusedLabelColor = OnCardText.copy(alpha = 0.48f),
                                                unfocusedLabelColor = OnCardText.copy(alpha = 0.48f),
                                                cursorColor = accentStrong
                                            )
                                        )
                                        OutlinedButton(
                                            onClick = { advancedTemplateGeneratedPrompt = onBuildAdvancedTemplatePromptFromRequest(advancedTemplateRequestDraft) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF58A8FF))
                                        ) { Text("Generate Prompt", maxLines = 1) }
                                        OutlinedButton(
                                            onClick = {
                                                if (advancedTemplateGeneratedPrompt.isNotBlank()) {
                                                    clipboard.setText(AnnotatedString(advancedTemplateGeneratedPrompt))
                                                    Toast.makeText(context, "Copied. Paste into your AI chat.", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            enabled = advancedTemplateGeneratedPrompt.isNotBlank(),
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8FA6B2))
                                        ) { Text("Copy for AI", maxLines = 1) }
                                        if (advancedTemplateGeneratedPrompt.isNotBlank()) {
                                            Text(
                                                "Prompt ready. Tap \"Copy for AI\" and paste it into your AI chat.",
                                                color = OnCardText.copy(alpha = 0.72f),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(sectionShape)
                                            .background(sectionBg)
                                            .border(1.dp, sectionBorder, sectionShape)
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        SettingsSubheading("Option 1) Paste JSON")
                                        OutlinedTextField(
                                            value = advancedTemplateJsonDraft,
                                            onValueChange = { advancedTemplateJsonDraft = it },
                                            modifier = Modifier.fillMaxWidth(),
                                            minLines = 5,
                                            maxLines = 12,
                                            label = { Text("Paste JSON") },
                                            placeholder = { Text("Paste AI-generated JSON here...", fontSize = 13.sp, color = OnCardText.copy(alpha = 0.40f)) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = OnCardText,
                                                unfocusedTextColor = OnCardText,
                                                focusedLabelColor = OnCardText.copy(alpha = 0.48f),
                                                unfocusedLabelColor = OnCardText.copy(alpha = 0.48f),
                                                cursorColor = accentStrong
                                            )
                                        )
                                    }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(sectionShape)
                                            .background(sectionBg)
                                            .border(1.dp, sectionBorder, sectionShape)
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        SettingsSubheading("Option 2) Starter File")
                                        OutlinedButton(
                                            onClick = { advancedTemplateExportLauncher.launch("questify_advanced_template.json") },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF26A69A))
                                        ) { Text("Download Starter JSON", maxLines = 1) }
                                        OutlinedButton(
                                            onClick = { advancedTemplateImportLauncher.launch(arrayOf("application/json", "text/plain")) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF7C8BFF))
                                        ) { Text("Upload JSON File", maxLines = 1) }
                                    }
                                    Button(
                                        onClick = {
                                            val draft = advancedTemplateJsonDraft
                                            if (draft.length > 1_000_000) {
                                                advancedTemplateImportResult = AdvancedTemplateImportResult(
                                                    success = false,
                                                    templateName = "Unnamed",
                                                    dailyAdded = 0,
                                                    mainAdded = 0,
                                                    errors = listOf("JSON too large. Keep text under ~1MB.")
                                                )
                                            } else {
                                                advancedTemplateImportBusy = true
                                                advancedTemplateScope.launch {
                                                    advancedTemplateImportResult = withContext(Dispatchers.Default) {
                                                        onImportAdvancedTemplateJson(draft)
                                                    }
                                                    advancedTemplateImportBusy = false
                                                }
                                            }
                                        },
                                        enabled = !advancedTemplateImportBusy,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = accentStrong, contentColor = Color.Black)
                                    ) { Text("Analyze & Create Template", fontWeight = FontWeight.Bold) }
                                    if (advancedTemplateImportBusy) {
                                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = accentStrong)
                                        Text("Analyzing JSON...", color = OnCardText.copy(alpha = 0.72f), fontSize = 12.sp)
                                    }
                                    Text(
                                        "Prompt tip: ask AI to return a downloadable file named questify_advanced_template.json (or raw JSON only if files are not supported).",
                                        color = OnCardText.copy(alpha = 0.68f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            "feedback" -> {
                                CardBlock {
                                    SettingsSubheading(stringResource(R.string.settings_creator_account))
                                    SettingsHint(stringResource(R.string.settings_creator_hint))
                                    SettingRow(stringResource(R.string.settings_creator_pass), premiumUnlocked, onPremiumUnlockedChanged)
                                    SettingRow(stringResource(R.string.settings_advanced_options), advancedOptions, onAdvancedOptionsChanged)
                                }
                                if (advancedOptions) {
                                    CardBlock {
                                        SettingsSubheading(stringResource(R.string.settings_dev_tools))
                                        SettingsHint(stringResource(R.string.settings_dev_hint))
                                        @OptIn(ExperimentalLayoutApi::class)
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(onClick = onExportBackup, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26A69A))) { Text(stringResource(R.string.settings_export_backup), color = Color.White) }
                                            Button(onClick = onImportBackup, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C6BC0))) { Text(stringResource(R.string.settings_import_backup), color = Color.White) }
                                            Button(onClick = { showFeedbackDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A5ACD))) { Text(stringResource(R.string.settings_send_feedback), color = Color.White) }
                                            Button(onClick = onExportLogs, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF546E7A))) { Text(stringResource(R.string.settings_export_logs), color = Color.White) }
                                        }
                                    }
                                } else {
                                    CardBlock {
                                        SettingsSubheading(stringResource(R.string.settings_support))
                                        SettingsHint(stringResource(R.string.settings_support_hint))
                                        @OptIn(ExperimentalLayoutApi::class)
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(onClick = { showFeedbackDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A5ACD))) { Text(stringResource(R.string.settings_send_feedback), color = Color.White) }
                                        }
                                    }
                                }
                            }
                            "data" -> {
                                CardBlock {
                                    SettingsSubheading(stringResource(R.string.settings_safety))
                                    SettingsHint(stringResource(R.string.settings_safety_hint))
                                    SettingRow(stringResource(R.string.settings_confirm_destructive), confirmDestructiveActions, onConfirmDestructiveChanged)
                                }
                                CardBlock {
                                    SettingsSubheading(stringResource(R.string.settings_maintenance))
                                    SettingsHint(stringResource(R.string.settings_maintenance_hint))
                                    @OptIn(ExperimentalLayoutApi::class)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = onRequestForceNewDay, colors = ButtonDefaults.buttonColors(containerColor = accentStrong)) { Text(stringResource(R.string.settings_start_new_day), color = readableTextColor(accentStrong)) }
                                    }
                                }
                                CardBlock {
                                    SettingsSubheading(stringResource(R.string.settings_danger_zone))
                                    SettingsHint(stringResource(R.string.settings_danger_hint))
                                    @OptIn(ExperimentalLayoutApi::class)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = onRequestResetAll, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B))) { Text(stringResource(R.string.settings_reset_all), color = Color.White) }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                    }
                }
            }
            }
        }
        if (showThemePresetDialog) {
            AlertDialog(
                onDismissRequest = { showThemePresetDialog = false },
                containerColor = CardDarkBlue,
                title = { Text(stringResource(R.string.settings_theme_library), color = OnCardText, fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.settings_theme_lib_hint), color = OnCardText.copy(alpha = 0.78f), fontSize = 12.sp)
                        Spacer(Modifier.height(10.dp))
                        val libPagerState = rememberPagerState { advancedThemePresets.size }
                        HorizontalPager(state = libPagerState, modifier = Modifier.fillMaxWidth()) { page ->
                            val preset = advancedThemePresets[page]
                            AdvancedThemePresetRow(
                                preset = preset,
                                onApply = {
                                    SoundManager.playClick()
                                    applyAdvancedExample(text = preset.text, appBg = preset.appBg, chromeBg = preset.chromeBg, cardBg = preset.cardBg, button = preset.button, journalPage = preset.journalPage, journalAccent = preset.journalAccent)
                                    showThemePresetDialog = false
                                }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            repeat(advancedThemePresets.size) { i ->
                                Box(modifier = Modifier.padding(horizontal = 3.dp).size(if (i == libPagerState.currentPage) 7.dp else 4.dp).clip(CircleShape).background(if (i == libPagerState.currentPage) accentStrong else OnCardText.copy(alpha = 0.25f)))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showThemePresetDialog = false }) {
                        Text(stringResource(R.string.close), color = OnCardText)
                    }
                }
            )
        }
        if (pendingTheme != null) {
            val targetTheme = pendingTheme!!
            AlertDialog(
                onDismissRequest = {
                    pendingTheme = null
                    pendingThemeAccentArgb = 0L
                },
                containerColor = CardDarkBlue,
                title = { Text(stringResource(R.string.settings_switch_theme_title, themeDisplayName(targetTheme)), color = OnCardText, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        if (advancedColoringEnabled) {
                            stringResource(R.string.settings_switch_theme_advanced)
                        } else {
                            stringResource(R.string.settings_switch_theme_basic)
                        },
                        color = OnCardText.copy(alpha = 0.82f)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (advancedColoringEnabled) {
                                onTextColorChanged(null)
                                onAppBackgroundColorChanged(null)
                                onChromeBackgroundColorChanged(null)
                                onCardColorChanged(null)
                                onButtonColorChanged(null)
                                onJournalPageColorChanged(null)
                                onJournalAccentColorChanged(null)
                            }
                            onThemeChanged(targetTheme)
                            if (pendingThemeAccentArgb != 0L && !advancedColoringEnabled) {
                                onAccentChanged(Color(pendingThemeAccentArgb.toInt()))
                            }
                            pendingTheme = null
                            pendingThemeAccentArgb = 0L
                        }
                    ) { Text(stringResource(R.string.settings_yes_switch), color = accentStrong, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = {
                            pendingTheme = null
                        pendingThemeAccentArgb = 0L
                    }) { Text(stringResource(R.string.cancel), color = OnCardText) }
                }
            )
        }
        if (advancedTemplateImportResult != null) {
            val result = advancedTemplateImportResult!!
            AlertDialog(
                onDismissRequest = { advancedTemplateImportResult = null },
                containerColor = CardDarkBlue,
                title = { Text(if (result.success) "Template Imported" else "Import Failed", color = OnCardText, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Template: ${result.templateName}", color = OnCardText, fontSize = 12.sp)
                        Text("Daily added: ${result.dailyAdded} • Main added: ${result.mainAdded}", color = OnCardText.copy(alpha = 0.84f), fontSize = 12.sp)
                        if (result.warnings.isNotEmpty()) {
                            Text("Warnings:", color = Color(0xFFFFC107), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(result.warnings.take(6).joinToString("\n"), color = OnCardText.copy(alpha = 0.76f), fontSize = 11.sp)
                        }
                        if (result.errors.isNotEmpty()) {
                            Text("Errors:", color = Color(0xFFE57373), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(result.errors.take(4).joinToString("\n"), color = OnCardText.copy(alpha = 0.76f), fontSize = 11.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { advancedTemplateImportResult = null }) { Text("OK", color = accentStrong, fontWeight = FontWeight.Bold) }
                },
                dismissButton = if (result.success && !result.packageId.isNullOrBlank()) {
                    {
                        TextButton(
                            onClick = {
                                val applied = onApplyAdvancedTemplateByPackage(result.packageId)
                                if (applied) advancedTemplateImportResult = null
                            }
                        ) { Text("Apply Now", color = Color(0xFF26A69A), fontWeight = FontWeight.Bold) }
                    }
                } else null
            )
        }
        if (showFeedbackDialog) {
            AlertDialog(
                onDismissRequest = { showFeedbackDialog = false },
                containerColor = CardDarkBlue,
                title = { Text(stringResource(R.string.settings_send_feedback), color = OnCardText, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExposedDropdownMenuBox(expanded = feedbackCategoryExpanded, onExpandedChange = { feedbackCategoryExpanded = !feedbackCategoryExpanded }) {
                            OutlinedTextField(
                                value = feedbackCategory,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = feedbackCategoryExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = feedbackCategoryExpanded, onDismissRequest = { feedbackCategoryExpanded = false }) {
                                feedbackCategories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category) },
                                        onClick = {
                                            feedbackCategory = category
                                            feedbackCategoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = feedbackMessage,
                            onValueChange = { feedbackMessage = it.take(600) },
                            label = { Text("Message") },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val message = feedbackMessage.trim()
                        if (message.isNotBlank()) {
                            onSendFeedback(feedbackCategory, message)
                            feedbackMessage = ""
                            feedbackCategory = "General"
                            showFeedbackDialog = false
                        }
                    }) { Text("Send", color = accentStrong, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showFeedbackDialog = false }) { Text(stringResource(R.string.cancel), color = OnCardText) }
                }
            )
        }
        if (colorPickerTarget != null) {
            val target = colorPickerTarget!!
            val pickerTitle = when (target) {
                "accent" -> stringResource(R.string.settings_color_theme_accent)
                "text" -> stringResource(R.string.settings_color_primary_text)
                "app_bg" -> stringResource(R.string.settings_color_app_bg)
                "chrome_bg" -> stringResource(R.string.settings_color_drawer_nav)
                "card_bg" -> stringResource(R.string.settings_color_cards)
                "journal_page" -> stringResource(R.string.settings_color_journal_page)
                "journal_accent" -> stringResource(R.string.settings_color_journal_accents)
                "button" -> stringResource(R.string.settings_color_buttons)
                else -> stringResource(R.string.settings_accent_color)
            }
            val initialColor = when (target) {
                "accent" -> accentStrong
                "text" -> textColorOverride ?: baseTextForTheme(appTheme)
                "app_bg" -> appBackgroundColorOverride ?: themeDefaultBg
                "chrome_bg" -> chromeBackgroundColorOverride ?: themeDefaultBg
                "card_bg" -> cardColorOverride ?: if (appTheme.isLightCategory()) Color.White else CardDarkBlue
                "journal_page" -> journalPageColorOverride ?: if (appTheme.isLightCategory()) Color.White else CardDarkBlue
                "journal_accent" -> journalAccentColorOverride ?: themeDefaultAccent
                "button" -> buttonColorOverride ?: themeDefaultAccent
                else -> accentStrong
            }
            val presets = when (target) {
                "accent", "button" -> listOf(
                    "Theme Accent" to themeDefaultAccent,
                    "Cyber Cyan" to themeCyberAccent,
                    "Neon Pink" to Color(0xFFFF2E97),
                    "Orange" to Color(0xFFB36A2E),
                    "Blue" to Color(0xFF1976D2),
                    "Teal" to Color(0xFF2B7A78)
                )
                "text" -> listOf(
                    "Theme Text" to baseTextForTheme(appTheme),
                    "Dark Text" to themeDarkText,
                    "Light Text" to themeLightText,
                    "Cyber Text" to themeCyberText,
                    "White" to Color.White,
                    "Deep Blue" to Color(0xFF102A43)
                )
                "journal_accent" -> listOf(
                    "Theme Accent" to themeDefaultAccent,
                    "Cyber Cyan" to themeCyberAccent,
                    "Neon Pink" to Color(0xFFFF2E97),
                    "Gold" to Color(0xFFB36A2E),
                    "Red" to Color(0xFFD84315),
                    "Teal" to Color(0xFF2B7A78)
                )
                else -> listOf(
                    "Theme Base" to themeDefaultBg,
                    "Classic Dark" to themeDarkBg,
                    "Light" to themeLightBg,
                    "Cyberpunk" to themeCyberBg,
                    "Slate" to Color(0xFF1A1F2B),
                    "Cloud" to Color(0xFFEAF0F7)
                )
            }
            ColorTuneDialog(
                title = pickerTitle,
                initialColor = initialColor,
                presets = presets,
                onDismiss = { colorPickerTarget = null },
                onApply = { picked ->
                    when (target) {
                        "accent" -> onAccentChanged(picked)
                        "text" -> onTextColorChanged(picked)
                        "app_bg" -> onAppBackgroundColorChanged(picked)
                        "chrome_bg" -> onChromeBackgroundColorChanged(picked)
                        "card_bg" -> onCardColorChanged(picked)
                        "journal_page" -> onJournalPageColorChanged(picked)
                        "journal_accent" -> onJournalAccentColorChanged(picked)
                        "button" -> onButtonColorChanged(picked)
                    }
                    colorPickerTarget = null
                }
            )
        }
    }
}

@Composable
fun AboutScreen(modifier: Modifier, accentStrong: Color, accentSoft: Color, onOpenDrawer: () -> Unit) {
    ScalableScreen(modifier) { uiScale ->
        Column(verticalArrangement = Arrangement.spacedBy((6.dp * uiScale))) {
            ScalableHeader(stringResource(R.string.title_about), uiScale, onOpenDrawer)
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CardBlock {
                    Text("Questify", color = accentStrong, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("Enjoy! Update 1.04", color = OnCardText.copy(alpha = 0.82f))
                }
                CardBlock {
                    Text("Tips", color = accentStrong, fontWeight = FontWeight.Bold)
                    HorizontalDivider(color = OnCardText.copy(alpha = 0.14f), modifier = Modifier.padding(vertical = 4.dp))
                    Text("• Swipe right on Home to go Main, then Journal.", color = OnCardText.copy(alpha = 0.75f))
                    Text("• Swipe left on Home to open drawer.", color = OnCardText.copy(alpha = 0.75f))
                    Text("• Enable Custom Mode in Settings to edit custom quests and shop items.", color = OnCardText.copy(alpha = 0.75f))
                }
            }
        }
    }
}

@Composable
fun IntroSplash(backgroundImageUri: String?) {
    val isLightTheme = ThemeRuntime.currentTheme.isLightCategory()
    val splashBackground = if (isLightTheme) Color(0xFFF1F3F6) else DarkBackground
    val imageOverlay = if (isLightTheme) Color.White.copy(alpha = 0.70f) else Color.Black.copy(alpha = 0.45f)
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = splashBackground
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (!backgroundImageUri.isNullOrBlank()) {
                AsyncImage(
                    model = backgroundImageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(Modifier.fillMaxSize().background(imageOverlay))
            }
            Text("Questify", fontSize = 42.sp, fontWeight = FontWeight.Black, color = accentForTheme())
        }
    }
}

@Composable
fun WelcomeSetupScreen(
    onDone: (OnboardingSetup) -> Unit,
    onLanguageChanged: (String) -> Unit = {},
    defaultSkipIntro: Boolean = false
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val systemPrefersDark = isSystemInDarkTheme()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var pickedAvatar by rememberSaveable { mutableStateOf("🧑‍🚀") }
    var pickedAvatarUri by rememberSaveable { mutableStateOf<String?>(null) }
    var pickedTemplate by rememberSaveable { mutableStateOf("default_pack") }
    val defaultTheme = if (systemPrefersDark) AppTheme.DEFAULT else AppTheme.LIGHT
    val defaultAccent = ThemeEngine.getColors(defaultTheme).first
    var pickedThemeKey by rememberSaveable { mutableStateOf(defaultTheme.name) }
    var pickedAccentArgb by rememberSaveable { mutableLongStateOf(defaultAccent.toArgbCompat().toLong()) }
    var pickedThemePresetId by rememberSaveable { mutableStateOf(if (systemPrefersDark) "midnight_blue" else "mist_blue") }
    data class IntroThemePreset(
        val id: String,
        val name: String,
        val description: String,
        val theme: AppTheme,
        val accent: Color,
        val previewBg: Color,
        val previewCard: Color,
        val previewText: Color
    )
    val introThemePresets = remember {
        listOf(
            IntroThemePreset(
                id = "midnight_blue",
                name = "Midnight Blue",
                description = "Balanced dark with clean blue focus",
                theme = AppTheme.DEFAULT,
                accent = Color(0xFF5C9DFF),
                previewBg = Color(0xFF0C1118),
                previewCard = Color(0xFF141D2C),
                previewText = Color(0xFFEAF0FA)
            ),
            IntroThemePreset(
                id = "forest_steel",
                name = "Forest Steel",
                description = "Calm dark with teal highlights",
                theme = AppTheme.DEFAULT,
                accent = Color(0xFF45B7A8),
                previewBg = Color(0xFF0D1518),
                previewCard = Color(0xFF162126),
                previewText = Color(0xFFE6F2F0)
            ),
            IntroThemePreset(
                id = "ashen_ember",
                name = "Ashen Ember",
                description = "Dark Souls-inspired ash with ember glow",
                theme = AppTheme.DEFAULT,
                accent = Color(0xFFB57A3E),
                previewBg = Color(0xFF120F10),
                previewCard = Color(0xFF1B1719),
                previewText = Color(0xFFE9E1D6)
            ),
            IntroThemePreset(
                id = "moonlit_steel",
                name = "Moonlit Steel",
                description = "Dark Souls-inspired steel blue night",
                theme = AppTheme.DEFAULT,
                accent = Color(0xFF6D86B3),
                previewBg = Color(0xFF10141D),
                previewCard = Color(0xFF171E2A),
                previewText = Color(0xFFE3EAF6)
            ),
            IntroThemePreset(
                id = "mist_blue",
                name = "Mist Blue",
                description = "Soft light with clear contrast",
                theme = AppTheme.LIGHT,
                accent = Color(0xFF3F7FC2),
                previewBg = Color(0xFFF6F8FC),
                previewCard = Color(0xFFFFFFFF),
                previewText = Color(0xFF1B2430)
            ),
            IntroThemePreset(
                id = "mint_light",
                name = "Mint Light",
                description = "Fresh light with green clarity",
                theme = AppTheme.LIGHT,
                accent = Color(0xFF2E9A86),
                previewBg = Color(0xFFF4F9F6),
                previewCard = Color(0xFFFFFFFF),
                previewText = Color(0xFF18252A)
            ),
            IntroThemePreset(
                id = "cyber_aqua",
                name = "Cyber Aqua",
                description = "Neon cyan on deep futuristic dark",
                theme = AppTheme.CYBERPUNK,
                accent = Color(0xFF00F5D4),
                previewBg = Color(0xFF090B1A),
                previewCard = Color(0xFF141938),
                previewText = Color(0xFFEAF8FF)
            ),
            IntroThemePreset(
                id = "cyber_rose",
                name = "Cyber Rose",
                description = "Neon rose with bold contrast",
                theme = AppTheme.CYBERPUNK,
                accent = Color(0xFFFF4FAE),
                previewBg = Color(0xFF090B1A),
                previewCard = Color(0xFF171E43),
                previewText = Color(0xFFEAF8FF)
            )
        )
    }
    val selectedPreset = remember(pickedThemePresetId, introThemePresets) {
        introThemePresets.firstOrNull { it.id == pickedThemePresetId } ?: introThemePresets.first()
    }
    var pickedLanguage by rememberSaveable { mutableStateOf("system") }
    var goal by rememberSaveable { mutableStateOf(OnboardingGoal.BALANCE) }
    var difficulty by rememberSaveable { mutableStateOf(DifficultyPreference.NORMAL) }
    var reminderHour by rememberSaveable { mutableIntStateOf(20) }
    val setupPreviewTheme = remember(selectedPreset, pickedThemeKey, systemPrefersDark) {
        runCatching { AppTheme.valueOf(pickedThemeKey) }
            .getOrDefault(selectedPreset.theme)
    }
    val setupAccent = remember(pickedAccentArgb, setupPreviewTheme) {
        runCatching { Color(pickedAccentArgb) }.getOrDefault(ThemeEngine.getColors(setupPreviewTheme).first)
    }
    val setupSurfaceBg = selectedPreset.previewBg
    val setupCardBg = selectedPreset.previewCard
    val setupSubtleBg = if (setupPreviewTheme.isLightCategory()) Color(0xFFEAF0F7) else Color.Black.copy(alpha = 0.25f)
    val setupTextColor = selectedPreset.previewText
    val avatars = listOf("🧑‍🚀", "🧙", "🦸", "🥷", "🧠", "🏹", "🛡️", "🐺")
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val playstyleEnabled = pickedTemplate != "empty_pack"
    val stepsTotal = if (playstyleEnabled) 6 else 5
    val lastStepIndex = if (playstyleEnabled) 5 else 4
    val stepNumber = (step.coerceIn(0, lastStepIndex) + 1).coerceIn(1, stepsTotal)
    val stepProgress = (stepNumber.toFloat() / stepsTotal.toFloat()).coerceIn(0f, 1f)
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked ->
        if (picked != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(picked, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            pickedAvatarUri = picked.toString()
        }
    }
    LaunchedEffect(pickedThemePresetId) {
        val preset = introThemePresets.firstOrNull { it.id == pickedThemePresetId } ?: return@LaunchedEffect
        pickedThemeKey = preset.theme.name
        pickedAccentArgb = preset.accent.toArgbCompat().toLong()
    }
    LaunchedEffect(playstyleEnabled, lastStepIndex) {
        if (!playstyleEnabled && step > lastStepIndex) {
            step = lastStepIndex
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = setupSurfaceBg) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.welcome_to_questify), color = setupAccent, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.character_setup), color = setupTextColor, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(setupTextColor.copy(alpha = 0.16f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(stepProgress)
                        .clip(RoundedCornerShape(999.dp))
                        .background(setupAccent)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.step_n_of_m_label, stepNumber, stepsTotal), color = setupTextColor.copy(alpha = 0.68f), fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(setupCardBg)
                    .padding(16.dp)
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        if ((targetState > initialState) != isRtl) {
                            slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it / 3 } + fadeOut()
                        } else {
                            slideInHorizontally { -it / 2 } + fadeIn() togetherWith slideOutHorizontally { it / 3 } + fadeOut()
                        }
                    },
                    label = "welcome-steps"
                ) { page ->
                    when (page) {
                        0 -> {
                            val languages = listOf(
                                "system" to "System Default",
                                "en" to "English",
                                "es" to "Español",
                                "ar" to "العربية"
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(stringResource(R.string.setup_select_language), color = setupTextColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.setup_language_hint), color = setupTextColor.copy(alpha = 0.74f), fontSize = 13.sp)
                                languages.forEach { (code, label) ->
                                    val isSelected = code == pickedLanguage
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isSelected) setupAccent.copy(alpha = 0.22f) else setupSubtleBg)
                                            .border(1.dp, if (isSelected) setupAccent else setupTextColor.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                            .clickable { pickedLanguage = code }
                                            .padding(12.dp)
                                    ) {
                                        Text(label, color = if (isSelected) setupAccent else setupTextColor, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                        1 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(stringResource(R.string.pick_starting_theme), color = setupTextColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.change_later_settings), color = setupTextColor.copy(alpha = 0.74f), fontSize = 13.sp)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Brush.verticalGradient(listOf(setupAccent.copy(alpha = 0.20f), Color.Transparent)))
                                        .border(1.dp, setupAccent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                        .padding(14.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(stringResource(R.string.preview_label), color = setupTextColor.copy(alpha = 0.70f), fontSize = 12.sp, modifier = Modifier.weight(1f))
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(setupAccent)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(setupCardBg)
                                                .padding(12.dp)
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("Questify", color = setupTextColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(8.dp)
                                                        .clip(RoundedCornerShape(99.dp))
                                                        .background(setupTextColor.copy(alpha = 0.16f))
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxHeight()
                                                            .fillMaxWidth(0.34f)
                                                            .clip(RoundedCornerShape(99.dp))
                                                            .background(setupAccent)
                                                    )
                                                }
                                                Text(
                                                    selectedPreset.name,
                                                    color = setupAccent,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                                Text(stringResource(R.string.theme_presets_label), color = setupTextColor.copy(alpha = 0.72f), fontSize = 12.sp)
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(vertical = 2.dp)
                                ) {
                                    items(introThemePresets, key = { it.id }) { preset ->
                                        val isSelected = preset.id == pickedThemePresetId
                                        Column(
                                            modifier = Modifier
                                                .width(192.dp)
                                                .height(166.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(preset.previewCard.copy(alpha = if (isSelected) 1f else 0.9f))
                                                .border(1.dp, if (isSelected) preset.accent else preset.previewText.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                                                .clickable { pickedThemePresetId = preset.id }
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(74.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Brush.verticalGradient(listOf(preset.previewBg, preset.previewCard)))
                                                    .padding(8.dp)
                                            ) {
                                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                                    Text("Questify", color = preset.previewText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(6.dp)
                                                            .clip(RoundedCornerShape(99.dp))
                                                            .background(preset.previewText.copy(alpha = 0.16f))
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxHeight()
                                                                .fillMaxWidth(0.38f)
                                                                .clip(RoundedCornerShape(99.dp))
                                                                .background(preset.accent)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(preset.name, color = preset.previewText, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                            Text(
                                                preset.description,
                                                color = preset.previewText.copy(alpha = 0.74f),
                                                fontSize = 11.sp,
                                                lineHeight = 13.sp,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(stringResource(R.string.your_name), color = setupTextColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.call_me_hint), color = setupTextColor.copy(alpha = 0.75f), fontSize = 13.sp)
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it.take(20) },
                                    singleLine = true,
                                    label = { Text(stringResource(R.string.player_name_label)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = setupTextColor,
                                        unfocusedTextColor = setupTextColor,
                                        cursorColor = setupAccent,
                                        focusedBorderColor = setupAccent.copy(alpha = 0.86f),
                                        unfocusedBorderColor = setupTextColor.copy(alpha = 0.24f),
                                        focusedLabelColor = setupAccent.copy(alpha = 0.88f),
                                        unfocusedLabelColor = setupTextColor.copy(alpha = 0.62f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        3 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.your_character), color = setupTextColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                                Text(stringResource(R.string.pick_preset_hint), color = setupTextColor.copy(alpha = 0.75f), fontSize = 13.sp, modifier = Modifier.align(Alignment.Start))
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(CircleShape)
                                        .background(setupSubtleBg)
                                        .border(2.dp, setupAccent.copy(alpha = 0.6f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!pickedAvatarUri.isNullOrBlank()) {
                                        AsyncImage(model = pickedAvatarUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    } else {
                                        Text(pickedAvatar, fontSize = 46.sp)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                    avatars.forEach { emoji ->
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (emoji == pickedAvatar && pickedAvatarUri == null) setupAccent.copy(alpha = 0.24f) else Color.Transparent)
                                                .border(1.dp, if (emoji == pickedAvatar && pickedAvatarUri == null) setupAccent else setupTextColor.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                                                .clickable { pickedAvatar = emoji; pickedAvatarUri = null },
                                            contentAlignment = Alignment.Center
                                        ) { Text(emoji, fontSize = 22.sp) }
                                    }
                                }
                                Button(onClick = { imagePicker.launch(arrayOf("image/*")) }, colors = ButtonDefaults.buttonColors(containerColor = setupAccent)) {
                                    Text(stringResource(R.string.use_custom_image), color = readableTextColor(setupAccent), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        4 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(stringResource(R.string.starter_template), color = setupTextColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.choose_story_style), color = setupTextColor.copy(alpha = 0.75f), fontSize = 13.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    val selectedDefault = pickedTemplate == "default_pack"
                                    Box(
                                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (selectedDefault) setupAccent.copy(alpha = 0.22f) else setupSubtleBg).border(1.dp, if (selectedDefault) setupAccent else setupTextColor.copy(alpha = 0.15f), RoundedCornerShape(14.dp)).clickable { pickedTemplate = "default_pack" }.padding(12.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(stringResource(R.string.default_label), color = setupTextColor, fontWeight = FontWeight.Bold)
                                            Text(stringResource(R.string.balanced_progression), color = setupTextColor.copy(alpha = 0.7f), fontSize = 12.sp)
                                        }
                                    }
                                    val selectedBreaker = pickedTemplate == "saitama_v1"
                                    Box(
                                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (selectedBreaker) setupAccent.copy(alpha = 0.22f) else setupSubtleBg).border(1.dp, if (selectedBreaker) setupAccent else setupTextColor.copy(alpha = 0.15f), RoundedCornerShape(14.dp)).clickable { pickedTemplate = "saitama_v1" }.padding(12.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(stringResource(R.string.limit_breaker_label), color = setupTextColor, fontWeight = FontWeight.Bold)
                                            Text(stringResource(R.string.hard_mode_anime), color = setupTextColor.copy(alpha = 0.7f), fontSize = 12.sp)
                                        }
                                    }
                                }
                                val selectedRealWorld = pickedTemplate == REAL_WORLD_MOMENTUM_PACKAGE_ID
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (selectedRealWorld) setupAccent.copy(alpha = 0.22f) else setupSubtleBg)
                                        .border(1.dp, if (selectedRealWorld) setupAccent else setupTextColor.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                        .clickable { pickedTemplate = REAL_WORLD_MOMENTUM_PACKAGE_ID }
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(stringResource(R.string.real_world_momentum), color = setupTextColor, fontWeight = FontWeight.Bold)
                                        Text(stringResource(R.string.real_world_momentum_desc), color = setupTextColor.copy(alpha = 0.7f), fontSize = 12.sp)
                                    }
                                }
                                val selectedEmpty = pickedTemplate == "empty_pack"
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (selectedEmpty) setupAccent.copy(alpha = 0.22f) else setupSubtleBg)
                                        .border(1.dp, if (selectedEmpty) setupAccent else setupTextColor.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                        .clickable { pickedTemplate = "empty_pack" }
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(stringResource(R.string.empty_label), color = setupTextColor, fontWeight = FontWeight.Bold)
                                        Text(stringResource(R.string.start_no_quests), color = setupTextColor.copy(alpha = 0.7f), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        else -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(stringResource(R.string.playstyle_label), color = setupTextColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.set_direction_reminder), color = setupTextColor.copy(alpha = 0.75f), fontSize = 13.sp)
                                Text(stringResource(R.string.goal_label), color = setupTextColor.copy(alpha = 0.75f), fontSize = 12.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                    OnboardingGoal.entries.forEach { g ->
                                        FilterChip(selected = goal == g, onClick = { goal = g }, label = { Text(g.name.lowercase().replaceFirstChar { it.uppercase() }) })
                                    }
                                }
                                Text(stringResource(R.string.difficulty_label), color = setupTextColor.copy(alpha = 0.75f), fontSize = 12.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DifficultyPreference.entries.forEach { d ->
                                        FilterChip(selected = difficulty == d, onClick = { difficulty = d }, label = { Text(d.name.lowercase().replaceFirstChar { it.uppercase() }) })
                                    }
                                }
                                Text(stringResource(R.string.daily_reminder_hour), color = setupTextColor.copy(alpha = 0.75f), fontSize = 12.sp)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    IconButton(onClick = { reminderHour = (reminderHour - 1).coerceIn(0, 23) }) { Icon(Icons.Default.Remove, null, tint = setupTextColor) }
                                    Text("%02d:00".format(reminderHour), color = setupAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    IconButton(onClick = { reminderHour = (reminderHour + 1).coerceIn(0, 23) }) { Icon(Icons.Default.Add, null, tint = setupTextColor) }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                if (step > 0) {
                    OutlinedButton(onClick = { step -= 1 }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.back), color = setupTextColor)
                    }
                }
                Button(
                    onClick = {
                        if (step < lastStepIndex) {
                            if (step == 0) {
                                step += 1
                                onLanguageChanged(pickedLanguage)
                            } else {
                                if (step == 2 && name.isBlank()) name = "Player"
                                step += 1
                            }
                        } else {
                            onDone(
                                OnboardingSetup(
                                    name = name.ifBlank { "Player" },
                                    avatar = pickedAvatar,
                                    avatarImageUri = pickedAvatarUri,
                                    templateId = pickedTemplate,
                                    theme = setupPreviewTheme,
                                    accentArgb = pickedAccentArgb,
                                    goal = goal,
                                    difficultyPreference = difficulty,
                                    reminderHour = reminderHour
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = setupAccent)
                ) {
                    Text(if (step == lastStepIndex) stringResource(R.string.start_journey_btn) else stringResource(R.string.next), color = readableTextColor(setupAccent), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun accentForTheme(theme: AppTheme = ThemeRuntime.currentTheme): Color = ThemeEngine.getColors(theme).first

@Composable
private fun BreathingRefreshHint(
    isRefreshing: Boolean,
    tint: Color
) {
    if (isRefreshing) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = tint,
            strokeWidth = 2.dp
        )
    } else {
        val pulse = rememberInfiniteTransition(label = "refresh_hint")
        val scale by pulse.animateFloat(
            initialValue = 0.88f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "refresh_hint_scale"
        )
        val alpha by pulse.animateFloat(
            initialValue = 0.45f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "refresh_hint_alpha"
        )
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Pull down to refresh",
            tint = tint,
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    modifier: Modifier,
    accentStrong: Color,
    accentSoft: Color,
    currentUserId: String,
    currentUserName: String,
    communityPosts: List<CommunityPost>,
    followedAuthorIds: Set<String>,
    mutedAuthorIds: Set<String>,
    blockedAuthorIds: Set<String>,
    myRatings: Map<String, Int>,
    pendingSyncCount: Int,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onChangeUserName: (String) -> Unit,
    onPublish: (String, String, String) -> Unit,
    onToggleFollow: (String) -> Unit,
    onToggleMute: (String) -> Unit,
    onToggleBlock: (String) -> Unit,
    onReport: (String) -> Unit,
    onRate: (String, Int) -> Unit,
    onRemix: (CommunityPost) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit
) {
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val communityLocked = true
    var selectedTab by remember { mutableIntStateOf(0) }
    var tabOffset by remember { mutableFloatStateOf(0f) }
    var tabPageWidthPx by remember { mutableFloatStateOf(1f) }
    var publishTitle by remember { mutableStateOf("") }
    var publishDesc by remember { mutableStateOf("") }
    var publishTags by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var communitySortMode by rememberSaveable { mutableStateOf("popular") }
    var publishSection by rememberSaveable { mutableIntStateOf(0) }
    var displayNameDraft by remember(currentUserName) { mutableStateOf(currentUserName) }
    var expandedPostIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val tabProgress = run {
        val rawOffset = if (isRtl) -tabOffset else tabOffset
        (selectedTab - (rawOffset / tabPageWidthPx.coerceAtLeast(1f))).coerceIn(0f, 2f)
    }
    val communityDateFormatter = remember {
        java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
    }
    fun sortCommunityPosts(posts: List<CommunityPost>): List<CommunityPost> {
        return when (communitySortMode) {
            "latest" -> posts.sortedByDescending { it.createdAtMillis }
            "top" -> posts.sortedWith(
                compareByDescending<CommunityPost> { it.ratingAverage }
                    .thenByDescending { it.ratingCount }
                    .thenByDescending { it.createdAtMillis }
            )
            else -> posts.sortedWith(
                compareByDescending<CommunityPost> {
                    (it.ratingAverage * 100.0) + (it.ratingCount * 3.0) + (it.remixCount * 5.0)
                }.thenByDescending { it.createdAtMillis }
            )
        }
    }
    val sortedPosts = remember(communityPosts, communitySortMode) {
        sortCommunityPosts(communityPosts)
    }
    val followingPosts = remember(sortedPosts, followedAuthorIds) {
        sortedPosts.filter { followedAuthorIds.contains(it.authorId) }
    }
    val myPublishedPosts = remember(communityPosts, currentUserId) {
        communityPosts
            .filter { it.authorId == currentUserId }
            .sortedByDescending { it.createdAtMillis }
    }
    val searchFilteredDiscover = remember(sortedPosts, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) sortedPosts else sortedPosts.filter { post ->
            post.title.lowercase().contains(q) ||
                post.description.lowercase().contains(q) ||
                post.authorName.lowercase().contains(q) ||
                post.tags.any { it.lowercase().contains(q) }
        }
    }
    val searchFilteredFollowing = remember(followingPosts, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) followingPosts else followingPosts.filter { post ->
            post.title.lowercase().contains(q) ||
                post.description.lowercase().contains(q) ||
                post.authorName.lowercase().contains(q) ||
                post.tags.any { it.lowercase().contains(q) }
        }
    }

    ScalableScreen(modifier) { uiScale ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy((6.dp * uiScale))
            ) {
                ScalableHeader(title = stringResource(R.string.title_community), uiScale = uiScale, onOpenDrawer = onOpenDrawer) {
                    if (pendingSyncCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(Color(0xFFFFA000).copy(alpha = 0.18f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Sync $pendingSyncCount", color = Color(0xFFFFCA28), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, "Open settings", tint = OnCardText, modifier = Modifier.size(22.dp * uiScale))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(0.45f)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardDarkBlue)
                        .border(1.4.dp, rememberNeonBorderBrush(accentStrong, neonPaletteColor(ThemeRuntime.neonGlowPalette, ThemeRuntime.neonLightBoostEnabled)), RoundedCornerShape(12.dp))
                ) {
                    TabButton(stringResource(R.string.tab_discover), selectedTab == 0, accentStrong, Modifier.weight(1f), emphasis = (1f - kotlin.math.abs(tabProgress - 0f)).coerceIn(0f, 1f)) { selectedTab = 0 }
                    TabButton(stringResource(R.string.tab_following), selectedTab == 1, accentStrong, Modifier.weight(1f), emphasis = (1f - kotlin.math.abs(tabProgress - 1f)).coerceIn(0f, 1f)) { selectedTab = 1 }
                    TabButton(stringResource(R.string.tab_publish), selectedTab == 2, accentStrong, Modifier.weight(1f), emphasis = (1f - kotlin.math.abs(tabProgress - 2f)).coerceIn(0f, 1f)) { selectedTab = 2 }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .alpha(0.42f)
                        .onSizeChanged { size ->
                            tabPageWidthPx = size.width.toFloat().coerceAtLeast(1f)
                        }
                ) {
                val widthPx = tabPageWidthPx.coerceAtLeast(1f)
                val threshold = widthPx * 0.28f
                val touchSlop = 24f
                val tabScope = rememberCoroutineScope()
                var settleJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

                @Composable
                fun renderTab(tab: Int) {
                    if (tab == 2) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .verticalScroll(rememberScrollState())
                                .clip(RoundedCornerShape(16.dp))
                                .background(CardDarkBlue)
                                .border(1.5.dp, rememberNeonBorderBrush(accentStrong, neonPaletteColor(ThemeRuntime.neonGlowPalette, ThemeRuntime.neonLightBoostEnabled)), RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black.copy(alpha = 0.22f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(R.string.comm_create), color = OnCardText.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.comm_published), color = OnCardText.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                            }
                            if (publishSection == 0) {
                                val centeredField = Modifier
                                    .fillMaxWidth(0.95f)
                                    .align(Alignment.CenterHorizontally)
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.2f)).padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(stringResource(R.string.comm_user_id), color = OnCardText.copy(alpha = 0.6f), fontSize = 11.sp)
                                        Text(currentUserId, color = OnCardText, fontSize = 11.sp)
                                    }
                                    IconButton(onClick = { clipboard.setText(AnnotatedString(currentUserId)) }) {
                                        Icon(Icons.Default.ContentCopy, null, tint = accentSoft)
                                    }
                                }
                                OutlinedTextField(
                                    value = displayNameDraft,
                                    onValueChange = { displayNameDraft = it },
                                    enabled = false,
                                    label = { Text(stringResource(R.string.comm_display_name), color = OnCardText.copy(alpha = 0.6f)) },
                                    singleLine = true,
                                    modifier = centeredField,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { onChangeUserName(displayNameDraft) }),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = OnCardText,
                                                unfocusedTextColor = OnCardText,
                                                focusedLabelColor = OnCardText.copy(alpha = 0.48f),
                                                unfocusedLabelColor = OnCardText.copy(alpha = 0.48f),
                                                cursorColor = accentStrong
                                            )
                                        )
                                OutlinedTextField(value = publishTitle, onValueChange = { publishTitle = it }, enabled = false, label = { Text(stringResource(R.string.comm_challenge_title), color = OnCardText.copy(alpha = 0.6f)) }, singleLine = true, modifier = centeredField, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OnCardText, unfocusedTextColor = OnCardText, cursorColor = accentStrong))
                                OutlinedTextField(value = publishDesc, onValueChange = { publishDesc = it }, enabled = false, label = { Text(stringResource(R.string.comm_description), color = OnCardText.copy(alpha = 0.6f)) }, modifier = centeredField, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OnCardText, unfocusedTextColor = OnCardText, cursorColor = accentStrong))
                                OutlinedTextField(value = publishTags, onValueChange = { publishTags = it }, enabled = false, label = { Text(stringResource(R.string.comm_tags_hint), color = OnCardText.copy(alpha = 0.6f)) }, singleLine = true, modifier = centeredField, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OnCardText, unfocusedTextColor = OnCardText, cursorColor = accentStrong))
                                Button(onClick = { }, enabled = false, modifier = Modifier.fillMaxWidth(0.95f).align(Alignment.CenterHorizontally), colors = ButtonDefaults.buttonColors(containerColor = accentStrong)) { Text(stringResource(R.string.comm_publish_btn), color = Color.Black, fontWeight = FontWeight.Black) }
                                Text(stringResource(R.string.comm_publish_hint), color = OnCardText.copy(alpha = 0.55f), fontSize = 12.sp)
                            } else {
                                if (myPublishedPosts.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(SubtlePanel)
                                            .padding(12.dp)
                                    ) {
                                        Text(stringResource(R.string.comm_no_published), color = OnCardText.copy(alpha = 0.78f))
                                    }
                                } else {
                                    myPublishedPosts.forEach { post ->
                                        val publishedLabel = runCatching { communityDateFormatter.format(java.util.Date(post.createdAtMillis)) }.getOrDefault("Unknown date")
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(SubtlePanel)
                                                .border(1.dp, OnCardText.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(post.template.templateName, color = OnCardText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(post.description, color = OnCardText.copy(alpha = 0.76f), fontSize = 12.sp, maxLines = 2)
                                            Text("Published $publishedLabel", color = OnCardText.copy(alpha = 0.58f), fontSize = 11.sp)
                                            Text("Rating ${"%.1f".format(post.ratingAverage)} (${post.ratingCount}) • Remixes ${post.remixCount}", color = OnCardText.copy(alpha = 0.62f), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                            Text(stringResource(R.string.comm_swipe_hint), color = OnCardText.copy(alpha = 0.45f), fontSize = 11.sp)
                        }
                    } else {
                        val toShow = if (tab == 0) searchFilteredDiscover else searchFilteredFollowing
                        Column(modifier = Modifier.fillMaxSize()) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                enabled = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Search, "Search", tint = OnCardText.copy(alpha = 0.6f)) },
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, null, tint = OnCardText.copy(alpha = 0.7f))
                                        }
                                    }
                                },
                                label = { Text(stringResource(R.string.search_hint), color = OnCardText.copy(alpha = 0.6f)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = OnCardText,
                                    unfocusedTextColor = OnCardText,
                                    cursorColor = accentStrong
                                )
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = communitySortMode == "latest",
                                    onClick = { },
                                    enabled = false,
                                    label = { Text(stringResource(R.string.sort_latest)) }
                                )
                                FilterChip(
                                    selected = communitySortMode == "popular",
                                    onClick = { },
                                    enabled = false,
                                    label = { Text(stringResource(R.string.sort_popular)) }
                                )
                                FilterChip(
                                    selected = communitySortMode == "top",
                                    onClick = { },
                                    enabled = false,
                                    label = { Text(stringResource(R.string.sort_top)) }
                                )
                            }
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                            ) {
                                if (toShow.isEmpty()) {
                                    item {
                                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardDarkBlue).padding(16.dp)) {
                                            Text(if (tab == 0) stringResource(R.string.comm_no_challenges) else stringResource(R.string.comm_follow_creators_hint), color = OnCardText.copy(alpha = 0.8f))
                                        }
                                    }
                                } else {
                                    items(items = toShow, key = { it.id }) { post ->
                                        CommunityPostCard(
                                            post = post,
                                            accentStrong = accentStrong,
                                            accentSoft = accentSoft,
                                            isExpanded = expandedPostIds.contains(post.id),
                                            isFollowing = followedAuthorIds.contains(post.authorId),
                                            isMuted = mutedAuthorIds.contains(post.authorId),
                                            isBlocked = blockedAuthorIds.contains(post.authorId),
                                            currentRating = myRatings[post.id] ?: 0,
                                            interactionsEnabled = !communityLocked,
                                            onToggleExpanded = {
                                                expandedPostIds = if (expandedPostIds.contains(post.id)) {
                                                    expandedPostIds - post.id
                                                } else {
                                                    expandedPostIds + post.id
                                                }
                                            },
                                            onToggleFollow = { onToggleFollow(post.authorId) },
                                            onToggleMute = { onToggleMute(post.authorId) },
                                            onToggleBlock = { onToggleBlock(post.authorId) },
                                            onReport = { onReport(post.authorId) },
                                            onRate = { onRate(post.id, it) },
                                            onRemix = { onRemix(post) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(selectedTab) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val pointerId = down.id
                                settleJob?.cancel()
                                settleJob = null
                                var totalDx = 0f
                                var totalDy = 0f
                                var locked = false
                                var aborted = false
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                    if (!change.pressed) break
                                    val delta = change.positionChange()
                                    val dx = delta.x
                                    val dy = delta.y
                                    totalDx += dx
                                    totalDy += dy
                                    if (!locked) {
                                        val absDx = kotlin.math.abs(totalDx)
                                        val absDy = kotlin.math.abs(totalDy)
                                        if (absDx > touchSlop && absDx > absDy * 2f) {
                                            locked = true
                                            val exitGesture = if (isRtl) selectedTab == 0 && totalDx < 0f else selectedTab == 0 && totalDx > 0f
                                            if (exitGesture) return@awaitEachGesture
                                        } else if (absDy > touchSlop && absDy > absDx) {
                                            aborted = true
                                            break
                                        }
                                    }
                                    if (aborted || !locked) {
                                        continue
                                    }
                                    val maxRight = if (isRtl) { if (selectedTab < 2) widthPx else 0f } else { if (selectedTab > 0) widthPx else 0f }
                                    val maxLeft = if (isRtl) { if (selectedTab > 0) -widthPx else 0f } else { if (selectedTab < 2) -widthPx else 0f }
                                    tabOffset = (tabOffset + dx).coerceIn(maxLeft, maxRight)
                                    change.consume()
                                }
                                if (aborted || !locked) return@awaitEachGesture
                                val goPrev = if (isRtl) tabOffset < -threshold && selectedTab > 0 else tabOffset > threshold && selectedTab > 0
                                val goNext = if (isRtl) tabOffset > threshold && selectedTab < 2 else tabOffset < -threshold && selectedTab < 2
                                val target = when {
                                    goPrev -> if (isRtl) -widthPx else widthPx
                                    goNext -> if (isRtl) widthPx else -widthPx
                                    else -> 0f
                                }
                                settleJob = tabScope.launch {
                                    animate(
                                        initialValue = tabOffset,
                                        targetValue = target,
                                        animationSpec = tween(durationMillis = if (target == 0f) 180 else 260)
                                    ) { value, _ ->
                                        tabOffset = value
                                    }
                                    when {
                                        goPrev -> selectedTab -= 1
                                        goNext -> selectedTab += 1
                                    }
                                    tabOffset = 0f
                                }
                            }
                        }
                ) {
                    if (tabOffset > 0f) {
                        val adj = if (isRtl) selectedTab + 1 else selectedTab - 1
                        if (adj in 0..2) Box(Modifier.fillMaxSize().graphicsLayer { translationX = tabOffset - widthPx }) { renderTab(adj) }
                    }
                    if (tabOffset < 0f) {
                        val adj = if (isRtl) selectedTab - 1 else selectedTab + 1
                        if (adj in 0..2) Box(Modifier.fillMaxSize().graphicsLayer { translationX = tabOffset + widthPx }) { renderTab(adj) }
                    }
                    Box(Modifier.fillMaxSize().graphicsLayer { translationX = tabOffset }) { renderTab(selectedTab) }
                }
                Text(
                    "COMING SOON",
                    color = OnCardText.copy(alpha = 0.26f),
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer { rotationZ = -20f }
                )
                }
            }
        }
    }
}

@Composable
fun CommunityPostCard(
    post: CommunityPost,
    accentStrong: Color,
    accentSoft: Color,
    isExpanded: Boolean,
    isFollowing: Boolean,
    isMuted: Boolean,
    isBlocked: Boolean,
    currentRating: Int,
    interactionsEnabled: Boolean,
    onToggleExpanded: () -> Unit,
    onToggleFollow: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleBlock: () -> Unit,
    onReport: () -> Unit,
    onRate: (Int) -> Unit,
    onRemix: () -> Unit
) {
    var showModerationMenu by remember { mutableStateOf(false) }
    val questCountLabel = "${post.template.dailyQuests.size} daily • ${post.template.mainQuests.size} main"
    val publishedLabel = remember(post.createdAtMillis) {
        runCatching {
            java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date(post.createdAtMillis))
        }.getOrDefault("Unknown date")
    }
    val postNeonEnabled = ThemeRuntime.neonFlowEnabled || ThemeRuntime.decorativeBordersEnabled
    val postBoostedNeon = ThemeRuntime.neonLightBoostEnabled
    val postNeonSecondary = neonPaletteColor(ThemeRuntime.neonGlowPalette, postBoostedNeon)
    val postNeonBrush = if (postNeonEnabled) rememberNeonBorderBrush(accentStrong, postNeonSecondary) else null
    val postCardShape = RoundedCornerShape(14.dp)
    val trustBadgeLabel = when (post.templateTrust) {
        TemplateTrustLevel.VERIFIED_SAFE -> "Verified Safe"
        TemplateTrustLevel.SANITIZED -> "Sanitized"
        TemplateTrustLevel.FLAGGED -> "Flagged"
    }
    val trustBadgeColor = when (post.templateTrust) {
        TemplateTrustLevel.VERIFIED_SAFE -> Color(0xFF26A69A)
        TemplateTrustLevel.SANITIZED -> Color(0xFFFFB300)
        TemplateTrustLevel.FLAGGED -> Color(0xFFE53935)
    }
    val canApplyTemplate = interactionsEnabled && post.templateTrust != TemplateTrustLevel.FLAGGED
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(postCardShape)
            .background(CardDarkBlue)
            .then(if (postNeonEnabled && postNeonBrush != null) Modifier.border(if (postBoostedNeon) 2.dp else 1.5.dp, postNeonBrush, postCardShape) else Modifier)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val trustScore = ((post.ratingAverage * post.ratingCount) + (post.remixCount * 2)).toInt()
        val trustLabel = when {
            trustScore >= 80 -> "Legend"
            trustScore >= 40 -> "Trusted"
            trustScore >= 15 -> "Rising"
            else -> "New"
        }

        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .then(
                        if (interactionsEnabled) {
                            Modifier.pointerInput(post.id) {
                                detectTapGestures(
                                    onTap = {
                                        SoundManager.playClick()
                                        onToggleExpanded()
                                    },
                                    onLongPress = {
                                        showModerationMenu = true
                                    }
                                )
                            }
                        } else Modifier
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            post.template.templateName,
                            color = OnCardText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(trustBadgeColor.copy(alpha = 0.18f))
                                .border(1.dp, trustBadgeColor.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(trustBadgeLabel, color = trustBadgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(6.dp))
                        Button(
                            onClick = onRemix,
                            enabled = canApplyTemplate,
                            colors = ButtonDefaults.buttonColors(containerColor = accentStrong),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text(
                                "Add Template",
                                color = readableTextColor(accentStrong),
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            null,
                            tint = OnCardText.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        "${post.authorName} • $publishedLabel",
                        color = OnCardText.copy(alpha = 0.62f),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
            if (post.template.isPremium) {
                Text(
                    "PRO",
                    color = Color(0xFFFFD54F),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-10).dp, y = (-14).dp)
                        .rotate(-18f)
                )
            }
            DropdownMenu(
                expanded = showModerationMenu && interactionsEnabled,
                onDismissRequest = { showModerationMenu = false },
                modifier = Modifier.background(CardDarkBlue)
            ) {
                DropdownMenuItem(
                    text = { Text(if (isMuted) "Unmute creator" else "Mute creator", color = OnCardText) },
                    onClick = {
                        showModerationMenu = false
                        onToggleMute()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (isBlocked) "Unblock creator" else "Block creator", color = OnCardText) },
                    onClick = {
                        showModerationMenu = false
                        onToggleBlock()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Report creator", color = OnCardText) },
                    onClick = {
                        showModerationMenu = false
                        onReport()
                    }
                )
            }
        }

        if (isExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "by ${post.authorName} • $publishedLabel • Trust: $trustLabel • Remixes ${post.remixCount}",
                        color = OnCardText.copy(alpha = 0.62f),
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (post.description.isNotBlank()) {
                    Text(
                        post.description,
                        color = OnCardText.copy(alpha = 0.78f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                if (post.tags.isNotEmpty()) {
                    val tagChipBg = OnCardText.copy(alpha = 0.10f)
                    val tagChipText = OnCardText.copy(alpha = 0.78f)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        post.tags.take(3).forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(tagChipBg)
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text("#$tag", color = tagChipText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (post.templateTrust == TemplateTrustLevel.FLAGGED) {
                    Text(
                        "Template flagged for safety review. Applying is disabled.",
                        color = Color(0xFFE57373),
                        fontSize = 12.sp
                    )
                } else if (post.templateTrust == TemplateTrustLevel.SANITIZED) {
                    Text(
                        "Template was auto-cleaned for safe import.",
                        color = Color(0xFFFFCC80),
                        fontSize = 12.sp
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            "${"%.1f".format(post.ratingAverage)} (${post.ratingCount})",
                            color = accentStrong,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        (1..5).forEach { star ->
                            Icon(
                                Icons.Default.Star,
                                null,
                                tint = if (star <= currentRating) Color(0xFFFFC107) else OnCardText.copy(alpha = 0.25f),
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable(enabled = interactionsEnabled) { onRate(star) }
                                    .padding(1.dp)
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        questCountLabel,
                        color = OnCardText.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                    TextButton(
                        onClick = onToggleFollow,
                        enabled = interactionsEnabled,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            if (isFollowing) "Following" else "Follow",
                            color = if (isFollowing) accentSoft else accentStrong,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

// === CUSTOM QUESTS SCREEN (Admin Mode + Daily/Main/Template tabs) ===
@Composable
fun QuestsScreen(
    modifier: Modifier, accentStrong: Color, accentSoft: Color,
    customMode: Boolean,
    dailyTemplates: List<CustomTemplate>, mainQuests: List<CustomMainQuest>, savedTemplates: List<GameTemplate>,
    activePackageIds: Set<String>,
    onTogglePackage: (GameTemplate, Boolean) -> Unit,
    onUpsertDaily: (CustomTemplate) -> Unit, onDeleteDaily: (String) -> Unit,
    onUpsertMain: (CustomMainQuest) -> Unit, onDeleteMain: (String) -> Unit,
    onRestoreDefaults: () -> Unit, onExportTemplate: (String) -> Unit,
    onSaveCurrentToLibrary: (String) -> Unit,
    onApplySavedTemplate: (GameTemplate, String?, Boolean) -> Unit,
    onDeleteSavedTemplate: (GameTemplate) -> Unit,
    onRequireCustomMode: () -> Unit,
    onOpenCommunityTemplates: () -> Unit,
    onOpenAdvancedTemplates: () -> Unit,
    showTutorial: Boolean,
    onTutorialDismiss: () -> Unit,
    initialTab: Int = 0,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var selectedTab by rememberSaveable(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    var tabOffset by remember { mutableFloatStateOf(0f) }
    var tabPageWidthPx by remember { mutableFloatStateOf(1f) }
    var showDailyEditor by remember { mutableStateOf(false) }
    var showMainEditor by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    var showExportNameDialog by remember { mutableStateOf(false) }
    var exportName by remember { mutableStateOf("My Custom MMO") }
    var showSaveLibraryDialog by remember { mutableStateOf(false) }
    var saveLibraryName by remember { mutableStateOf("My Current Setup") }

    var templateToApply by remember { mutableStateOf<GameTemplate?>(null) }
    var backupBeforeApply by remember { mutableStateOf(true) }
    var clearExistingBeforeApply by remember { mutableStateOf(true) }
    var backupName by remember { mutableStateOf("Backup") }
    var restoreBackupBeforeApply by remember { mutableStateOf(true) }
    var restoreBackupName by remember { mutableStateOf("Backup") }

    var editingDaily by remember { mutableStateOf<CustomTemplate?>(null) }
    var editingMain by remember { mutableStateOf<CustomMainQuest?>(null) }
    var deleteDailyId by remember { mutableStateOf<String?>(null) }
    var deleteMainId by remember { mutableStateOf<String?>(null) }
    var templateToDelete by remember { mutableStateOf<GameTemplate?>(null) }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val tabProgress = run {
        val rawOffset = if (isRtl) -tabOffset else tabOffset
        (selectedTab - (rawOffset / tabPageWidthPx.coerceAtLeast(1f))).coerceIn(0f, 2f)
    }
    LaunchedEffect(initialTab) {
        val target = initialTab.coerceIn(0, 2)
        if (selectedTab != target) {
            selectedTab = target
            tabOffset = 0f
        }
    }

    val sortedDaily = remember(dailyTemplates) { dailyTemplates.sortedWith(compareByDescending<CustomTemplate> { it.isPinned }.thenBy { it.title }) }
    val groupedDaily = remember(sortedDaily) { sortedDaily.groupBy { it.category } }

    ScalableScreen(modifier) { uiScale ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(verticalArrangement = Arrangement.spacedBy((6.dp * uiScale))) {
                ScalableHeader(title = stringResource(R.string.title_quests_templates), uiScale = uiScale, onOpenDrawer = onOpenDrawer) {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, "Open settings", tint = OnCardText, modifier = Modifier.size(22.dp * uiScale))
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(12.dp)).background(CardDarkBlue).border(1.4.dp, rememberNeonBorderBrush(accentStrong, neonPaletteColor(ThemeRuntime.neonGlowPalette, ThemeRuntime.neonLightBoostEnabled)), RoundedCornerShape(12.dp))) {
                    TabButton(stringResource(R.string.tab_daily), selectedTab == 0, accentStrong, Modifier.weight(1f), emphasis = (1f - kotlin.math.abs(tabProgress - 0f)).coerceIn(0f, 1f)) { selectedTab = 0 }
                    TabButton(stringResource(R.string.tab_main), selectedTab == 1, accentStrong, Modifier.weight(1f), emphasis = (1f - kotlin.math.abs(tabProgress - 1f)).coerceIn(0f, 1f)) { selectedTab = 1 }
                    TabButton(stringResource(R.string.tab_template), selectedTab == 2, accentStrong, Modifier.weight(1f), emphasis = (1f - kotlin.math.abs(tabProgress - 2f)).coerceIn(0f, 1f)) { selectedTab = 2 }
                }
                if (!customMode) {
                    Text(
                        stringResource(R.string.quests_admin_hint),
                        color = OnCardText.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .onSizeChanged { size ->
                            tabPageWidthPx = size.width.toFloat().coerceAtLeast(1f)
                        }
                ) {
                    val widthPx = tabPageWidthPx.coerceAtLeast(1f)
                    val threshold = widthPx * 0.28f
                    val touchSlop = 24f
                    val tabScope = rememberCoroutineScope()
                    var settleJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

                    @Composable
                    fun renderQuestTab(tab: Int) {
                        val listSpacing = if (tab == 2) 4.dp else 10.dp
                        val topPad = if (tab == 2) 0.dp else 8.dp
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(listSpacing),
                            contentPadding = PaddingValues(top = topPad, bottom = 80.dp)
                        ) {
                            if (tab == 0) {
                                if (dailyTemplates.isEmpty()) {
                                    item {
                                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(18.dp)).background(CardDarkBlue).padding(14.dp)) {
                                            Text("No daily quests.", color = OnCardText.copy(alpha = 0.85f))
                                        }
                                    }
                                } else {
                                    QuestCategory.entries.forEach { cat ->
                                        val items = groupedDaily[cat].orEmpty()
                                        if (items.isNotEmpty()) {
                                            item {
                                                QuestCategoryHeader(
                                                    cat, items, accentSoft,
                                                    customMode = customMode,
                                                    onEdit = { if (customMode) { editingDaily = it; showDailyEditor = true } },
                                                    onDelete = { if (customMode) deleteDailyId = it.id },
                                                    onToggle = { t, active -> onUpsertDaily(t.copy(isActive = active)) }
                                                )
                                            }
                                        }
                                    }
                                }
                            } else if (tab == 1) {
                                if (mainQuests.isEmpty()) {
                                    item {
                                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(18.dp)).background(CardDarkBlue).padding(14.dp)) {
                                            Text("No main quests.", color = OnCardText.copy(alpha = 0.85f))
                                        }
                                    }
                                } else {
                                    data class ParsedMainPoolQuest(val quest: CustomMainQuest, val index: Int, val familyKey: String, val familyTier: Int?)
                                    fun parseFamily(title: String): Pair<String, Int?> {
                                        val m = Regex("""^(.*?)(?:\s+(\d+))$""").find(title.trim())
                                        val base = (m?.groupValues?.getOrNull(1)?.trim().takeUnless { it.isNullOrBlank() } ?: title.trim())
                                        val tier = m?.groupValues?.getOrNull(2)?.toIntOrNull()
                                        return base to tier
                                    }
                                    val familyChains = mainQuests
                                        .mapIndexed { idx, q ->
                                            val (familyKey, familyTier) = parseFamily(q.title)
                                            ParsedMainPoolQuest(q, idx, familyKey, familyTier)
                                        }
                                        .groupBy { "${it.quest.packageId}|${it.familyKey.lowercase()}" }
                                        .values
                                        .sortedBy { g -> g.minOf { it.index } }
                                        .map { group ->
                                            val tiered = group.filter { it.familyTier != null }
                                            val ordered = if (tiered.size >= 2) {
                                                group.sortedWith(compareBy<ParsedMainPoolQuest> { it.familyTier ?: Int.MAX_VALUE }.thenBy { it.index })
                                            } else {
                                                group.sortedBy { it.index }
                                            }
                                            ordered.first().quest.title to ordered.map { it.quest }
                                        }
                                    familyChains.forEach { (familyTitle, chain) ->
                                        item {
                                            MainQuestChainHeader(
                                                familyTitle = familyTitle,
                                                quests = chain,
                                                accentSoft = accentSoft,
                                                customMode = customMode,
                                                onEdit = { if (customMode) { editingMain = it; showMainEditor = true } },
                                                onDelete = { if (customMode) deleteMainId = it.id },
                                                onToggle = { mq, active -> onUpsertMain(mq.copy(isActive = active)) }
                                            )
                                        }
                                    }
                                }
                            } else {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp, vertical = 0.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Public,
                                                contentDescription = null,
                                                tint = accentStrong.copy(alpha = 0.9f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            TextButton(
                                                onClick = {
                                                    SoundManager.playClick()
                                                    onOpenCommunityTemplates()
                                                },
                                                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                                            ) {
                                                Text("Community Templates", color = accentStrong, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                            }
                                        }
                                        TextButton(
                                            onClick = {
                                                SoundManager.playClick()
                                                onOpenAdvancedTemplates()
                                            },
                                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = accentStrong.copy(alpha = 0.92f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text("Advanced Template", color = accentStrong.copy(alpha = 0.92f), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                                item {
                                    val def = getDefaultGameTemplate()
                                    val isActive = activePackageIds.contains(def.packageId)
                                    TemplateLibraryCard(def, isActive, accentStrong, accentSoft, uiScale, onToggle = { onTogglePackage(def, it) }, onDelete = null)
                                }
                                item {
                                    val realWorld = getRealWorldMomentumTemplate()
                                    val isActive = activePackageIds.contains(realWorld.packageId)
                                    TemplateLibraryCard(realWorld, isActive, accentStrong, accentSoft, uiScale, onToggle = { onTogglePackage(realWorld, it) }, onDelete = null)
                                }
                                item {
                                    val saitama = getLimitBreakerTemplate()
                                    val isActive = activePackageIds.contains(saitama.packageId)
                                    TemplateLibraryCard(saitama, isActive, accentStrong, accentSoft, uiScale, onToggle = { onTogglePackage(saitama, it) }, onDelete = null)
                                }
                                items(savedTemplates) { t ->
                                    val isActive = activePackageIds.contains(t.packageId)
                                    TemplateLibraryCard(
                                        t,
                                        isActive,
                                        accentStrong,
                                        accentSoft,
                                        uiScale,
                                        onToggle = { enabled ->
                                            if (enabled) {
                                                templateToApply = t
                                            } else {
                                                onTogglePackage(t, false)
                                            }
                                        },
                                        onDelete = { templateToDelete = t }
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(selectedTab) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val pointerId = down.id
                                    settleJob?.cancel()
                                    settleJob = null
                                    var totalDx = 0f
                                    var totalDy = 0f
                                    var locked = false
                                    var aborted = false
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                        if (!change.pressed) break
                                        val delta = change.positionChange()
                                        val dx = delta.x
                                        val dy = delta.y
                                        totalDx += dx
                                        totalDy += dy
                                        if (!locked) {
                                            val absDx = kotlin.math.abs(totalDx)
                                            val absDy = kotlin.math.abs(totalDy)
                                            if (absDx > touchSlop && absDx > absDy * 2f) {
                                                locked = true
                                                val exitGesture = if (isRtl) selectedTab == 0 && totalDx < 0f else selectedTab == 0 && totalDx > 0f
                                                if (exitGesture) return@awaitEachGesture
                                            } else if (absDy > touchSlop && absDy > absDx) {
                                                aborted = true
                                                break
                                            }
                                        }
                                        if (aborted || !locked) {
                                            continue
                                        }
                                        val maxRight = if (isRtl) { if (selectedTab < 2) widthPx else 0f } else { if (selectedTab > 0) widthPx else 0f }
                                        val maxLeft = if (isRtl) { if (selectedTab > 0) -widthPx else 0f } else { if (selectedTab < 2) -widthPx else 0f }
                                        tabOffset = (tabOffset + dx).coerceIn(maxLeft, maxRight)
                                        change.consume()
                                    }
                                    if (aborted || !locked) return@awaitEachGesture
                                    val goPrev = if (isRtl) tabOffset < -threshold && selectedTab > 0 else tabOffset > threshold && selectedTab > 0
                                    val goNext = if (isRtl) tabOffset > threshold && selectedTab < 2 else tabOffset < -threshold && selectedTab < 2
                                    val target = when {
                                        goPrev -> if (isRtl) -widthPx else widthPx
                                        goNext -> if (isRtl) widthPx else -widthPx
                                        else -> 0f
                                    }
                                    settleJob = tabScope.launch {
                                        animate(
                                            initialValue = tabOffset,
                                            targetValue = target,
                                            animationSpec = tween(durationMillis = if (target == 0f) 180 else 260)
                                        ) { value, _ ->
                                            tabOffset = value
                                        }
                                        when {
                                            goPrev -> selectedTab -= 1
                                            goNext -> selectedTab += 1
                                        }
                                        tabOffset = 0f
                                    }
                                }
                            }
                    ) {
                        if (tabOffset > 0f) {
                            val adj = if (isRtl) selectedTab + 1 else selectedTab - 1
                            if (adj in 0..2) Box(Modifier.fillMaxSize().graphicsLayer { translationX = tabOffset - widthPx }) { renderQuestTab(adj) }
                        }
                        if (tabOffset < 0f) {
                            val adj = if (isRtl) selectedTab - 1 else selectedTab + 1
                            if (adj in 0..2) Box(Modifier.fillMaxSize().graphicsLayer { translationX = tabOffset + widthPx }) { renderQuestTab(adj) }
                        }
                        Box(Modifier.fillMaxSize().graphicsLayer { translationX = tabOffset }) { renderQuestTab(selectedTab) }
                    }
                }
            }

            val addFabBaseContainerColor = if (customMode) accentStrong else OnCardText.copy(alpha = 0.25f)
            val addFabBaseContentColor = if (customMode) Color.Black else OnCardText.copy(alpha = 0.45f)
            val templateBlend = (tabProgress - 1f).coerceIn(0f, 1f)
            val addFabAlpha = 1f - templateBlend
            val saveFabAlpha = templateBlend
            val addFabContainerColor = addFabBaseContainerColor.copy(alpha = addFabBaseContainerColor.alpha * addFabAlpha)
            val addFabContentColor = addFabBaseContentColor.copy(alpha = addFabBaseContentColor.alpha * addFabAlpha)
            val saveFabContainerColor = accentStrong.copy(alpha = saveFabAlpha)
            val saveFabContentColor = Color.Black.copy(alpha = saveFabAlpha)

            val addFabDefaultElevation = if (customMode) (6.dp * addFabAlpha) else 0.dp
            val addFabPressedElevation = if (customMode) (8.dp * addFabAlpha) else 0.dp
            val addFabElevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = addFabDefaultElevation,
                pressedElevation = addFabPressedElevation,
                focusedElevation = addFabDefaultElevation,
                hoveredElevation = addFabPressedElevation
            )
            val saveFabDefaultElevation = 6.dp * saveFabAlpha
            val saveFabPressedElevation = 8.dp * saveFabAlpha
            val saveFabElevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = saveFabDefaultElevation,
                pressedElevation = saveFabPressedElevation,
                focusedElevation = saveFabDefaultElevation,
                hoveredElevation = saveFabPressedElevation
            )

            val onQuestsFabClick = {
                if (selectedTab == 2) {
                    showSaveLibraryDialog = true
                } else if (customMode) {
                    SoundManager.playClick()
                    if (selectedTab == 0) {
                        editingDaily = null
                        showDailyEditor = true
                    } else {
                        editingMain = null
                        showMainEditor = true
                    }
                } else {
                    onRequireCustomMode()
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                FloatingActionButton(
                    onClick = onQuestsFabClick,
                    shape = CircleShape,
                    elevation = addFabElevation,
                    containerColor = addFabContainerColor,
                    contentColor = addFabContentColor
                ) {
                    Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(LocalContentColor.current)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(LocalContentColor.current)
                        )
                    }
                }
                FloatingActionButton(
                    onClick = onQuestsFabClick,
                    shape = CircleShape,
                    elevation = saveFabElevation,
                    containerColor = saveFabContainerColor,
                    contentColor = saveFabContentColor
                ) {
                    Icon(Icons.Default.Save, "Save template")
                }
            }
        }
    }

    // --- DIALOGS ---
    if (showExportNameDialog) {
        AlertDialog(onDismissRequest = { showExportNameDialog = false }, containerColor = CardDarkBlue, title = { Text("Share Template", color = accentStrong, fontWeight = FontWeight.Bold) }, text = { OutlinedTextField(value = exportName, onValueChange = { exportName = it }, label = { Text("Template Name", color = OnCardText.copy(alpha=0.6f)) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OnCardText, unfocusedTextColor = OnCardText, cursorColor = accentStrong)) }, confirmButton = { Button(onClick = { onExportTemplate(exportName.ifBlank { "My Custom MMO" }); showExportNameDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = accentStrong)) { Text("Export", color = Color.Black, fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = { showExportNameDialog = false }) { Text("Cancel", color = OnCardText) } })
    }

    if (showSaveLibraryDialog) {
        AlertDialog(onDismissRequest = { showSaveLibraryDialog = false }, containerColor = CardDarkBlue, title = { Text("Save to Template", color = accentStrong, fontWeight = FontWeight.Bold) }, text = { OutlinedTextField(value = saveLibraryName, onValueChange = { saveLibraryName = it }, label = { Text("Template Name", color = OnCardText.copy(alpha=0.6f)) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OnCardText, unfocusedTextColor = OnCardText, cursorColor = accentStrong)) }, confirmButton = { Button(onClick = { onSaveCurrentToLibrary(saveLibraryName.ifBlank { "My Custom MMO" }); showSaveLibraryDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = accentStrong)) { Text("Save", color = Color.Black, fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = { showSaveLibraryDialog = false }) { Text("Cancel", color = OnCardText) } })
    }

    if (templateToApply != null) {
        AlertDialog(
            onDismissRequest = { templateToApply = null }, containerColor = CardDarkBlue, title = { Text("Equip Template?", color = accentStrong, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Are you sure? This will apply theme, background, advanced options, and quests from this template.", color = OnCardText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Theme: ${templateToApply!!.appTheme.name} • Daily quests: ${templateToApply!!.dailyQuests.size}", color = OnCardText.copy(alpha = 0.8f), fontSize = 13.sp)
                    HorizontalDivider(color = OnCardText.copy(alpha=0.1f))

                    // Clear Existing Checkbox
                    Row(modifier = Modifier.fillMaxWidth().clickable { clearExistingBeforeApply = !clearExistingBeforeApply }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = clearExistingBeforeApply, onCheckedChange = { clearExistingBeforeApply = it }, colors = CheckboxDefaults.colors(checkedColor = accentStrong))
                        Text("Clear my current quests first", color = OnCardText, fontSize = 14.sp)
                    }

                    // Backup Checkbox
                    Row(modifier = Modifier.fillMaxWidth().clickable { backupBeforeApply = !backupBeforeApply }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = backupBeforeApply, onCheckedChange = { backupBeforeApply = it }, colors = CheckboxDefaults.colors(checkedColor = accentStrong))
                        Text("Backup current setup to Template", color = OnCardText, fontSize = 14.sp)
                    }
                    if (backupBeforeApply) { OutlinedTextField(value = backupName, onValueChange = { backupName = it }, label = { Text("Backup Name", color = OnCardText.copy(alpha=0.5f)) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OnCardText, unfocusedTextColor = OnCardText, cursorColor = accentStrong)) }
                }
            },
            confirmButton = { Button(onClick = { val finalBackupName = if (backupBeforeApply) backupName.ifBlank { "My Backup" } else null; onApplySavedTemplate(templateToApply!!, finalBackupName, clearExistingBeforeApply); templateToApply = null }, colors = ButtonDefaults.buttonColors(containerColor = accentStrong)) { Text("Equip", color = Color.Black, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { templateToApply = null }) { Text("Cancel", color = OnCardText) } }
        )
    }

    if (showDailyEditor) { AddEditQuestDialog(accentStrong, editingDaily, onSave = { onUpsertDaily(it); showDailyEditor = false }, onDismiss = { showDailyEditor = false }) }

    if (showMainEditor) {
        AddMainQuestDialog(
            accentStrong = accentStrong,
            editingQuest = editingMain,
            existingQuests = mainQuests,
            onSave = { onUpsertMain(it); showMainEditor = false },
            onDismiss = { showMainEditor = false }
        )
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false }, containerColor = CardDarkBlue, title = { Text("Equip Default Package?", color = OnCardText, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("This will add back any missing default quests to your current pool.", color = OnCardText.copy(alpha = 0.8f))
                    HorizontalDivider(color = OnCardText.copy(alpha=0.1f))
                    Row(modifier = Modifier.fillMaxWidth().clickable { restoreBackupBeforeApply = !restoreBackupBeforeApply }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = restoreBackupBeforeApply, onCheckedChange = { restoreBackupBeforeApply = it }, colors = CheckboxDefaults.colors(checkedColor = accentStrong))
                        Text("Backup current setup to Template", color = OnCardText, fontSize = 14.sp)
                    }
                    if (restoreBackupBeforeApply) { OutlinedTextField(value = restoreBackupName, onValueChange = { restoreBackupName = it }, label = { Text("Backup Name", color = OnCardText.copy(alpha=0.5f)) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OnCardText, unfocusedTextColor = OnCardText, cursorColor = accentStrong)) }
                }
            },
            confirmButton = { TextButton(onClick = { if (restoreBackupBeforeApply) { onSaveCurrentToLibrary(restoreBackupName.ifBlank { "My Backup" }) }; onRestoreDefaults(); showRestoreConfirm = false }) { Text("Equip", color = accentStrong) } },
            dismissButton = { TextButton(onClick = { showRestoreConfirm = false }) { Text("Cancel", color = OnCardText) } }
        )
    }

    if (deleteDailyId != null) { AlertDialog(onDismissRequest = { deleteDailyId = null }, containerColor = CardDarkBlue, title = { Text("Delete?", color = OnCardText) }, confirmButton = { TextButton(onClick = { onDeleteDaily(deleteDailyId!!); deleteDailyId = null }) { Text("Delete", color = Color.Red) } }, dismissButton = { TextButton(onClick = { deleteDailyId = null }) { Text("Cancel", color = OnCardText) } }) }
    if (deleteMainId != null) { AlertDialog(onDismissRequest = { deleteMainId = null }, containerColor = CardDarkBlue, title = { Text("Delete?", color = OnCardText) }, confirmButton = { TextButton(onClick = { onDeleteMain(deleteMainId!!); deleteMainId = null }) { Text("Delete", color = Color.Red) } }, dismissButton = { TextButton(onClick = { deleteMainId = null }) { Text("Cancel", color = OnCardText) } }) }

    if (templateToDelete != null) {
        AlertDialog(
            onDismissRequest = { templateToDelete = null },
            containerColor = CardDarkBlue,
            title = { Text("Delete Template?", color = OnCardText, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete '${templateToDelete!!.templateName}' from your Template list?", color = OnCardText.copy(alpha=0.8f)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSavedTemplate(templateToDelete!!)
                    templateToDelete = null
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { templateToDelete = null }) { Text("Cancel", color = OnCardText) } }
        )
    }
    if (showTutorial) {
        CoachmarkOverlay(
            steps = listOf(
                CoachStep(
                    title = "Daily Tab",
                    body = "Repeatable categories and daily quest pool.",
                    panelAlignment = Alignment.TopStart,
                    pointerAlignment = Alignment.TopCenter,
                    panelOffsetY = 24.dp,
                    pointerPosition = Offset(0.25f, 0.18f)
                ),
                CoachStep(
                    title = "Main Tab",
                    body = "Long-term quest packages and progression goals.",
                    panelAlignment = Alignment.CenterStart,
                    pointerAlignment = Alignment.Center,
                    pointerPosition = Offset(0.50f, 0.18f)
                ),
                CoachStep(
                    title = "Template Tab",
                    body = "Save/load builds and manage your challenge setups.",
                    panelAlignment = Alignment.BottomStart,
                    pointerAlignment = Alignment.BottomCenter,
                    pointerPosition = Offset(0.76f, 0.18f)
                )
            ),
            accent = accentStrong,
            onDone = onTutorialDismiss
        )
    }
}

@Composable
fun TabButton(
    text: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier,
    emphasis: Float = if (selected) 1f else 0f,
    alwaysShowBorder: Boolean = false,
    onClick: () -> Unit
) {
    val clamped = emphasis.coerceIn(0f, 1f)
    val textColor = lerp(OnCardText.copy(alpha = 0.5f), accent, clamped)
    Box(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
@Composable
fun SettingsTabChip(
    text: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    emphasis: Float = if (selected) 1f else 0f,
    onClick: () -> Unit
) {
    val animated = if (selected) 1f else emphasis.coerceIn(0f, 1f)
    val shape = RoundedCornerShape(12.dp)
    val textColor = lerp(OnCardText.copy(alpha = 0.42f), accent.copy(alpha = 0.98f), animated)
    Box(
        modifier = modifier
            .clip(shape)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = if (animated > 0.5f) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NeonPaletteChips(
    selected: String,
    accentStrong: Color,
    onSelect: (String) -> Unit
) {
    val options = listOf(
        "magenta" to "Magenta",
        "cyan" to "Cyan",
        "violet" to "Violet",
        "sunset" to "Sunset",
        "lime" to "Lime",
        "ice" to "Ice"
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (key, label) ->
            FilterChip(
                selected = selected.equals(key, ignoreCase = true),
                onClick = { onSelect(key) },
                label = { Text(label) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(neonPaletteColor(key, ThemeRuntime.neonLightBoostEnabled))
                            .border(1.dp, accentStrong.copy(alpha = 0.35f), CircleShape)
                    )
                }
            )
        }
    }
}
@Composable
fun PoolMainQuestRow(mq: CustomMainQuest, accentSoft: Color, customMode: Boolean, onEdit: () -> Unit, onDelete: () -> Unit, onToggle: (Boolean) -> Unit) {
    val pNeonEnabled = ThemeRuntime.neonFlowEnabled || ThemeRuntime.decorativeBordersEnabled
    val pBoostedNeon = ThemeRuntime.neonLightBoostEnabled
    val pNeonBrush = if (pNeonEnabled) rememberNeonBorderBrush(accentSoft, neonPaletteColor(ThemeRuntime.neonGlowPalette, pBoostedNeon)) else null
    val pShape = RoundedCornerShape(12.dp)
    Row(modifier = Modifier.fillMaxWidth().alpha(if (customMode) 1f else 0.72f).clip(pShape).background(CardDarkBlue).then(if (pNeonEnabled && pNeonBrush != null) Modifier.border(if (pBoostedNeon) 2.dp else 1.5.dp, pNeonBrush, pShape) else Modifier).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        // 1. The Checkbox
        Checkbox(
            checked = mq.isActive,
            onCheckedChange = { onToggle(it) },
            colors = CheckboxDefaults.colors(checkedColor = accentSoft, uncheckedColor = OnCardText.copy(alpha=0.3f))
        )
        Spacer(Modifier.width(8.dp))

        // 2. The Icon / Image
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(if(mq.isActive) accentSoft.copy(alpha=0.2f) else Color.Black.copy(alpha=0.2f)).border(1.dp, if(mq.isActive) accentSoft else Color.Transparent, CircleShape), contentAlignment = Alignment.Center) {
            if (!mq.imageUri.isNullOrBlank()) {
                AsyncImage(model = mq.imageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text(mq.icon, color = if (mq.isActive) OnCardText else OnCardText.copy(alpha = 0.45f), fontSize = 16.sp)
            }
        }
        Spacer(Modifier.width(12.dp))

        // 3. The Text
        Column(modifier = Modifier.weight(1f)) {
            Text(mq.title, color = if(mq.isActive) OnCardText else OnCardText.copy(alpha=0.5f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("${mq.xpReward} XP • ${mq.steps.size} Steps", color = OnCardText.copy(alpha = 0.5f), fontSize = 12.sp)
        }

        // 4. Buttons
        IconButton(onClick = onEdit, enabled = customMode) { Icon(Icons.Default.Edit, null, tint = if (customMode) OnCardText else OnCardText.copy(alpha = 0.3f)) }
        IconButton(onClick = onDelete, enabled = customMode) { Icon(Icons.Default.Delete, null, tint = if (customMode) Color.Red.copy(alpha=0.6f) else OnCardText.copy(alpha = 0.3f)) }
    }
}
@Composable
fun TemplateLibraryCard(
    template: GameTemplate,
    isActive: Boolean,
    accentStrong: Color,
    accentSoft: Color,
    uiScale: Float = 1f, // NEW: Accepts scale factor
    onToggle: (Boolean) -> Unit,
    onDelete: (() -> Unit)?
) {
    val libNeonBrush = rememberNeonBorderBrush(accentStrong, neonPaletteColor(ThemeRuntime.neonGlowPalette, ThemeRuntime.neonLightBoostEnabled))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp * uiScale))
            .background(CardDarkBlue)
            .border(1.5.dp, libNeonBrush, RoundedCornerShape(12.dp * uiScale))
            .clickable { onToggle(!isActive) }
            .padding(12.dp * uiScale),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox
        Checkbox(
            checked = isActive,
            onCheckedChange = { onToggle(it) },
            colors = CheckboxDefaults.colors(checkedColor = accentStrong, uncheckedColor = OnCardText.copy(alpha=0.5f)),
            modifier = Modifier.scale(uiScale) // Scale the checkbox too!
        )

        Spacer(Modifier.width(12.dp * uiScale))

        // Icon Box
        Box(modifier = Modifier.size(40.dp * uiScale).clip(RoundedCornerShape(10.dp * uiScale)).background(if(isActive) accentSoft.copy(alpha=0.2f) else Color.Black.copy(alpha=0.3f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Inventory, null, tint = if(isActive) accentSoft else OnCardText.copy(alpha=0.4f), modifier = Modifier.size(24.dp * uiScale))
        }

        Spacer(Modifier.width(12.dp * uiScale))

        // Text Info
        Column(modifier = Modifier.weight(1f)) {
            Text(template.templateName, color = if(isActive) OnCardText else OnCardText.copy(alpha=0.5f), fontWeight = FontWeight.Bold, fontSize = (14f * uiScale).sp)
            Text("${template.dailyQuests.size} Daily • ${template.mainQuests.size} Main", color = OnCardText.copy(alpha = 0.5f), fontSize = (11f * uiScale).sp)
        }

        // STAMP vs DELETE
        if (onDelete != null) {
            // User Template -> Delete Button
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp * uiScale)) {
                Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha=0.6f), modifier = Modifier.size(24.dp * uiScale))
            }
        } else {
            // System Template -> Clean, Big, Scalable "DEFAULT" Stamp
            Box(modifier = Modifier.padding(end = 8.dp * uiScale).rotate(-10f)) {
                Text(
                    text = "DEFAULT",
                    color = OnCardText.copy(alpha = 0.2f), // Slightly more visible since shadow is gone
                    fontWeight = FontWeight.Black,
                    fontSize = (16f * uiScale).sp, // BIGGER and SCALED
                    letterSpacing = 2.sp,
                    style = androidx.compose.ui.text.TextStyle(platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false))
                )
            }
        }
    }
}
@Composable
fun QuestCategoryHeader(
    cat: QuestCategory,
    items: List<CustomTemplate>,
    accentSoft: Color,
    customMode: Boolean,
    onEdit: (CustomTemplate) -> Unit,
    onDelete: (CustomTemplate) -> Unit,
    onToggle: (CustomTemplate, Boolean) -> Unit // NEW PARAMETER
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)
    val categoryNeonBrush = rememberNeonBorderBrush(accentSoft, neonPaletteColor(ThemeRuntime.neonGlowPalette, ThemeRuntime.neonLightBoostEnabled))
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardDarkBlue)
                .border(1.5.dp, categoryNeonBrush, RoundedCornerShape(12.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 12.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(cat.name, color = OnCardText, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            Box(modifier = Modifier.clip(CircleShape).background(OnCardText.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 2.dp)) { Text("${items.size}", color = OnCardText, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(10.dp))
            Icon(Icons.Default.KeyboardArrowDown, null, tint = OnCardText, modifier = Modifier.rotate(rotation))
        }
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                items.sortedWith(compareBy<CustomTemplate> { it.difficulty }.thenBy { it.title }).forEach { t ->
                    CustomTemplateRow(
                        t = t,
                        accentSoft = accentSoft,
                        customMode = customMode,
                        onEdit = { onEdit(t) },
                        onDelete = { onDelete(t) },
                        onToggle = { active -> onToggle(t, active) } // Pass the toggle up!
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomTemplateRow(t: CustomTemplate, accentSoft: Color, customMode: Boolean, onEdit: () -> Unit, onDelete: () -> Unit, onToggle: (Boolean) -> Unit) {
    val rowNeonBrush = rememberNeonBorderBrush(accentSoft, neonPaletteColor(ThemeRuntime.neonGlowPalette, ThemeRuntime.neonLightBoostEnabled))
    val rowInteraction = remember { MutableInteractionSource() }
    val rowPressed by rowInteraction.collectIsPressedAsState()
    val holdPulse by rememberInfiniteTransition(label = "daily_hold_pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(animation = tween(650), repeatMode = RepeatMode.Reverse),
        label = "daily_hold_pulse_value"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (customMode) 1f else 0.72f)
            .clip(RoundedCornerShape(12.dp))
            .background(CardDarkBlue)
            .border(1.5.dp, rowNeonBrush, RoundedCornerShape(12.dp))
            .graphicsLayer {
                val pressedScale = if (rowPressed) holdPulse else 1f
                scaleX = pressedScale
                scaleY = pressedScale
                alpha = if (rowPressed) 0.92f else 1f
            }
            .combinedClickable(
                enabled = customMode,
                interactionSource = rowInteraction,
                indication = null,
                onClick = { },
                onLongClick = { SoundManager.playClick() }
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. The Checkbox
        Checkbox(
            checked = t.isActive,
            onCheckedChange = { onToggle(it) },
            colors = CheckboxDefaults.colors(checkedColor = accentSoft, uncheckedColor = OnCardText.copy(alpha=0.3f))
        )
        Spacer(Modifier.width(8.dp))

        // 2. The Icon
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(if(t.isActive) accentSoft else Color.Black.copy(alpha=0.2f)), contentAlignment = Alignment.Center) {
            Text(t.icon, fontSize = 18.sp, color = if(t.isActive) Color.Black else OnCardText.copy(alpha=0.5f))
        }
        Spacer(Modifier.width(12.dp))

        // 3. The Text
        Column(modifier = Modifier.weight(1f)) {
            Text(t.title, color = if(t.isActive) OnCardText else OnCardText.copy(alpha=0.5f), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("Tier ${t.difficulty} • ${t.xp} XP", color = OnCardText.copy(alpha = 0.5f), fontSize = 12.sp)
        }

        // 4. Buttons
        IconButton(onClick = onEdit, enabled = customMode) { Icon(Icons.Default.Edit, null, tint = if (customMode) OnCardText else OnCardText.copy(alpha = 0.3f)) }
        IconButton(onClick = onDelete, enabled = customMode) { Icon(Icons.Default.Delete, null, tint = if (customMode) OnCardText else OnCardText.copy(alpha = 0.3f)) }
    }
}

@Composable
fun AdminMainQuestRow(mq: CustomMainQuest, accentSoft: Color, adminMode: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val amNeonEnabled = ThemeRuntime.neonFlowEnabled || ThemeRuntime.decorativeBordersEnabled
    val amBoostedNeon = ThemeRuntime.neonLightBoostEnabled
    val amNeonBrush = if (amNeonEnabled) rememberNeonBorderBrush(accentSoft, neonPaletteColor(ThemeRuntime.neonGlowPalette, amBoostedNeon)) else null
    val amShape = RoundedCornerShape(12.dp)
    Row(modifier = Modifier.fillMaxWidth().clip(amShape).background(CardDarkBlue).then(if (amNeonEnabled && amNeonBrush != null) Modifier.border(if (amBoostedNeon) 2.dp else 1.5.dp, amNeonBrush, amShape) else Modifier).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(accentSoft.copy(alpha=0.2f)).border(1.dp, accentSoft, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Star, null, tint = accentSoft, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(mq.title, color = OnCardText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("${mq.xpReward} XP • ${mq.steps.size} Steps", color = OnCardText.copy(alpha = 0.7f), fontSize = 12.sp)
        }
        if (adminMode) {
            // NOTE: Using Add icon as edit placeholder since we re-use the AddMainQuestDialog
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha=0.6f)) }
        }
    }
}
// === UPDATED EDITOR (With Pin Toggle) ===
@Composable
fun AddEditQuestDialog(accentStrong: Color, initial: CustomTemplate?, onSave: (CustomTemplate) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var icon by remember { mutableStateOf(initial?.icon ?: "⭐") }
    var category by remember { mutableStateOf(initial?.category ?: QuestCategory.FITNESS) }
    var difficulty by remember { mutableIntStateOf(initial?.difficulty ?: 1) }
    var xp by remember { mutableIntStateOf(initial?.xp ?: 20) }
    var target by remember { mutableIntStateOf(initial?.target ?: 1) }
    var isPinned by remember { mutableStateOf(initial?.isPinned ?: false) }

    // NEW: Image Picker Logic
    var imageUri by remember { mutableStateOf(initial?.imageUri) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            imageUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = CardDarkBlue,
        title = { Text(if (initial == null) "New Daily Quest" else "Edit Daily Quest", fontWeight = FontWeight.Bold, color = OnCardText) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Quest name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OnCardText,
                        unfocusedTextColor = OnCardText,
                        cursorColor = accentStrong
                    )
                )

                Text("Icon / Image", color = OnCardText.copy(alpha = 0.75f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(OnCardText.copy(alpha = 0.10f))
                            .border(1.dp, OnCardText.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                            .clickable { imagePicker.launch(arrayOf("image/*")) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUri != null) {
                            AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Icon(Icons.Default.AddPhotoAlternate, null, tint = OnCardText)
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = icon,
                            onValueChange = { icon = it.take(4) },
                            label = { Text("Icon fallback") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = OnCardText,
                                unfocusedTextColor = OnCardText,
                                cursorColor = accentStrong
                            )
                        )
                        if (imageUri != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Image selected", color = accentStrong, fontSize = 11.sp)
                                TextButton(onClick = { imageUri = null }, contentPadding = PaddingValues(0.dp)) {
                                    Text("Remove", color = Color(0xFFE57373), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Text("Category", color = OnCardText.copy(alpha = 0.75f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                CategoryChips(selected = category, accentStrong = accentStrong, onSelect = { category = it })

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SubtlePanel.copy(alpha = 0.6f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Target Count: $target", color = OnCardText, fontSize = 12.sp)
                    Slider(
                        value = target.toFloat(),
                        onValueChange = { target = it.toInt() },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(thumbColor = accentStrong, activeTrackColor = accentStrong)
                    )
                    Text(
                        if (target == 1) "Standard quest" else "Counter quest (e.g. 8 cups)",
                        color = OnCardText.copy(alpha = 0.58f),
                        fontSize = 10.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SubtlePanel.copy(alpha = 0.45f))
                        .clickable { isPinned = !isPinned }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Checkbox(checked = isPinned, onCheckedChange = { isPinned = it }, colors = CheckboxDefaults.colors(checkedColor = accentStrong))
                    Text("Critical Daily (Always appear)", color = if (isPinned) accentStrong else OnCardText, fontSize = 13.sp)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SubtlePanel.copy(alpha = 0.45f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Difficulty tier", color = OnCardText, modifier = Modifier.weight(1f))
                        IconButton(onClick = { difficulty = (difficulty - 1).coerceAtLeast(1) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = accentStrong)
                        }
                        Text("Tier $difficulty", color = accentStrong, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        IconButton(onClick = { difficulty = (difficulty + 1).coerceAtMost(5) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = accentStrong)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("XP reward", color = OnCardText, modifier = Modifier.weight(1f))
                        IconButton(onClick = { xp = (xp - 5).coerceAtLeast(5) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = accentStrong)
                        }
                        Text("$xp XP", color = accentStrong, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        IconButton(onClick = { xp = (xp + 5).coerceAtMost(500) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = accentStrong)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // FIXED: If target is 1 (Standard), save it as 2 to enable "Start -> Done -> Claim" flow.
                val safeTarget = if (target == 1) 2 else target

                onSave(CustomTemplate(
                    id = initial?.id ?: UUID.randomUUID().toString(),
                    category = category,
                    difficulty = difficulty,
                    title = title.trim(),
                    icon = icon.trim(),
                    xp = xp,
                    target = safeTarget, // Use the fixed target
                    isPinned = isPinned,
                    imageUri = imageUri
                ))
            }) {
                Text("Save", color = accentStrong)
            }
        },        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = OnCardText) } }
    )
}
@OptIn(ExperimentalLayoutApi::class) @Composable fun CategoryChips(selected: QuestCategory, accentStrong: Color, onSelect: (QuestCategory) -> Unit) { FlowRow(modifier = Modifier.fillMaxWidth(), maxItemsInEachRow = 3, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { QuestCategory.entries.forEach { c -> Box(modifier = Modifier.height(32.dp).clip(RoundedCornerShape(99.dp)).background(if (c == selected) accentStrong.copy(alpha=0.4f) else Color.Transparent).border(1.dp, if(c==selected) accentStrong else OnCardText.copy(alpha=0.3f), RoundedCornerShape(99.dp)).clickable { onSelect(c) }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) { Text(c.name, color = OnCardText, fontSize = 11.sp) } } } }
@Composable
fun rememberNeonBorderBrush(primary: Color, secondary: Color = Color(0xFFFF2E97)): Brush {
    val neonEnabled = ThemeRuntime.neonFlowEnabled || ThemeRuntime.decorativeBordersEnabled
    if (!neonEnabled) {
        return Brush.linearGradient(
            colors = listOf(
                OnCardText.copy(alpha = 0.22f),
                OnCardText.copy(alpha = 0.22f)
            )
        )
    }
    val lightTheme = ThemeRuntime.currentTheme.isLightCategory()
    val boosted = ThemeRuntime.neonLightBoostEnabled
    val flowEnabled = ThemeRuntime.neonFlowEnabled
    val speed = ThemeRuntime.neonFlowSpeed.coerceIn(0, 2)
    val paletteGlow = neonPaletteColor(ThemeRuntime.neonGlowPalette, boosted)
    val glowColor = if (ThemeRuntime.neonGlowPalette.isBlank()) secondary else paletteGlow
    val phaseDuration = when (speed) {
        0 -> if (boosted) 2600 else 3200
        2 -> if (boosted) 1500 else 1900
        else -> if (boosted) 2100 else 2600
    }
    val glowDuration = when (speed) {
        0 -> if (boosted) 1800 else 2200
        2 -> if (boosted) 940 else 1180
        else -> if (boosted) 1400 else 1700
    }
    val neonTransition = if (ThemeRuntime.reduceAnimationsEnabled || !flowEnabled) null else rememberInfiniteTransition(label = "neon_border_flow")
    val phase = neonTransition?.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = phaseDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "neon_phase"
    )?.value ?: 0f
    val glowAlpha = neonTransition?.animateFloat(
        initialValue = when {
            lightTheme && boosted -> 0.44f
            lightTheme -> 0.36f
            boosted -> 0.72f
            else -> 0.58f
        },
        targetValue = when {
            lightTheme && boosted -> 0.66f
            lightTheme -> 0.56f
            boosted -> 1f
            else -> 0.92f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = glowDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "neon_glow"
    )?.value ?: when {
        lightTheme && boosted -> 0.54f
        lightTheme -> 0.44f
        boosted -> 0.86f
        else -> 0.76f
    }
    val angle = phase * 360f
    val radians = angle * (kotlin.math.PI.toFloat() / 180f)
    val axisX = kotlin.math.cos(radians)
    val axisY = kotlin.math.sin(radians)
    val travel = when {
        lightTheme && boosted -> 620f
        lightTheme -> 520f
        boosted -> 740f
        else -> 600f
    }
    val drift = when {
        lightTheme && boosted -> 180f
        lightTheme -> 145f
        boosted -> 220f
        else -> 170f
    }
    val start = Offset(
        x = (-axisX * travel) + (axisY * drift),
        y = (-axisY * travel) - (axisX * drift)
    )
    val end = Offset(
        x = (axisX * travel) - (axisY * drift),
        y = (axisY * travel) + (axisX * drift)
    )
    val highlight = Color.White.copy(
        alpha = when {
            lightTheme && boosted -> 0.42f
            lightTheme -> 0.34f
            boosted -> 0.74f
            else -> 0.48f
        }
    )
    val leadAlpha = when {
        lightTheme && boosted -> 0.24f
        lightTheme -> 0.18f
        boosted -> 0.42f
        else -> 0.30f
    }
    val coreAlpha = when {
        lightTheme -> 0.74f
        boosted -> 1f
        else -> 0.9f
    }
    return Brush.linearGradient(
        colors = listOf(
            primary.copy(alpha = leadAlpha),
            glowColor.copy(alpha = glowAlpha),
            highlight,
            primary.copy(alpha = coreAlpha),
            glowColor.copy(alpha = glowAlpha),
            highlight.copy(alpha = highlight.alpha * 0.55f),
            primary.copy(alpha = leadAlpha)
        ),
        start = start,
        end = end
    )
}
@Composable
fun CardBlock(content: @Composable ColumnScope.() -> Unit) {
    val neonBordersEnabled = ThemeRuntime.neonFlowEnabled || ThemeRuntime.decorativeBordersEnabled
    val lightTheme = ThemeRuntime.currentTheme.isLightCategory()
    val boostedNeon = ThemeRuntime.neonLightBoostEnabled
    val neonSecondary = neonPaletteColor(ThemeRuntime.neonGlowPalette, boostedNeon)
    val neonActive = neonBordersEnabled
    val neonPrimary = mixForBackground(ThemeEngine.getColors(ThemeRuntime.currentTheme).first, CardDarkBlue)
    val neonBrush = if (neonActive) rememberNeonBorderBrush(neonPrimary, neonSecondary) else null
    val cardBorder = when {
        !neonBordersEnabled -> Color.Transparent
        boostedNeon -> neonPrimary.copy(alpha = if (lightTheme) 0.56f else 0.82f)
        ThemeRuntime.currentTheme.isLightCategory() -> OnCardText.copy(alpha = 0.18f)
        else -> OnCardText.copy(alpha = 0.24f)
    }
    val borderWidth = if (neonBordersEnabled) {
        if (neonActive) {
            if (boostedNeon) {
                if (lightTheme) 1.5.dp else 2.1.dp
            } else {
                if (lightTheme) 1.1.dp else 1.6.dp
            }
        } else {
            1.dp
        }
    } else 0.dp
    val cardShape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(CardDarkBlue)
            .then(
                if (neonActive && borderWidth > 0.dp) {
                    Modifier.border(borderWidth, neonBrush!!, cardShape)
                } else {
                    Modifier.border(borderWidth, cardBorder, cardShape)
                }
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content
    )
}
@Composable fun SettingsSubheading(text: String) { Text(text, color = OnCardText.copy(alpha = 0.55f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, modifier = Modifier.padding(top = 4.dp)) }
@Composable fun SettingsHint(text: String) { Text(text, color = OnCardText.copy(alpha = 0.55f), fontSize = 11.sp, lineHeight = 13.sp, modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)) }
@Composable fun SettingsDivider() { HorizontalDivider(color = OnCardText.copy(alpha = 0.12f), modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)) }
private data class IntroStyleThemePreset(
    val id: String,
    val name: String,
    val description: String,
    val theme: AppTheme,
    val accent: Color,
    val previewBg: Color,
    val previewCard: Color,
    val previewText: Color
)
private data class AdvancedThemePreset(
    val name: String,
    val subtitle: String,
    val text: Color,
    val appBg: Color,
    val chromeBg: Color,
    val cardBg: Color,
    val button: Color,
    val journalPage: Color,
    val journalAccent: Color
)
@Composable
private fun AdvancedThemePresetRow(
    preset: AdvancedThemePreset,
    onApply: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SubtlePanel)
            .clickable { onApply() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(preset.name, color = OnCardText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(preset.subtitle, color = OnCardText.copy(alpha = 0.72f), fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(preset.appBg, preset.cardBg, preset.button, preset.journalAccent).forEach { swatch ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(swatch)
                            .border(1.dp, OnCardText.copy(alpha = 0.18f), CircleShape)
                    )
                }
            }
        }
    }
}
@Composable
fun AdvancedColorMiniPreview(
    textColor: Color,
    appBackground: Color,
    chromeBackground: Color,
    cardBackground: Color,
    buttonColor: Color,
    journalPageBackground: Color,
    journalAccent: Color
) {
    val chromeText = readableTextColor(chromeBackground)
    val cardText = readableTextColor(cardBackground)
    val buttonText = readableTextColor(buttonColor)
    val journalText = readableTextColor(journalPageBackground)
    val journalRibbon = mixForBackground(journalAccent, journalPageBackground)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(appBackground)
            .border(1.dp, OnCardText.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(chromeBackground)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Daily Quests", color = chromeText, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(buttonColor)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("START", color = buttonText, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(cardBackground)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(mixForBackground(buttonColor, cardBackground))
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Quest card title", color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("16 XP", color = cardText.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("CLAIM", color = buttonColor, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(journalPageBackground)
                .border(1.dp, journalAccent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Journal", color = journalText, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(journalRibbon)
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text("Log", color = readableTextColor(journalRibbon), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
private fun formatColorHex(color: Color): String {
    val r = (color.red * 255f).roundToInt().coerceIn(0, 255)
    val g = (color.green * 255f).roundToInt().coerceIn(0, 255)
    val b = (color.blue * 255f).roundToInt().coerceIn(0, 255)
    return String.format(Locale.US, "#%02X%02X%02X", r, g, b)
}
private fun parseHexColor(raw: String): Color? {
    val clean = raw.trim().removePrefix("#")
    if (clean.length != 6 && clean.length != 8) return null
    val value = clean.toLongOrNull(16) ?: return null
    val argb = if (clean.length == 6) (0xFF000000L or value) else value
    return Color(argb.toInt())
}
@Composable
fun ColorPickerControlRow(
    label: String,
    value: Color?,
    onAuto: () -> Unit,
    onPick: () -> Unit,
    autoLabel: String = "Auto",
    pickEnabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(value ?: SubtlePanel)
                .border(1.dp, OnCardText.copy(alpha = 0.25f), CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = OnCardText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(value?.let { formatColorHex(it) } ?: "Auto", color = OnCardText.copy(alpha = 0.7f), fontSize = 11.sp)
        }
        TextButton(onClick = { SoundManager.playClick(); onAuto() }) { Text(autoLabel, color = OnCardText.copy(alpha = 0.82f)) }
        OutlinedButton(onClick = { SoundManager.playClick(); onPick() }, enabled = pickEnabled) {
            Text("Pick", color = if (pickEnabled) OnCardText else OnCardText.copy(alpha = 0.45f))
        }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorTuneDialog(
    title: String,
    initialColor: Color,
    presets: List<Pair<String, Color>> = emptyList(),
    onDismiss: () -> Unit,
    onApply: (Color) -> Unit
) {
    fun colorToHsv(color: Color): FloatArray {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(color.toArgbCompat(), hsv)
        return hsv
    }
    fun hsvToColor(hue: Float, saturation: Float, value: Float): Color {
        val hsv = floatArrayOf(
            hue.coerceIn(0f, 360f),
            saturation.coerceIn(0f, 1f),
            value.coerceIn(0f, 1f)
        )
        return Color(AndroidColor.HSVToColor(hsv))
    }
    fun buildHsvWheelImage(size: Int, value: Float): ImageBitmap {
        val safeSize = size.coerceAtLeast(64)
        val bitmap = createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(safeSize * safeSize)
        val center = (safeSize - 1) / 2f
        val radius = center
        var index = 0
        for (y in 0 until safeSize) {
            val dy = y - center
            for (x in 0 until safeSize) {
                val dx = x - center
                val dist = sqrt(dx * dx + dy * dy)
                if (dist <= radius) {
                    val saturation = (dist / radius).coerceIn(0f, 1f)
                    val hue = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360.0) % 360.0).toFloat()
                    pixels[index] = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value.coerceIn(0f, 1f)))
                } else {
                    pixels[index] = AndroidColor.TRANSPARENT
                }
                index++
            }
        }
        bitmap.setPixels(pixels, 0, safeSize, 0, 0, safeSize, safeSize)
        return bitmap.asImageBitmap()
    }

    val initialHsv = remember(initialColor) { colorToHsv(initialColor) }
    var hue by remember(initialColor) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(initialColor) { mutableFloatStateOf(initialHsv[1]) }
    var value by remember(initialColor) { mutableFloatStateOf(initialHsv[2]) }
    var hexInput by remember(initialColor) { mutableStateOf(formatColorHex(initialColor)) }
    var wheelSizePx by remember { mutableFloatStateOf(0f) }

    fun currentColor(): Color = hsvToColor(hue, saturation, value)
    fun applyColor(color: Color) {
        val hsv = colorToHsv(color)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
        hexInput = formatColorHex(color)
    }
    fun updateFromWheel(point: Offset) {
        val size = wheelSizePx
        if (size <= 0f) return
        val center = size / 2f
        val radius = size / 2f
        var dx = point.x - center
        var dy = point.y - center
        val distance = sqrt(dx * dx + dy * dy)
        if (distance > radius && distance > 0f) {
            val scale = radius / distance
            dx *= scale
            dy *= scale
        }
        saturation = (sqrt(dx * dx + dy * dy) / radius).coerceIn(0f, 1f)
        hue = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360.0) % 360.0).toFloat()
        hexInput = formatColorHex(currentColor())
    }

    val wheelBitmap = remember(value) { buildHsvWheelImage(300, value) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDarkBlue,
        title = { Text(title, color = OnCardText, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { wheelSizePx = it.width.toFloat() }
                            .clip(CircleShape)
                            .pointerInput(wheelSizePx) { detectTapGestures { updateFromWheel(it) } }
                            .pointerInput(wheelSizePx) {
                                detectDragGestures(onDragStart = { updateFromWheel(it) }) { change, _ ->
                                    updateFromWheel(change.position)
                                }
                            }
                    ) {
                        drawImage(wheelBitmap, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()))
                        val radius = size.minDimension / 2f
                        val angle = Math.toRadians(hue.toDouble()).toFloat()
                        val markerRadius = radius * saturation
                        val marker = Offset(
                            x = center.x + cos(angle) * markerRadius,
                            y = center.y + sin(angle) * markerRadius
                        )
                        drawCircle(
                            color = OnCardText.copy(alpha = 0.32f),
                            radius = radius,
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                        )
                        drawCircle(color = Color.White, radius = 8f, center = marker)
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.45f),
                            radius = 8f,
                            center = marker,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2f)
                        )
                    }
                }
                Text("Value ${ (value * 100f).roundToInt() }%", color = OnCardText.copy(alpha = 0.8f), fontSize = 11.sp)
                Slider(
                    value = value,
                    onValueChange = {
                        value = it.coerceIn(0f, 1f)
                        hexInput = formatColorHex(currentColor())
                    },
                    valueRange = 0f..1f
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(currentColor())
                        .border(1.dp, OnCardText.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                )
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { raw ->
                        val cleaned = raw.uppercase(Locale.US).take(9)
                        hexInput = cleaned
                        parseHexColor(cleaned)?.let { applyColor(it) }
                    },
                    singleLine = true,
                    label = { Text("Hex (#RRGGBB)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OnCardText,
                        unfocusedTextColor = OnCardText,
                        cursorColor = OnCardText
                    )
                )
                if (presets.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        presets.forEach { (name, color) ->
                            FilterChip(
                                selected = formatColorHex(currentColor()) == formatColorHex(color),
                                onClick = { applyColor(color) },
                                label = { Text(name) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(currentColor()) }) {
                Text("Apply", color = OnCardText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = OnCardText) }
        }
    )
}
@Composable
fun SettingsGroupHeader(title: String, expanded: Boolean, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "settings_group_arrow")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onToggle() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = OnCardText.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Icon(
            Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = OnCardText.copy(alpha = 0.7f),
            modifier = Modifier.rotate(rotation)
        )
    }
}
@Composable
fun FontStyleSelector(selected: AppFontStyle, onSelect: (AppFontStyle) -> Unit) {
    val options = listOf(
        AppFontStyle.DEFAULT to "Default",
        AppFontStyle.SANS to "Sans",
        AppFontStyle.SERIF to "Serif",
        AppFontStyle.MONO to "Mono",
        AppFontStyle.DISPLAY to "Display",
        AppFontStyle.ROUNDED to "Rounded",
        AppFontStyle.TERMINAL to "Terminal",
        AppFontStyle.ELEGANT to "Elegant",
        AppFontStyle.HANDWRITTEN to "Handwritten"
    )
    var localIndex by remember(selected) {
        mutableIntStateOf(options.indexOfFirst { it.first == selected }.coerceAtLeast(0))
    }
    SettingsSelectorRow(
        title = "Font Style",
        valueLabel = options[localIndex].second,
        onPrev = { localIndex = (localIndex - 1 + options.size) % options.size; onSelect(options[localIndex].first) },
        onNext = { localIndex = (localIndex + 1) % options.size; onSelect(options[localIndex].first) }
    )
}
@Composable
fun LanguageSelector(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(
        "system" to "System Default",
        "en" to "English",
        "es" to "Español",
        "ar" to "العربية"
    )
    var localIndex by remember(selected) {
        mutableIntStateOf(options.indexOfFirst { it.first == selected }.coerceAtLeast(0))
    }
    val savedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val isDirty = localIndex != savedIndex
    SettingsSelectorRow(
        title = "Language",
        valueLabel = options[localIndex].second,
        onPrev = { localIndex = (localIndex - 1 + options.size) % options.size },
        onNext = { localIndex = (localIndex + 1) % options.size }
    )
    if (isDirty) {
        Button(
            onClick = { SoundManager.playClick(); onSelect(options[localIndex].first) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("Apply")
        }
    }
}
@Composable
private fun SettingsArrowChip(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val isLightTheme = ThemeRuntime.currentTheme.isLightCategory()
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(
                if (enabled) {
                    if (isLightTheme) Color(0x1F000000) else OnCardText.copy(alpha = 0.12f)
                } else {
                    OnCardText.copy(alpha = 0.06f)
                }
            )
            .clickable(enabled = enabled) {
                SoundManager.playClick()
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) OnCardText else OnCardText.copy(alpha = 0.34f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SettingsSelectorRow(
    title: String,
    valueLabel: String,
    enabled: Boolean = true,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onRowClick: (() -> Unit)? = null
) {
    val controlShape = RoundedCornerShape(999.dp)
    val isLightTheme = ThemeRuntime.currentTheme.isLightCategory()
    val verticalPad = if (ThemeRuntime.largerTouchTargetsEnabled) 12.dp else 8.dp
    val controlBg = if (isLightTheme) Color(0x17000000) else Color.Black.copy(alpha = 0.30f)
    val controlBorder = OnCardText.copy(alpha = if (isLightTheme) 0.12f else 0.18f)
    val rowClickModifier = if (onRowClick != null) {
        Modifier.clickable(enabled = enabled) {
            SoundManager.playClick()
            onRowClick()
        }
    } else {
        Modifier
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = verticalPad)
            .heightIn(min = 48.dp)
            .then(rowClickModifier)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            title,
            color = if (enabled) OnCardText else OnCardText.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier
                .widthIn(min = 170.dp, max = 228.dp)
                .clip(controlShape)
                .background(controlBg)
                .border(1.dp, controlBorder, controlShape)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SettingsArrowChip(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                enabled = enabled
            ) { onPrev() }
            Text(
                valueLabel,
                color = if (enabled) OnCardText else OnCardText.copy(alpha = 0.45f),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            SettingsArrowChip(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                enabled = enabled
            ) { onNext() }
        }
    }
}

@Composable
fun SettingsStepperRow(
    title: String,
    valueLabel: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    enabled: Boolean = true
) {
    SettingsSelectorRow(
        title = title,
        valueLabel = valueLabel,
        enabled = enabled,
        onPrev = onDecrement,
        onNext = onIncrement
    )
}

@Composable
fun SettingsDropdownRow(
    title: String,
    valueLabel: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    menuContent: @Composable ColumnScope.() -> Unit
) {
    val fieldShape = RoundedCornerShape(12.dp)
    val isLightTheme = ThemeRuntime.currentTheme.isLightCategory()
    val fieldBg = if (isLightTheme) Color(0x17000000) else Color.Black.copy(alpha = 0.30f)
    val fieldBorder = OnCardText.copy(alpha = if (isLightTheme) 0.12f else 0.18f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .heightIn(min = 48.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            title,
            color = OnCardText,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Box(modifier = Modifier.widthIn(min = 170.dp, max = 228.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(fieldShape)
                    .background(fieldBg)
                    .border(1.dp, fieldBorder, fieldShape)
                    .clickable {
                        SoundManager.playClick()
                        onExpandedChange(true)
                    }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    valueLabel,
                    color = OnCardText,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Open options",
                    tint = accentForTheme().copy(alpha = 0.92f)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.background(CardDarkBlue)
            ) {
                menuContent()
            }
        }
    }
}

@Composable
fun SettingRow(title: String, value: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    SettingsSelectorRow(
        title = title,
        valueLabel = if (value) "On" else "Off",
        enabled = enabled,
        onPrev = { if (value) onChange(false) },
        onNext = { if (!value) onChange(true) },
        onRowClick = { onChange(!value) }
    )
}
@Composable fun AchievementRow(a: Achievement, unlocked: Boolean, accentSoft: Color) { Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (unlocked) CardDarkBlue else CardDarkBlue.copy(alpha = 0.5f)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(if (unlocked) accentSoft else OnCardText.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) { Text(if (unlocked) a.icon else "❓", fontSize = 22.sp) }; Spacer(Modifier.width(14.dp)); Column(modifier = Modifier.weight(1f)) { Text(text = a.title, color = if (unlocked) OnCardText else OnCardText.copy(alpha = 0.4f), fontWeight = FontWeight.Bold, fontSize = 15.sp); Text(text = a.description, color = if (unlocked) OnCardText.copy(alpha = 0.7f) else OnCardText.copy(alpha = 0.25f), fontSize = 12.sp) }; if (unlocked) { Icon(Icons.Default.CheckCircle, null, tint = accentSoft, modifier = Modifier.size(20.dp)) } } }
@Composable fun HistoryRow(day: Long, entry: HistoryEntry, accentSoft: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.18f))
            .border(1.dp, accentSoft.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(formatEpochDayFull(day), color = OnCardText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("${entry.done}/${entry.total} quests", color = OnCardText.copy(alpha = 0.72f), fontSize = 11.sp)
        }
        Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (entry.allDone) accentSoft else ProgressTrack).padding(horizontal = 9.dp, vertical = 5.dp)) {
            Text(text = if (entry.allDone) "Completed" else "Missed", color = OnCardText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
@Composable
fun XpBar(
    levelInfo: LevelInfo,
    accentStrong: Color,
    showValue: Boolean = true,
    compact: Boolean = false,
    showContainer: Boolean = true
) {
    val progress = (levelInfo.currentXpInLevel.toFloat() / levelInfo.xpForNextLevel.toFloat()).coerceIn(0f, 1f)
    val outerModifier = if (showContainer) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SubtlePanel)
            .padding(if (compact) 8.dp else 10.dp)
    } else {
        Modifier.fillMaxWidth()
    }

    Column(modifier = outerModifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 8.dp else 10.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(ProgressTrack.copy(alpha = 0.85f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accentStrong)
            )
        }
        if (showValue) {
            Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
            Text(
                text = "${levelInfo.currentXpInLevel} / ${levelInfo.xpForNextLevel} XP",
                color = OnCardText.copy(alpha = 0.85f),
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun QuestCard(
    quest: Quest,
    accentStrong: Color,
    accentSoft: Color,
    onClaimQuest: () -> Unit,
    onProgress: (Int) -> Unit,
    modifier: Modifier = Modifier,
    uiScale: Float = 1f,
    alwaysShowProgress: Boolean = false
) {
    val target = quest.target
    val current = quest.currentProgress
    val displayProgress = current.coerceAtMost(target)

    // State: 0=Start, 1=Active, 2=Claim, 3=Completed
    val phase = when {
        quest.completed -> 3
        current > target -> 2
        current > 0 -> 1
        else -> 0
    }
    val isDone = phase == 3
    val isClaiming = phase == 2
    val isReadyToConfirmDone = !isDone && !isClaiming && current == target && target > 0
    val isAttentionState = isClaiming || isReadyToConfirmDone
    val isLightTheme = ThemeRuntime.currentTheme.isLightCategory()
    val neonBordersEnabled = ThemeRuntime.neonFlowEnabled || ThemeRuntime.decorativeBordersEnabled
    val boostedNeon = ThemeRuntime.neonLightBoostEnabled
    val neonSecondary = neonPaletteColor(ThemeRuntime.neonGlowPalette, boostedNeon)
    val neonQuestBorderActive = neonBordersEnabled
    val neonQuestBorderBrush = if (neonQuestBorderActive) rememberNeonBorderBrush(accentStrong, neonSecondary) else null

    // Restore breathing feedback for actionable quest states (DONE/CLAIM).
    val pulseTransition = if (ThemeRuntime.reduceAnimationsEnabled) null else rememberInfiniteTransition(label = "quest_pulse")
    val pulseMinAlpha = if (isClaiming) 0.52f else 0.76f
    val pulseAlpha = pulseTransition?.animateFloat(
        initialValue = 1f,
        targetValue = pulseMinAlpha,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha"
    )?.value ?: 1f
    val cardPulseScale = pulseTransition?.animateFloat(
        initialValue = 1f,
        targetValue = 1.012f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "card_scale"
    )?.value ?: 1f
    val claimPulseScale = pulseTransition?.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "claim_scale"
    )?.value ?: 1f
    val finalAlpha = when {
        isDone -> 1f
        isAttentionState -> pulseAlpha
        else -> 1f
    }
    val finalScale = if (isReadyToConfirmDone) cardPulseScale else 1f

    val bgBrush = when {
        isDone && isLightTheme -> Brush.verticalGradient(listOf(Color(0xFFF8FAFD), Color(0xFFF1F4F8)))
        isDone -> Brush.verticalGradient(listOf(CardDarkBlue.copy(alpha = 0.94f), CardDarkBlue.copy(alpha = 0.88f)))
        isLightTheme -> Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF5F8FC)))
        else -> Brush.verticalGradient(listOf(CardDarkBlue.copy(alpha = 0.95f), CardDarkBlue.copy(alpha = 0.75f)))
    }
    val questBorderColor = when {
        !neonBordersEnabled -> Color.Transparent
        boostedNeon -> accentStrong.copy(alpha = if (isLightTheme) {
            if (isDone) 0.46f else 0.58f
        } else {
            if (isDone) 0.72f else 0.86f
        })
        isLightTheme -> OnCardText.copy(alpha = if (isDone) 0.14f else 0.18f)
        else -> OnCardText.copy(alpha = if (isDone) 0.22f else 0.28f)
    }
    val questBorderWidth = if (neonBordersEnabled) {
        if (neonQuestBorderActive) {
            ((if (boostedNeon) {
                if (isLightTheme) 1.4.dp else 2.0.dp
            } else {
                if (isLightTheme) 1.05.dp else 1.5.dp
            }) * uiScale).coerceAtLeast(if (isLightTheme) 1.0.dp else 1.2.dp)
        } else {
            (1.1.dp * uiScale).coerceAtLeast(1.dp)
        }
    } else 0.dp
    val questCardShape = RoundedCornerShape(16.dp * uiScale)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(questCardShape)
            .background(brush = bgBrush)
            .then(
                if (neonQuestBorderActive && questBorderWidth > 0.dp) {
                    Modifier.border(questBorderWidth, neonQuestBorderBrush!!, questCardShape)
                } else {
                    Modifier.border(questBorderWidth, questBorderColor, questCardShape)
                }
            )
            .animateContentSize(animationSpec = tween(durationMillis = 220))
            .graphicsLayer {
                alpha = finalAlpha
                scaleX = finalScale
                scaleY = finalScale
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp * uiScale)
        ) {
            // 1. Icon / Image
            Box(
                modifier = Modifier
                    .size((42.dp * uiScale).coerceAtLeast(36.dp))
                    .clip(RoundedCornerShape(12.dp * uiScale))
                    .background(OnCardText.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (quest.imageUri != null) {
                    AsyncImage(model = quest.imageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Text(text = quest.icon, fontSize = (18f * uiScale).sp)
                }
            }

            Spacer(Modifier.width(12.dp * uiScale))

            // 2. Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quest.title,
                    color = OnCardText,
                    fontSize = (14f * uiScale).sp,
                    fontWeight = if (isDone) FontWeight.Normal else FontWeight.Bold,
                    maxLines = 1,
                    textDecoration = if (isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                )
                Spacer(Modifier.height(2.dp * uiScale))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val xpColor = when {
                        isDone -> OnCardText.copy(alpha = 0.52f)
                        else -> OnCardText.copy(alpha = 0.9f)
                    }
                    Text(text = "${quest.xpReward} XP", color = xpColor, fontSize = (11f * uiScale).sp, fontWeight = FontWeight.ExtraBold)
                    if (!isDone && (target != 2 || alwaysShowProgress)) {
                        Spacer(Modifier.width(8.dp))
                        Text("$displayProgress/$target", color = OnCardText.copy(alpha = 0.6f), fontSize = (10f * uiScale).sp)
                    }
                }
            }

            // 3. Main Action Button / Done Watermark Slot
            if (!isDone) {
                if (phase == 1 && !isReadyToConfirmDone && !isClaiming) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        SmallActionPill(
                            text = "-",
                            enabled = true,
                            accentSoft = accentSoft,
                            onClick = { onProgress((current - 1).coerceAtLeast(0)) }
                        )
                        SmallActionPill(
                            text = "+",
                            enabled = true,
                            accentSoft = accentSoft,
                            onClick = { onProgress((current + 1).coerceAtMost(target)) }
                        )
                    }
                } else {
                    val btnText = when {
                        isClaiming -> "CLAIM"
                        phase == 0 -> "START"
                        isReadyToConfirmDone -> "DONE"
                        else -> "+"
                    }
                    val btnColor = if (isClaiming) accentStrong else accentSoft
                    val isTextButton = btnText.length > 1
                    val actionModifier = if (isTextButton) Modifier.widthIn(min = (80.dp * uiScale)) else Modifier

                    Box {
                        SmallActionPill(
                            text = btnText,
                            enabled = true,
                            accentSoft = btnColor,
                            modifier = actionModifier.graphicsLayer {
                                if (isClaiming) {
                                    scaleX = claimPulseScale
                                    scaleY = claimPulseScale
                                }
                            },
                            onClick = {
                                if (isClaiming) {
                                    onClaimQuest()
                                } else if (isReadyToConfirmDone) {
                                    onProgress(target + 1)
                                } else {
                                    onProgress((current + 1).coerceAtMost(target))
                                }
                            }
                        )
                        if (isClaiming) {
                            Text(
                                text = "+${quest.xpReward}",
                                color = OnCardText,
                                fontSize = (10f * uiScale).sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (8.dp * uiScale), y = (-8.dp * uiScale))
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .widthIn(min = (80.dp * uiScale))
                        .heightIn(min = (34.dp * uiScale).coerceAtLeast(30.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DONE",
                        color = if (isLightTheme) OnCardText.copy(alpha = 0.16f) else Color(0xFF5A6472).copy(alpha = 0.38f),
                        fontSize = (16f * uiScale).sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.rotate(-15f)
                    )
                }
            }
        }
    }
}

@Composable
fun SmallActionPill(
    text: String,
    enabled: Boolean,
    accentSoft: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.Unspecified
) {
    val shape = RoundedCornerShape(12.dp)
    val isLightTheme = ThemeRuntime.currentTheme.isLightCategory()
    val containerColor = if (enabled) {
        if (isLightTheme) {
            mixForBackground(accentSoft, Color.White).copy(alpha = 0.94f)
        } else {
            mixForBackground(accentSoft, CardDarkBlue).copy(alpha = 0.90f)
        }
    } else {
        OnCardText.copy(alpha = 0.10f)
    }
    val resolvedContentColor = when {
        !enabled -> OnCardText.copy(alpha = 0.45f)
        contentColor != Color.Unspecified -> contentColor
        else -> readableTextColor(containerColor)
    }

    val verticalPad = if (ThemeRuntime.largerTouchTargetsEnabled) 9.dp else 6.dp
    Box(
        modifier = modifier // Apply the width here
            .heightIn(min = 0.dp)
            .clip(shape)
            .background(containerColor, shape = shape)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp, vertical = verticalPad),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = resolvedContentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}
@Composable
fun AvatarCard(avatar: Avatar, accentSoft: Color, uiScale: Float = 1f, onClick: () -> Unit) {
    val circle = (72.dp * uiScale).coerceAtLeast(56.dp)
    val pin = (22.dp * uiScale).coerceAtLeast(18.dp)
    Column(
        modifier = Modifier
            .widthIn(min = (92.dp * uiScale).coerceAtLeast(82.dp), max = (120.dp * uiScale).coerceAtLeast(100.dp))
            .clip(RoundedCornerShape((18.dp * uiScale)))
            .background(CardDarkBlue)
            .clickable { onClick() }
            .padding(12.dp * uiScale),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(circle + pin * 0.35f), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(circle).clip(CircleShape).background(AvatarBackground), contentAlignment = Alignment.Center) {
                when (avatar) {
                    is Avatar.Preset -> Text(avatar.emoji, fontSize = maxOf(36f * uiScale, 26f).sp)
                    is Avatar.Custom -> AsyncImage(model = avatar.uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (2.dp * uiScale), y = (2.dp * uiScale))
                    .size(pin)
                    .clip(CircleShape)
                    .background(accentSoft)
                    .border(1.dp, CardDarkBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = OnCardText, modifier = Modifier.size((14.dp * uiScale).coerceAtLeast(12.dp)))
            }
        }
    }
}
@Composable
fun FocusTimerDialog(accentStrong: Color, accentSoft: Color, onDismiss: () -> Unit, onComplete: (Int) -> Unit) {
    var seconds by remember { mutableIntStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }
    var sessionXp by remember { mutableIntStateOf(0) }
    var presetMinutes by remember { mutableIntStateOf(25) }
    var interruptions by remember { mutableIntStateOf(0) }
    var completedCycles by remember { mutableIntStateOf(0) }
    var breakSecondsLeft by remember { mutableIntStateOf(0) }
    val inBreak = breakSecondsLeft > 0

    LaunchedEffect(isRunning, presetMinutes, inBreak) {
        while (isRunning) {
            delay(1000L)
            if (inBreak) {
                breakSecondsLeft = (breakSecondsLeft - 1).coerceAtLeast(0)
                if (breakSecondsLeft == 0) {
                    isRunning = false
                }
            } else {
                seconds++
                if (seconds % 60 == 0) sessionXp++
                if (seconds >= presetMinutes * 60) {
                    completedCycles++
                    val bonus = if (completedCycles % 4 == 0) 4 else 1
                    sessionXp += bonus
                    breakSecondsLeft = if (completedCycles % 4 == 0) 15 * 60 else 5 * 60
                }
            }
        }
    }

    val displaySeconds = if (inBreak) breakSecondsLeft else seconds
    val fmtTime = remember(displaySeconds) {
        val m = displaySeconds / 60
        val s = displaySeconds % 60
        "%02d:%02d".format(m, s)
    }
    val isLightTheme = ThemeRuntime.currentTheme.isLightCategory()
    val dialogContainer = if (isLightTheme) Color(0xFFF8FBFF) else CardDarkBlue
    val dialogSubText = OnCardText.copy(alpha = if (isLightTheme) 0.68f else 0.62f)
    val stopButtonColor = if (isLightTheme) Color(0xFFC84B5E) else Color(0xFF9A3648)
    val chipSelectedContainer = accentStrong.copy(alpha = if (isLightTheme) 0.18f else 0.30f)
    val chipSelectedLabel = if (isLightTheme) accentStrong else OnCardText
    val chipBorderColor = if (isLightTheme) accentStrong.copy(alpha = 0.45f) else OnCardText.copy(alpha = 0.22f)
    val chipUnselectedContainer = if (isLightTheme) Color(0xFFF1F6FC) else SubtlePanel

    AlertDialog(
        onDismissRequest = { if (!isRunning) onDismiss() },
        containerColor = dialogContainer,
        title = { Text("Deep Focus", color = accentStrong, fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isRunning && !inBreak) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(25, 50, 90).forEach { minutes ->
                            FilterChip(
                                selected = presetMinutes == minutes,
                                onClick = { presetMinutes = minutes },
                                label = { Text("$minutes min") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = chipSelectedContainer,
                                    selectedLabelColor = chipSelectedLabel,
                                    containerColor = chipUnselectedContainer,
                                    labelColor = OnCardText.copy(alpha = 0.86f)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = presetMinutes == minutes,
                                    borderColor = chipBorderColor,
                                    selectedBorderColor = chipBorderColor
                                )
                            )
                        }
                    }
                }
                Text(fmtTime, fontSize = 48.sp, fontWeight = FontWeight.Black, color = OnCardText, letterSpacing = 2.sp)
                Text(
                    if (inBreak) "Break mode: recover, then continue." else "Earn 1 XP per minute + cycle bonuses.",
                    color = dialogSubText,
                    fontSize = 12.sp
                )
                Text("Cycles: $completedCycles • Interruptions: $interruptions", color = dialogSubText, fontSize = 12.sp)
                if (sessionXp > 0) {
                    Text("Current Reward: +$sessionXp XP", color = accentStrong, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            if (isRunning) {
                Button(
                    onClick = {
                        interruptions++
                        isRunning = false
                        onComplete(sessionXp)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = stopButtonColor)
                ) { Text("Stop & Claim", color = Color.White) }
            } else {
                Button(
                    onClick = {
                        if (!inBreak) {
                            seconds = 0
                        }
                        isRunning = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentStrong)
                ) { Text(if (inBreak) "Resume Break" else "Start Focus", color = readableTextColor(accentStrong)) }
            }
        },
        dismissButton = {
            if (!isRunning) {
                TextButton(onClick = onDismiss) { Text("Close", color = OnCardText) }
            }
        }
    )
}

// === NEW COMPONENTS (Added for Production) ===
@Composable
fun BossSection(boss: Boss, accentStrong: Color, uiScale: Float) {
    val progress = (boss.currentHp.toFloat() / boss.totalHp.toFloat()).coerceIn(0f, 1f)
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1E0B0B)).border(1.dp, Color(0xFF8B0000).copy(alpha = 0.4f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(boss.icon, fontSize = 20.sp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(boss.name, color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("${boss.currentHp}/${boss.totalHp}", color = Color(0xFFFF5252).copy(alpha=0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))) { Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress).background(Brush.horizontalGradient(listOf(Color(0xFFD32F2F), Color(0xFFFF5252))))) }
        }
    }
}
@Composable
fun InventoryGrid(inventory: List<InventoryItem>, accentSoft: Color, onConsume: (InventoryItem) -> Unit) {
    val rows = 4; val cols = 5
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (r in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (c in 0 until cols) {
                    val index = r * cols + c; val item = inventory.getOrNull(index)
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.3f)).border(1.dp, if (item != null) accentSoft.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(6.dp)).clickable(enabled = item != null) { if (item != null) { SoundManager.playClick(); onConsume(item) } }, contentAlignment = Alignment.Center) {
                        if (item != null) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(item.icon, fontSize = 20.sp); if (item.ownedCount > 1) Text("x${item.ownedCount}", fontSize = 9.sp, color = OnCardText.copy(alpha = 0.7f)) } }
                        else { Box(Modifier.size(8.dp).clip(CircleShape).background(OnCardText.copy(alpha = 0.05f))) }
                    }
                }
            }
        }
    }
}

@Composable
fun CharacterView(data: CharacterData, scale: Float = 1f) {
    Canvas(modifier = Modifier.size(30.dp * scale, 60.dp * scale)) {
        val w = size.width; val h = size.height
        drawRect(color = Color(data.headColor), topLeft = androidx.compose.ui.geometry.Offset(w * 0.25f, 0f), size = androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.25f))
        drawRect(color = Color(data.bodyColor), topLeft = androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.25f), size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.35f))
        drawRect(color = Color(data.legsColor), topLeft = androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.60f), size = androidx.compose.ui.geometry.Size(w * 0.2f, h * 0.3f))
        drawRect(color = Color(data.legsColor), topLeft = androidx.compose.ui.geometry.Offset(w * 0.55f, h * 0.60f), size = androidx.compose.ui.geometry.Size(w * 0.2f, h * 0.3f))
        drawRect(color = Color(data.shoesColor), topLeft = androidx.compose.ui.geometry.Offset(w * 0.20f, h * 0.90f), size = androidx.compose.ui.geometry.Size(w * 0.25f, h * 0.1f))
        drawRect(color = Color(data.shoesColor), topLeft = androidx.compose.ui.geometry.Offset(w * 0.55f, h * 0.90f), size = androidx.compose.ui.geometry.Size(w * 0.25f, h * 0.1f))
    }
}

@Composable
fun CharacterEditorDialog(initial: CharacterData, accentStrong: Color, onSave: (CharacterData) -> Unit, onDismiss: () -> Unit) {
    var head by remember { mutableLongStateOf(initial.headColor) }; var body by remember { mutableLongStateOf(initial.bodyColor) }; var legs by remember { mutableLongStateOf(initial.legsColor) }; var shoes by remember { mutableLongStateOf(initial.shoesColor) }
    val colors = listOf(0xFFFACE8D, 0xFF8D5524, 0xFFC68642, 0xFFE0AC69, 0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF3F51B5, 0xFF2196F3, 0xFF00BCD4, 0xFF4CAF50, 0xFF8BC34A, 0xFFFFEB3B, 0xFFFF9800, 0xFF795548, 0xFF9E9E9E, 0xFF607D8B, 0xFF000000, 0xFFFFFFFF)
    AlertDialog(onDismissRequest = onDismiss, containerColor = CardDarkBlue, title = { Text("Customize Hero", color = OnCardText, fontWeight = FontWeight.Bold) }, text = { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) { CharacterView(CharacterData(head, body, legs, shoes), scale = 3f); ColorPickerRow("Skin", colors, head) { head = it }; ColorPickerRow("Shirt", colors, body) { body = it }; ColorPickerRow("Pants", colors, legs) { legs = it }; ColorPickerRow("Shoes", colors, shoes) { shoes = it } } }, confirmButton = { TextButton(onClick = { onSave(CharacterData(head, body, legs, shoes)) }) { Text("Save Look", color = accentStrong, fontWeight = FontWeight.Bold) } })
}

@Composable
fun ColorPickerRow(label: String, colors: List<Long>, selected: Long, onSelect: (Long) -> Unit) {
    Column { Text(label, color = OnCardText.copy(alpha = 0.5f), fontSize = 10.sp); Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { colors.forEach { c -> Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(c)).border(2.dp, if (c == selected) Color.White else Color.Transparent, CircleShape).clickable { onSelect(c) }) } } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopItemRow(
    item: ShopItem,
    accentStrong: Color,
    accentSoft: Color,
    canAfford: Boolean,
    hasStock: Boolean,
    onBuy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    customMode: Boolean
) {
    @Suppress("DEPRECATION")
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (!customMode) return@rememberSwipeToDismissBoxState false
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onEdit()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = customMode,
        enableDismissFromEndToStart = customMode,
        backgroundContent = {
            val bg = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Color(0xFF2E7D32)
                SwipeToDismissBoxValue.EndToStart -> Color(0xFFC62828)
                else -> Color.Transparent
            }
            val icon = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Edit
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                else -> null
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg.copy(alpha = if (customMode) 0.28f else 0f))
                    .padding(horizontal = 14.dp),
                contentAlignment = when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                }
            ) {
                if (icon != null) Icon(icon, null, tint = Color.White)
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardDarkBlue)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(accentSoft.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) { Text(item.icon, fontSize = 18.sp) }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, color = OnCardText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${item.cost} G  •  stock ${item.stock}/${item.maxStock}", color = OnCardText.copy(alpha = 0.55f), fontSize = 12.sp)
            }
            Button(
                onClick = onBuy,
                enabled = hasStock,
                colors = ButtonDefaults.buttonColors(containerColor = accentStrong),
                modifier = Modifier.height(32.dp)
            ) { Text(if (!hasStock) "Sold Out" else if (!canAfford) "Buy" else "Buy", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun ShopItemEditorDialog(accentStrong: Color, initial: ShopItem?, onSave: (ShopItem) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var icon by remember { mutableStateOf(initial?.icon ?: "🍎") }
    var imageUri by remember { mutableStateOf(initial?.imageUri.orEmpty()) }
    var cost by remember { mutableStateOf((initial?.cost ?: 5).toString()) }
    var stock by remember { mutableStateOf((initial?.stock ?: 5).toString()) }
    var maxStock by remember { mutableStateOf((initial?.maxStock ?: 5).toString()) }
    var description by remember { mutableStateOf(initial?.description ?: "Custom shop item") }
    var isConsumable by remember { mutableStateOf(initial?.isConsumable ?: true) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            }
            imageUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDarkBlue,
        title = { Text(if (initial == null) "Create Shop Item" else "Edit Shop Item", color = accentStrong, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OnCardText, unfocusedTextColor = OnCardText, cursorColor = accentStrong))
                OutlinedTextField(value = icon, onValueChange = { icon = it.take(2) }, label = { Text("Icon") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OnCardText, unfocusedTextColor = OnCardText, cursorColor = accentStrong))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { imagePicker.launch(arrayOf("image/*")) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentStrong)
                    ) { Text(if (imageUri.isBlank()) "Pick image" else "Change image", color = readableTextColor(accentStrong)) }
                    if (imageUri.isNotBlank()) {
                        TextButton(onClick = { imageUri = "" }) { Text("Clear", color = OnCardText) }
                    }
                }
                if (imageUri.isNotBlank()) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Item image preview",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, accentStrong.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OnCardText, unfocusedTextColor = OnCardText, cursorColor = accentStrong))
                OutlinedTextField(value = cost, onValueChange = { if (it.all(Char::isDigit)) cost = it }, label = { Text("Cost (gold)") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OnCardText, unfocusedTextColor = OnCardText, cursorColor = accentStrong))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = stock, onValueChange = { if (it.all(Char::isDigit)) stock = it }, label = { Text("Stock") }, singleLine = true, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OnCardText, unfocusedTextColor = OnCardText, cursorColor = accentStrong))
                    OutlinedTextField(value = maxStock, onValueChange = { if (it.all(Char::isDigit)) maxStock = it }, label = { Text("Max") }, singleLine = true, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OnCardText, unfocusedTextColor = OnCardText, cursorColor = accentStrong))
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { isConsumable = !isConsumable }) {
                    Checkbox(checked = isConsumable, onCheckedChange = { isConsumable = it }, colors = CheckboxDefaults.colors(checkedColor = accentStrong))
                    Text("Consumable item", color = OnCardText, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val item = ShopItem(
                    id = initial?.id ?: UUID.randomUUID().toString(),
                    name = name.ifBlank { "Unnamed Item" },
                    icon = icon.ifBlank { "🧩" },
                    description = description.ifBlank { "Custom shop item" },
                    cost = cost.toIntOrNull() ?: 5,
                    stock = stock.toIntOrNull() ?: 5,
                    maxStock = maxStock.toIntOrNull() ?: 5,
                    isConsumable = isConsumable,
                    imageUri = imageUri.ifBlank { null }
                )
                onSave(item)
            }) { Text("Save", color = accentStrong) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = OnCardText) } }
    )
}

@Composable
fun DashboardScreen(
    modifier: Modifier,
    levelInfo: LevelInfo,
    attributes: PlayerAttributes,
    gold: Int,
    streak: Int,
    history: Map<Long, HistoryEntry>,
    unlockedAchievementIds: Set<String>,
    accentStrong: Color,
    accentSoft: Color,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit
) {
    // Calculate Stats
    val totalQuestsDone = history.values.sumOf { it.done }
    val daysActive = history.size.coerceAtLeast(1)
    val avgCompletion = if (daysActive > 0) {
        val sum = history.values.sumOf { it.done.toDouble() / it.total.coerceAtLeast(1).toDouble() }
        (sum / daysActive * 100).toInt()
    } else 0
    val totalPerfectDays = history.values.count { it.allDone }
    val achievedCount = remember(unlockedAchievementIds) { unlockedAchievementIds.size }
    val achievementTitleById = remember { getAchievementDefinitions().associate { it.id to it.title } }
    val recentHistory = remember(history) { history.toList().sortedByDescending { it.first }.take(6) }
    val sortedDays = remember(history) { history.toList().sortedByDescending { it.first } }
    val last7Days = remember(sortedDays) { sortedDays.take(7) }
    val last30Days = remember(sortedDays) { sortedDays.take(30) }
    val recent7Avg = remember(last7Days) {
        if (last7Days.isEmpty()) 0 else ((last7Days.sumOf { (_, e) -> e.done } * 100f) / last7Days.sumOf { (_, e) -> e.total.coerceAtLeast(1) }).toInt()
    }
    val recent30Avg = remember(last30Days) {
        if (last30Days.isEmpty()) 0 else ((last30Days.sumOf { (_, e) -> e.done } * 100f) / last30Days.sumOf { (_, e) -> e.total.coerceAtLeast(1) }).toInt()
    }
    val currentPerfectStreak = remember(sortedDays) {
        var count = 0
        for ((_, e) in sortedDays) {
            if (!e.allDone) break
            count++
        }
        count
    }
    val bestWeekday = remember(history) { bestWeekdayByCompletion(history) }
    val trendPoints = remember(last7Days) {
        last7Days.reversed().map { (_, e) ->
            (e.done.toFloat() / e.total.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
        }
    }
    val heatmapLast30 = remember(last30Days) {
        last30Days.reversed().map { (_, e) ->
            (e.done.toFloat() / e.total.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
        }
    }

    ScalableScreen(modifier) { uiScale ->
        Column(verticalArrangement = Arrangement.spacedBy((12.dp * uiScale))) {
            ScalableHeader(stringResource(R.string.title_dashboard), uiScale, onOpenDrawer, showMenu = true) {
                IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, null, tint = OnCardText, modifier = Modifier.size(24.dp * uiScale)) }
            }

            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

                // 1. Hero Overview
                CardBlock {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(accentSoft.copy(alpha=0.2f)).border(2.dp, accentSoft, CircleShape), contentAlignment = Alignment.Center) {
                            Text("${levelInfo.level}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = accentStrong)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("LEVEL ${levelInfo.level} HERO", color = OnCardText, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp)
                            Text("$gold Gold • $streak Day Streak", color = OnCardText.copy(alpha=0.6f), fontSize = 13.sp)
                            Spacer(Modifier.height(8.dp))
                            XpBar(levelInfo, accentStrong, showContainer = false)
                        }
                    }
                }

                // 2. Lifetime Stats
                CardBlock {
                    Text("LIFETIME STATS", color = OnCardText.copy(alpha=0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatTile(Icons.Default.CheckCircle, "Total Quests", "$totalQuestsDone", accentStrong, Modifier.weight(1f))
                            StatTile(Icons.Default.DateRange, "Days Active", "$daysActive", Color(0xFF29B6F6), Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatTile(Icons.Default.DataUsage, "Completion", "$avgCompletion%", Color(0xFF66BB6A), Modifier.weight(1f))
                            StatTile(Icons.Default.Star, "Perfect Days", "$totalPerfectDays", Color(0xFFFFCA28), Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatTile(Icons.Default.Timeline, "7-Day Avg", "$recent7Avg%", Color(0xFF80CBC4), Modifier.weight(1f))
                            @Suppress("DEPRECATION")
                            StatTile(Icons.Default.TrendingUp, "30-Day Avg", "$recent30Avg%", Color(0xFF81C784), Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatTile(Icons.Default.Bolt, "Perfect Streak", "$currentPerfectStreak", Color(0xFFFFB74D), Modifier.weight(1f))
                            StatTile(Icons.Default.EmojiEvents, "Achievements", "$achievedCount", Color(0xFFBA68C8), Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatTile(Icons.Default.EventAvailable, "Best Day", bestWeekday, Color(0xFF4FC3F7), Modifier.weight(1f))
                            StatTile(Icons.Default.Insights, "Trend", if ((trendPoints.lastOrNull() ?: 0f) >= (trendPoints.firstOrNull() ?: 0f)) "Up" else "Down", Color(0xFF26A69A), Modifier.weight(1f))
                        }
                    }
                }

                // 3. Attribute Matrix
                CardBlock {
                    Text("ATTRIBUTE MATRIX", color = OnCardText.copy(alpha=0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AttributeRow("Strength", attributes.str)
                        AttributeRow("Intellect", attributes.int)
                        AttributeRow("Vitality", attributes.vit)
                        AttributeRow("Endurance", attributes.end)
                        AttributeRow("Faith", attributes.fth)
                    }
                }

                // 4. Analytics
                CardBlock {
                    Text("7-DAY TREND", color = OnCardText.copy(alpha=0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth().height(80.dp)) {
                        if (trendPoints.isEmpty()) {
                            Text("No trend data yet.", color = OnCardText.copy(alpha = 0.6f), fontSize = 12.sp)
                        } else {
                            trendPoints.forEach { ratio ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(ratio.coerceAtLeast(0.08f))
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(accentStrong.copy(alpha = 0.35f + ratio * 0.55f))
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("30-DAY HEATMAP", color = OnCardText.copy(alpha=0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        heatmapLast30.forEach { ratio ->
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(accentSoft.copy(alpha = 0.12f + (ratio * 0.88f)))
                            )
                        }
                    }
                }

                // 5. Achievements
                CardBlock {
                    Text("ACHIEVEMENTS", color = OnCardText.copy(alpha=0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Unlocked: $achievedCount / ${getAchievementDefinitions().size}", color = accentStrong, fontWeight = FontWeight.Bold)
                    Text(
                        unlockedAchievementIds.mapNotNull { achievementTitleById[it] }.take(4).joinToString(", ").ifBlank { "No achievements yet" },
                        color = OnCardText.copy(alpha = 0.75f),
                        fontSize = 12.sp
                    )
                }

                // 6. Recent History
                CardBlock {
                    Text("RECENT HISTORY", color = OnCardText.copy(alpha=0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(6.dp))
                    if (recentHistory.isEmpty()) {
                        Text("No recent history yet.", color = OnCardText.copy(alpha = 0.6f), fontSize = 12.sp)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            recentHistory.forEach { (day, entry) -> HistoryRow(day, entry, accentSoft) }
                        }
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun StatTile(icon: ImageVector, label: String, value: String, color: Color, modifier: Modifier) {
    val tileShape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .clip(tileShape)
            .background(Color.Black.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.36f), tileShape)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(15.dp))
        Text(label, fontSize = 11.sp, color = OnCardText.copy(alpha = 0.76f), maxLines = 1, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OnCardText, maxLines = 1)
    }
}

// ==========================================
// === MISSING HELPER COMPONENTS (PASTE AT BOTTOM OF FILE) ===
// ==========================================

@Composable
fun AttributeRow(label: String, value: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.14f))
            .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.30f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = OnCardText.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(72.dp))
        Box(modifier = Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(99.dp)).background(ProgressTrack.copy(alpha = 0.75f))) { Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((value / 100f).coerceIn(0.05f, 1f)).background(Color(0xFF4CAF50))) }
        Spacer(Modifier.width(10.dp))
        Text("$value", color = OnCardText, fontWeight = FontWeight.Black, fontSize = 12.sp, modifier = Modifier.width(22.dp), textAlign = TextAlign.End)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainQuestItem(
    quest: CustomMainQuest,
    isLocked: Boolean = false,
    lockedByTitle: String? = null, // NEW: Receive the title
    accentStrong: Color,
    accentSoft: Color,
    onOpenWizard: () -> Unit,
    onUpdate: (CustomMainQuest) -> Unit,
    onResetProgress: () -> Unit,
    onDelete: (String) -> Unit
) {
    var expanded by rememberSaveable(quest.id) { mutableStateOf(false) }
    var showLockInfo by rememberSaveable(quest.id) { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)

    val isComplete = quest.currentStep >= quest.steps.size
    val isReadyToClaim = isComplete && !quest.isClaimed

    val mqNeonEnabled = ThemeRuntime.neonFlowEnabled || ThemeRuntime.decorativeBordersEnabled
    val mqBoostedNeon = ThemeRuntime.neonLightBoostEnabled
    val mqNeonBrush = if (mqNeonEnabled) rememberNeonBorderBrush(accentStrong, neonPaletteColor(ThemeRuntime.neonGlowPalette, mqBoostedNeon)) else null
    val mqCardShape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isLocked) 0.78f else 1f)
            .animateContentSize()
            .clip(mqCardShape)
            .background(CardDarkBlue)
            .then(if (mqNeonEnabled && mqNeonBrush != null) Modifier.border(if (mqBoostedNeon) 2.dp else 1.5.dp, mqNeonBrush, mqCardShape) else Modifier)
            .clickable(enabled = !isLocked) {
                SoundManager.playClick()
                expanded = !expanded
            }
    ) {
        Column {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = if (isLocked) 6.dp else 10.dp), verticalAlignment = Alignment.CenterVertically) {
                // Icon Logic Updated
                val icon = when {
                    isLocked -> Icons.Default.Lock // 🔒 PADLOCK!
                    quest.isClaimed -> Icons.Default.CheckCircle
                    isReadyToClaim -> Icons.Default.EmojiEvents
                    !quest.hasStarted -> Icons.Default.PriorityHigh
                    else -> Icons.Default.Map
                }
                val tint = if(isLocked) OnCardText.copy(alpha=0.5f) else if(quest.isClaimed) Color.Green else if(isReadyToClaim || !quest.hasStarted) accentStrong else accentSoft

                Icon(icon, null, tint = tint, modifier = Modifier.size(if (isLocked) 16.dp else 20.dp))
                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(quest.title, color = OnCardText, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    val statusText = when {
                        isLocked -> ""
                        quest.isClaimed -> ""
                        isReadyToClaim -> "Ready to Claim!"
                        !quest.hasStarted -> "Tap to Start"
                        else -> "Step ${quest.currentStep + 1}/${quest.steps.size.coerceAtLeast(1)}"
                    }
                    if (statusText.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(statusText, color = OnCardText.copy(alpha=0.6f), fontSize = 11.sp)
                    }
                }

                if (quest.isClaimed) {
                    IconButton(onClick = { onDelete(quest.id) }) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha=0.5f)) }
                } else if (isLocked) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .clickable {
                                SoundManager.playClick()
                                showLockInfo = !showLockInfo
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PriorityHigh, null, tint = accentStrong.copy(alpha = 0.98f), modifier = Modifier.size(20.dp))
                    }
                } else if (!isLocked) {
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = OnCardText, modifier = Modifier.rotate(rotation))
                }
            }
            if (isLocked && showLockInfo) {
                HorizontalDivider(color = OnCardText.copy(alpha=0.1f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.2f))
                        .border(1.dp, OnCardText.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Locked: Complete '${lockedByTitle ?: "previous quest"}'",
                        color = OnCardText.copy(alpha = 0.75f),
                        fontSize = 12.sp
                    )
                }
            }

            // Dropdown Content
            if (expanded && !quest.isClaimed && !isLocked) {
                HorizontalDivider(color = OnCardText.copy(alpha=0.1f))
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    if (quest.description.isNotBlank()) {
                        Text(quest.description, color = OnCardText.copy(alpha=0.8f), fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                    }

                    if (!quest.hasStarted) {
                        Button(onClick = { SoundManager.playAccept(); onOpenWizard() }, colors = ButtonDefaults.buttonColors(containerColor = accentStrong), modifier = Modifier.fillMaxWidth()) {
                            Text("START QUEST", color = readableTextColor(accentStrong), fontWeight = FontWeight.Bold)
                        }
                    }
                    else if (!isReadyToClaim) {
                        if (quest.steps.isNotEmpty()) {
                            quest.steps.forEachIndexed { index, stepName ->
                                val isDone = index < quest.currentStep
                                val isCurrent = index == quest.currentStep
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (isDone) Icons.Default.CheckCircle else if (isCurrent) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked, null, tint = if(isDone) accentSoft.copy(alpha=0.5f) else if(isCurrent) accentStrong else OnCardText.copy(alpha=0.3f), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stepName, color = if(isDone) OnCardText.copy(alpha=0.5f) else OnCardText, fontSize = 13.sp, textDecoration = if(isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else null)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        Button(onClick = { SoundManager.playClick(); if (quest.steps.isEmpty()) onOpenWizard() else onUpdate(quest.copy(currentStep = quest.currentStep + 1)) }, colors = ButtonDefaults.buttonColors(containerColor = accentSoft), modifier = Modifier.fillMaxWidth()) {
                            Text(if(quest.steps.isEmpty()) "FINISH QUEST" else "COMPLETE STEP", color = OnCardText)
                        }
                    }
                    else {
                        Button(onClick = { SoundManager.playClick(); onOpenWizard() }, colors = ButtonDefaults.buttonColors(containerColor = accentStrong), modifier = Modifier.fillMaxWidth()) {
                            Text("FINISH", color = readableTextColor(accentStrong), fontWeight = FontWeight.Bold)
                        }
                    }

                    if (quest.hasStarted || quest.currentStep > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.22f))
                                .clickable {
                                    SoundManager.playClick()
                                    onResetProgress()
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Tap to Reset Progress", color = OnCardText.copy(alpha = 0.78f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun AddMainQuestDialog(
    accentStrong: Color,
    editingQuest: CustomMainQuest? = null,
    existingQuests: List<CustomMainQuest>, // NEW PARAMETER
    onSave: (CustomMainQuest) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(editingQuest?.title ?: "") }
    var description by remember { mutableStateOf(editingQuest?.description ?: "") }
    var xpReward by remember { mutableStateOf(editingQuest?.xpReward?.toString() ?: "500") }
    var steps by remember { mutableStateOf(editingQuest?.steps ?: listOf("")) }
    var icon by remember { mutableStateOf(editingQuest?.icon ?: "🏆") }
    var imageUri by remember { mutableStateOf(editingQuest?.imageUri.orEmpty()) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            imageUri = uri.toString()
        }
    }

    // NEW: Prerequisite Dropdown State
    var prerequisiteId by remember { mutableStateOf(editingQuest?.prerequisiteId) }
    var showPrereqDropdown by remember { mutableStateOf(false) }

    // We filter out the current quest so you can't lock a quest behind itself!
    val validPrereqs = existingQuests.filter { it.id != editingQuest?.id }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDarkBlue,
        title = { Text(if (editingQuest == null) "New Main Quest" else "Edit Quest", color = accentStrong, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Quest Title", color = OnCardText.copy(alpha=0.5f)) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OnCardText, unfocusedTextColor = OnCardText, cursorColor = accentStrong), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description", color = OnCardText.copy(alpha=0.5f)) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OnCardText, unfocusedTextColor = OnCardText, cursorColor = accentStrong), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = xpReward, onValueChange = { xpReward = it.filter { char -> char.isDigit() } }, label = { Text("XP Reward", color = OnCardText.copy(alpha=0.5f)) }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OnCardText, unfocusedTextColor = OnCardText, cursorColor = accentStrong), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = icon, onValueChange = { icon = it.take(4) }, label = { Text("Icon", color = OnCardText.copy(alpha=0.5f)) }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OnCardText, unfocusedTextColor = OnCardText, cursorColor = accentStrong), modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { imagePicker.launch(arrayOf("image/*")) }, colors = ButtonDefaults.buttonColors(containerColor = accentStrong)) { Text(if (imageUri.isBlank()) "Pick image" else "Change image", color = readableTextColor(accentStrong)) }
                    if (imageUri.isNotBlank()) {
                        TextButton(onClick = { imageUri = "" }) { Text("Clear", color = OnCardText) }
                    }
                }
                if (imageUri.isNotBlank()) {
                    AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.fillMaxWidth().height(88.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                }

                // NEW: The Prerequisite Dropdown UI
                if (validPrereqs.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Prerequisite (Locked until complete):", color = OnCardText.copy(alpha=0.8f), fontSize = 12.sp)
                        Box {
                            val selectedName = validPrereqs.find { it.id == prerequisiteId }?.title ?: "None"
                            val fieldShape = RoundedCornerShape(12.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(fieldShape)
                                    .background(if (ThemeRuntime.currentTheme.isLightCategory()) Color(0x17000000) else Color.Black.copy(alpha = 0.30f))
                                    .border(1.dp, OnCardText.copy(alpha = if (ThemeRuntime.currentTheme.isLightCategory()) 0.12f else 0.18f), fieldShape)
                                    .clickable {
                                        SoundManager.playClick()
                                        showPrereqDropdown = true
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedName, color = OnCardText, modifier = Modifier.weight(1f), maxLines = 1)
                                Icon(Icons.Default.KeyboardArrowDown, null, tint = accentStrong.copy(alpha = 0.92f))
                            }
                            DropdownMenu(expanded = showPrereqDropdown, onDismissRequest = { showPrereqDropdown = false }, modifier = Modifier.background(CardDarkBlue)) {
                                DropdownMenuItem(text = { Text("None", color = OnCardText) }, onClick = { prerequisiteId = null; showPrereqDropdown = false })
                                validPrereqs.forEach { pq ->
                                    DropdownMenuItem(
                                        text = { Text(pq.title, color = OnCardText) },
                                        onClick = { prerequisiteId = pq.id; showPrereqDropdown = false }
                                    )
                                }
                            }
                        }
                    }
                }

                Text("Quest Steps:", color = OnCardText, fontWeight = FontWeight.Bold)
                steps.forEachIndexed { index, step ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = step, onValueChange = { newStep -> val mut = steps.toMutableList(); mut[index] = newStep; steps = mut }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OnCardText, unfocusedTextColor = OnCardText, cursorColor = accentStrong))
                        IconButton(onClick = { val mut = steps.toMutableList(); mut.removeAt(index); steps = mut }) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha=0.6f)) }
                    }
                }
                TextButton(onClick = { steps = steps + "" }) { Text("+ Add Step", color = accentStrong) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalSteps = steps.filter { it.isNotBlank() }
                val newQuest = CustomMainQuest(
                    id = editingQuest?.id ?: java.util.UUID.randomUUID().toString(),
                    title = title.ifBlank { "New Quest" },
                    description = description,
                    xpReward = xpReward.toIntOrNull() ?: 500,
                    steps = if (finalSteps.isEmpty()) listOf("Complete Quest") else finalSteps,
                    currentStep = editingQuest?.currentStep ?: 0,
                    isClaimed = editingQuest?.isClaimed ?: false,
                    hasStarted = editingQuest?.hasStarted ?: false,
                    prerequisiteId = prerequisiteId, // Assign the chosen lock!
                    icon = icon.ifBlank { "🏆" },
                    imageUri = imageUri.ifBlank { null }
                )
                onSave(newQuest)
            }, colors = ButtonDefaults.buttonColors(containerColor = accentStrong)) { Text("Save", color = Color.Black, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = OnCardText) } }
    )
}
@Composable
fun NpcQuestDialog(
    quest: CustomMainQuest,
    accentStrong: Color,
    onUpdate: (CustomMainQuest) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(CardDarkBlue)
                .border(1.dp, OnCardText.copy(alpha = 0.18f), RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                // 1. Wizard Avatar
                Box(
                    modifier = Modifier.size(100.dp).clip(CircleShape).border(2.dp, accentStrong, CircleShape).background(Color.Black),
                    contentAlignment = Alignment.Center
                ) { Text("🧙‍♂️", fontSize = 50.sp) }

                Spacer(Modifier.height(16.dp))
                Text("- ${quest.title.uppercase()} -", color = accentStrong.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Spacer(Modifier.height(24.dp))

                val isIntro = !quest.hasStarted

                val dialogue = if (isIntro) {
                    "Greetings, hero. A new path reveals itself.\n\n${quest.description}\n\nThe reward is ${quest.xpReward} XP.\nDo you accept this challenge?"
                } else {
                    "You have returned triumphant! The deed is done, and your legend grows.\n\nTake this reward, hero. You earned it."
                }

                Text(
                    text = dialogue, color = OnCardText.copy(alpha = 0.92f), fontSize = 16.sp, lineHeight = 24.sp, textAlign = TextAlign.Center,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                )

                Spacer(Modifier.height(32.dp))

                // 3. Button
                Button(
                    onClick = {
                        if (isIntro) {
                            // ACTION: ACCEPT QUEST -> Play Accept Sound
                            SoundManager.playAccept()
                            onUpdate(quest.copy(hasStarted = true))
                            onDismiss()
                        } else {
                            // ACTION: CLAIM REWARD -> Play Congratz Sound
                            SoundManager.playSuccess()
                            onUpdate(quest.copy(isClaimed = true))
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentStrong),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(if (isIntro) "I ACCEPT" else "CLAIM REWARD", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss) { Text("Close", color = OnCardText.copy(alpha = 0.6f)) }
            }
        }
    }
}

@Composable
fun MainQuestPackageHeader(
    packageLabel: String,
    quests: List<CustomMainQuest>,
    accentSoft: Color,
    customMode: Boolean,
    onEdit: (CustomMainQuest) -> Unit,
    onDelete: (CustomMainQuest) -> Unit,
    onToggle: (CustomMainQuest, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) } // Closed by default
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)
    val packageNeonBrush = rememberNeonBorderBrush(accentSoft, neonPaletteColor(ThemeRuntime.neonGlowPalette, ThemeRuntime.neonLightBoostEnabled))
    data class ParsedMainQuest(val quest: CustomMainQuest, val index: Int, val familyKey: String, val familyTier: Int?)
    fun parseFamily(title: String): Pair<String, Int?> {
        val m = Regex("""^(.*?)(?:\s+(\d+))$""").find(title.trim())
        val base = (m?.groupValues?.getOrNull(1)?.trim().takeUnless { it.isNullOrBlank() } ?: title.trim())
        val tier = m?.groupValues?.getOrNull(2)?.toIntOrNull()
        return base to tier
    }
    val groupedFamilies = remember(quests) {
        val parsed = quests.mapIndexed { idx, q ->
            val (familyKey, familyTier) = parseFamily(q.title)
            ParsedMainQuest(q, idx, familyKey, familyTier)
        }
        parsed.groupBy { it.familyKey.lowercase() }
            .values
            .sortedBy { g -> g.minOf { it.index } }
            .map { group ->
                val tiered = group.filter { it.familyTier != null }
                val ordered = if (tiered.size >= 2) {
                    group.sortedWith(compareBy<ParsedMainQuest> { it.familyTier ?: Int.MAX_VALUE }.thenBy { it.index })
                } else {
                    group.sortedBy { it.index }
                }
                val root = ordered.firstOrNull()?.quest
                root to ordered.map { it.quest }
            }
            .filter { it.first != null }
            .map { it.first!! to it.second }
    }

    Column {
        // The Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardDarkBlue)
                .border(1.5.dp, packageNeonBrush, RoundedCornerShape(12.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 12.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Bookmarks, null, tint = OnCardText.copy(alpha=0.7f), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(packageLabel, color = OnCardText, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))

            // Count Badge
            Box(modifier = Modifier.clip(CircleShape).background(OnCardText.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text("${quests.size}", color = OnCardText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Default.KeyboardArrowDown, null, tint = OnCardText, modifier = Modifier.rotate(rotation))
        }

        // The Dropdown Content
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                groupedFamilies.forEach { (root, chain) ->
                    var familyExpanded by rememberSaveable(root.id) { mutableStateOf(false) }
                    val familyRotation by animateFloatAsState(if (familyExpanded) 180f else 0f, label = "main_pkg_family")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardDarkBlue.copy(alpha = 0.52f))
                            .border(1.dp, OnCardText.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                            .clickable { familyExpanded = !familyExpanded }
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(root.title, color = OnCardText, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1)
                        Box(modifier = Modifier.clip(CircleShape).background(OnCardText.copy(alpha = 0.08f)).padding(horizontal = 7.dp, vertical = 2.dp)) {
                            Text("${chain.size}", color = OnCardText.copy(alpha = 0.85f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = OnCardText.copy(alpha = 0.75f), modifier = Modifier.rotate(familyRotation))
                    }
                    if (familyExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 8.dp)) {
                            chain.forEach { mq ->
                                PoolMainQuestRow(
                                    mq = mq,
                                    accentSoft = accentSoft,
                                    customMode = customMode,
                                    onEdit = { onEdit(mq) },
                                    onDelete = { onDelete(mq) },
                                    onToggle = { active -> onToggle(mq, active) }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp)) // Breathing room after list
        }
    }
}

@Composable
fun MainQuestChainHeader(
    familyTitle: String,
    quests: List<CustomMainQuest>,
    accentSoft: Color,
    customMode: Boolean,
    onEdit: (CustomMainQuest) -> Unit,
    onDelete: (CustomMainQuest) -> Unit,
    onToggle: (CustomMainQuest, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)
    val chainNeonBrush = rememberNeonBorderBrush(accentSoft, neonPaletteColor(ThemeRuntime.neonGlowPalette, ThemeRuntime.neonLightBoostEnabled))
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardDarkBlue)
                .border(1.5.dp, chainNeonBrush, RoundedCornerShape(12.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 10.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(familyTitle, color = OnCardText, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            Box(modifier = Modifier.clip(CircleShape).background(OnCardText.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text("${quests.size}", color = OnCardText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Icon(Icons.Default.KeyboardArrowDown, null, tint = OnCardText, modifier = Modifier.rotate(rotation))
        }
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                quests.forEach { mq ->
                    PoolMainQuestRow(
                        mq = mq,
                        accentSoft = accentSoft,
                        customMode = customMode,
                        onEdit = { onEdit(mq) },
                        onDelete = { onDelete(mq) },
                        onToggle = { active -> onToggle(mq, active) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
