package eu.kanade.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction

@PreviewLightDark
@Composable
private fun NoActionPreview() {
    TachiyomiPreviewTheme {
        Surface {
            EmptyScreen(
                stringRes = MR.strings.empty_screen,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun WithActionPreview() {
    val actions = remember {
        persistentListOf(
            EmptyScreenAction(
                stringRes = MR.strings.action_retry,
                icon = Icons.Outlined.Refresh,
                onClick = {},
            ),
            EmptyScreenAction(
                stringRes = MR.strings.getting_started_guide,
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                onClick = {},
            ),
        )
    }

    TachiyomiPreviewTheme {
        Surface {
            EmptyScreen(
                stringRes = MR.strings.empty_screen,
                actions = actions,
            )
        }
    }
}

// Enhanced preview with multiple scenarios
@PreviewLightDark
@Composable
private fun EmptyScreenPreview(
    @PreviewParameter(EmptyScreenPreviewProvider::class) config: EmptyScreenPreviewConfig,
) {
    TachiyomiPreviewTheme {
        Surface {
            EmptyScreen(
                stringRes = config.messageRes,
                actions = config.actions,
                modifier = config.modifier,
            )
        }
    }
}

// Single preview with all variants using parameter provider
@PreviewLightDark
@Composable
private fun EmptyScreenAllVariantsPreview() {
    TachiyomiPreviewTheme {
        Surface {
            EmptyScreenPreviewParameterProvider().values.forEach { config ->
                EmptyScreen(
                    stringRes = config.messageRes,
                    actions = config.actions,
                    modifier = config.modifier,
                )
            }
        }
    }
}

// Data class for preview configuration
data class EmptyScreenPreviewConfig(
    val messageRes: MR.strings,
    val actions: ImmutableList<EmptyScreenAction> = persistentListOf(),
    val modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
)

// Preview parameter provider for multiple scenarios
class EmptyScreenPreviewProvider : PreviewParameterProvider<EmptyScreenPreviewConfig> {
    override val values: Sequence<EmptyScreenPreviewConfig>
        get() = sequenceOf(
            EmptyScreenPreviewConfig(
                messageRes = MR.strings.empty_screen,
            ),
            EmptyScreenPreviewConfig(
                messageRes = MR.strings.empty_screen,
                actions = persistentListOf(
                    EmptyScreenAction(
                        stringRes = MR.strings.action_retry,
                        icon = Icons.Outlined.Refresh,
                        onClick = {},
                    ),
                ),
            ),
            EmptyScreenPreviewConfig(
                messageRes = MR.strings.empty_screen,
                actions = persistentListOf(
                    EmptyScreenAction(
                        stringRes = MR.strings.action_retry,
                        icon = Icons.Outlined.Refresh,
                        onClick = {},
                    ),
                    EmptyScreenAction(
                        stringRes = MR.strings.getting_started_guide,
                        icon = Icons.AutoMirrored.Outlined.HelpOutline,
                        onClick = {},
                    ),
                ),
            ),
            EmptyScreenPreviewConfig(
                messageRes = MR.strings.information_no_results,
            ),
            EmptyScreenPreviewConfig(
                messageRes = MR.strings.information_no_results,
                actions = persistentListOf(
                    EmptyScreenAction(
                        stringRes = MR.strings.action_search,
                        icon = Icons.Outlined.Refresh,
                        onClick = {},
                    ),
                ),
            ),
        )
}

// Individual focused previews for specific use cases
@PreviewLightDark
@Composable
private fun EmptyScreenNoResultsPreview() {
    TachiyomiPreviewTheme {
        Surface {
            EmptyScreen(
                stringRes = MR.strings.information_no_results,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun EmptyScreenWithRetryPreview() {
    val retryAction = remember {
        persistentListOf(
            EmptyScreenAction(
                stringRes = MR.strings.action_retry,
                icon = Icons.Outlined.Refresh,
                onClick = {},
            ),
        )
    }

    TachiyomiPreviewTheme {
        Surface {
            EmptyScreen(
                stringRes = MR.strings.error_network,
                actions = retryAction,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun EmptyScreenWithHelpPreview() {
    val helpAction = remember {
        persistentListOf(
            EmptyScreenAction(
                stringRes = MR.strings.getting_started_guide,
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                onClick = {},
            ),
        )
    }

    TachiyomiPreviewTheme {
        Surface {
            EmptyScreen(
                stringRes = MR.strings.empty_screen,
                actions = helpAction,
            )
        }
    }
}

// Preview for long text scenarios
@PreviewLightDark
@Composable
private fun EmptyScreenLongTextPreview() {
    TachiyomiPreviewTheme {
        Surface {
            EmptyScreen(
                stringRes = MR.strings.information_empty_category_dialog,
            )
        }
    }
}

// Utility function for creating preview actions
@Composable
private fun rememberPreviewActions(): ImmutableList<EmptyScreenAction> {
    return remember {
        persistentListOf(
            EmptyScreenAction(
                stringRes = MR.strings.action_retry,
                icon = Icons.Outlined.Refresh,
                onClick = {},
            ),
            EmptyScreenAction(
                stringRes = MR.strings.action_search,
                icon = Icons.Outlined.Refresh, // Using refresh as placeholder
                onClick = {},
            ),
            EmptyScreenAction(
                stringRes = MR.strings.getting_started_guide,
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                onClick = {},
            ),
        )
    }
}

// Component preview that shows the EmptyScreen in different states
@PreviewLightDark
@Composable
private fun EmptyScreenStatesPreview() {
    TachiyomiPreviewTheme {
        Surface {
            androidx.compose.foundation.layout.Column {
                // No actions
                EmptyScreen(
                    stringRes = MR.strings.empty_screen,
                    modifier = androidx.compose.ui.Modifier.weight(1f)
                )
                
                // With single action
                EmptyScreen(
                    stringRes = MR.strings.error_network,
                    actions = persistentListOf(
                        EmptyScreenAction(
                            stringRes = MR.strings.action_retry,
                            icon = Icons.Outlined.Refresh,
                            onClick = {},
                        ),
                    ),
                    modifier = androidx.compose.ui.Modifier.weight(1f)
                )
                
                // With multiple actions
                EmptyScreen(
                    stringRes = MR.strings.information_no_results,
                    actions = rememberPreviewActions(),
                    modifier = androidx.compose.ui.Modifier.weight(1f)
                )
            }
        }
    }
}
