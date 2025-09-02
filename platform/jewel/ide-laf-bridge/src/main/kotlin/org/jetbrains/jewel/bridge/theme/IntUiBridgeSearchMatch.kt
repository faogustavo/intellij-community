// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.jewel.bridge.theme

import com.intellij.util.ui.UIUtil
import org.jetbrains.jewel.bridge.toComposeColor
import org.jetbrains.jewel.ui.component.styling.SearchMatchColors
import org.jetbrains.jewel.ui.component.styling.SearchMatchStyle

internal fun readSearchMatchStyle(isDark: Boolean) =
    SearchMatchStyle(
        colors =
            SearchMatchColors(
                startBackground = UIUtil.getSearchMatchGradientStartColor().toComposeColor(),
                endBackground = UIUtil.getSearchMatchGradientEndColor().toComposeColor(),
                foreground = SearchMatchColors.defaultForegroundColor(isDark),
            )
    )
