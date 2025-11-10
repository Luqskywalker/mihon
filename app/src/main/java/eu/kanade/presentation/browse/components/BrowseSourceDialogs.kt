package eu.kanade.presentation.browse.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun RemoveMangaDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    mangaToRemove: Manga,
) {
    val dialogText = remember(mangaToRemove.title) {
        stringResource(MR.strings.remove_manga, mangaToRemove.title)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        dismissButton = {
            DialogDismissButton(onDismissRequest)
        },
        confirmButton = {
            DialogConfirmButton(
                onDismissRequest = onDismissRequest,
                onConfirm = onConfirm
            )
        },
        title = {
            DialogTitle()
        },
        text = {
            DialogText(text = dialogText)
        },
    )
}

@Composable
private fun DialogDismissButton(
    onDismissRequest: () -> Unit,
) {
    TextButton(onClick = onDismissRequest) {
        Text(text = stringResource(MR.strings.action_cancel))
    }
}

@Composable
private fun DialogConfirmButton(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    TextButton(
        onClick = {
            onDismissRequest()
            onConfirm()
        },
    ) {
        Text(text = stringResource(MR.strings.action_remove))
    }
}

@Composable
private fun DialogTitle() {
    Text(text = stringResource(MR.strings.are_you_sure))
}

@Composable
private fun DialogText(
    text: String,
) {
    Text(text = text)
}

// Alternative optimized version with better state handling
@Composable
fun RemoveMangaDialogOptimized(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    mangaToRemove: Manga,
) {
    val dialogContent = remember(mangaToRemove.title) {
        DialogContent(
            title = stringResource(MR.strings.are_you_sure),
            text = stringResource(MR.strings.remove_manga, mangaToRemove.title),
            confirmText = stringResource(MR.strings.action_remove),
            dismissText = stringResource(MR.strings.action_cancel)
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = dialogContent.dismissText)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    onConfirm()
                },
            ) {
                Text(text = dialogContent.confirmText)
            }
        },
        title = {
            Text(text = dialogContent.title)
        },
        text = {
            Text(text = dialogContent.text)
        },
    )
}

private data class DialogContent(
    val title: String,
    val text: String,
    val confirmText: String,
    val dismissText: String,
)
