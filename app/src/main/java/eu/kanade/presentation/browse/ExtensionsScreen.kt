package eu.kanade.presentation.browse

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.browse.components.BaseBrowseItem
import eu.kanade.presentation.browse.components.ExtensionIcon
import eu.kanade.presentation.components.WarningBanner
import eu.kanade.presentation.manga.components.DotSeparatorNoSpaceText
import eu.kanade.presentation.more.settings.screen.browse.ExtensionReposScreen
import eu.kanade.presentation.util.animateItemFastScroll
import eu.kanade.presentation.util.rememberRequestPackageInstallsPermissionState
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionUiModel
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionsScreenModel
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.system.launchRequestPackageInstallsPermission
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.theme.header
import tachiyomi.presentation.core.util.plus
import tachiyomi.presentation.core.util.secondaryItemAlpha

@Composable
fun ExtensionScreen(
    state: ExtensionsScreenModel.State,
    contentPadding: PaddingValues,
    searchQuery: String?,
    onLongClickItem: (Extension) -> Unit,
    onClickItemCancel: (Extension) -> Unit,
    onOpenWebView: (Extension.Available) -> Unit,
    onInstallExtension: (Extension.Available) -> Unit,
    onUninstallExtension: (Extension) -> Unit,
    onUpdateExtension: (Extension.Installed) -> Unit,
    onTrustExtension: (Extension.Untrusted) -> Unit,
    onOpenExtension: (Extension.Installed) -> Unit,
    onClickUpdateAll: () -> Unit,
    onRefresh: () -> Unit,
) {
    val navigator = LocalNavigator.currentOrThrow

    PullRefresh(
        refreshing = state.isRefreshing,
        onRefresh = onRefresh,
        enabled = !state.isLoading,
    ) {
        when {
            state.isLoading -> LoadingScreen(Modifier.padding(contentPadding))
            state.isEmpty -> ExtensionEmptyScreen(
                searchQuery = searchQuery,
                contentPadding = contentPadding,
                onOpenRepos = { navigator.push(ExtensionReposScreen()) },
            )
            else -> ExtensionContent(
                state = state,
                contentPadding = contentPadding,
                onLongClickItem = onLongClickItem,
                onClickItemCancel = onClickItemCancel,
                onOpenWebView = onOpenWebView,
                onInstallExtension = onInstallExtension,
                onUninstallExtension = onUninstallExtension,
                onUpdateExtension = onUpdateExtension,
                onTrustExtension = onTrustExtension,
                onOpenExtension = onOpenExtension,
                onClickUpdateAll = onClickUpdateAll,
            )
        }
    }
}

@Composable
private fun ExtensionEmptyScreen(
    searchQuery: String?,
    contentPadding: PaddingValues,
    onOpenRepos: () -> Unit,
) {
    val message = remember(searchQuery) {
        if (!searchQuery.isNullOrEmpty()) {
            MR.strings.no_results_found
        } else {
            MR.strings.empty_screen
        }
    }

    EmptyScreen(
        stringRes = message,
        modifier = Modifier.padding(contentPadding),
        actions = persistentListOf(
            EmptyScreenAction(
                stringRes = MR.strings.label_extension_repos,
                icon = Icons.Outlined.Settings,
                onClick = onOpenRepos,
            ),
        ),
    )
}

@Composable
private fun ExtensionContent(
    state: ExtensionsScreenModel.State,
    contentPadding: PaddingValues,
    onLongClickItem: (Extension) -> Unit,
    onClickItemCancel: (Extension) -> Unit,
    onOpenWebView: (Extension.Available) -> Unit,
    onInstallExtension: (Extension.Available) -> Unit,
    onUninstallExtension: (Extension) -> Unit,
    onUpdateExtension: (Extension.Installed) -> Unit,
    onTrustExtension: (Extension.Untrusted) -> Unit,
    onOpenExtension: (Extension.Installed) -> Unit,
    onClickUpdateAll: () -> Unit,
) {
    val context = LocalContext.current
    var trustState by remember { mutableStateOf<Extension.Untrusted?>(null) }
    val installGranted = rememberRequestPackageInstallsPermissionState(initialValue = true)

    // Auto-clear trust state after dialog is shown
    LaunchedEffect(trustState) {
        if (trustState != null) {
            // Auto-dismiss after 10 seconds if user doesn't interact
            kotlinx.coroutines.delay(10000)
            trustState = null
        }
    }

    FastScrollLazyColumn(
        contentPadding = contentPadding + topSmallPaddingValues,
    ) {
        if (!installGranted && state.installer?.requiresSystemPermission == true) {
            item(key = "extension-permissions-warning") {
                WarningBanner(
                    textRes = MR.strings.ext_permission_install_apps_warning,
                    modifier = Modifier.clickable {
                        context.launchRequestPackageInstallsPermission()
                    },
                )
            }
        }

        state.items.forEach { (header, items) ->
            item(
                contentType = "header",
                key = "extensionHeader-${header.hashCode()}",
            ) {
                ExtensionHeader(
                    header = header,
                    onClickUpdateAll = onClickUpdateAll,
                    modifier = Modifier.animateItemFastScroll(),
                )
            }

            items(
                items = items,
                contentType = { "item" },
                key = { item -> getExtensionItemKey(item) },
            ) { item ->
                ExtensionItem(
                    modifier = Modifier.animateItemFastScroll(),
                    item = item,
                    onClickItem = {
                        when (it) {
                            is Extension.Available -> onInstallExtension(it)
                            is Extension.Installed -> onOpenExtension(it)
                            is Extension.Untrusted -> trustState = it
                        }
                    },
                    onLongClickItem = onLongClickItem,
                    onClickItemSecondaryAction = {
                        when (it) {
                            is Extension.Available -> onOpenWebView(it)
                            is Extension.Installed -> onOpenExtension(it)
                            else -> {}
                        }
                    },
                    onClickItemCancel = onClickItemCancel,
                    onClickItemAction = {
                        when (it) {
                            is Extension.Available -> onInstallExtension(it)
                            is Extension.Installed -> {
                                if (it.hasUpdate) onUpdateExtension(it) else onOpenExtension(it)
                            }
                            is Extension.Untrusted -> trustState = it
                        }
                    },
                )
            }
        }
    }

    if (trustState != null) {
        ExtensionTrustDialog(
            extension = trustState!!,
            onTrust = {
                onTrustExtension(trustState!!)
                trustState = null
            },
            onUninstall = {
                onUninstallExtension(trustState!!)
                trustState = null
            },
            onDismiss = { trustState = null },
        )
    }
}

