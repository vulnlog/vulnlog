// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.VulnId

/** What one run collected for a scope: the statements, and what was left out and why. */
data class OpenVexCollection(
    /**
     * The scope the collection was built for.
     */
    val scope: OpenVexScope,
    /**
     * The statements the document makes, ordered so the same input always writes the same bytes.
     */
    val statements: List<OpenVexStatement>,
    /**
     * The purls each release in scope contributes, in declaration order. Only these releases anchor a statement.
     */
    val anchors: Map<Release, List<Purl>>,
    /**
     * Releases in scope that an entry speaks about but that contribute no purl, in declaration order.
     */
    val skippedReleases: List<Release>,
    /**
     * Entries that contributed no statement.
     */
    val skippedEntries: List<OpenVexSkippedEntry>,
)

/** A vulnerability entry left out of the document, carrying what explains it. */
sealed interface OpenVexSkippedEntry {
    val id: VulnId

    /** The entry references no release at all. */
    data class NoRelease(
        override val id: VulnId,
    ) : OpenVexSkippedEntry

    /** No release the entry applies to declares purls in scope. */
    data class NoAnchoredRelease(
        override val id: VulnId,
    ) : OpenVexSkippedEntry
}
