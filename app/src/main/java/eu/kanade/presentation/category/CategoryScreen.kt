package eu.kanade.presentation.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import eu.kanade.presentation.category.components.CategoryFloatingActionButton
import eu.kanade.presentation.category.components.CategoryListItem
import eu.kanade.presentation.components.AppBar
import eu.kanade.tachiyomi.ui.category.CategoryScreenState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.util.plus

@Composable
fun CategoryScreen(
    state: CategoryScreenState.Success,
    onClickCreate: () -> Unit,
    onClickRename: (Category) -> Unit,
    onClickDelete: (Category) -> Unit,
    onChangeOrder: (Category, Int) -> Unit,
    navigateUp: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(MR.strings.action_edit_categories),
                navigateUp = navigateUp,
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            CategoryFloatingActionButton(
                lazyListState = lazyListState,
                onCreate = onClickCreate,
            )
        },
    ) { paddingValues ->
        when {
            state.isEmpty -> EmptyCategoryScreen(paddingValues)
            else -> CategoryContent(
                categories = state.categories,
                lazyListState = lazyListState,
                paddingValues = paddingValues,
                onClickRename = onClickRename,
                onClickDelete = onClickDelete,
                onChangeOrder = onChangeOrder,
            )
        }
    }
}

@Composable
private fun EmptyCategoryScreen(
    paddingValues: PaddingValues,
) {
    EmptyScreen(
        stringRes = MR.strings.information_empty_category,
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
    )
}

@Composable
private fun CategoryContent(
    categories: List<Category>,
    lazyListState: LazyListState,
    paddingValues: PaddingValues,
    onClickRename: (Category) -> Unit,
    onClickDelete: (Category) -> Unit,
    onChangeOrder: (Category, Int) -> Unit,
) {
    // Use derived state to optimize list updates
    val categoriesState = remember(categories) {
        categories.toMutableStateList()
    }

    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        contentPadding = paddingValues,
        onMove = { from, to ->
            val item = categoriesState.removeAt(from.index)
            categoriesState.add(to.index, item)
            onChangeOrder(item, to.index)
        }
    )

    // Only update the state when not dragging and categories actually changed
    LaunchedEffect(categories) {
        if (!reorderableState.isAnyItemDragging && categories != categoriesState) {
            categoriesState.updateFrom(categories)
        }
    }

    CategoryList(
        categories = categoriesState,
        reorderableState = reorderableState,
        paddingValues = paddingValues,
        onClickRename = onClickRename,
        onClickDelete = onClickDelete,
    )
}

@Composable
private fun CategoryList(
    categories: SnapshotStateList<Category>,
    reorderableState: sh.calvin.reorderable.ReorderableLazyListState,
    paddingValues: PaddingValues,
    onClickRename: (Category) -> Unit,
    onClickDelete: (Category) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = reorderableState.lazyListState,
        contentPadding = paddingValues +
            topSmallPaddingValues +
            PaddingValues(horizontal = MaterialTheme.padding.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        items(
            items = categories,
            key = { category -> category.key },
        ) { category ->
            ReorderableItem(reorderableState, category.key) {
                CategoryListItem(
                    modifier = Modifier.animateItem(),
                    category = category,
                    onRename = { onClickRename(category) },
                    onDelete = { onClickDelete(category) },
                )
            }
        }
    }
}

/**
 * Efficiently updates the state list only if the content actually changed.
 */
private fun SnapshotStateList<Category>.updateFrom(newCategories: List<Category>) {
    if (this == newCategories) return
    
    // Clear and add all if sizes are different or we can't do a smart update
    if (size != newCategories.size || !containsAll(newCategories)) {
        clear()
        addAll(newCategories)
        return
    }
    
    // Smart update: only update items that changed
    for (i in indices) {
        if (this[i] != newCategories[i]) {
            this[i] = newCategories[i]
        }
    }
}

private val Category.key: String
    get() = "category-$id"

// Extension for better performance tracking
private val List<Category>.key: String
    get() = joinToString(prefix = "categories-") { it.id.toString() }
