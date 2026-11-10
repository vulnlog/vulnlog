// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.cyclonedx

import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VulnId

/** Always one release, see [CycloneDxVexDocument]. */
data class CycloneDxVexScope(
    val release: Release,
    /** An empty set means the dimension is not restricted. */
    val tags: Set<Tag> = emptySet(),
)

data class CycloneDxVexCollection(
    val scope: CycloneDxVexScope,
    /** Sorted, so the same input always writes the same bytes. */
    val vulnerabilities: List<CycloneDxVexVulnerability>,
    /** The purls of the release in scope that are in tag scope: the parts of the product. */
    val productPurls: List<Purl>,
    val skippedEntries: List<CycloneDxVexSkippedEntry>,
)

sealed interface CycloneDxVexSkippedEntry {
    val id: VulnId

    data class NoRelease(
        override val id: VulnId,
    ) : CycloneDxVexSkippedEntry

    data class NotOnRelease(
        override val id: VulnId,
    ) : CycloneDxVexSkippedEntry

    data class NoTags(
        override val id: VulnId,
    ) : CycloneDxVexSkippedEntry

    data class NoMatchingReleasePurl(
        override val id: VulnId,
    ) : CycloneDxVexSkippedEntry
}

enum class CycloneDxVexEmptyReason {
    RELEASE_DECLARES_NO_PURLS,
    NO_RELEASE_PURL_IN_TAG_SCOPE,
    NO_ENTRY_MATCHES_RELEASE_PURL_TAGS,
    NO_ENTRY_IN_TAG_SCOPE,
    NO_ENTRY_ON_RELEASE,
}
