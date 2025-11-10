package eu.kanade.presentation.browse

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.DisplayMetrics
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.kanade.domain.extension.interactor.ExtensionSourceItem
import eu.kanade.presentation.browse.components.ExtensionIcon
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.WarningBanner
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.presentation.more.settings.widget.TrailingWidgetBuffer
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.ui.browse.extension.details.ExtensionDetailsScreenModel
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.system.copyToClipboard
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen

@Composable
fun ExtensionDetailsScreen(
    navigateUp: () -> Unit,
    state: ExtensionDetailsScreenModel.State,
    onClickSourcePreferences: (sourceId: Long) -> Unit,
    onClickEnableAll: () -> Unit,
    onClickDisableAll: () -> Unit,
    onClickClearCookies: () -> Unit,
    onClickUninstall: () -> Unit,
    onClickSource: (sourceId: Long) -> Unit,
    onClickIncognito: (Boolean) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val repoUrl = remember(state.extension) {
        state.extension?.repoUrl?.let { extractGitHubUrl(it) } ?: state.extension?.repoUrl
    }

    Scaffold(
        topBar = { scrollBehavior ->
            ExtensionDetailsAppBar(
                repoUrl = repoUrl,
                navigateUp = navigateUp,
                onEnableAll = onClickEnableAll,
                onDisableAll = onClickDisableAll,
                onClearCookies = onClickClearCookies,
                scrollBehavior = scrollBehavior,
                uriHandler = uriHandler,
            )
        },
    ) { paddingValues ->
        ExtensionDetailsContent(
            state = state,
            contentPadding = paddingValues,
            onClickSourcePreferences = onClickSourcePreferences,
            onClickUninstall = onClickUninstall,
            onClickSource = onClickSource,
            onClickIncognito = onClickIncognito,
        )
    }
}

@Composable
private fun ExtensionDetailsAppBar(
    repoUrl: String?,
    navigateUp: () -> Unit,
    onEnableAll: () -> Unit,
    onDisableAll: () -> Unit,
    onClearCookies: () -> Unit,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior,
    uriHandler: androidx.compose.ui.platform.UriHandler,
) {
    AppBar(
        title = stringResource(MR.strings.label_extension_info),
        navigateUp = navigateUp,
        actions = {
            AppBarActions(
                actions = buildAppBarActions(
                    repoUrl = repoUrl,
                    onOpenRepo = { repoUrl?.let { uriHandler.openUri(it) } },
                    onEnableAll = onEnableAll,
                    onDisableAll = onDisableAll,
                    onClearCookies = onClearCookies,
                ),
            )
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun buildAppBarActions(
    repoUrl: String?,
    onOpenRepo: () -> Unit,
    onEnableAll: () -> Unit,
    onDisableAll: () -> Unit,
    onClearCookies: () -> Unit,
): ImmutableList<AppBar.AppBarAction> = remember(repoUrl) {
    persistentListOf<AppBar.AppBarAction>().builder()
        .apply {
            if (repoUrl != null) {
                add(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_open_repo),
                        icon = Icons.AutoMirrored.Outlined.Launch,
                        onClick = onOpenRepo,
                    ),
                )
            }
            addAll(
                listOf(
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_enable_all),
                        onClick = onEnableAll,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_disable_all),
                        onClick = onDisableAll,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.pref_clear_cookies),
                        onClick = onClearCookies,
                    ),
                ),
            )
        }
        .build()
}

@Composable
private fun ExtensionDetailsContent(
    state: ExtensionDetailsScreenModel.State,
    contentPadding: PaddingValues,
    onClickSourcePreferences: (sourceId: Long) -> Unit,
    onClickUninstall: () -> Unit,
    onClickSource: (sourceId: Long) -> Unit,
    onClickIncognito: (Boolean) -> Unit,
) {
    if (state.extension == null) {
        EmptyScreen(
            stringResource(MR.strings.empty_screen),
            modifier = Modifier.padding(contentPadding),
        )
        return
    }

    ExtensionDetails(
        contentPadding = contentPadding,
        extension = state.extension,
        sources = state.sources,
        incognitoMode = state.isIncognito,
        onClickSourcePreferences = onClickSourcePreferences,
        onClickUninstall = onClickUninstall,
        onClickSource = onClickSource,
        onClickIncognito = onClickIncognito,
    )
}

