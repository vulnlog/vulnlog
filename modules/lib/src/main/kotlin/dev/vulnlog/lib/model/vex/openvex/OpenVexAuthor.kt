// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

import dev.vulnlog.lib.model.Project

/** The author line of the VEX document */
@JvmInline
value class OpenVexAuthor private constructor(
    val name: String,
) {
    constructor(project: Project) : this(authorLine(project.author, project.contact))
}

private fun authorLine(
    author: String,
    contact: String?,
): String {
    require(author.isNotBlank()) { "VEX author is required" }
    return contact?.let { "$author ($contact)" } ?: author
}
