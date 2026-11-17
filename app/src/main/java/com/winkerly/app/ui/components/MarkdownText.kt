package com.winkerly.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.material3.RichText

@Composable
fun MarkdownText(text: String) {
    RichText(modifier = Modifier.fillMaxWidth()) {
        Markdown(content = text)
    }
}
