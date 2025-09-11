package com.example.verticaltexteditor.view

import android.graphics.Matrix
import android.text.InputType
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireView
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputModifierNode
import androidx.compose.ui.platform.establishTextInputSession
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import com.example.verticaltexteditor.ui.theme.VerticalTextNativeCanvasTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CustomEditorState(
    text: AnnotatedString = buildAnnotatedString {},
    selection: TextRange = TextRange.Zero,
    composition: TextRange? = null,
    rects: List<Rect> = emptyList(),
    rect: Rect = Rect.Zero,
) {
    var text by mutableStateOf(buildAnnotatedString { append(text) })
    var selection by mutableStateOf(selection)
    var composition: TextRange? by mutableStateOf(composition)
    var rects by mutableStateOf(rects)
    var rect by mutableStateOf(rect)
    var node by mutableStateOf<EditorInputNode?>(null)

    companion object {
        val Saver: Saver<CustomEditorState, Any> = mapSaver(
            save = { state ->
                mapOf(
                    "text" to state.text.toString(),
                    "selStart" to state.selection.start,
                    "selEnd" to state.selection.end,
                    "compStart" to state.composition?.start,
                    "compEnd" to state.composition?.end
                )
            },
            restore = { map ->
                val text = map["text"] as String
                val selStart = map["selStart"] as Int
                val selEnd = map["selEnd"] as Int
                val compStart = map["compStart"] as Int?
                val compEnd = map["compEnd"] as Int?
                val selection = TextRange(selStart, selEnd)
                val composition = if (compStart != null && compEnd != null) TextRange(compStart, compEnd) else null
                CustomEditorState(buildAnnotatedString { append(text) }, selection, composition)
            }
        )
    }
}

@Composable
fun rememberCustomEditorState(): CustomEditorState =
    rememberSaveable(saver = CustomEditorState.Saver) { CustomEditorState() }

@Composable
fun CustomEditor(modifier: Modifier = Modifier) {
    val state = rememberCustomEditorState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    VerticalCanvas(
        modifier = modifier
            .fillMaxSize()
            .platformTextInput(state)
            .focusRequester(focusRequester)
            .focusTarget()
            .focusable(),
        text = buildAnnotatedString { append(state.text) },
        selection = state.selection,
        composition = state.composition,
        onSelectionChange = {
            state.selection = it
            state.node?.notifySelection()
                            },
        onTap = {
            focusRequester.requestFocus()
            keyboardController?.show()
        },
        onDraw = { rect, rects ->
            state.rect = rect
            state.rects = rects
            state.node?.notifyCursor()
        }
    )
}

fun Modifier.platformTextInput(state: CustomEditorState): Modifier = this.then(EditorInputElement(state))

private data class EditorInputElement(
    val state: CustomEditorState
) : ModifierNodeElement<EditorInputNode>() {
    override fun create(): EditorInputNode = EditorInputNode(state)
    override fun update(node: EditorInputNode) { node.state = state }
    override fun InspectorInfo.inspectableProperties() {
        name = "platformTextInput"
        properties["state"] = state
    }
}

