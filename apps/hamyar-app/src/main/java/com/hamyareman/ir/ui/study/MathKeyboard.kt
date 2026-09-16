package com.hamyareman.ir.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val KEY_ROWS = listOf(
    listOf("۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹"),
    listOf("+", "−", "×", "÷", "=", "/", "^", "√"),
    listOf("(", ")", "{", "}", "[", "]", "|", ","),
    listOf("∈", "∉", "⊂", "⊆", "∪", "∩", "∅", "\\"),
    listOf("≤", "≥", "≠", "≈", "π", "²", "³", "ⁿ"),
)

/** کیبورد نمادهای ریاضی نهم — درج در فیلد جواب. */
@Composable
fun MathSymbolKeyboard(
    value: TextFieldValue,
    onValue: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
    ) {
        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            KEY_ROWS.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    row.forEach { key ->
                        OutlinedButton(
                            onClick = { onValue(insertAtCursor(value, key)) },
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        ) { Text(key, fontSize = 13.sp, maxLines = 1) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                OutlinedButton(onClick = { onValue(insertAtCursor(value, " ")) }, modifier = Modifier.weight(1f).height(36.dp)) {
                    Text("فاصله", fontSize = 12.sp)
                }
                OutlinedButton(onClick = { onValue(backspace(value)) }, modifier = Modifier.weight(1f).height(36.dp)) {
                    Text("⌫", fontSize = 14.sp)
                }
            }
        }
    }
}

internal fun insertAtCursor(value: TextFieldValue, insert: String): TextFieldValue {
    val start = value.selection.min.coerceIn(0, value.text.length)
    val end = value.selection.max.coerceIn(0, value.text.length)
    val next = value.text.substring(0, start) + insert + value.text.substring(end)
    val pos = start + insert.length
    return TextFieldValue(next, TextRange(pos))
}

internal fun backspace(value: TextFieldValue): TextFieldValue {
    val start = value.selection.min
    val end = value.selection.max
    if (start != end) {
        val next = value.text.removeRange(start, end)
        return TextFieldValue(next, TextRange(start))
    }
    if (start <= 0) return value
    val next = value.text.removeRange(start - 1, start)
    return TextFieldValue(next, TextRange(start - 1))
}
