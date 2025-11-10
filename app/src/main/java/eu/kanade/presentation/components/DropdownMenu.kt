package eu.kanade.presentation.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import androidx.compose.material3.DropdownMenu as ComposeDropdownMenu

/**
 * DropdownMenu but overlaps anchor and has width constraints to better
 * match non-Compose implementation.
 */
@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(8.dp, (-56).dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded) return

    ComposeDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.sizeIn(minWidth = 196.dp, maxWidth = 196.dp),
        offset = offset,
        scrollState = scrollState,
        properties = properties,
        content = content,
    )
}

/**
 * Optimized version of DropdownMenu with additional configuration options.
 */
@Composable
fun DropdownMenuOptimized(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(8.dp, (-56).dp),
    minWidth: androidx.compose.ui.unit.Dp = 196.dp,
    maxWidth: androidx.compose.ui.unit.Dp = 196.dp,
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded) return

    ComposeDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.sizeIn(minWidth = minWidth, maxWidth = maxWidth),
        offset = offset,
        scrollState = scrollState,
        properties = properties,
        content = content,
    )
}

@Composable
fun RadioMenuItem(
    text: @Composable () -> Unit,
    isChecked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val iconTint = if (isChecked) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val contentDescription = if (isChecked) {
        stringResource(MR.strings.selected)
    } else {
        stringResource(MR.strings.not_selected)
    }

    DropdownMenuItem(
        text = text,
        onClick = onClick,
        trailingIcon = {
            Icon(
                imageVector = if (isChecked) {
                    Icons.Outlined.RadioButtonChecked
                } else {
                    Icons.Outlined.RadioButtonUnchecked
                },
                contentDescription = contentDescription,
                tint = iconTint,
            )
        },
        modifier = modifier,
        enabled = enabled,
    )
}

/**
 * Optimized RadioMenuItem with memoized properties.
 */
@Composable
fun RadioMenuItemOptimized(
    text: @Composable () -> Unit,
    isChecked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val (icon, contentDescription, tint) = remember(isChecked) {
        when {
            isChecked -> Triple(
                Icons.Outlined.RadioButtonChecked,
                stringResource(MR.strings.selected),
                MaterialTheme.colorScheme.primary
            )
            else -> Triple(
                Icons.Outlined.RadioButtonUnchecked,
                stringResource(MR.strings.not_selected),
                MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    DropdownMenuItem(
        text = text,
        onClick = onClick,
        trailingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
            )
        },
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
fun NestedMenuItem(
    text: @Composable () -> Unit,
    children: @Composable ColumnScope.(() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var nestedExpanded by remember { mutableStateOf(false) }
    val closeMenu = remember { { nestedExpanded = false } }

    // Auto-close when parent menu closes
    LaunchedEffect(enabled) {
        if (!enabled) {
            nestedExpanded = false
        }
    }

    Box {
        DropdownMenuItem(
            text = text,
            onClick = { nestedExpanded = true },
            trailingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowRight,
                    contentDescription = stringResource(MR.strings.action_open_submenu),
                )
            },
            enabled = enabled,
        )

        DropdownMenu(
            expanded = nestedExpanded && enabled,
            onDismissRequest = closeMenu,
            modifier = modifier,
        ) {
            children(closeMenu)
        }
    }
}

/**
 * Optimized nested menu with better state management.
 */
@Composable
fun NestedMenuItemOptimized(
    text: @Composable () -> Unit,
    children: @Composable ColumnScope.(() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var nestedExpanded by remember { mutableStateOf(false) }
    val closeMenu = remember { { nestedExpanded = false } }

    // Close nested menu when disabled
    LaunchedEffect(enabled) {
        if (!enabled) {
            nestedExpanded = false
        }
    }

    Box {
        DropdownMenuItem(
            text = text,
            onClick = { if (enabled) nestedExpanded = true },
            trailingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowRight,
                    contentDescription = stringResource(MR.strings.action_open_submenu),
                    tint = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            },
            enabled = enabled,
        )

        if (enabled) {
            DropdownMenuOptimized(
                expanded = nestedExpanded,
                onDismissRequest = closeMenu,
                modifier = modifier,
            ) {
                children(closeMenu)
            }
        }
    }
}

/**
 * Checkbox-style menu item for multi-select scenarios.
 */
@Composable
fun CheckboxMenuItem(
    text: @Composable () -> Unit,
    isChecked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val (icon, contentDescription, tint) = remember(isChecked) {
        when {
            isChecked -> Triple(
                Icons.Outlined.RadioButtonChecked, // Using same icon for consistency
                stringResource(MR.strings.checked),
                MaterialTheme.colorScheme.primary
            )
            else -> Triple(
                Icons.Outlined.RadioButtonUnchecked,
                stringResource(MR.strings.unchecked),
                MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    DropdownMenuItem(
        text = text,
        onClick = onClick,
        trailingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
            )
        },
        modifier = modifier,
        enabled = enabled,
    )
}

/**
 * Simple menu item without icons for maximum performance.
 */
@Composable
fun SimpleMenuItem(
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    DropdownMenuItem(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    )
}

// Configuration data class for dropdown menus
data class DropdownMenuConfig(
    val minWidth: androidx.compose.ui.unit.Dp = 196.dp,
    val maxWidth: androidx.compose.ui.unit.Dp = 196.dp,
    val offset: DpOffset = DpOffset(8.dp, (-56).dp),
    val focusable: Boolean = true,
)

@Composable
fun rememberDropdownMenuConfig(
    minWidth: androidx.compose.ui.unit.Dp = 196.dp,
    maxWidth: androidx.compose.ui.unit.Dp = 196.dp,
    offset: DpOffset = DpOffset(8.dp, (-56).dp),
    focusable: Boolean = true,
): DropdownMenuConfig {
    return remember(minWidth, maxWidth, offset, focusable) {
        DropdownMenuConfig(
            minWidth = minWidth,
            maxWidth = maxWidth,
            offset = offset,
            focusable = focusable,
        )
    }
}