class EditorInputNode(
    var state: CustomEditorState,
) : Modifier.Node(), PlatformTextInputModifierNode, FocusEventModifierNode {

    init {
        state.node = this
    }

    fun notifySelection() {
        node.requireView().context.getSystemService(InputMethodManager::class.java)
            .updateSelection(requireView(), state.selection.start, state.selection.end, state.composition?.start ?: -1, state.composition?.end ?: -1)
    }

    fun notifyCursor() {
        val builder = CursorAnchorInfo.Builder()
        builder.setSelectionRange(state.selection.start, state.selection.end)
        state.composition?.let { comp ->
            val s = comp.start.coerceIn(0, state.text.length)
            val e = comp.end.coerceIn(0, state.text.length)
            val composingText = state.text.substring(s, e)
            builder.setComposingText(s, composingText)
        }
        if (state.selection.end < state.rects.size) {
            builder.setInsertionMarkerLocation(
                state.rects[state.selection.end].center.x,
                state.rects[state.selection.end].top,
                state.rects[state.selection.end].top,
                state.rects[state.selection.end].top,
                CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION
            )
        } else if (state.rects.isEmpty()) {
            builder.setInsertionMarkerLocation(
                state.rect.width / 2,
                0f,
                0f,
                0f,
                CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION
            )
        } else {
            builder.setInsertionMarkerLocation(
                state.rects.last().center.x,
                state.rects.last().bottom,
                state.rects.last().bottom,
                state.rects.last().bottom,
                CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION
            )
        }
        for (i in 0 until state.rects.size) {
            builder.addCharacterBounds(
                i,
                state.rects[i].left, state.rects[i].top, state.rects[i].right, state.rects[i].bottom,
                CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION
            )
        }
        val m = Matrix()
        m.postTranslate(state.rect.left, state.rect.top)
        builder.setMatrix(m)
        node.requireView().context.getSystemService(InputMethodManager::class.java)
            .updateCursorAnchorInfo(node.requireView(), builder.build())
    }

    private var sessionJob: Job? = null

    override fun onDetach() {
        sessionJob?.cancel()
        sessionJob = null
    }

    override fun onFocusEvent(focusState: FocusState) {
        if (focusState.isFocused) {
            if (sessionJob == null) {
                sessionJob = coroutineScope.launch { establishSession() }
            }
        } else {
            sessionJob?.cancel()
            sessionJob = null
        }
    }

    private suspend fun establishSession() {
        establishTextInputSession {
            val request = object : PlatformTextInputMethodRequest {
                override fun createInputConnection(outAttributes: EditorInfo): InputConnection {
                    outAttributes.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    outAttributes.imeOptions = EditorInfo.IME_ACTION_DONE
                    return CustomInputConnection(state)
                }
            }
            startInputMethod(request)
        }
    }

    private class CustomInputConnection(
        private val editor: CustomEditorState,
    ) : InputConnection {
        override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence {
            val start = (editor.selection.end - n).coerceAtLeast(0)
            val end = editor.selection.end.coerceIn(0, editor.text.length)
            return editor.text.substring(start, end)
        }

        override fun getTextAfterCursor(n: Int, flags: Int): CharSequence {
            val start = editor.selection.end.coerceIn(0, editor.text.length)
            val end = (editor.selection.end + n).coerceAtMost(editor.text.length)
            return editor.text.substring(start, end)
        }

        override fun getSelectedText(flags: Int): CharSequence? =
            if (editor.selection.collapsed) null
            else editor.text.substring(editor.selection.min.coerceAtLeast(0), editor.selection.max.coerceAtMost(editor.text.length))

        override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
            val t = text?.toString() ?: ""
            val range = editor.composition ?: editor.selection
            val s = range.min.coerceIn(0, editor.text.length)
            val e = range.max.coerceIn(0, editor.text.length)
            editor.text = buildAnnotatedString {
                append(editor.text.replaceRange(s, e, t))
            }
            val cursor = (s + t.length).coerceIn(0, editor.text.length)
            editor.selection = TextRange(cursor)
            editor.composition = null
            return true
        }

        override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
            val composingText = text ?: ""
            val range = editor.composition ?: editor.selection
            val s = range.min.coerceIn(0, editor.text.length)
            val e = range.max.coerceIn(0, editor.text.length)
            editor.text = buildAnnotatedString {
                append(editor.text.substring(0, s))
                append(composingText.toAnnotatedString())
                append(editor.text.substring(e))
            }
            val compEnd = (s + composingText.length).coerceIn(0, editor.text.length)
            editor.composition = if (composingText.isEmpty()) null else TextRange(s, compEnd)
            editor.selection = TextRange(compEnd)
            return true
        }

        override fun finishComposingText(): Boolean {
            editor.composition = null
            return true
        }

        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
            if (!editor.selection.collapsed) {
                val start = editor.selection.min.coerceAtLeast(0)
                val end = editor.selection.max.coerceAtMost(editor.text.length)
                editor.text = buildAnnotatedString { append(editor.text.removeRange(start, end)) }
                editor.selection = TextRange(start)
                editor.composition = null
                return true
            }
            val caret = editor.selection.end.coerceIn(0, editor.text.length)
            val newStart = try { Character.offsetByCodePoints(editor.text, caret, -beforeLength) } catch (_: Exception) { (caret - beforeLength).coerceAtLeast(0) }
            val end = try { Character.offsetByCodePoints(editor.text, caret, afterLength) } catch (_: Exception) { (caret + afterLength).coerceAtMost(editor.text.length) }
            if (newStart >= end) return true
            editor.text = buildAnnotatedString { append(editor.text.removeRange(newStart, end)) }
            editor.selection = TextRange(newStart)
            editor.composition = null
            return true
        }

        override fun setSelection(start: Int, end: Int): Boolean {
            val s = start.coerceIn(0, editor.text.length)
            val e = end.coerceIn(0, editor.text.length)
            editor.selection = TextRange(s, e)
            return true
        }

        override fun getCursorCapsMode(reqModes: Int): Int = 0
        override fun getExtractedText(request: android.view.inputmethod.ExtractedTextRequest?, flags: Int): android.view.inputmethod.ExtractedText? = null
        override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean = deleteSurroundingText(beforeLength, afterLength)
        override fun setComposingRegion(start: Int, end: Int): Boolean {
            editor.composition = TextRange(start.coerceAtLeast(0), end.coerceAtMost(editor.text.length))
            return true
        }
        override fun performEditorAction(editorAction: Int): Boolean = true
        override fun performContextMenuAction(id: Int): Boolean = false
        override fun beginBatchEdit(): Boolean = true
        override fun endBatchEdit(): Boolean = true
        override fun sendKeyEvent(event: android.view.KeyEvent?): Boolean {
            if (event == null) return false
            if (event.action != android.view.KeyEvent.ACTION_DOWN) return false
            if (editor.composition != null) return false
            // 本当はupdateSelectionを送らないといけない
            return when (event.keyCode) {
                android.view.KeyEvent.KEYCODE_DEL -> {
                    editor.text = buildAnnotatedString {
                        if (editor.selection.collapsed) {
                            val newStart = try { Character.offsetByCodePoints(editor.text, editor.selection.end, -1) } catch (_: Exception) { (editor.selection.end - 1).coerceAtLeast(0) }
                            append(editor.text.removeRange(newStart, editor.selection.end))
                            editor.selection = TextRange(newStart)
                        } else {
                            append(editor.text.removeRange(editor.selection.min, editor.selection.max))
                            editor.selection = TextRange(editor.selection.min)
                        }
                    }
                    true
                }
                android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                    if (editor.selection.end > 0) {
                        val start = try { Character.offsetByCodePoints(editor.text, editor.selection.end, -1) } catch (_: Exception) { (editor.selection.end - 1).coerceAtLeast(0) }
                        editor.selection = TextRange(start)
                    }
                    true
                }
                android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
                android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (editor.selection.end < editor.text.length) {
                        val start = try { Character.offsetByCodePoints(editor.text, editor.selection.end, 1) } catch (_: Exception) { (editor.selection.end + 1).coerceAtMost(editor.text.length) }
                        editor.selection = TextRange(start)
                    }
                    true
                }
                android.view.KeyEvent.KEYCODE_MOVE_HOME -> {
                    editor.selection = TextRange(0)
                    true
                }
                android.view.KeyEvent.KEYCODE_MOVE_END -> {
                    editor.selection = TextRange(editor.text.length)
                    true
                }
                else -> false
            }
        }
        override fun clearMetaKeyStates(states: Int): Boolean = false
        override fun reportFullscreenMode(enabled: Boolean): Boolean = true
        override fun performPrivateCommand(action: String?, data: android.os.Bundle?): Boolean = false
        override fun requestCursorUpdates(cursorUpdateMode: Int): Boolean = false
        override fun getHandler(): android.os.Handler? = null
        override fun closeConnection() {}
        override fun commitCompletion(text: android.view.inputmethod.CompletionInfo?): Boolean = false
        override fun commitCorrection(correctionInfo: android.view.inputmethod.CorrectionInfo?): Boolean = false
        override fun commitContent(inputContentInfo: android.view.inputmethod.InputContentInfo, flags: Int, opts: android.os.Bundle?): Boolean = false
    }
}

fun CharSequence.toAnnotatedString(): AnnotatedString {
    if (this !is Spanned) return AnnotatedString(this.toString())
    val spanned = this
    return buildAnnotatedString {
        append(spanned.toString())
        val len = spanned.length
        for (span in spanned.getSpans(0, len, UnderlineSpan::class.java)) {
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            if (start in 0..len && end in 0..len && start < end) {
                addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
            }
        }
        for (span in spanned.getSpans(0, len, ForegroundColorSpan::class.java)) {
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            if (start in 0..len && end in 0..len && start < end) {
                addStyle(SpanStyle(color = Color(span.foregroundColor)), start, end)
            }
        }
        for (span in spanned.getSpans(0, len, BackgroundColorSpan::class.java)) {
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            if (start in 0..len && end in 0..len && start < end) {
                addStyle(SpanStyle(background = Color(span.backgroundColor)), start, end)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CustomEditorPreview() {
    VerticalTextNativeCanvasTheme { CustomEditor() }
}
