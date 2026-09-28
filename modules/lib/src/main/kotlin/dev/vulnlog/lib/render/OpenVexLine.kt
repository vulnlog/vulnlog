// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

/** One line a driver prints about an OpenVEX run, at the level it is printed at. Drivers keep the order. */
sealed interface OpenVexLine {
    val text: String

    /** Printed on every run: something the document leaves out. */
    data class Warning(
        override val text: String,
    ) : OpenVexLine

    /** Printed with `-v`. */
    data class Verbose(
        override val text: String,
    ) : OpenVexLine

    /** Printed with `-vv`. */
    data class Debug(
        override val text: String,
    ) : OpenVexLine
}
