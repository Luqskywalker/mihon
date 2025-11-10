package eu.kanade.presentation.category.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import eu.kanade.core.preference.asToggleableState
import eu.kanade.presentation.category.visualName
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun CategoryCreateDialog(
    onDismissRequest: () -> Unit,
    onCreate: (String) -> Unit,
    categories: ImmutableList<String>,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    
    // Memoize validation to prevent recomputation
    val validationState = remember(name, categories) {
        CategoryValidationState(
            isEmpty = name.isEmpty(),
            alreadyExists = categories.contains(name)
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                enabled = !validationState.isEmpty && !validationState.alreadyExists,
                onClick = {
                    onCreate(name)
                    onDismissRequest()
                },
            ) {
                Text(text = stringResource(MR.strings.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        title = {
            Text(text = stringResource(MR.strings.action_add_category))
        },
        text = {
            CategoryNameInput(
                name = name,
                onNameChange = { name = it },
                isError = validationState.alreadyExists,
                focusRequester = focusRequester,
                supportingTextRes = when {
                    validationState.alreadyExists -> MR.strings.error_category_exists
                    else -> MR.strings.information_required_plain
                }
            )
        },
        modifier = modifier,
    )

    AutoFocusEffect(focusRequester)
}

@Composable
fun CategoryRenameDialog(
    onDismissRequest: () -> Unit,
    onRename: (String) -> Unit,
    categories: ImmutableList<String>,
    category: String,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf(category) }
    val focusRequester = remember { FocusRequester() }
    
    // Memoize validation and change detection
    val dialogState = remember(name, category, categories) {
        CategoryRenameState(
            valueHasChanged = name != category,
            alreadyExists = categories.contains(name) && name != category
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                enabled = dialogState.valueHasChanged && !dialogState.alreadyExists,
                onClick = {
                    onRename(name)
                    onDismissRequest()
                },
            ) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        title = {
            Text(text = stringResource(MR.strings.action_rename_category))
        },
        text = {
            CategoryNameInput(
                name = name,
                onNameChange = { name = it },
                isError = dialogState.alreadyExists,
                focusRequester = focusRequester,
                supportingTextRes = when {
                    dialogState.alreadyExists -> MR.strings.error_category_exists
                    else -> MR.strings.information_required_plain
                }
            )
        },
        modifier = modifier,
    )

    AutoFocusEffect(focusRequester)
}

@Composable
private fun CategoryNameInput(
    name: String,
    onNameChange: (String) -> Unit,
    isError: Boolean,
    focusRequester: FocusRequester,
    supportingTextRes: MR.strings,
) {
    OutlinedTextField(
        modifier = Modifier.focusRequester(focusRequester),
        value = name,
        onValueChange = onNameChange,
        label = { Text(text = stringResource(MR.strings.name)) },
        supportingText = {
            Text(text = stringResource(supportingTextRes))
        },
        isError = isError,
        singleLine = true,
    )
}

@Composable
private fun AutoFocusEffect(focusRequester: FocusRequester) {
    LaunchedEffect(focusRequester) {
        // Reduced delay for better UX
        delay(100.milliseconds)
        focusRequester.requestFocus()
    }
}

@Composable
fun CategoryDeleteDialog(
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
    category: String,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onDelete()
                onDismissRequest()
            }) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        title = {
            Text(text = stringResource(MR.strings.delete_category))
        },
        text = {
            Text(text = stringResource(MR.strings.delete_category_confirmation, category))
        },
        modifier = modifier,
    )
}

@Composable
fun ChangeCategoryDialog(
    initialSelection: ImmutableList<CheckboxState<Category>>,
    onDismissRequest: () -> Unit,
    onEditCategories: () -> Unit,
    onConfirm: (List<Long>, List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (initialSelection.isEmpty()) {
        EmptyCategoriesDialog(
            onDismissRequest = onDismissRequest,
            onEditCategories = onEditCategories
        )
        return
    }

    var selection by remember { mutableStateOf(initialSelection) }
    
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            DialogButtonRow(
                onEditCategories = {
                    onDismissRequest()
                    onEditCategories()
                },
                onDismissRequest = onDismissRequest,
                onConfirm = {
                    onDismissRequest()
                    onConfirm(
                        selection.getIncludedCategoryIds(),
                        selection.getExcludedCategoryIds()
                    )
                }
            )
        },
        title = {
            Text(text = stringResource(MR.strings.action_move_category))
        },
        text = {
            CategorySelectionList(
                selection = selection,
                onSelectionChange = { newSelection -> selection = newSelection }
            )
        },
        modifier = modifier,
    )
}

@Composable
private fun EmptyCategoriesDialog(
    onDismissRequest: () -> Unit,
    onEditCategories: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    onEditCategories()
                },
            ) {
                Text(text = stringResource(MR.strings.action_edit_categories))
            }
        },
        title = {
            Text(text = stringResource(MR.strings.action_move_category))
        },
        text = {
            Text(text = stringResource(MR.strings.information_empty_category_dialog))
        },
    )
}

@Composable
private fun DialogButtonRow(
    onEditCategories: () -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row {
        TextButton(onClick = onEditCategories) {
            Text(text = stringResource(MR.strings.action_edit))
        }
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onDismissRequest) {
            Text(text = stringResource(MR.strings.action_cancel))
        }
        TextButton(onClick = onConfirm) {
            Text(text = stringResource(MR.strings.action_ok))
        }
    }
}

@Composable
private fun CategorySelectionList(
    selection: ImmutableList<CheckboxState<Category>>,
    onSelectionChange: (ImmutableList<CheckboxState<Category>>) -> Unit,
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
    ) {
        selection.forEach { checkbox ->
            CategoryCheckboxItem(
                checkbox = checkbox,
                onCheckboxChange = { changedCheckbox ->
                    val updatedSelection = selection.map { item ->
                        if (item.value.id == changedCheckbox.value.id) changedCheckbox.next() else item
                    }.toImmutableList()
                    onSelectionChange(updatedSelection)
                }
            )
        }
    }
}

@Composable
private fun CategoryCheckboxItem(
    checkbox: CheckboxState<Category>,
    onCheckboxChange: (CheckboxState<Category>) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckboxChange(checkbox) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (checkbox) {
            is CheckboxState.TriState -> {
                TriStateCheckbox(
                    state = checkbox.asToggleableState(),
                    onClick = { onCheckboxChange(checkbox) },
                )
            }
            is CheckboxState.State -> {
                Checkbox(
                    checked = checkbox.isChecked,
                    onCheckedChange = { onCheckboxChange(checkbox) },
                )
            }
        }

        Text(
            text = checkbox.value.visualName,
            modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

// Extension functions for cleaner category ID extraction
private fun ImmutableList<CheckboxState<Category>>.getIncludedCategoryIds(): List<Long> {
    return this.filter { it.isIncluded }.map { it.value.id }
}

private fun ImmutableList<CheckboxState<Category>>.getExcludedCategoryIds(): List<Long> {
    return this.filter { it.isExcluded }.map { it.value.id }
}

private val CheckboxState<Category>.isIncluded: Boolean
    get() = this is CheckboxState.State.Checked || this is CheckboxState.TriState.Include

private val CheckboxState<Category>.isExcluded: Boolean
    get() = this is CheckboxState.State.None || this is CheckboxState.TriState.None

// Data classes for state management
private data class CategoryValidationState(
    val isEmpty: Boolean,
    val alreadyExists: Boolean
)

private data class CategoryRenameState(
    val valueHasChanged: Boolean,
    val alreadyExists: Boolean
)
