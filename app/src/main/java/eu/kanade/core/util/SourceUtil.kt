package eu.kanade.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Composable function to check if sources are loaded and initialized.
 * 
 * @return A [State] holding the initialization status of sources
 * 
 * @sample eu.kanade.core.util.SourceUtilsTest.testIfSourcesLoadedComposable
 */
@Composable
fun rememberSourcesLoadedState(): State<Boolean> {
    val sourceManager = remember { Injekt.get<SourceManager>() }
    return sourceManager.isInitialized.collectAsState()
}

/**
 * Composable function to check if sources are loaded and initialized.
 * 
 * @return true if sources are loaded, false otherwise
 */
@Composable
fun isSourcesLoaded(): Boolean {
    return rememberSourcesLoadedState().value
}

/**
 * Composable that executes [onLoaded] callback when sources become available.
 * 
 * @param onLoaded Callback invoked when sources are loaded
 * @param onLoading Optional callback invoked while sources are loading
 */
@Composable
fun WhenSourcesLoaded(
    onLoaded: @Composable () -> Unit,
    onLoading: @Composable (() -> Unit)? = null
) {
    val sourcesLoaded = rememberSourcesLoadedState()
    
    if (sourcesLoaded.value) {
        onLoaded()
    } else {
        onLoading?.invoke()
    }
}

/**
 * Composable that provides the source manager only when sources are loaded.
 * 
 * @param content Composable content that receives the initialized SourceManager
 */
@Composable
fun WithSourcesLoaded(
    content: @Composable (SourceManager) -> Unit
) {
    val sourceManager = remember { Injekt.get<SourceManager>() }
    val sourcesLoaded = rememberSourcesLoadedState()
    
    if (sourcesLoaded.value) {
        content(sourceManager)
    }
}

/**
 * Composable that remembers the source manager and its initialization state.
 * 
 * @return A pair containing the SourceManager and its initialization state
 */
@Composable
fun rememberSourceManager(): Pair<SourceManager, State<Boolean>> {
    val sourceManager = remember { Injekt.get<SourceManager>() }
    val isInitialized = sourceManager.isInitialized.collectAsState()
    return remember(sourceManager, isInitialized) {
        sourceManager to isInitialized
    }
}

/**
 * Composable that provides derived state based on source initialization.
 * 
 * @param transform Transformation function to apply when sources are loaded
 * @return A State holding the transformed value or null if sources aren't loaded
 */
@Composable
fun <T> rememberWithSourcesLoaded(transform: (SourceManager) -> T): State<T?> {
    val (sourceManager, isInitialized) = rememberSourceManager()
    
    return remember(isInitialized) {
        derivedStateOf {
            if (isInitialized.value) transform(sourceManager) else null
        }
    }
}

/**
 * Composable that collects any flow only when sources are loaded.
 * 
 * @param flow The flow to collect
 * @param initial The initial value to use
 * @param transform Transformation function for the flow data
 * @return A State holding the flow data or initial value if sources aren't loaded
 */
@Composable
fun <T, R> rememberFlowWithSourcesLoaded(
    flow: Flow<T>,
    initial: R,
    transform: (SourceManager, T) -> R
): State<R> {
    val (sourceManager, isInitialized) = rememberSourceManager()
    val flowState = flow.collectAsState(initial = initial)
    
    return remember(isInitialized, flowState) {
        derivedStateOf {
            if (isInitialized.value) transform(sourceManager, flowState.value) else initial
        }
    }
}

/**
 * Composable effect that triggers when sources are loaded.
 * 
 * @param action The action to perform when sources are loaded
 */
@Composable
fun LaunchedEffectWhenSourcesLoaded(action: suspend (SourceManager) -> Unit) {
    val (sourceManager, isInitialized) = rememberSourceManager()
    val currentAction by rememberUpdatedState(action)
    
    LaunchedEffect(isInitialized.value) {
        if (isInitialized.value) {
            currentAction(sourceManager)
        }
    }
}

/**
 * Composable that provides the source manager with lifecycle-aware initialization.
 */
@Composable
fun rememberSourceManagerWithLifecycle(): SourceManager {
    val sourceManager = remember { Injekt.get<SourceManager>() }
    
    // Optionally trigger initialization if needed
    LaunchedEffect(Unit) {
        // If the source manager needs explicit initialization
        // sourceManager.initializeIfNeeded()
    }
    
    return sourceManager
}

/**
 * Extension function for SourceManager to create a state in Compose.
 */
@Composable
fun SourceManager.collectInitializationState(): State<Boolean> {
    return this.isInitialized.collectAsState()
}

// Deprecated original function with improved replacement
@Composable
@Deprecated(
    "Use rememberSourcesLoadedState() or isSourcesLoaded() instead",
    ReplaceWith("isSourcesLoaded()")
)
fun ifSourcesLoaded(): Boolean {
    return isSourcesLoaded()
}

// Usage examples:
/*
 * // Basic usage
 * if (isSourcesLoaded()) {
 *     Text("Sources are ready")
 * }
 * 
 * // With state
 * val sourcesLoaded by rememberSourcesLoadedState()
 * 
 * // Conditional composition
 * WhenSourcesLoaded(
 *     onLoaded = { SourceList() },
 *     onLoading = { LoadingIndicator() }
 * )
 * 
 * // With manager access
 * WithSourcesLoaded { sourceManager ->
 *     SourceGrid(sourceManager.getSources())
 * }
 * 
 * // Derived state
 * val sourceCount by rememberWithSourcesLoaded { it.getSources().size }
 * 
 * // Effect when loaded
 * LaunchedEffectWhenSourcesLoaded { sourceManager ->
 *     sourceManager.refreshSources()
 * }
 */
