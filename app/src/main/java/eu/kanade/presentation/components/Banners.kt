package eu.kanade.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Stable
val DownloadedOnlyBannerBackgroundColor
    @Composable get() = MaterialTheme.colorScheme.tertiary

@Stable
val IncognitoModeBannerBackgroundColor
    @Composable get() = MaterialTheme.colorScheme.primary

@Stable
val IndexingBannerBackgroundColor
    @Composable get() = MaterialTheme.colorScheme.secondary

@Composable
fun WarningBanner(
    textRes: StringResource,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(textRes),
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.error)
            .padding(16.dp),
        color = MaterialTheme.colorScheme.onError,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun AppStateBanners(
    downloadedOnlyMode: Boolean,
    incognitoMode: Boolean,
    indexing: Boolean,
    modifier: Modifier = Modifier,
) {
    val hasAnyBanner by remember(downloadedOnlyMode, incognitoMode, indexing) {
        derivedStateOf { downloadedOnlyMode || incognitoMode || indexing }
    }

    if (!hasAnyBanner) return

    val density = LocalDensity.current
    val mainInsets = WindowInsets.statusBars
    val mainInsetsTop = with(density) { mainInsets.getTop().toDp() }

    SubcomposeLayout(modifier = modifier) { constraints ->
        // Measure each banner and calculate positions
        val bannersData = listOf(
            BannerType.Indexing to indexing,
            BannerType.DownloadedOnly to downloadedOnlyMode,
            BannerType.Incognito to incognitoMode
        )

        val measuredBanners = bannersData.mapNotNull { (bannerType, isVisible) ->
            if (!isVisible) return@mapNotNull null
            
            val placeable = subcompose(bannerType.ordinal) {
                BannerContent(bannerType, mainInsetsTop, measuredBanners)
            }.fastMap { it.measure(constraints) }.firstOrNull()
            
            placeable?.let { BannerMeasurement(bannerType, it) }
        }

        val totalHeight = measuredBanners.sumOf { it.placeable.height }

        layout(constraints.maxWidth, totalHeight) {
            var currentY = 0
            measuredBanners.forEach { bannerMeasurement ->
                bannerMeasurement.placeable.place(0, currentY)
                currentY += bannerMeasurement.placeable.height
            }
        }
    }
}

@Composable
private fun BannerContent(
    bannerType: BannerType,
    mainInsetsTop: androidx.compose.ui.unit.Dp,
    previousBanners: List<BannerMeasurement>,
) {
    val accumulatedHeight = remember(previousBanners) {
        previousBanners.takeWhile { it.type != bannerType }
            .sumOf { it.placeable.height }
            .toDp()
    }

    val topPadding = (mainInsetsTop - accumulatedHeight).coerceAtLeast(0.dp)

    AnimatedVisibility(
        visible = true,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        when (bannerType) {
            BannerType.Indexing -> IndexingDownloadBanner(
                modifier = Modifier.windowInsetsPadding(WindowInsets(top = topPadding))
            )
            BannerType.DownloadedOnly -> DownloadedOnlyModeBanner(
                modifier = Modifier.windowInsetsPadding(WindowInsets(top = topPadding))
            )
            BannerType.Incognito -> IncognitoModeBanner(
                modifier = Modifier.windowInsetsPadding(WindowInsets(top = topPadding))
            )
        }
    }
}

@Composable
private fun DownloadedOnlyModeBanner(modifier: Modifier = Modifier) {
    BannerContent(
        text = stringResource(MR.strings.label_downloaded_only),
        backgroundColor = DownloadedOnlyBannerBackgroundColor,
        textColor = MaterialTheme.colorScheme.onTertiary,
        modifier = modifier,
    )
}

@Composable
private fun IncognitoModeBanner(modifier: Modifier = Modifier) {
    BannerContent(
        text = stringResource(MR.strings.pref_incognito_mode),
        backgroundColor = IncognitoModeBannerBackgroundColor,
        textColor = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier,
    )
}

@Composable
private fun IndexingDownloadBanner(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    var textHeight by remember { mutableStateOf(0.dp) }

    Row(
        modifier = Modifier
            .background(color = IndexingBannerBackgroundColor)
            .fillMaxWidth()
            .padding(8.dp)
            .then(modifier),
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.requiredSize(textHeight),
            color = MaterialTheme.colorScheme.onSecondary,
            strokeWidth = with(density) { (textHeight / 8).toPx() },
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(MR.strings.download_notifier_cache_renewal),
            color = MaterialTheme.colorScheme.onSecondary,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            onTextLayout = { textLayoutResult ->
                with(density) {
                    textHeight = textLayoutResult.size.height.toDp()
                }
            },
        )
    }
}

@Composable
private fun BannerContent(
    text: String,
    backgroundColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = Modifier
            .background(backgroundColor)
            .fillMaxWidth()
            .padding(4.dp)
            .then(modifier),
        color = textColor,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelMedium,
    )
}

// Alternative optimized version using Layout for better performance
@Composable
fun AppStateBannersOptimized(
    downloadedOnlyMode: Boolean,
    incognitoMode: Boolean,
    indexing: Boolean,
    modifier: Modifier = Modifier,
) {
    val banners = remember(downloadedOnlyMode, incognitoMode, indexing) {
        listOf(
            BannerConfig(BannerType.Indexing, indexing),
            BannerConfig(BannerType.DownloadedOnly, downloadedOnlyMode),
            BannerConfig(BannerType.Incognito, incognitoMode)
        ).filter { it.isVisible }
    }

    if (banners.isEmpty()) return

    Layout(
        modifier = modifier,
        content = {
            banners.forEach { config ->
                key(config.type) {
                    AnimatedVisibility(
                        visible = true,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        when (config.type) {
                            BannerType.Indexing -> IndexingDownloadBanner()
                            BannerType.DownloadedOnly -> DownloadedOnlyModeBanner()
                            BannerType.Incognito -> IncognitoModeBanner()
                        }
                    }
                }
            }
        }
    ) { measurables, constraints ->
        val placeables = measurables.fastMap { it.measure(constraints) }
        val totalHeight = placeables.sumOf { it.height }

        layout(constraints.maxWidth, totalHeight) {
            var currentY = 0
            placeables.fastForEach { placeable ->
                placeable.place(0, currentY)
                currentY += placeable.height
            }
        }
    }
}

// Data classes for better type safety and organization
private data class BannerMeasurement(
    val type: BannerType,
    val placeable: androidx.compose.ui.layout.Placeable
)

private data class BannerConfig(
    val type: BannerType,
    val isVisible: Boolean
)

private enum class BannerType {
    Indexing,
    DownloadedOnly,
    Incognito
}

// Extension functions for utility
private fun List<BannerMeasurement>.heightUpTo(type: BannerType): Int {
    return takeWhile { it.type != type }.sumOf { it.placeable.height }
}
