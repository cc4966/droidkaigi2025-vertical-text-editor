package com.example.verticaltexteditor.view

import android.graphics.Paint
import android.graphics.Paint.VERTICAL_TEXT_FLAG
import android.graphics.Typeface
import android.graphics.text.TextRunShaper
import android.text.TextPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.verticaltexteditor.ui.theme.VerticalTextNativeCanvasTheme

@Composable
fun VerticalCanvas(
    modifier: Modifier = Modifier,
    text: AnnotatedString,
    selection: TextRange = TextRange.Zero,
    composition: TextRange? = null,
    onSelectionChange: (TextRange) -> Unit = {},
    onTap: () -> Unit = {},
    onDraw: (Rect, List<Rect>) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val caretColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onPrimaryContainer
    val selectionColor = MaterialTheme.colorScheme.secondaryContainer
    val fontFamily = MaterialTheme.typography.bodyLarge.fontFamily
    val basePaint = TextPaint().apply {
        color = textColor.toArgb()
        textSize = with(LocalDensity.current) { 40.sp.toPx() }
        flags = flags or VERTICAL_TEXT_FLAG
        fontFamily?.let { family ->
            val typeface = createFontFamilyResolver(context).resolve(
                family, FontWeight.Normal, FontStyle.Normal, FontSynthesis.All
            ).value
            if (typeface is Typeface) {
                this.typeface = typeface
            }
        }
    }
    var currentScale by remember { mutableFloatStateOf(1f) }
    var screenPosition by remember { mutableStateOf(Offset.Zero) }
    Canvas(
        modifier = modifier
            .onGloballyPositioned { layoutCoordinates ->
                screenPosition = layoutCoordinates.positionOnScreen()
            }
            .pointerInput(text) {
                detectTapGestures { offset ->
                    val scaledPaint = TextPaint(basePaint).apply { textSize = basePaint.textSize * currentScale }
                    val index = scaledPaint.getOffsetForAdvance(text, 0, text.length, 0, text.length, false, offset.y)
                    onSelectionChange(TextRange(index))
                    onTap()
                }
            }
            .pointerInput(text) {
                var selection = TextRange(0)
                detectDragGestures(
                    onDragStart = { offset ->
                        val scaledPaint = TextPaint(basePaint).apply { textSize = basePaint.textSize * currentScale }
                        val index = scaledPaint.getOffsetForAdvance(text, 0, text.length, 0, text.length, false, offset.y)
                        selection = TextRange(index)
                        onSelectionChange(selection)
                    },
                    onDragEnd = {
                        onTap()
                    },
                ) { change, _ ->
                    val scaledPaint = TextPaint(basePaint).apply { textSize = basePaint.textSize * currentScale }
                    val endIndex = scaledPaint.getOffsetForAdvance(text, 0, text.length, 0, text.length, false, change.position.y)
                    selection = TextRange(selection.start, endIndex)
                    onSelectionChange(selection)
                }
            }
    ) {
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.clipRect(0, 0, size.width.toInt(), size.height.toInt())
            val length = basePaint.getRunAdvance(text, 0, text.length, 0, text.length, false, text.length)
            val scale = if (length > size.height) {
                size.height / length
            } else 1f
            currentScale = scale
            val scaledPaint = TextPaint(basePaint).apply {
                textSize = basePaint.textSize * scale
            }
            val originX = size.width / 2
            val originY = 0
            if (selection.collapsed) {
                val caretY = scaledPaint.getRunAdvance(text, 0, text.length, 0, text.length, false, selection.end.coerceIn(0, text.length))
                val rect = Rect(Offset(originX - scaledPaint.textSize / 2, caretY - 1.dp.toPx()), Size(scaledPaint.textSize, 2.dp.toPx()))
                drawRect(
                    color = caretColor,
                    topLeft = rect.topLeft,
                    size = rect.size
                )
            } else {
                val start = selection.min.coerceIn(0, text.length)
                val end = selection.max.coerceIn(0, text.length)
                val startY = scaledPaint.getRunAdvance(text, 0, text.length, 0, text.length, false, start)
                val endY = scaledPaint.getRunAdvance(text, 0, text.length, 0, text.length, false, end)
                val rect = Rect(Offset(originX - scaledPaint.textSize / 2, startY), Size(scaledPaint.textSize, endY - startY))
                drawRect(
                    color = selectionColor,
                    topLeft = rect.topLeft,
                    size = rect.size
                )
            }
            val rects = mutableListOf<Rect>()
            var prevY = 0f
            for (i in 1 until text.length) {
                val caretY = scaledPaint.getRunAdvance(text, 0, text.length, 0, text.length, false, i)
                rects.add(Rect(originX - scaledPaint.textSize / 2, prevY, originX + scaledPaint.textSize / 2, caretY))
                prevY = caretY
            }
            onDraw(
                Rect(
                    Offset(screenPosition.x, screenPosition.y + scaledPaint.textSize), // 本当は違うんだけどテキストサイズ足すとちょうどいいので……一旦
                    Size(canvas.nativeCanvas.width.toFloat(), canvas.nativeCanvas.height.toFloat()),
                ),
                rects,
            )
            val positionedGlyphs = TextRunShaper.shapeTextRun(text, 0, text.length, 0, text.length, 0f, 0f, false, scaledPaint)
            text.spanStyles.map { spanStyle ->
                val start = spanStyle.start.coerceIn(0, text.length)
                val end = spanStyle.end.coerceIn(0, text.length)
                val startY = scaledPaint.getRunAdvance(text, 0, text.length, 0, text.length, false, start)
                val endY = scaledPaint.getRunAdvance(text, 0, text.length, 0, text.length, false, end)
                if (spanStyle.item.background.alpha > 0) {
                    drawRect(
                        color = spanStyle.item.background,
                        topLeft = Offset(originX - scaledPaint.textSize / 2, startY),
                        size = Size(scaledPaint.textSize, endY - startY)
                    )
                }
                if (spanStyle.item.textDecoration?.contains(TextDecoration.Underline) == true) {
                    drawLine(
                        color = textColor,
                        start = Offset(originX + scaledPaint.textSize / 2, startY),
                        end = Offset(originX + scaledPaint.textSize / 2, endY),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
            for (i in 0 until positionedGlyphs.glyphCount()) {
                val font = positionedGlyphs.getFont(i)
                val isEmoji = font.toString().contains("Emoji", ignoreCase = true)
                val glyphX = originX + positionedGlyphs.getGlyphX(i)
                val glyphY = originY + if (isEmoji) {
                    val metrics = Paint.FontMetrics()
                    font.getMetrics(scaledPaint, metrics)
                    positionedGlyphs.getGlyphY(i) - metrics.ascent
                } else {
                    positionedGlyphs.getGlyphY(i)
                }
                canvas.nativeCanvas.drawGlyphs(
                    intArrayOf(positionedGlyphs.getGlyphId(i)),
                    0,
                    floatArrayOf(glyphX, glyphY),
                    0,
                    1,
                    font,
                    scaledPaint
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VerticalCanvasPreview() {
    VerticalTextNativeCanvasTheme {
        var selection by remember { mutableStateOf(TextRange(0, 3)) }
        val annotatedText = remember {
            AnnotatedString.Builder().apply {
                append("こんにちは")
                addStyle(SpanStyle(color = Color.Red), 0, 2)
                addStyle(SpanStyle(color = Color.Red), 2, 4)
                addStyle(SpanStyle(background = Color.Red), 2, 4)
                append("🌏")
                addStyle(SpanStyle(textDecoration = TextDecoration.Underline), 5, 6)
                append("Á👨‍👩‍👧‍👦B")
            }.toAnnotatedString()
        }

        VerticalCanvas(
            text = annotatedText,
            selection = selection,
            onSelectionChange = { selection = it },
            modifier = Modifier.fillMaxSize()
        )
    }
}