// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.jewel.intui.standalone.styling

import org.jetbrains.jewel.intui.core.theme.IntUiDarkTheme
import org.jetbrains.jewel.intui.core.theme.IntUiLightTheme
import org.jetbrains.jewel.ui.component.styling.SearchMatchColors
import org.jetbrains.jewel.ui.component.styling.SearchMatchStyle

public fun SearchMatchStyle.Companion.light(colors: SearchMatchColors = SearchMatchColors.light()): SearchMatchStyle =
    SearchMatchStyle(colors)

public fun SearchMatchStyle.Companion.dark(colors: SearchMatchColors = SearchMatchColors.dark()): SearchMatchStyle =
    SearchMatchStyle(colors)

public fun SearchMatchColors.Companion.light(): SearchMatchColors =
    SearchMatchColors(
        startBackground = IntUiLightTheme.colors.yellow(7),
        endBackground = IntUiLightTheme.colors.yellow(7),
        foreground = SearchMatchColors.defaultForegroundColor(false),
    )

public fun SearchMatchColors.Companion.dark(): SearchMatchColors =
    SearchMatchColors(
        startBackground = IntUiDarkTheme.colors.yellow(5),
        endBackground = IntUiDarkTheme.colors.yellow(5),
        foreground = SearchMatchColors.defaultForegroundColor(false),
    )
