// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model

data class PurlEntry(
    /**
     * The versioned Package URL for this artifact in this release.
     */
    val purl: Purl,
    /**
     * Tags associated with this purl. Used to match vulnerability tags for scoped VEX generation. Must reference tags defined in the tags section.
     */
    val tags: List<Tag> = emptyList(),
)
