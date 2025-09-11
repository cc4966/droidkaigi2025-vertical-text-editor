package com.example.verticaltexteditor

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.verticaltexteditor.ui.theme.VerticalTextNativeCanvasTheme
import com.example.verticaltexteditor.view.CustomEditor
import com.example.verticaltexteditor.view.CustomTextInput
import com.example.verticaltexteditor.view.InterceptEditor
import com.example.verticaltexteditor.view.TextFieldStateEditor
import com.example.verticaltexteditor.view.TextFieldValueEditor
import com.example.verticaltexteditor.view.VerticalCanvasSample
import com.example.verticaltexteditor.view.VerticalTextLayoutSample

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VerticalTextNativeCanvasTheme {
                var selectedTab by rememberSaveable { mutableIntStateOf(0) }
                var menuExpanded by rememberSaveable { mutableStateOf(false) }
                val stateHolder = rememberSaveableStateHolder()
                val tabs = EditorTab.entries.toTypedArray()
                val interceptState: TextFieldState = rememberTextFieldState("")
                val selectionTestState: TextFieldState = rememberTextFieldState("Hello, world!")
                var selectionTestValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
                    mutableStateOf(TextFieldValue(text = "Hello, world!"))
                }
                var verticalCanvasText by rememberSaveable(stateSaver = TextFieldValue.Saver) {
                    mutableStateOf(TextFieldValue(text = "Android🌏"))
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        Surface(modifier = Modifier.fillMaxWidth()) {
                            ExposedDropdownMenuBox(
                                expanded = menuExpanded,
                                onExpandedChange = { menuExpanded = !menuExpanded }
                            ) {
                                androidx.compose.material3.TextField(
                                    readOnly = true,
                                    value = tabs[selectedTab].label,
                                    onValueChange = {},
                                    singleLine = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    tabs.forEachIndexed { index, tab ->
                                        DropdownMenuItem(
                                            text = { Text(tab.label) },
                                            onClick = {
                                                selectedTab = index
                                                menuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        stateHolder.SaveableStateProvider(key = tabs[selectedTab].name) {
                            when (tabs[selectedTab]) {
                                EditorTab.CustomEditor -> CustomEditor(modifier = Modifier.imePadding())
                                EditorTab.CustomTextInput -> CustomTextInput(modifier = Modifier.imePadding())
                                EditorTab.InterceptEditor -> InterceptEditor(
                                    modifier = Modifier.imePadding(),
                                    state = interceptState,
                                )
                                EditorTab.TextFieldStateEditor -> TextFieldStateEditor(
                                    state = selectionTestState,
                                )
                                EditorTab.TextFieldValueEditor -> TextFieldValueEditor(
                                    value = selectionTestValue,
                                    onValueChange = { selectionTestValue = it },
                                )
                                EditorTab.WebView -> AndroidView(
                                    modifier = Modifier
                                        .fillMaxSize(),
                                    factory = { context ->
                                        WebView(context).apply {
                                            webViewClient = WebViewClient()
                                            settings.javaScriptEnabled = false
                                        }
                                    },
                                    update = { webView ->
                                        webView.loadDataWithBaseURL(null, "<!doctype html><head><meta charset=\"utf-8\">"
                                                + "<style>body{writing-mode:vertical-rl;white-space:nowrap;}</style>"
                                                + "</head><body><p>縦書きイーハトーヴォ。英ABCや数字123もOK。</p></body></html>", "text/html", "utf-8", null)
                                    }
                                )
                                EditorTab.VerticalTextLayoutSample -> VerticalTextLayoutSample()
                                EditorTab.VerticalCanvasSample -> VerticalCanvasSample(
                                    modifier = Modifier
                                        .fillMaxSize(),
                                    text1 = "あのイーハトーヴォのすきとおったかぜ",
                                    text2 = "あの🤖イーハトーヴォのすきとおったかぜ",
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class EditorTab(val label: String) {
    CustomEditor("PlatformTextInputModifierNode"),
    CustomTextInput("TextInputService"),
    InterceptEditor("PlatformTextInputInterceptor"),
    TextFieldStateEditor("BasicTextField (TextFieldState)"),
    TextFieldValueEditor("BasicTextField (TextFieldValue)"),
    WebView("WebView writing-mode"),
    VerticalTextLayoutSample("VerticalTextLayout sample"),
    VerticalCanvasSample("VERTICAL_TEXT_FLAG sample"),
}
