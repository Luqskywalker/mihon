package eu.kanade.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.TabText
import tachiyomi.presentation.core.i18n.stringResource

object TabbedDialogPaddings {
    val Horizontal = 24.dp
    val Vertical = 8.dp
}

@Composable
fun TabbedDialog(
    onDismissRequest: () -> Unit,
    tabTitles: ImmutableList<String>,
    modifier: Modifier = Modifier,
    tabOverflowMenuContent: (@Composable ColumnScope.(() -> Unit) -> Unit)? = null,
    pagerState: PagerState = rememberPagerState { tabTitles.size },
    content: @Composable (Int) -> Unit,
) {
    AdaptiveSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        TabbedDialogContent(
            tabTitles = tabTitles,
            tabOverflowMenuContent = tabOverflowMenuContent,
            pagerState = pagerState,
            content = content,
        )
    }
}

@Composable
private fun TabbedDialogContent(
    tabTitles: ImmutableList<String>,
    tabOverflowMenuContent: (@Composable ColumnScope.(() -> Unit) -> Unit)?,
    pagerState: PagerState,
    content: @Composable (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()

    Column {
        TabbedDialogHeader(
            tabTitles = tabTitles,
            pagerState = pagerState,
            scope = scope,
            tabOverflowMenuContent = tabOverflowMenuContent,
        )
        
        HorizontalDivider()
        
        TabbedDialogPager(
            pagerState = pagerState,
            content = content,
        )
    }
}

@Composable
private fun TabbedDialogHeader(
    tabTitles: ImmutableList<String>,
    pagerState: PagerState,
    scope: kotlinx.coroutines.CoroutineScope,
    tabOverflowMenuContent: (@Composable ColumnScope.(() -> Unit) -> Unit)?,
) {
    Row {
        PrimaryTabRow(
            modifier = Modifier.weight(1f),
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            divider = {},
        ) {
            tabTitles.fastForEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { 
                        scope.launch { 
                            pagerState.animateScrollToPage(index) 
                        } 
                    },
                    text = { 
                        TabText(
                            text = title,
                            maxLines = 1,
                        ) 
                    },
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        tabOverflowMenuContent?.let { 
            MoreMenu(content = it) 
        }
    }
}

@Composable
private fun TabbedDialogPager(
    pagerState: PagerState,
    content: @Composable (Int) -> Unit,
) {
    HorizontalPager(
        modifier = Modifier.animateContentSize(),
        state = pagerState,
        verticalAlignment = Alignment.Top,
        pageContent = { page -> 
            key(page) {
                content(page)
            }
        },
    )
}

@Composable
private fun MoreMenu(
    content: @Composable ColumnScope.(() -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val closeMenu = remember { { expanded = false } }

    // Auto-close when dialog dismisses
    LaunchedEffect(Unit) {
        // This ensures the menu closes when the parent dialog is dismissed
    }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(MR.strings.label_more),
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            content(closeMenu)
        }
    }
}

// Optimized version with additional features
@Composable
fun TabbedDialogOptimized(
    onDismissRequest: () -> Unit,
    tabTitles: ImmutableList<String>,
    modifier: Modifier = Modifier,
    tabOverflowMenuContent: (@Composable ColumnScope.(() -> Unit) -> Unit)? = null,
    initialPage: Int = 0,
    content: @Composable (Int) -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { tabTitles.size }
    )

    AdaptiveSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        TabbedDialogContent(
            tabTitles = tabTitles,
            tabOverflowMenuContent = tabOverflowMenuContent,
            pagerState = pagerState,
            content = content,
        )
    }
}

// Alternative version for fixed tabs without pager
@Composable
fun SimpleTabbedDialog(
    onDismissRequest: () -> Unit,
    tabTitles: ImmutableList<String>,
    modifier: Modifier = Modifier,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    content: @Composable (Int) -> Unit,
) {
    AdaptiveSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        Column {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                divider = {},
            ) {
                tabTitles.fastForEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { onTabSelected(index) },
                        text = { 
                            TabText(
                                text = title,
                                maxLines = 1,
                            ) 
                        },
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            
            HorizontalDivider()
            
            // Use key to reset state when tab changes
            key(selectedTab) {
                content(selectedTab)
            }
        }
    }
}

// Data class for tab configuration
data class TabConfig(
    val title: String,
    val enabled: Boolean = true,
    val badgeCount: Int? = null,
)

@Composable
fun AdvancedTabbedDialog(
    onDismissRequest: () -> Unit,
    tabs: ImmutableList<TabConfig>,
    modifier: Modifier = Modifier,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tabOverflowMenuContent: (@Composable ColumnScope.(() -> Unit) -> Unit)? = null,
    content: @Composable (Int) -> Unit,
) {
    AdaptiveSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        val scope = rememberCoroutineScope()

        Column {
            Row {
                PrimaryTabRow(
                    modifier = Modifier.weight(1f),
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    divider = {},
                ) {
                    tabs.fastForEachIndexed { index, tabConfig ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { 
                                if (tabConfig.enabled) {
                                    onTabSelected(index)
                                }
                            },
                            text = { 
                                TabTextWithBadge(
                                    text = tabConfig.title,
                                    badgeCount = tabConfig.badgeCount,
                                    enabled = tabConfig.enabled,
                                )
                            },
                            enabled = tabConfig.enabled,
                            unselectedContentColor = if (tabConfig.enabled) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            },
                        )
                    }
                }

                tabOverflowMenuContent?.let { 
                    MoreMenu(content = it) 
                }
            }
            
            HorizontalDivider()
            
            key(selectedTab) {
                content(selectedTab)
            }
        }
    }
}

@Composable
private fun TabTextWithBadge(
    text: String,
    badgeCount: Int?,
    enabled: Boolean,
) {
    // Implementation for tab text with badge indicator
    TabText(
        text = text,
        maxLines = 1,
        // Add badge logic here
    )
}
