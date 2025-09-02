// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.jewel.foundation.system

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.jewel.foundation.InternalJewelApi

@InternalJewelApi
@ApiStatus.Internal
/**
 * System info singleton for platform-specific checks.
 *
 * **IC Version:**
 * [SystemInfoRt](https://github.com/JetBrains/intellij-community/blob/master/platform/util-rt/src/com/intellij/openapi/util/SystemInfoRt.java)
 */
public object SystemInfo {
    private val osName: String = System.getProperty("os.name").lowercase()

    public val isMac: Boolean = osName.contains("mac")
    public val isWindows: Boolean = osName.contains("windows")
    public val isLinux: Boolean = osName.contains("linux")
}
