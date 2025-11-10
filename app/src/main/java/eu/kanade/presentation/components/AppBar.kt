package eu.kanade.presentation.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.clearFocusOnSoftKeyboardHide
import tachiyomi.presentation.core.util.runOnEnterKeyPressed
import tachiyomi.presentation.core.util.secondaryItemAlpha
import tachiyomi.presentation.core.util.showSoftKeyboard

const val SEARCH_DEBOUNCE_MILLIS = 250L

@Composable
fun AppBar(
    title: String?,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    subtitle: String? = null,
    navigateUp: (() -> Unit)? = null,
    navigationIcon: ImageVector? = null,
    actions: @Composable RowScope.() -> Unit = {},
    actionModeCounter: Int = 0,
    onCancelActionMode: () -> Unit = {},
    actionModeActions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val isActionMode by remember(actionModeCounter) {
        derivedStateOf { actionModeCounter > 0 }
    }

    AppBar(
        modifier = modifier,
        backgroundColor = backgroundColor,
        titleContent = {
            if (isActionMode) {
                AppBarTitle(actionModeCounter.toString())
            } else {
                AppBarTitle(title, subtitle = subtitle)
            }
        },
        navigateUp = navigateUp,
        navigationIcon = navigationIcon,
        actions = {
            if (isActionMode) {
                actionModeActions()
            } else {
                actions()
            }
        },
        isActionMode = isActionMode,
        onCancelActionMode = onCancelActionMode,
        scrollBehavior = scrollBehavior,
    )
}

@Composable
fun AppBar(
    titleContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    navigateUp: (() -> Unit)? = null,
    navigationIcon: ImageVector? = null,
    actions: @Composable RowScope.() -> Unit = {},
    isActionMode: Boolean = false,
    onCancelActionMode: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val containerColor = remember(backgroundColor, isActionMode) {
        backgroundColor ?: MaterialTheme.colorScheme.surfaceColorAtElevation(
            elevation = if (isActionMode) 3.dp else 0.dp,
        )
    }

    Column(modifier = modifier) {
        TopAppBar(
            navigationIcon = {
                if (isActionMode) {
                    IconButton(onClick = onCancelActionMode) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(MR.strings.action_cancel),
                        )
                    }
                } else {
                    navigateUp?.let {
                        IconButton(onClick = it) {
                            UpIcon(navigationIcon = navigationIcon)
                        }
                    }
                }
            },
            title = titleContent,
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
            ),
            scrollBehavior = scrollBehavior,
        )
    }
}

@Composable
fun AppBarTitle(
    title: String?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(modifier = modifier) {
        title?.let {
            Text(
                text = it,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(
                    repeatDelayMillis = 2_000,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun AppBarActions(
    actions: ImmutableList<AppBar.AppBarAction>,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    // Separate regular actions from overflow actions
    val (regularActions, overflowActions) = remember(actions) {
        actions.partition { it is AppBar.Action }
    }

    // Render regular actions with tooltips
    regularActions.forEach { action ->
        (action as? AppBar.Action)?.let { appBarAction ->
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text(appBarAction.title) } },
                state = rememberTooltipState(),
                focusable = false,
            ) {
                IconButton(
                    onClick = appBarAction.onClick,
                    enabled = appBarAction.enabled,
                    modifier = modifier,
                ) {
                    Icon(
                        imageVector = appBarAction.icon,
                        tint = appBarAction.iconTint ?: LocalContentColor.current,
                        contentDescription = appBarAction.title,
                    )
                }
            }
        }
    }

    // Render overflow menu if there are overflow actions
    if (overflowActions.isNotEmpty()) {
        OverflowMenu(
            actions = overflowActions,
            showMenu = showMenu,
            onShowMenuChange = { showMenu = it },
            modifier = modifier,
        )
    }
}

@Composable
private fun OverflowMenu(
    actions: List<AppBar.AppBarAction>,
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(stringResource(MR.strings.action_menu_overflow_description))
            }
        },
        state = rememberTooltipState(),
        focusable = false,
    ) {
        IconButton(
            onClick = { onShowMenuChange(!showMenu) },
            modifier = modifier,
        ) {
            Icon(
                Icons.Outlined.MoreVert,
                contentDescription = stringResource(MR.strings.action_menu_overflow_description),
            )
        }
    }

    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { onShowMenuChange(false) },
    ) {
        actions.forEach { action ->
            (action as? AppBar.OverflowAction)?.let { overflowAction ->
                DropdownMenuItem(
                    onClick = {
                        overflowAction.onClick()
                        onShowMenuChange(false)
                    },
                    text = { 
                        Text(
                            overflowAction.title, 
                            fontWeight = FontWeight.Normal,
                            style = MaterialTheme.typography.bodyMedium,
                        ) 
                    },
                )
            }
        }
    }
}

