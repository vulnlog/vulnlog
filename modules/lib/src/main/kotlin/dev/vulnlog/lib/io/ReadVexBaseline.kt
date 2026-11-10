// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.io

import dev.vulnlog.lib.model.vex.VexBaselineRead
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.notExists
import kotlin.io.path.readText

fun readVexBaseline(path: Path): VexBaselineRead {
    if (path.notExists()) return VexBaselineRead.Absent
    return try {
        VexBaselineRead.Present(path.readText())
    } catch (e: IOException) {
        VexBaselineRead.Unreadable(e.reason())
    }
}
