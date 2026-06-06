// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model

/** Verbatim, unparsed text of a Vulnlog file, as read from disk or STDIN. */
data class VulnlogFileRaw(
    val content: String,
)
