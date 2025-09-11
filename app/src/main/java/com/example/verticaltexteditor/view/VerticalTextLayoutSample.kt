package com.example.verticaltexteditor.view

import android.graphics.Matrix
import android.graphics.Paint.VERTICAL_TEXT_FLAG
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.withSave
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.core.graphics.withSave
import androidx.core.text.toSpannable
import androidx.text.vertical.EmphasisSpan
import androidx.text.vertical.EmphasisSpan.Companion.STYLE_SESAME
import androidx.text.vertical.RubySpan
import androidx.text.vertical.TextOrientationSpan
import androidx.text.vertical.VerticalTextLayout
import com.example.verticaltexteditor.ui.theme.VerticalTextNativeCanvasTheme

class DrawText(val text: Spannable, val rotate: Boolean = false)

@Composable
fun VerticalTextLayoutSample(modifier: Modifier = Modifier, initialIndex: Int = 0) {
    val textColor = MaterialTheme.colorScheme.onPrimaryContainer
    var currentTextIndex by remember { mutableIntStateOf(initialIndex) }

    val texts = remember {
        listOf(
            DrawText(SpannableStringBuilder()
                .append("とある科学の")
                .append("超電磁砲")
                .toSpannable()),
            DrawText(SpannableStringBuilder()
                .append("とある科学の")
                .append("超電磁砲", RubySpan.Builder(
                    "レールガン"
                ).build(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                .toSpannable()),
            DrawText(SpannableStringBuilder()
                .append("今日、2025年9月12日。")
                .toSpannable()),
            DrawText(SpannableStringBuilder()
                .append("今日、2025年9月12日。", EmphasisSpan(STYLE_SESAME), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                .toSpannable()),
            DrawText(SpannableStringBuilder()
                .append("今日、")
                .append("2025",
                    TextOrientationSpan.TextCombineUpright(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                .append("年")
                .append("9",
                    TextOrientationSpan.TextCombineUpright(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                .append("月")
                .append("12",
                    TextOrientationSpan.TextCombineUpright(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                .append("日。")
                .toSpannable()
            ),
            DrawText(SpannableStringBuilder()
                //　傍点の分まで横に広がる
                .append("2025", TextOrientationSpan.TextCombineUpright(), 0)
                .append("年")
                .append("9", TextOrientationSpan.TextCombineUpright(), 0)
                .append("月")
                .append("12", TextOrientationSpan.TextCombineUpright(), 0)
                .append("日")
                .toSpannable()
                .apply { setSpan(EmphasisSpan(STYLE_SESAME),  0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
            ),
            DrawText(SpannableStringBuilder()
                .append("とある科学の")
                .append("超電磁砲", EmphasisSpan(STYLE_SESAME),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                .toSpannable()),
            DrawText(SpannableStringBuilder()
                .append("とある科学の")
                .append("超電磁砲", RubySpan.Builder("レールガン".toSpannable().apply {
                    setSpan(TextOrientationSpan.Sideways(), 0, length, 0)
                }).build(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                .toSpannable()
                .apply { setSpan(TextOrientationSpan.Sideways(), 0, length, 0) }),
            DrawText(SpannableStringBuilder()
                .append("とある科学の")
                .append("超電磁砲", RubySpan.Builder("レールガン".toSpannable().apply {
                    setSpan(TextOrientationSpan.Sideways(), 0, length, 0)
                }).build(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                .toSpannable()
                .apply { setSpan(TextOrientationSpan.Sideways(), 0, length, 0) },
                rotate = true),
            DrawText(SpannableStringBuilder()
                .append("とある科学の")
                .append("超電磁砲".toSpannable().apply {
                    setSpan(RubySpan.Builder("レールガン".toSpannable()).build(), 0, length, 0)
                })
                .toSpannable()
                .apply { setSpan(EmphasisSpan(STYLE_SESAME), 0, length, 0) }),
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        currentTextIndex = currentTextIndex + 1
                    }
                )
            }
    ) {
        drawIntoCanvas { canvas ->
            val paint = TextPaint().apply {
                textSize = 36.sp.toPx()
            }
            canvas.nativeCanvas.withSave {
                val drawText = texts[currentTextIndex % texts.size]
                val layout = VerticalTextLayout.Builder(
                    drawText.text,
                    0,
                    drawText.text.length,
                    paint,
                    canvas.nativeCanvas.height.toFloat()
                ).build()
                if (drawText.rotate) {
                    val advance = TextPaint().apply {
                        color = textColor.toArgb()
                        textSize = 36.sp.toPx()
                        flags = flags or VERTICAL_TEXT_FLAG
                    }.getRunAdvance(drawText.text, 0, drawText.text.length, 0, drawText.text.length, false, drawText.text.length)
                    setMatrix(Matrix().apply {
                        // 座標系よくわかってないのでソースコード読んだ方がいい
                        preTranslate(advance, 0f)
                        postRotate(-90f)
                        postTranslate(0f, layout.height)
                    })
                }
                layout.draw(
                    canvas.nativeCanvas,
                    canvas.nativeCanvas.width.toFloat(),
                    0f
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VerticalTextLayoutSamplePreview() {
    VerticalTextNativeCanvasTheme {
        VerticalTextLayoutSample(initialIndex = 5)
    }
}