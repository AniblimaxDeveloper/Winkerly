package com.winkerly.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikepenz.markdown.m3.Markdown

@Composable
fun MarkdownText(text: String) {
    // Markdown dari mikepenz sudah terintegrasi dengan Material 3
    Markdown(
        content = text,
        modifier = Modifier.fillMaxWidth()
    )
}
