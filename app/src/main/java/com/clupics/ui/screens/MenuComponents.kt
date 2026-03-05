package com.clupics.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clupics.R

// ─────────────────────────────────────────────────────────────────────────────
// Data model
// ─────────────────────────────────────────────────────────────────────────────

/** Single action item shown in the bottom sheet */
data class MenuDropdownItem(
    val icon           : ImageVector,
    val label          : String,
    val isDestructive  : Boolean = false,
    val isChecked      : Boolean = false,   // ✓ indicator for sort options
    // true = el sheet permanece abierto al tocar (p.ej. opciones de sort)
    val keepSheetOpen  : Boolean = false,
    val onClick        : () -> Unit
)

/** A group that can be collapsed — used for the Sort submenu */
data class MenuDropdownGroup(
    val icon      : ImageVector,
    val label     : String,
    val items     : List<MenuDropdownItem>
)

/** Top-level sheet entry — either a single action or a collapsible group */
sealed class MenuEntry {
    data class Action(val item : MenuDropdownItem)  : MenuEntry()
    data class Group (val group: MenuDropdownGroup) : MenuEntry()
}

// ─────────────────────────────────────────────────────────────────────────────
// MenuSplitButton
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MenuSplitButton(
    menuLabel    : String,
    menuIcon     : ImageVector,
    onLeading    : () -> Unit,
    dropdownItems: List<MenuDropdownItem>,   // kept for backwards-compat — use entries for groups
    modifier     : Modifier = Modifier
) {
    MenuSplitButtonEntries(
        menuLabel = menuLabel,
        menuIcon  = menuIcon,
        onLeading = onLeading,
        entries   = dropdownItems.map { MenuEntry.Action(it) },
        modifier  = modifier
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MenuSplitButtonEntries(
    menuLabel : String,
    menuIcon  : ImageVector,
    onLeading : () -> Unit,
    entries   : List<MenuEntry>,
    modifier  : Modifier = Modifier
) {
    var sheetOpen by remember { mutableStateOf(false) }
    val chevronAngle by animateFloatAsState(
        targetValue   = if (sheetOpen) 180f else 0f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label         = "chevron"
    )

    SplitButtonLayout(
        modifier       = modifier,
        leadingButton  = {
            SplitButtonDefaults.LeadingButton(onClick = onLeading) {
                Icon(menuIcon, null, Modifier.size(SplitButtonDefaults.LeadingIconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(menuLabel, style = MaterialTheme.typography.labelLarge)
            }
        },
        trailingButton = {
            SplitButtonDefaults.TrailingButton(
                checked         = sheetOpen,
                onCheckedChange = { sheetOpen = it }
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    if (sheetOpen) "Cerrar" else "Abrir",
                    Modifier
                        .size(SplitButtonDefaults.TrailingIconSize)
                        .graphicsLayer { rotationZ = chevronAngle }
                )
            }
        }
    )

    if (sheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor   = MaterialTheme.colorScheme.surface,
            tonalElevation   = 0.dp
        ) {
            MenuSheetBody(entries = entries, onDismiss = { sheetOpen = false })
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sheet body — renders Action items and collapsible Group rows
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MenuSheetBody(entries: List<MenuEntry>, onDismiss: () -> Unit) {
    // Partition: actions first (normal + destructive), groups last
    val actionEntries      = entries.filterIsInstance<MenuEntry.Action>()
    val groupEntries       = entries.filterIsInstance<MenuEntry.Group>()
    val normalActions      = actionEntries.filter { !it.item.isDestructive }
    val destructiveActions = actionEntries.filter {  it.item.isDestructive }

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp, top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (normalActions.isNotEmpty())
            ActionGroup(normalActions.map { it.item }, onDismiss)
        if (groupEntries.isNotEmpty())
            groupEntries.forEach { GroupRow(it.group, onDismiss) }
        if (destructiveActions.isNotEmpty())
            ActionGroup(destructiveActions.map { it.item }, onDismiss)
    }
}

// ── Normal/destructive items grouped in a rounded container ──────────────────
@Composable
private fun ActionGroup(items: List<MenuDropdownItem>, onDismiss: () -> Unit) {
    Surface(
        modifier       = Modifier.fillMaxWidth(),
        shape          = RoundedCornerShape(20.dp),
        color          = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    ) {
        Column {
            items.forEachIndexed { index, item ->
                if (index > 0)
                    HorizontalDivider(
                        modifier  = Modifier.padding(start = 78.dp, end = 16.dp),
                        thickness = 0.5.dp,
                        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                SheetItemRow(item, onDismiss)
            }
        }
    }
}

// ── Collapsible group row (e.g. "Sort") ──────────────────────────────────────
@Composable
private fun GroupRow(group: MenuDropdownGroup, onDismiss: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val arrowAngle by animateFloatAsState(
        targetValue   = if (expanded) 180f else 0f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label         = "groupArrow"
    )
    // Checked item label to show as subtitle when collapsed
    val checkedLabel = group.items.firstOrNull { it.isChecked }?.label

    Surface(
        modifier       = Modifier.fillMaxWidth(),
        shape          = RoundedCornerShape(20.dp),
        color          = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    ) {
        Column {
            // Header row — tapping toggles expansion
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape    = RoundedCornerShape(12.dp),
                    color    = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(group.icon, null, Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        group.label,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Normal,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    if (!expanded && checkedLabel != null) {
                        Text(
                            checkedLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    if (expanded) "Contraer" else "Expandir",
                    Modifier.size(20.dp).graphicsLayer { rotationZ = arrowAngle },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expandable items
            AnimatedVisibility(
                visible = expanded,
                enter   = expandVertically() + fadeIn(),
                exit    = shrinkVertically() + fadeOut()
            ) {
                Column {
                    group.items.forEachIndexed { index, item ->
                        HorizontalDivider(
                            modifier  = Modifier.padding(start = 78.dp, end = 16.dp),
                            thickness = 0.5.dp,
                            color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                        SheetItemRow(item) { if (!item.keepSheetOpen) onDismiss(); item.onClick() }
                    }
                }
            }
        }
    }
}

// ── Single sheet item row ─────────────────────────────────────────────────────
@Composable
private fun SheetItemRow(item: MenuDropdownItem, onDismiss: () -> Unit = {}) {
    val textColor = if (item.isDestructive) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
    val iconBg    = if (item.isDestructive) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.secondaryContainer
    val iconTint  = if (item.isDestructive) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onSecondaryContainer

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDismiss(); item.onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape    = RoundedCornerShape(12.dp),
            color    = iconBg,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(item.icon, null, Modifier.size(24.dp), tint = iconTint)
            }
        }
        Text(
            item.label,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = if (item.isChecked) FontWeight.SemiBold else FontWeight.Normal,
            color      = if (item.isChecked) MaterialTheme.colorScheme.primary else textColor,
            modifier   = Modifier.weight(1f)
        )
        if (item.isChecked) {
            Icon(
                Icons.Filled.KeyboardArrowDown, // placeholder — we use a checkmark approach via color+weight
                null,
                Modifier.size(0.dp)  // invisible — weight+color convey active state
            )
        }
    }
}
