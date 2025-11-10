package eu.kanade.presentation.browse.components

import android.util.DisplayMetrics
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil3.compose.AsyncImage
import eu.kanade.domain.source.model.icon
import eu.kanade.presentation.util.rememberResourceBitmapPainter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.source.model.Source
import tachiyomi.source.local.isLocal

private val defaultSourceIconModifier = Modifier
    .height(40.dp)
    .aspectRatio(1f)

@Composable
fun SourceIcon(
    source: Source,
    modifier: Modifier = Modifier,
) {
    val effectiveModifier = remember(modifier) {
        modifier.then(defaultSourceIconModifier)
    }

    when {
        source.isStub && source.icon == null -> WarningIcon(effectiveModifier)
        source.icon != null -> BitmapIcon(source.icon, effectiveModifier)
        source.isLocal() -> LocalSourceIcon(effectiveModifier)
        else -> DefaultSourceIcon(effectiveModifier)
    }
}

@Composable
private fun WarningIcon(modifier: Modifier) {
    Image(
        imageVector = Icons.Filled.Warning,
        contentDescription = null,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.error),
        modifier = modifier,
    )
}

@Composable
private fun BitmapIcon(bitmap: ImageBitmap, modifier: Modifier) {
    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
private fun LocalSourceIcon(modifier: Modifier) {
    Image(
        painter = painterResource(R.mipmap.ic_local_source),
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
private fun DefaultSourceIcon(modifier: Modifier) {
    Image(
        painter = painterResource(R.mipmap.ic_default_source),
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
fun ExtensionIcon(
    extension: Extension,
    modifier: Modifier = Modifier,
    density: Int = DisplayMetrics.DENSITY_DEFAULT,
) {
    val effectiveModifier = remember(modifier) {
        modifier.then(defaultSourceIconModifier)
    }

    when (extension) {
        is Extension.Available -> AvailableExtensionIcon(extension, effectiveModifier)
        is Extension.Installed -> InstalledExtensionIcon(extension, effectiveModifier, density)
        is Extension.Untrusted -> UntrustedExtensionIcon(effectiveModifier)
    }
}

@Composable
private fun AvailableExtensionIcon(
    extension: Extension.Available,
    modifier: Modifier,
) {
    AsyncImage(
        model = extension.iconUrl,
        contentDescription = null,
        placeholder = remember { ColorPainter(Color(0x1F888888)) },
        error = rememberResourceBitmapPainter(id = R.drawable.cover_error),
        modifier = modifier.clip(MaterialTheme.shapes.extraSmall),
    )
}

@Composable
private fun InstalledExtensionIcon(
    extension: Extension.Installed,
    modifier: Modifier,
    density: Int,
) {
    val iconState by extension.rememberIconState(density)
    
    when (iconState) {
        is IconResult.Loading -> Box(modifier = modifier)
        is IconResult.Success -> Image(
            bitmap = (iconState as IconResult.Success).bitmap,
            contentDescription = null,
            modifier = modifier,
        )
        is IconResult.Error -> DefaultExtensionIcon(modifier)
    }
}

@Composable
private fun UntrustedExtensionIcon(modifier: Modifier) {
    Image(
        imageVector = Icons.Filled.Dangerous,
        contentDescription = null,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.error),
        modifier = modifier,
    )
}

@Composable
private fun DefaultExtensionIcon(modifier: Modifier) {
    Image(
        bitmap = ImageBitmap.imageResource(id = R.mipmap.ic_default_source),
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
private fun Extension.rememberIconState(density: Int): State<IconResult> {
    val context = LocalContext.current
    return remember(pkgName, density) {
        produceState<IconResult>(
            initialValue = IconResult.Loading,
            key1 = pkgName,
            key2 = density,
            producer = {
                withIOContext {
                    value = try {
                        val appInfo = ExtensionLoader.getExtensionPackageInfoFromPkgName(context, pkgName)
                            ?.applicationInfo
                            ?: return@withIOContext IconResult.Error
                        
                        val appResources = context.packageManager.getResourcesForApplication(appInfo)
                        val drawable = appResources.getDrawableForDensity(appInfo.icon, density, null)
                            ?: return@withIOContext IconResult.Error
                        
                        IconResult.Success(drawable.toBitmap().asImageBitmap())
                    } catch (e: Exception) {
                        IconResult.Error
                    }
                }
            }
        )
    }
}

sealed class IconResult {
    data object Loading : IconResult()
    data object Error : IconResult()
    data class Success(val bitmap: ImageBitmap) : IconResult()
}
