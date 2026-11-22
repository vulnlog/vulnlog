// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.impact

/**
 * The room the text report gets: the [width] it wraps to, and whether it may use [colors]. The driver decides both
 * from where the report goes, so rendering never asks a terminal and the same input renders the same bytes.
 */
data class TextLayout(
    val width: Int,
    val colors: Boolean,
) {
    companion object {
        /** For files and pipes: no escape codes, and a width that reads well in an editor or a CI log. */
        val PLAIN = TextLayout(width = 120, colors = false)
    }
}
