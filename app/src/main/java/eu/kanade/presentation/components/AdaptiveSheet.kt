// Standard usage
NavigatorAdaptiveSheet(
    screen = MyScreen(),
    onDismissRequest = { /* handle dismiss */ }
)

// With custom swipe dismiss logic
NavigatorAdaptiveSheet(
    screen = MyScreen(),
    enableSwipeDismiss = { navigator -> 
        navigator.lastItem is DismissibleScreen 
    },
    onDismissRequest = { /* handle dismiss */ }
)

// Using extension function
navigator.adaptiveSheet(
    screen = MyScreen(),
    onDismissRequest = { /* handle dismiss */ }
)

// Optimized version with custom dismiss behavior
AdaptiveSheetOptimized(
    onDismissRequest = { /* handle dismiss */ },
    dismissOnBackPress = true,
    dismissOnClickOutside = false,
) {
    // Sheet content
}

// Minimal version for performance
AdaptiveSheetMinimal(
    onDismissRequest = { /* handle dismiss */ }
) {
    // Sheet content
}
