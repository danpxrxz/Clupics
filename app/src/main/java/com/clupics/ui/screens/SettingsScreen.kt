package com.clupics.ui.screens

import android.os.Build
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clupics.R
import com.clupics.ui.settings.AppLanguage
import com.clupics.ui.settings.SettingsViewModel
import com.clupics.utils.BiometricAuthManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel, onBack: () -> Unit = {}) {
    val settings by settingsViewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    // El gesto predictivo nativo lo maneja el sistema directamente.
    // No se necesita PredictiveBackHandler aquí — con enableOnBackInvokedCallback=true
    // en el manifest, Android entrega la animación de tarjeta nativa sin código extra.
    // CRÍTICO: No envolver en Surface(fillMaxSize) — su fondo sólido opaco bloquea
    // completamente la animación de tarjeta predictiva nativa (no deja ver la pantalla
    // de atrás durante el gesto). Usamos Column con background explícito: el fondo
    // es visible y opaco normalmente, pero durante el gesto predictivo Android puede
    // aplicar el efecto de tarjeta correctamente porque el composable no tiene un
    // Surface que intercepte el renderizado de la ventana subyacente.
    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surface)) {

        LargeTopAppBar(
            title = {
                Text(
                    text  = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.viewer_back))
                }
            },
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.largeTopAppBarColors(
                containerColor         = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Apariencia ─────────────────────────────────────────────────
            SettingsGroupLabel(stringResource(R.string.settings_appearance))

            // Dark mode chip row
            SettingsGroupItem(position = GroupPosition.TOP) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text       = stringResource(R.string.settings_dark_mode),
                        style      = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    DarkModeChips(
                        current    = settings.darkModeOverride,
                        onSelected = { settingsViewModel.setDarkModeOverride(it) }
                    )
                }
            }

            SettingsGroupItem(position = GroupPosition.MIDDLE) {
                RefToggleItem(
                    title           = stringResource(R.string.settings_amoled_mode),
                    subtitle        = stringResource(R.string.settings_amoled_mode_sub),
                    checked         = settings.amoledMode,
                    onCheckedChange = { settingsViewModel.setAmoledMode(it) }
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SettingsGroupItem(position = GroupPosition.MIDDLE) {
                    RefToggleItem(
                        title           = stringResource(R.string.settings_dynamic_color),
                        subtitle        = stringResource(R.string.settings_dynamic_color_sub),
                        checked         = settings.dynamicColor,
                        onCheckedChange = { settingsViewModel.setDynamicColor(it) }
                    )
                }
            }

            // Grid columns slider
            SettingsGroupItem(position = GroupPosition.BOTTOM) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text       = stringResource(R.string.settings_grid_columns),
                            style      = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text     = settings.gridColumns.toString(),
                                style    = MaterialTheme.typography.labelMedium,
                                color    = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    GridColumnsSlider(
                        value    = settings.gridColumns,
                        onChange = { settingsViewModel.setGridColumns(it) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Filtros de Multimedia ───────────────────────────────────────
            SettingsGroupLabel(stringResource(R.string.settings_media_filters))

            SettingsGroupItem(position = GroupPosition.TOP) {
                RefToggleItem(
                    title           = stringResource(R.string.settings_show_images),
                    subtitle        = stringResource(R.string.settings_show_images_sub),
                    checked         = settings.showImages,
                    onCheckedChange = { settingsViewModel.setShowImages(it) }
                )
            }
            SettingsGroupItem(position = GroupPosition.BOTTOM) {
                RefToggleItem(
                    title           = stringResource(R.string.settings_show_videos),
                    subtitle        = stringResource(R.string.settings_show_videos_sub),
                    checked         = settings.showVideos,
                    onCheckedChange = { settingsViewModel.setShowVideos(it) }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Privacidad ─────────────────────────────────────────────────
            val context = LocalContext.current
            val biometricAvailable = remember { BiometricAuthManager.isAvailable(context) }

            SettingsGroupLabel(stringResource(R.string.settings_privacy))

            SettingsGroupItem(position = GroupPosition.SINGLE) {
                if (biometricAvailable) {
                    RefToggleItem(
                        title           = stringResource(R.string.settings_app_lock),
                        subtitle        = stringResource(R.string.settings_app_lock_sub),
                        checked         = settings.appLockEnabled,
                        onCheckedChange = { settingsViewModel.setAppLockEnabled(it) }
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Filled.Lock,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier           = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text       = stringResource(R.string.settings_app_lock),
                                style      = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color      = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text  = stringResource(R.string.settings_app_lock_unavailable),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Idioma ─────────────────────────────────────────────────────
            SettingsGroupLabel(stringResource(R.string.settings_language))

            SettingsGroupItem(position = GroupPosition.SINGLE) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text       = stringResource(R.string.settings_language),
                        style      = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    LanguageSelector(
                        current            = settings.language,
                        onLanguageSelected = { settingsViewModel.setLanguage(it) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.height(16.dp))

            // ── Acerca de ──────────────────────────────────────────────────
            SettingsGroupLabel(stringResource(R.string.settings_about))

            // App info row
            SettingsGroupItem(position = GroupPosition.TOP) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.PhotoLibrary,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier           = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = stringResource(R.string.settings_about_app),
                            style      = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text  = stringResource(R.string.settings_about_version),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Developer row
            SettingsGroupItem(position = GroupPosition.MIDDLE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.Person,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier           = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = stringResource(R.string.settings_about_author_label),
                            style      = MaterialTheme.typography.bodySmall,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text       = stringResource(R.string.settings_about_author),
                            style      = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Source code row
            val repoUrl = stringResource(R.string.settings_about_repo_url)
            SettingsGroupItem(position = GroupPosition.BOTTOM) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(repoUrl)
                            )
                            context.startActivity(intent)
                        }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.Code,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier           = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = stringResource(R.string.settings_about_repo),
                            style      = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text  = repoUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector        = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Android 16-style grouped settings primitives ─────────────────────────────

enum class GroupPosition { TOP, MIDDLE, BOTTOM, SINGLE }

@Composable
private fun SettingsGroupLabel(title: String) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.labelLarge,
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 4.dp)
    )
}

/**
 * A settings item that clips its corners based on its position in a group,
 * creating the Android 16 "almost unified" container effect.
 * TOP: top corners large, bottom corners small
 * MIDDLE: all corners small (2dp)
 * BOTTOM: bottom corners large, top corners small
 * SINGLE: all corners large
 */
@Composable
private fun SettingsGroupItem(
    position : GroupPosition,
    content  : @Composable () -> Unit
) {
    val topRadius    = if (position == GroupPosition.TOP    || position == GroupPosition.SINGLE) 20.dp else 4.dp
    val bottomRadius = if (position == GroupPosition.BOTTOM || position == GroupPosition.SINGLE) 20.dp else 4.dp
    val shape = RoundedCornerShape(
        topStart    = topRadius,
        topEnd      = topRadius,
        bottomStart = bottomRadius,
        bottomEnd   = bottomRadius
    )
    val spacingAfter = when (position) {
        GroupPosition.MIDDLE, GroupPosition.TOP -> 1.5.dp
        else -> 0.dp
    }
    Surface(
        modifier  = Modifier.fillMaxWidth(),
        shape     = shape,
        color     = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) { content() }
    if (spacingAfter > 0.dp) Spacer(Modifier.height(spacingAfter))
}

@Composable
private fun SectionLabel(title: String) = SettingsGroupLabel(title)

@Composable
private fun RefCard(content: @Composable () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = MaterialTheme.shapes.extraLarge,
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) { content() }
}

@Composable
private fun RefDivider() {
    HorizontalDivider(
        modifier  = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    )
}

@Composable
private fun RefToggleItem(
    title          : String,
    subtitle       : String? = null,
    checked        : Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = title,
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text  = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            thumbContent    = if (checked) {
                {
                    Icon(
                        imageVector        = Icons.Outlined.Check,
                        contentDescription = null,
                        modifier           = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            } else null,
            colors = SwitchDefaults.colors(
                checkedThumbColor        = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor        = MaterialTheme.colorScheme.primary,
                checkedIconColor         = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor      = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor      = MaterialTheme.colorScheme.surfaceContainerHighest,
                uncheckedBorderColor     = MaterialTheme.colorScheme.outline,
            )
        )
    }
}

// ── Chips modo oscuro ──────────────────────────────────────────────────────────

@Composable
private fun DarkModeChips(
    current   : Boolean?,
    onSelected: (Boolean?) -> Unit
) {
    data class ChipOption(
        val value   : Boolean?,
        val labelRes: Int,
        val icon    : androidx.compose.ui.graphics.vector.ImageVector
    )

    val options = listOf(
        ChipOption(null,  R.string.settings_dark_mode_system, Icons.Filled.BrightnessAuto),
        ChipOption(false, R.string.settings_dark_mode_light,  Icons.Filled.Brightness7),
        ChipOption(true,  R.string.settings_dark_mode_dark,   Icons.Filled.Brightness4),
    )

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEachIndexed { i, option ->
            val selected = current == option.value

            val bgColor by animateColorAsState(
                targetValue   = if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                animationSpec = tween(200),
                label         = "chipBg$i"
            )
            val contentColor by animateColorAsState(
                targetValue   = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(200),
                label         = "chipFg$i"
            )
            val borderWidth by animateDpAsState(
                targetValue   = if (selected) 1.5.dp else 0.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label         = "chipBorder$i"
            )

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .border(
                        width = borderWidth,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(0f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                shape   = RoundedCornerShape(14.dp),
                color   = bgColor,
                onClick = { onSelected(option.value) }
            ) {
                Column(
                    modifier            = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    AnimatedContent(
                        targetState = selected,
                        transitionSpec = {
                            (fadeIn(tween(140)) + scaleIn(tween(140), initialScale = 0.6f)) togetherWith
                                    fadeOut(tween(100))
                        },
                        label = "chipIcon$i"
                    ) { isSel ->
                        Icon(
                            imageVector        = if (isSel) Icons.Outlined.Check else option.icon,
                            contentDescription = null,
                            tint               = contentColor,
                            modifier           = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text       = stringResource(option.labelRes),
                        style      = MaterialTheme.typography.labelSmall,
                        color      = contentColor,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ── Slider de columnas ────────────────────────────────────────────────────────

@Composable
private fun GridColumnsSlider(value: Int, onChange: (Int) -> Unit) {
    Slider(
        value         = value.toFloat(),
        onValueChange = { onChange(Math.round(it)) },
        valueRange    = 2f..5f,
        steps         = 2,
        modifier      = Modifier.fillMaxWidth(),
        colors = SliderDefaults.colors(
            thumbColor         = MaterialTheme.colorScheme.primary,
            activeTrackColor   = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            activeTickColor    = MaterialTheme.colorScheme.onPrimary,
            inactiveTickColor  = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
        )
    )
}

// ── Language selector ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelector(
    current            : AppLanguage,
    onLanguageSelected : (AppLanguage) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val languageLabels = mapOf(
        AppLanguage.SYSTEM     to stringResource(R.string.lang_system),
        AppLanguage.SPANISH    to stringResource(R.string.lang_es),
        AppLanguage.ENGLISH    to stringResource(R.string.lang_en),
        AppLanguage.FRENCH     to stringResource(R.string.lang_fr),
        AppLanguage.PORTUGUESE to stringResource(R.string.lang_pt),
        AppLanguage.GERMAN     to stringResource(R.string.lang_de),
    )

    ExposedDropdownMenuBox(
        expanded         = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier         = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value         = languageLabels[current] ?: stringResource(R.string.lang_system),
            onValueChange = {},
            readOnly      = true,
            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier      = Modifier
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            shape  = MaterialTheme.shapes.large,
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AppLanguage.values().forEach { lang ->
                DropdownMenuItem(
                    text     = { Text(languageLabels[lang] ?: lang.code) },
                    onClick  = { onLanguageSelected(lang); expanded = false },
                    trailingIcon = if (lang == current) ({
                        Icon(
                            Icons.Outlined.Check, null,
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }) else null
                )
            }
        }
    }
}