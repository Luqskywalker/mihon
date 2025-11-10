package eu.kanade.presentation.category.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.ExtendedFloatingActionButton
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.shouldExpandFAB

@Composable
fun CategoryFloatingActionButton(
    lazyListState: LazyListState,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val expanded by remember(lazyListState) {
        lazyListState.shouldExpandFAB()
    }

    ExtendedFloatingActionButton(
        text = { 
            CategoryFABText(expanded = expanded) 
        },
        icon = { 
            CategoryFABIcon() 
        },
        onClick = onCreate,
        expanded = expanded,
        modifier = modifier,
    )
}

@Composable
private fun CategoryFABText(
    expanded: Boolean,
) {
    if (expanded) {
        Text(text = stringResource(MR.strings.action_add))
    }
}

@Composable
private fun CategoryFABIcon() {
    Icon(
        imageVector = Icons.Outlined.Add,
        contentDescription = stringResource(MR.strings.action_add_category),
    )
}

// Alternative optimized version with additional features
@Composable
fun CategoryFloatingActionButtonOptimized(
    lazyListState: LazyListState,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val expanded by remember(lazyListState) {
        lazyListState.shouldExpandFAB()
    }

    ExtendedFloatingActionButton(
        text = { 
            CategoryFABText(expanded = expanded) 
        },
        icon = { 
            CategoryFABIcon() 
        },
        onClick = onCreate,
        expanded = expanded,
        modifier = modifier,
        enabled = enabled,
    )
}

// Minimal version for maximum performance
@Composable
fun CategoryFloatingActionButtonMinimal(
    lazyListState: LazyListState,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val expanded by remember(lazyListState) {
        lazyListState.shouldExpandFAB()
    }

    ExtendedFloatingActionButton(
        text = { 
            if (expanded) {
                Text(text = stringResource(MR.strings.action_add))
            }
        },
        icon = { 
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(MR.strings.action_add_category),
            )
        },
        onClick = onCreate,
        expanded = expanded,
        modifier = modifier,
    )
}
