package com.example.verticaltexteditor.view

import android.graphics.Paint.VERTICAL_TEXT_FLAG
import android.graphics.Typeface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.verticaltexteditor.ui.theme.VerticalTextNativeCanvasTheme

@Composable
fun VerticalCanvasSample(
    modifier: Modifier = Modifier.fillMaxSize(),
    text1: String,
    text2: String,
    initialIndex: Int = 0,
    fontFamily: FontFamily? = MaterialTheme.typography.bodyLarge.fontFamily,
) {
    var currentTextIndex by remember { mutableIntStateOf(initialIndex) }
    val context = LocalContext.current
    val basePaint = TextPaint().apply {
        fontFamily?.let { family ->
            val typeface = createFontFamilyResolver(context).resolve(
                family, FontWeight.Normal, FontStyle.Normal, FontSynthesis.All
            ).value
            if (typeface is Typeface) {
                this.typeface = typeface
            }
        }
    }
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        currentTextIndex = currentTextIndex + 1
                        if (currentTextIndex > 4) {
                            currentTextIndex = 0
                        }
                    }
                )
            }
    ) {
        val paint = TextPaint(basePaint).apply {
            textSize = 40.sp.toPx()
        }
        val verticalPaint = TextPaint(paint).apply {
            flags = flags or VERTICAL_TEXT_FLAG
        }
        drawIntoCanvas { canvas ->
            val text = if (currentTextIndex > 3) text2 else text1
            canvas.nativeCanvas.drawText(text, 0, text.length, 100f, 100f, verticalPaint)
            if (currentTextIndex == 0) return@drawIntoCanvas
            canvas.nativeCanvas.drawText(text, 0, text.length, 100f, 100f, paint)
            if (currentTextIndex == 1) return@drawIntoCanvas
            canvas.drawRect(0f, 0f, 100f, 100f, Paint())
            if (currentTextIndex == 2) return@drawIntoCanvas
            canvas.drawLine(Offset(100f, 100f), Offset(9999f, 100f), Paint())
            canvas.drawLine(Offset(100f, 100f), Offset(100f, 9999f), Paint())
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VerticalCanvasSamplePreview() {
    VerticalTextNativeCanvasTheme {
        VerticalCanvasSample(
            text1 = "あのイーハトーヴォのすきとおったかぜ",
            text2 = "あの🤖イーハトーヴォのすきとおったかぜ",
            initialIndex = 4
        )
    }
}