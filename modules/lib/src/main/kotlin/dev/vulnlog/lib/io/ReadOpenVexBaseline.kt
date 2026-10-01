// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.io

import dev.vulnlog.lib.model.vex.openvex.OpenVexBaselineRead
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.notExists
import kotlin.io.path.readText

fun readOpenVexBaseline(path: Path): OpenVexBaselineRead {
    if (path.notExists()) return OpenVexBaselineRead.Absent
    return try {
        OpenVexBaselineRead.Present(path.readText())
    } catch (e: IOException) {
        OpenVexBaselineRead.Unreadable(e.message ?: e.javaClass.simpleName)
    }
}
