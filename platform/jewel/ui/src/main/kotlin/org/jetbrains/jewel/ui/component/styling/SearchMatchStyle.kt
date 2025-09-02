// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.jewel.ui.component.styling

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import org.jetbrains.jewel.foundation.GenerateDataFunctions

@GenerateDataFunctions
public class SearchMatchStyle(public val colors: SearchMatchColors) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SearchMatchStyle

        return colors == other.colors
    }

    override fun hashCode(): Int = colors.hashCode()

    override fun toString(): String = "SearchMatchStyle(colors=$colors)"

    public companion object
}

@GenerateDataFunctions
public class SearchMatchColors(
    public val startBackground: Color,
    public val endBackground: Color,
    public val foreground: Color,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SearchMatchColors

        if (startBackground != other.startBackground) return false
        if (endBackground != other.endBackground) return false
        if (foreground != other.foreground) return false

        return true
    }

    override fun hashCode(): Int {
        var result = startBackground.hashCode()
        result = 31 * result + endBackground.hashCode()
        result = 31 * result + foreground.hashCode()
        return result
    }

    override fun toString(): String =
        "SearchMatchColors(startBackground=$startBackground, endBackground=$endBackground, foreground=$foreground)"

    public companion object {
        public fun defaultForegroundColor(isDark: Boolean): Color = if (isDark) Color(0xFF000000) else Color(0xFF323232)
    }
}

public val LocalSearchMatchStyle: ProvidableCompositionLocal<SearchMatchStyle> = staticCompositionLocalOf {
    error("No SearchMatchStyle provided. Have you forgotten the theme?")
}
