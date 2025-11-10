package eu.kanade.domain.source.model

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import eu.kanade.tachiyomi.extension.ExtensionManager
import tachiyomi.domain.source.model.Source
import uy.kohesive.injekt.Injekt

private val extensionManager: ExtensionManager by lazy { Injekt.get() }

val Source.icon: ImageBitmap?
    get() = extensionManager
        .getAppIconForSource(id)
        ?.toBitmap()
        ?.asImageBitmap()
