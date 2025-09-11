package com.example.verticaltexteditor.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TextFieldStateEditor(
    state: TextFieldState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BasicTextField(
            state = state,
            textStyle = TextStyle(fontSize = 20.sp),
            modifier = Modifier.fillMaxWidth()
        )

        val text = state.text.toString()
        Text("text=\"$text\" (${text.length} chars)")
        Text("selection=${state.selection.start}..${state.selection.end}  composition=${state.composition}")
    }
}