@Composable
private fun ExtensionHeader(
    header: ExtensionUiModel.Header,
    onClickUpdateAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (header) {
        is ExtensionUiModel.Header.Resource -> {
            val action: @Composable RowScope.() -> Unit =
                if (header.textRes == MR.strings.ext_updates_pending) {
                    {
                        Button(onClick = onClickUpdateAll) {
                            Text(stringResource(MR.strings.ext_update_all))
                        }
                    }
                } else {
                    {}
                }
            ExtensionHeader(
                text = stringResource(header.textRes),
                modifier = modifier,
                action = action,
            )
        }
        is ExtensionUiModel.Header.Text -> {
            ExtensionHeader(
                text = header.text,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun ExtensionItem(
    item: ExtensionUiModel.Item,
    onClickItem: (Extension) -> Unit,
    onLongClickItem: (Extension) -> Unit,
    onClickItemCancel: (Extension) -> Unit,
    onClickItemAction: (Extension) -> Unit,
    onClickItemSecondaryAction: (Extension) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (extension, installStep) = item
    
    BaseBrowseItem(
        modifier = modifier
            .combinedClickable(
                onClick = { onClickItem(extension) },
                onLongClick = { onLongClickItem(extension) },
            ),
        onClickItem = { onClickItem(extension) },
        onLongClickItem = { onLongClickItem(extension) },
        icon = {
            ExtensionIconWithProgress(
                extension = extension,
                installStep = installStep,
            )
        },
        action = {
            ExtensionItemActions(
                extension = extension,
                installStep = installStep,
                onClickItemCancel = onClickItemCancel,
                onClickItemAction = onClickItemAction,
                onClickItemSecondaryAction = onClickItemSecondaryAction,
            )
        },
    ) {
        ExtensionItemContent(
            extension = extension,
            installStep = installStep,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ExtensionIconWithProgress(
    extension: Extension,
    installStep: InstallStep,
) {
    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        val isIdle = installStep.isCompleted()
        
        if (!isIdle) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                strokeWidth = 2.dp,
            )
        }

        val padding by animateDpAsState(
            targetValue = if (isIdle) 0.dp else 8.dp,
            label = "iconPadding",
        )
        
        ExtensionIcon(
            extension = extension,
            modifier = Modifier
                .matchParentSize()
                .padding(padding),
        )
    }
}

@Composable
private fun ExtensionItemContent(
    extension: Extension,
    installStep: InstallStep,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(start = MaterialTheme.padding.medium),
    ) {
        Text(
            text = extension.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
        
        ExtensionMetadata(
            extension = extension,
            installStep = installStep,
        )
    }
}

@Composable
private fun ExtensionMetadata(
    extension: Extension,
    installStep: InstallStep,
) {
    FlowRow(
        modifier = Modifier.secondaryItemAlpha(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
    ) {
        ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
            val metadataItems = remember(extension, installStep) {
                buildExtensionMetadata(extension, installStep)
            }
            
            metadataItems.forEachIndexed { index, item ->
                if (index > 0) DotSeparatorNoSpaceText()
                Text(
                    text = item.text,
                    color = item.color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ExtensionItemActions(
    extension: Extension,
    installStep: InstallStep,
    modifier: Modifier = Modifier,
    onClickItemCancel: (Extension) -> Unit = {},
    onClickItemAction: (Extension) -> Unit = {},
    onClickItemSecondaryAction: (Extension) -> Unit = {},
) {
    val isIdle = installStep.isCompleted()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        when {
            !isIdle -> {
                CancelButton(
                    onClick = { onClickItemCancel(extension) }
                )
            }
            installStep == InstallStep.Error -> {
                RetryButton(
                    onClick = { onClickItemAction(extension) }
                )
            }
            installStep == InstallStep.Idle -> {
                when (extension) {
                    is Extension.Installed -> {
                        SettingsButton(
                            onClick = { onClickItemSecondaryAction(extension) }
                        )
                        if (extension.hasUpdate) {
                            UpdateButton(
                                onClick = { onClickItemAction(extension) }
                            )
                        }
                    }
                    is Extension.Untrusted -> {
                        TrustButton(
                            onClick = { onClickItemAction(extension) }
                        )
                    }
                    is Extension.Available -> {
                        if (extension.sources.isNotEmpty()) {
                            WebViewButton(
                                onClick = { onClickItemSecondaryAction(extension) }
                            )
                        }
                        InstallButton(
                            onClick = { onClickItemAction(extension) }
                        )
                    }
                }
            }
        }
    }
}

// Action button components for better reusability
@Composable
private fun CancelButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = stringResource(MR.strings.action_cancel),
        )
    }
}

@Composable
private fun RetryButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Outlined.Refresh,
            contentDescription = stringResource(MR.strings.action_retry),
        )
    }
}

@Composable
private fun SettingsButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = stringResource(MR.strings.action_settings),
        )
    }
}