@Composable
private fun ExtensionDetails(
    contentPadding: PaddingValues,
    extension: Extension.Installed,
    sources: ImmutableList<ExtensionSourceItem>,
    incognitoMode: Boolean,
    onClickSourcePreferences: (sourceId: Long) -> Unit,
    onClickUninstall: () -> Unit,
    onClickSource: (sourceId: Long) -> Unit,
    onClickIncognito: (Boolean) -> Unit,
) {
    var showNsfwWarning by remember { mutableStateOf(false) }

    LaunchedEffect(showNsfwWarning) {
        // Auto-dismiss NSFW warning after 5 seconds
        if (showNsfwWarning) {
            kotlinx.coroutines.delay(5000)
            showNsfwWarning = false
        }
    }

    ScrollbarLazyColumn(
        contentPadding = contentPadding,
    ) {
        if (extension.isObsolete) {
            item {
                WarningBanner(MR.strings.obsolete_extension_message)
            }
        }

        item {
            DetailsHeader(
                extension = extension,
                extIncognitoMode = incognitoMode,
                onClickUninstall = onClickUninstall,
                onClickAgeRating = { showNsfwWarning = true },
                onExtIncognitoChange = onClickIncognito,
            )
        }

        items(
            items = sources,
            key = { it.source.id },
        ) { source ->
            SourceSwitchPreference(
                modifier = Modifier.animateItem(),
                source = source,
                onClickSourcePreferences = onClickSourcePreferences,
                onClickSource = onClickSource,
            )
        }
    }

    if (showNsfwWarning) {
        NsfwWarningDialog(
            onDismiss = { showNsfwWarning = false },
        )
    }
}

@Composable
private fun DetailsHeader(
    extension: Extension,
    extIncognitoMode: Boolean,
    onClickAgeRating: () -> Unit,
    onClickUninstall: () -> Unit,
    onExtIncognitoChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val isShared = extension is Extension.Installed && extension.isShared

    Column {
        ExtensionInfoHeader(
            extension = extension,
            onClickAgeRating = onClickAgeRating,
            onCopyDebugInfo = {
                context.copyToClipboard("Extension Debug information", buildDebugInfo(extension))
            },
        )

        ExtensionActionButtons(
            onUninstall = onClickUninstall,
            onAppInfo = if (isShared) {
                {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", extension.pkgName, null)
                        context.startActivity(this)
                    }
                }
            } else null,
        )

        IncognitoModePreference(
            enabled = extIncognitoMode,
            onEnabledChange = onExtIncognitoChange,
        )

        HorizontalDivider()
    }
}

@Composable
private fun ExtensionInfoHeader(
    extension: Extension,
    onClickAgeRating: () -> Unit,
    onCopyDebugInfo: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.padding.medium)
            .padding(
                top = MaterialTheme.padding.medium,
                bottom = MaterialTheme.padding.small,
            )
            .clickable(onClick = onCopyDebugInfo),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ExtensionIcon(
            modifier = Modifier.size(112.dp),
            extension = extension,
            density = DisplayMetrics.DENSITY_XXXHIGH,
        )

        Text(
            text = extension.name,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )

        Text(
            text = extension.pkgName.substringAfter("eu.kanade.tachiyomi.extension."),
            style = MaterialTheme.typography.bodySmall,
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.padding.extraLarge,
                vertical = MaterialTheme.padding.small,
            ),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InfoText(
            modifier = Modifier.weight(1f),
            primaryText = extension.versionName,
            secondaryText = stringResource(MR.strings.ext_info_version),
        )

        InfoDivider()

        InfoText(
            modifier = Modifier.weight(if (extension.isNsfw) 1.5f else 1f),
            primaryText = LocaleHelper.getSourceDisplayName(extension.lang, LocalContext.current),
            secondaryText = stringResource(MR.strings.ext_info_language),
        )

        if (extension.isNsfw) {
            InfoDivider()

            InfoText(
                modifier = Modifier.weight(1f),
                primaryText = stringResource(MR.strings.ext_nsfw_short),
                primaryTextStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                ),
                secondaryText = stringResource(MR.strings.ext_info_age_rating),
                onClick = onClickAgeRating,
            )
        }
    }
}

