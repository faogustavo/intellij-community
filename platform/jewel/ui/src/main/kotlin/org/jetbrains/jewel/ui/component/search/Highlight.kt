// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.jewel.ui.component.search

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import org.jetbrains.jewel.ui.component.styling.SearchMatchStyle

internal fun CharSequence.highlightSearch(style: SearchMatchStyle, matches: List<IntRange>?): AnnotatedString =
    buildAnnotatedString {
        append(this@highlightSearch)
        if (matches.isNullOrEmpty()) return@buildAnnotatedString

        for (match in matches) {
            addStyle(
                SpanStyle(background = style.colors.startBackground, color = style.colors.foreground),
                match.first,
                match.last,
            )
        }
    }
