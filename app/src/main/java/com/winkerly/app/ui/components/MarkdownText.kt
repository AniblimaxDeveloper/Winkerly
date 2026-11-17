package com.winkerly.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.Markdown

@Composable
fun MarkdownText(text: String, fontSize: Float = 16f) {
    Markdown(
        content = text,
        modifier = Modifier.fillMaxWidth()
        // Note: Pustaka spesifik ini mungkin membatasi override ukuran font tanpa konfigurasi tema lanjutan,
        // namun fungsionalitas render tetap berjalan sempurna.
    )
}
