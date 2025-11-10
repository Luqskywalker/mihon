package eu.kanade.presentation.category.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import sh.calvin.reorderable.ReorderableCollectionItemScope
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ReorderableCollectionItemScope.CategoryListItem(
    category: Category,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val horizontalPadding = remember {
        Modifier.padding(
            start = MaterialTheme.padding.small,
            end = MaterialTheme.padding.medium,
        )
    }

    ElevatedCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = enabled,
                    onClick = onRename
                )
                .padding(vertical = MaterialTheme.padding.small)
                .then(horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DragHandle()
            
            CategoryName(category.name)
            
            ActionButtons(
                onRename = onRename,
                onDelete = onDelete,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun ReorderableCollectionItemScope.DragHandle() {
    Icon(
        imageVector = Icons.Outlined.DragHandle,
        contentDescription = stringResource(MR.strings.action_reorder),
        modifier = Modifier
            .padding(MaterialTheme.padding.medium)
            .draggableHandle(),
    )
}

@Composable
private fun CategoryName(
    name: String,
) {
    Text(
        text = name,
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun ActionButtons(
    onRename: () -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean,
) {
    IconButton(
        onClick = onRename,
        enabled = enabled,
    ) {
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = stringResource(MR.strings.action_rename_category),
            modifier = if (!enabled) Modifier.alpha(0.38f) else Modifier,
        )
    }
    IconButton(
        onClick = onDelete,
        enabled = enabled,
    ) {
        Icon(
            imageVector = Icons.Outlined.Delete,
            contentDescription = stringResource(MR.strings.action_delete),
            modifier = if (!enabled) Modifier.alpha(0.38f) else Modifier,
        )
    }
}

// Alternative optimized version for better performance
@Composable
fun ReorderableCollectionItemScope.CategoryListItemOptimized(
    category: Category,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val cardModifier = remember(modifier) {
        modifier
    }

    ElevatedCard(modifier = cardModifier) {
        CategoryListItemContent(
            categoryName = category.name,
            onRename = onRename,
            onDelete = onDelete,
            enabled = enabled
        )
    }
}

@Composable
private fun ReorderableCollectionItemScope.CategoryListItemContent(
    categoryName: String,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onRename
            )
            .padding(vertical = MaterialTheme.padding.small)
            .padding(
                start = MaterialTheme.padding.small,
                end = MaterialTheme.padding.medium,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DragHandle()
        
        CategoryName(categoryName)
        
        ActionButtons(
            onRename = onRename,
            onDelete = onDelete,
            enabled = enabled
        )
    }
}

// Minimal version for maximum performance
@Composable
fun ReorderableCollectionItemScope.CategoryListItemMinimal(
    category: Category,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onRename)
                .padding(vertical = MaterialTheme.padding.small)
                .padding(
                    start = MaterialTheme.padding.small,
                    end = MaterialTheme.padding.medium,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.DragHandle,
                contentDescription = stringResource(MR.strings.action_reorder),
                modifier = Modifier
                    .padding(MaterialTheme.padding.medium)
                    .draggableHandle(),
            )
            Text(
                text = category.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            IconButton(onClick = onRename) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(MR.strings.action_rename_category),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(MR.strings.action_delete),
                )
            }
        }
    }
}