@Composable
private fun UpdateButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Outlined.GetApp,
            contentDescription = stringResource(MR.strings.ext_update),
        )
    }
}

@Composable
private fun TrustButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Outlined.VerifiedUser,
            contentDescription = stringResource(MR.strings.ext_trust),
        )
    }
}

@Composable
private fun WebViewButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Outlined.Public,
            contentDescription = stringResource(MR.strings.action_open_in_web_view),
        )
    }
}

@Composable
private fun InstallButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Outlined.GetApp,
            contentDescription = stringResource(MR.strings.ext_install),
        )
    }
}

@Composable
private fun ExtensionHeader(
    text: String,
    modifier: Modifier = Modifier,
    action: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.padding(horizontal = MaterialTheme.padding.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .weight(1f),
            style = MaterialTheme.typography.header,
        )
        action()
    }
}

@Composable
private fun ExtensionTrustDialog(
    extension: Extension.Untrusted,
    onTrust: () -> Unit,
    onUninstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(text = stringResource(MR.strings.untrusted_extension))
        },
        text = {
            Text(
                text = stringResource(
                    MR.strings.untrusted_extension_message,
                    extension.name,
                    extension.pkgName
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onTrust) {
                Text(text = stringResource(MR.strings.ext_trust))
            }
        },
        dismissButton = {
            TextButton(onClick = onUninstall) {
                Text(text = stringResource(MR.strings.ext_uninstall))
            }
        },
        onDismissRequest = onDismiss,
    )
}

// Helper functions
private fun getExtensionItemKey(item: ExtensionUiModel.Item): String {
    return when (val extension = item.extension) {
        is Extension.Untrusted -> "untrusted-${extension.pkgName}"
        is Extension.Installed -> "installed-${extension.pkgName}"
        is Extension.Available -> "available-${extension.pkgName}"
    }
}

private data class ExtensionMetadataItem(
    val text: String,
    val color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
)

private fun buildExtensionMetadata(
    extension: Extension,
    installStep: InstallStep,
): List<ExtensionMetadataItem> {
    val items = mutableListOf<ExtensionMetadataItem>()
    val context = LocalContext.current

    if (extension is Extension.Installed && extension.lang.isNotEmpty()) {
        items.add(
            ExtensionMetadataItem(
                text = LocaleHelper.getSourceDisplayName(extension.lang, context)
            )
        )
    }

    if (extension.versionName.isNotEmpty()) {
        items.add(ExtensionMetadataItem(text = extension.versionName))
    }

    // Warning states
    when {
        extension is Extension.Untrusted -> {
            items.add(
                ExtensionMetadataItem(
                    text = stringResource(MR.strings.ext_untrusted).uppercase(),
                    color = MaterialTheme.colorScheme.error,
                )
            )
        }
        extension is Extension.Installed && extension.isObsolete -> {
            items.add(
                ExtensionMetadataItem(
                    text = stringResource(MR.strings.ext_obsolete).uppercase(),
                    color = MaterialTheme.colorScheme.error,
                )
            )
        }
        extension.isNsfw -> {
            items.add(
                ExtensionMetadataItem(
                    text = stringResource(MR.strings.ext_nsfw_short).uppercase(),
                    color = MaterialTheme.colorScheme.error,
                )
            )
        }
    }

    if (extension is Extension.Installed && !extension.isShared) {
        items.add(ExtensionMetadataItem(text = stringResource(MR.strings.ext_installer_private)))
    }

    // Installation state
    if (!installStep.isCompleted()) {
        items.add(
            ExtensionMetadataItem(
                text = when (installStep) {
                    InstallStep.Pending -> stringResource(MR.strings.ext_pending)
                    InstallStep.Downloading -> stringResource(MR.strings.ext_downloading)
                    InstallStep.Installing -> stringResource(MR.strings.ext_installing)
                    else -> error("Must not show non-install process text")
                }
            )
        )
    }

    return items
}
