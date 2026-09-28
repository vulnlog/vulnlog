// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

/** The level decides where a driver prints the line: always, with `-v`, or with `-vv`. */
sealed interface OpenVexLine {
    val text: String

    data class Warning(
        override val text: String,
    ) : OpenVexLine

    data class Verbose(
        override val text: String,
    ) : OpenVexLine

    data class Debug(
        override val text: String,
    ) : OpenVexLine
}