@Composable
fun SearchToolbar(
    searchQuery: String?,
    onChangeSearchQuery: (String?) -> Unit,
    modifier: Modifier = Modifier,
    titleContent: @Composable () -> Unit = {},
    navigateUp: (() -> Unit)? = null,
    searchEnabled: Boolean = true,
    placeholderText: String? = null,
    onSearch: (String) -> Unit = {},
    onClickCloseSearch: () -> Unit = { onChangeSearchQuery(null) },
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val focusRequester = remember { FocusRequester() }
    val isSearchActive = searchQuery != null

    // Auto-focus when search becomes active
    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    AppBar(
        modifier = modifier,
        titleContent = {
            if (!isSearchActive) {
                titleContent()
            } else {
                SearchTextField(
                    searchQuery = searchQuery ?: "",
                    onChangeSearchQuery = onChangeSearchQuery,
                    onSearch = onSearch,
                    focusRequester = focusRequester,
                    placeholderText = placeholderText,
                    visualTransformation = visualTransformation,
                    interactionSource = interactionSource,
                )
            }
        },
        navigateUp = if (!isSearchActive) navigateUp else onClickCloseSearch,
        actions = {
            SearchActions(
                searchEnabled = searchEnabled,
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onChangeSearchQuery = onChangeSearchQuery,
                focusRequester = focusRequester,
            )
            key("custom-actions") { actions() }
        },
        isActionMode = false,
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun SearchTextField(
    searchQuery: String,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    focusRequester: FocusRequester,
    placeholderText: String?,
    visualTransformation: VisualTransformation,
    interactionSource: MutableInteractionSource,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val searchAndClearFocus: () -> Unit = {
        if (searchQuery.isNotBlank()) {
            onSearch(searchQuery)
        }
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    BasicTextField(
        value = searchQuery,
        onValueChange = onChangeSearchQuery,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .runOnEnterKeyPressed(action = searchAndClearFocus)
            .showSoftKeyboard(remember { searchQuery.isEmpty() })
            .clearFocusOnSoftKeyboardHide(),
        textStyle = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { searchAndClearFocus() }),
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            TextFieldDefaults.DecorationBox(
                value = searchQuery,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = true,
                visualTransformation = visualTransformation,
                interactionSource = interactionSource,
                placeholder = {
                    Text(
                        modifier = Modifier.secondaryItemAlpha(),
                        text = placeholderText ?: stringResource(MR.strings.action_search_hint),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                container = {},
            )
        },
    )
}

@Composable
private fun SearchActions(
    searchEnabled: Boolean,
    isSearchActive: Boolean,
    searchQuery: String?,
    onChangeSearchQuery: (String?) -> Unit,
    focusRequester: FocusRequester,
) {
    if (!searchEnabled) return

    key("search-actions") {
        when {
            !isSearchActive -> {
                SearchActionButton(
                    title = stringResource(MR.strings.action_search),
                    icon = Icons.Outlined.Search,
                    onClick = { onChangeSearchQuery("") }
                )
            }
            !searchQuery.isNullOrEmpty() -> {
                SearchActionButton(
                    title = stringResource(MR.strings.action_reset),
                    icon = Icons.Outlined.Close,
                    onClick = {
                        onChangeSearchQuery("")
                        focusRequester.requestFocus()
                    }
                )
            }
        }
    }
}

@Composable
private fun SearchActionButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(title) } },
        state = rememberTooltipState(),
        focusable = false,
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = title)
        }
    }
}

@Composable
fun UpIcon(
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
) {
    val icon = navigationIcon ?: Icons.AutoMirrored.Outlined.ArrowBack
    Icon(
        imageVector = icon,
        contentDescription = stringResource(MR.strings.action_bar_up_description),
        modifier = modifier,
    )
}

sealed interface AppBar {
    sealed interface AppBarAction

    data class Action(
        val title: String,
        val icon: ImageVector,
        val iconTint: Color? = null,
        val onClick: () -> Unit,
        val enabled: Boolean = true,
    ) : AppBarAction

    data class OverflowAction(
        val title: String,
        val onClick: () -> Unit,
    ) : AppBarAction
}
