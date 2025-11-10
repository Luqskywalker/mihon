package eu.kanade.presentation.browse.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import tachiyomi.presentation.core.components.material.padding

@Composable
fun BaseBrowseItem(
    modifier: Modifier = Modifier,
    onClickItem: () -> Unit = {},
    onLongClickItem: (() -> Unit)? = null,
    enabled: Boolean = true,
    contentAlpha: Float = 1f,
    icon: @Composable RowScope.() -> Unit = {},
    action: @Composable RowScope.() -> Unit = {},
    content: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .alpha(if (enabled) contentAlpha else contentAlpha * 0.5f)
            .browseItemClickable(
                onClick = onClickItem,
                onLongClick = onLongClickItem,
                enabled = enabled,
            )
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        content()
        action()
    }
}

@Composable
fun BaseBrowseItem(
    modifier: Modifier = Modifier,
    onClickItem: () -> Unit = {},
    onLongClickItem: (() -> Unit)? = null,
    enabled: Boolean = true,
    contentAlpha: Float = 1f,
    icon: @Composable () -> Unit = {},
    action: @Composable () -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .alpha(if (enabled) contentAlpha else contentAlpha * 0.5f)
            .browseItemClickable(
                onClick = onClickItem,
                onLongClick = onLongClickItem,
                enabled = enabled,
            )
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        content()
        action()
    }
}

private fun Modifier.browseItemClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    enabled: Boolean,
): Modifier {
    return if (onLongClick != null) {
        this.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
            enabled = enabled,
            role = Role.Button,
        )
    } else {
        this.clickable(
            onClick = onClick,
            enabled = enabled,
            role = Role.Button,
        )
    }
}

// Specialized variants for common use cases
@Composable
fun CompactBrowseItem(
    modifier: Modifier = Modifier,
    onClickItem: () -> Unit = {},
    onLongClickItem: (() -> Unit)? = null,
    enabled: Boolean = true,
    contentAlpha: Float = 1f,
    content: @Composable RowScope.() -> Unit = {},
    action: @Composable RowScope.() -> Unit = {},
) {
    BaseBrowseItem(
        modifier = modifier,
        onClickItem = onClickItem,
        onLongClickItem = onLongClickItem,
        enabled = enabled,
        contentAlpha = contentAlpha,
        content = content,
        action = action,
    )
}

@Composable
fun IconBrowseItem(
    modifier: Modifier = Modifier,
    onClickItem: () -> Unit = {},
    onLongClickItem: (() -> Unit)? = null,
    enabled: Boolean = true,
    contentAlpha: Float = 1f,
    icon: @Composable RowScope.() -> Unit = {},
    content: @Composable RowScope.() -> Unit = {},
) {
    BaseBrowseItem(
        modifier = modifier,
        onClickItem = onClickItem,
        onLongClickItem = onLongClickItem,
        enabled = enabled,
        contentAlpha = contentAlpha,
        icon = icon,
        content = content,
    )
}

@Composable
fun ActionBrowseItem(
    modifier: Modifier = Modifier,
    onClickItem: () -> Unit = {},
    onLongClickItem: (() -> Unit)? = null,
    enabled: Boolean = true,
    contentAlpha: Float = 1f,
    content: @Composable RowScope.() -> Unit = {},
    action: @Composable RowScope.() -> Unit = {},
) {
    BaseBrowseItem(
        modifier = modifier,
        onClickItem = onClickItem,
        onLongClickItem = onLongClickItem,
        enabled = enabled,
        contentAlpha = contentAlpha,
        content = content,
        action = action,
    )
}

// Performance-optimized version for lists
@Composable
fun BrowseItem(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    selected: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = modifier
            .background(backgroundColor)
            .browseItemClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                enabled = enabled,
            )
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

// Extension for conditional background
private fun Modifier.background(color: androidx.compose.ui.graphics.Color): Modifier {
    return this.then(androidx.compose.foundation.background(color))
}

// Utility for consistent spacing
@Composable
fun BrowseItemDefaults.ContentSpacing() {
    androidx.compose.foundation.layout.Spacer(
        modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium)
    )
}

// Standard icon size for browse items
val BrowseItemIconSize = 40.dp

// Standard content modifiers
val BrowseItemContentModifier = Modifier.weight(1f)

// Predefined content arrangements
object BrowseItemArrangements {
    val IconContentAction = @Composable { 
        icon: @Composable () -> Unit,
        content: @Composable () -> Unit,
        action: @Composable () -> Unit,
    ->
        icon()
        BrowseItemDefaults.ContentSpacing()
        content()
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
        action()
    }

    val ContentAction = @Composable { 
        content: @Composable () -> Unit,
        action: @Composable () -> Unit,
    ->
        content()
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
        action()
    }

    val IconContent = @Composable { 
        icon: @Composable () -> Unit,
        content: @Composable () -> Unit,
    ->
        icon()
        BrowseItemDefaults.ContentSpacing()
        content()
    }
}

// Usage examples:
/*
// Basic usage
BaseBrowseItem(
    onClickItem = { /* handle click */ },
    onLongClickItem = { /* handle long click */ },
    icon = {
        ExtensionIcon(extension = extension)
    },
    content = {
        Text(text = extension.name)
    },
    action = {
        InstallButton(onClick = { /* install */ })
    }
)

// Compact version
CompactBrowseItem(
    onClickItem = { /* handle click */ },
    content = {
        Text(text = "Compact item")
    },
    action = {
        Badge(text = "42")
    }
)

// Performance-optimized for lists
BrowseItem(
    onClick = { /* handle click */ },
    selected = isSelected,
    content = {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null
        )
        BrowseItemDefaults.ContentSpacing()
        Text(
            text = "Optimized item",
            modifier = BrowseItemContentModifier
        )
        Badge(text = "New")
    }
)
*/
