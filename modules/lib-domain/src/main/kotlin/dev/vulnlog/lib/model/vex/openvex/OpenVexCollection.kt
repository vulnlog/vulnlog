// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VulnId

data class OpenVexScope(
    val release: OpenVexReleaseScope = OpenVexReleaseScope.Published,
    /** An empty set means the dimension is not restricted. */
    val tags: Set<Tag> = emptySet(),
)

sealed interface OpenVexReleaseScope {
    /** Consumers act on what they can install, so an unpublished release is only written when named. */
    data object Published : OpenVexReleaseScope

    data class Named(
        val release: Release,
    ) : OpenVexReleaseScope
}

data class OpenVexCollection(
    val scope: OpenVexScope,
    /** Sorted, so the same input always writes the same bytes. */
    val statements: List<OpenVexStatement>,
    val anchors: Map<Release, List<Purl>>,
    val unpublishedReleases: List<Release>,
    val skippedReleases: List<Release>,
    val skippedEntries: List<OpenVexSkippedEntry>,
)

sealed interface OpenVexSkippedEntry {
    val id: VulnId

    data class NoRelease(
        override val id: VulnId,
    ) : OpenVexSkippedEntry

    data class NoAnchoredRelease(
        override val id: VulnId,
    ) : OpenVexSkippedEntry

    data class NoTags(
        override val id: VulnId,
    ) : OpenVexSkippedEntry

    data class NoMatchingReleasePurl(
        override val id: VulnId,
    ) : OpenVexSkippedEntry
}

enum class OpenVexEmptyReason {
    NO_RELEASE_DECLARES_PURLS,
    NO_PUBLISHED_RELEASE_DECLARES_PURLS,
    NO_ENTRY_MATCHES_RELEASE_PURL_TAGS,
    NO_ENTRY_IN_TAG_SCOPE,
    NO_ENTRY_IN_RELEASE_SCOPE,
    NO_ENTRY_ON_ANCHORED_RELEASE,
}