@Composable
private fun ExtensionActionButtons(
    onUninstall: () -> Unit,
    onAppInfo: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = MaterialTheme.padding.medium)
            .padding(top = MaterialTheme.padding.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
    ) {
        OutlinedButton(
            modifier = Modifier.weight(1f),
            onClick = onUninstall,
        ) {
            Text(stringResource(MR.strings.ext_uninstall))
        }

        if (onAppInfo != null) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = onAppInfo,
            ) {
                Text(stringResource(MR.strings.ext_app_info))
            }
        }
    }
}

@Composable
private fun IncognitoModePreference(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    TextPreferenceWidget(
        modifier = Modifier.padding(horizontal = MaterialTheme.padding.small),
        title = stringResource(MR.strings.pref_incognito_mode),
        subtitle = stringResource(MR.strings.pref_incognito_mode_extension_summary),
        icon = ImageVector.vectorResource(R.drawable.ic_glasses_24dp),
        widget = {
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                modifier = Modifier.padding(start = TrailingWidgetBuffer),
            )
        },
    )
}

@Composable
private fun InfoText(
    primaryText: String,
    secondaryText: String,
    modifier: Modifier = Modifier,
    primaryTextStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    onClick: (() -> Unit)? = null,
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(interactionSource = null, indication = null, onClick = onClick)
    } else {
        Modifier
    }

    Column(
        modifier = modifier.then(clickableModifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = primaryText,
            textAlign = TextAlign.Center,
            style = primaryTextStyle,
        )

        Text(
            text = secondaryText + if (onClick != null) " ⓘ" else "",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun InfoDivider() {
    VerticalDivider(
        modifier = Modifier.height(20.dp),
    )
}

@Composable
private fun SourceSwitchPreference(
    source: ExtensionSourceItem,
    onClickSourcePreferences: (sourceId: Long) -> Unit,
    onClickSource: (sourceId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sourceName = remember(source) {
        if (source.labelAsName) {
            source.source.toString()
        } else {
            LocaleHelper.getSourceDisplayName(source.source.lang, context)
        }
    }

    TextPreferenceWidget(
        modifier = modifier,
        title = sourceName,
        widget = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (source.source is ConfigurableSource) {
                    IconButton(onClick = { onClickSourcePreferences(source.source.id) }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(MR.strings.label_settings),
                        )
                    }
                }

                Switch(
                    checked = source.enabled,
                    onCheckedChange = null,
                    modifier = Modifier.padding(start = TrailingWidgetBuffer),
                )
            }
        },
        onPreferenceClick = { onClickSource(source.source.id) },
    )
}

@Composable
private fun NsfwWarningDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(MR.strings.ext_nsfw_warning_title)) },
        text = { Text(stringResource(MR.strings.ext_nsfw_warning)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(MR.strings.action_ok))
            }
        },
    )
}

// Helper functions
private fun extractGitHubUrl(repoUrl: String): String? {
    val regex = """https://raw.githubusercontent.com/(.+?)/(.+?)/.+""".toRegex()
    return regex.find(repoUrl)?.let {
        val (user, repo) = it.destructured
        "https://github.com/$user/$repo"
    }
}

private fun buildDebugInfo(extension: Extension): String = buildString {
    append(
        """
        Extension name: ${extension.name} (lang: ${extension.lang}; package: ${extension.pkgName})
        Extension version: ${extension.versionName} (lib: ${extension.libVersion}; version code: ${extension.versionCode})
        NSFW: ${extension.isNsfw}
        """.trimIndent(),
    )

    if (extension is Extension.Installed) {
        append("\n\n")
        append(
            """
            Update available: ${extension.hasUpdate}
            Obsolete: ${extension.isObsolete}
            Shared: ${extension.isShared}
            Repository: ${extension.repoUrl}
            """.trimIndent(),
        )
    }
}
