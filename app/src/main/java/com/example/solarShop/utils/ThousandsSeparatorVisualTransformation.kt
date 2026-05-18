package com.example.solarShop.utils

import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat


class ThousandsSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): TransformedText {
        val originalText = text.text
        val formattedText = DecimalFormat("#,###").format(originalText.toLongOrNull() ?: 0)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = formattedText.length
            override fun transformedToOriginal(offset: Int): Int = originalText.length
        }

        return TransformedText(text = androidx.compose.ui.text.AnnotatedString(formattedText), offsetMapping = offsetMapping)
    }
}