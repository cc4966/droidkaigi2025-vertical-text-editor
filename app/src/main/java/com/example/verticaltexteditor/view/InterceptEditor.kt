package com.example.verticaltexteditor.view

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.example.verticaltexteditor.ui.theme.VerticalTextNativeCanvasTheme

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InterceptEditor(
    modifier: Modifier = Modifier,
    state: TextFieldState
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val currentState by rememberUpdatedState(state)
    val compositionString = remember { mutableStateOf(buildAnnotatedString {}) }
    val interceptor = remember {
        LoggingImeInterceptor {
            compositionString.value = it.toAnnotatedString()
        }
    }

    InterceptPlatformTextInput(interceptor = interceptor) {
        Box(
            modifier = modifier
                .fillMaxSize()
        ) {
            CompositionLocalProvider(
                LocalTextSelectionColors provides TextSelectionColors(
                    handleColor = Color.Transparent,
                    backgroundColor = Color.Transparent
                )
            ) {
                BasicTextField(
                    state = state,
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .alpha(0f),
                    cursorBrush = SolidColor(Color.Transparent)
                )
            }
            VerticalCanvas(
                modifier = Modifier
                    .fillMaxSize(),
                text = AnnotatedString.Builder().apply {
                    val composition = currentState.composition
                    if (composition != null) {
                        append(currentState.text.substring(0, composition.min))
                        if (composition.length == compositionString.value.length) {
                            append(compositionString.value)
                        } else {
                            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                                append(
                                    currentState.text.substring(
                                        composition.min,
                                        composition.max
                                    )
                                )
                            }
                        }
                        append(currentState.text.substring(composition.max))
                    } else {
                        append(currentState.text)
                    }
                }.toAnnotatedString(),
                selection = currentState.selection,
                onSelectionChange = {
                    state.edit {
                        selection = it
                    }
                },
                onTap = {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
            )
        }
    }
}


@OptIn(ExperimentalComposeUiApi::class)
private class LoggingImeInterceptor(
    private val onCompositionChanged: (CharSequence) -> Unit
) : PlatformTextInputInterceptor {
    override suspend fun interceptStartInputMethod(
        request: PlatformTextInputMethodRequest,
        nextHandler: PlatformTextInputSession
    ): Nothing {
        val wrapped = object : PlatformTextInputMethodRequest {
            override fun createInputConnection(outAttributes: EditorInfo): InputConnection {
                val base = request.createInputConnection(outAttributes)
                return LoggingInputConnectionWrapper(base, onCompositionChanged)
            }
        }
        nextHandler.startInputMethod(wrapped)
    }
}

private class LoggingInputConnectionWrapper(
    target: InputConnection,
    private val onCompositionChanged: (CharSequence) -> Unit
) : InputConnectionWrapper(target, /* mutable= */ false) {

    override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
        text?.let {
            onCompositionChanged(text)
        }
        return super.setComposingText(text, newCursorPosition)
    }

    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        onCompositionChanged("")
        return super.commitText(text, newCursorPosition)
    }

    override fun finishComposingText(): Boolean {
        onCompositionChanged("")
        return super.finishComposingText()
    }

    override fun requestCursorUpdates(cursorUpdateMode: Int): Boolean {
        return super.requestCursorUpdates(cursorUpdateMode)
    }

    override fun requestCursorUpdates(cursorUpdateMode: Int, cursorUpdateFilter: Int): Boolean {
        // ここをうまくやれば変換候補の位置を調整できるはず？
        return super.requestCursorUpdates(cursorUpdateMode, cursorUpdateFilter)
    }
}

@Preview(showBackground = true)
@Composable
private fun InterceptEditorPreview() {
    val previewState = rememberTextFieldState("test")
    VerticalTextNativeCanvasTheme {
        InterceptEditor(state = previewState)
    }
}
