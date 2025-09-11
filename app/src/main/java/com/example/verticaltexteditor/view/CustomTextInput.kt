@file:Suppress("DEPRECATION")

package com.example.verticaltexteditor.view

import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.BackspaceCommand
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.DeleteSurroundingTextCommand
import androidx.compose.ui.text.input.DeleteSurroundingTextInCodePointsCommand
import androidx.compose.ui.text.input.FinishComposingTextCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.MoveCursorCommand
import androidx.compose.ui.text.input.SetComposingRegionCommand
import androidx.compose.ui.text.input.SetComposingTextCommand
import androidx.compose.ui.text.input.SetSelectionCommand
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputSession
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.example.verticaltexteditor.ui.theme.VerticalTextNativeCanvasTheme

@Composable
fun CustomTextInput(modifier: Modifier = Modifier) {
    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    val textInputService = LocalTextInputService.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val imm = context.getSystemService(InputMethodManager::class.java)
    val view = LocalView.current
    val focusRequester = remember { FocusRequester() }
    var textInputSession by remember { mutableStateOf<TextInputSession?>(null) }
    var isFocused by remember { mutableStateOf(false) }
    LaunchedEffect(textFieldValue) {
        imm.updateSelection(
            view,
            textFieldValue.selection.start,
            textFieldValue.selection.end,
            textFieldValue.composition?.start ?: -1,
            textFieldValue.composition?.end ?: -1
        )
    }
    VerticalCanvas(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                if (!state.isFocused) {
                    // Stop immediately on blur to avoid stray session/IME
                    textInputSession?.let { session ->
                        keyboardController?.hide()
                        textInputService?.stopInput(session)
                    }
                    textInputSession = null
                }
                isFocused = state.isFocused
            }
            .focusable()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                })
            },
        text = textFieldValue.annotatedString,
        selection = textFieldValue.selection,
        onSelectionChange = {
            val old = textFieldValue.copy()
            textFieldValue = textFieldValue.copy(selection = it)
            textInputSession?.updateState(old, textFieldValue)
        },
        onTap = {
            focusRequester.requestFocus()
            keyboardController?.show()
        },
        onDraw = { rect, rects ->
            // 大変なので保留
//            textInputSession?.updateTextLayoutResult()
        },
    )
    // Start/stop session tied to focus
    if (isFocused) {
        DisposableEffect(Unit) {
            textInputSession = textInputService?.startInput(
                value = textFieldValue,
                imeOptions = ImeOptions(
                    singleLine = true,
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Default,
                ),
                onEditCommand = { commands ->
                    commands.forEach { command ->
                        when (command) {
                            is CommitTextCommand -> {
                                val selection = textFieldValue.composition ?: textFieldValue.selection
                                val start = selection.min
                                val end = selection.max
                                val newText = textFieldValue.text.substring(0, start) + command.text + textFieldValue.text.substring(end)
                                val newCursor = start + command.text.length
                                textFieldValue = TextFieldValue(
                                    text = newText,
                                    selection = TextRange(newCursor),
                                )
                            }
                            is BackspaceCommand -> {
                                if (!textFieldValue.selection.collapsed) {
                                    val selection = textFieldValue.selection
                                    val start = selection.min
                                    val end = selection.max
                                    val newText = textFieldValue.text.removeRange(start, end)
                                    textFieldValue = textFieldValue.copy(
                                        text = newText,
                                        selection = TextRange(start),
                                    )
                                } else if (textFieldValue.selection.end > 0) {
                                    val newStart = textFieldValue.text.offsetByCodePoints(textFieldValue.selection.end, -1)
                                    val newText = textFieldValue.text.removeRange(newStart, textFieldValue.selection.end)
                                    textFieldValue = textFieldValue.copy(
                                        text = newText,
                                        selection = TextRange(newStart),
                                    )
                                }
                            }
                            is DeleteSurroundingTextCommand -> {
                                val selection = textFieldValue.selection
                                val start = selection.min
                                val end = selection.max
                                val newText = textFieldValue.text.removeRange(
                                    start - command.lengthBeforeCursor,
                                    end + command.lengthAfterCursor
                                )
                                textFieldValue = textFieldValue.copy(
                                    text = newText,
                                    selection = TextRange(textFieldValue.selection.start - command.lengthBeforeCursor),
                                )
                            }
                            is SetSelectionCommand -> {
                                textFieldValue = textFieldValue.copy(
                                    selection = TextRange(command.start, command.end),
                                )
                            }
                            is SetComposingTextCommand -> {
                                val selection = textFieldValue.composition ?: textFieldValue.selection
                                val start = selection.min
                                val end = selection.max
                                val newText = AnnotatedString.Builder().apply {
                                    append(textFieldValue.annotatedString.substring(0, start))
                                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                                        append(command.annotatedString)
                                    }
                                    append(textFieldValue.annotatedString.substring(end))
                                }.toAnnotatedString()
                                val newCursor = start + command.text.length
                                textFieldValue = TextFieldValue(
                                    annotatedString = newText,
                                    selection = TextRange(newCursor),
                                    composition = TextRange(start, newCursor),
                                )
                            }
                            is FinishComposingTextCommand -> {
                                textFieldValue = TextFieldValue(
                                    text = textFieldValue.text,
                                    selection = TextRange((textFieldValue.composition ?: textFieldValue.selection).end),
                                )
                            }
                            is MoveCursorCommand -> {
                                val newPosition = (textFieldValue.selection.start + command.amount).coerceIn(0, textFieldValue.text.length)
                                textFieldValue = textFieldValue.copy(
                                    selection = TextRange(newPosition)
                                )
                            }
                            is SetComposingRegionCommand -> {
                                textFieldValue = textFieldValue.copy(
                                    composition = TextRange(command.start, command.end)
                                )
                            }
                            is DeleteSurroundingTextInCodePointsCommand -> {
                                val newStart = try { textFieldValue.text.offsetByCodePoints(textFieldValue.selection.end, -command.lengthBeforeCursor) } catch (_: Exception) { (textFieldValue.selection.end - command.lengthBeforeCursor).coerceAtLeast(0) }
                                val end = try { textFieldValue.text.offsetByCodePoints(textFieldValue.selection.end, command.lengthAfterCursor) } catch (_: Exception) { (textFieldValue.selection.end + command.lengthAfterCursor).coerceAtMost(textFieldValue.text.length) }
                                if (newStart < end) {
                                    val newText = textFieldValue.text.removeRange(newStart, end)
                                    textFieldValue = textFieldValue.copy(
                                        text = newText,
                                        selection = TextRange(newStart)
                                    )
                                }
                            }
                        }
                    }
                },
                onImeActionPerformed = { action ->
                    when (action) {
                        ImeAction.None -> {}
                        else -> {
                            val selection = textFieldValue.selection
                            val start = selection.min
                            val end = selection.max
                            val newText = textFieldValue.text.substring(0, start) + "↩︎" + textFieldValue.text.substring(end)
                            textFieldValue = TextFieldValue(
                                text = newText,
                                selection = TextRange(start + "↩︎".length),
                            )
                        }
                    }
                }
            )
            onDispose {
                textInputSession?.let { session ->
                    keyboardController?.hide()
                    textInputService?.stopInput(session)
                }
                textInputSession = null
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CustomTextInputPreview() {
    VerticalTextNativeCanvasTheme {
        CustomTextInput()
    }
}
