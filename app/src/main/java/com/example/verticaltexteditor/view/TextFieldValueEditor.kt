package com.example.verticaltexteditor.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TextFieldValueEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(fontSize = 20.sp),
            modifier = Modifier.fillMaxWidth()
        )

        val text = value.text
        Text("text=\"$text\" (${text.length} chars)")
        Text("selection=${value.selection.start}..${value.selection.end}  composition=${value.composition}")

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(fontSize = 20.sp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
