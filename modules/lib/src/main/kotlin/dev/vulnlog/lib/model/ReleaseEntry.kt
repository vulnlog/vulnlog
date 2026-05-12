// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model

import java.time.LocalDate

data class ReleaseEntry(
    /**
     * Unique release identifier.
     */
    val id: Release,
    /**
     * Publication date of the release. Absence indicates the release is not yet published.
     */
    val publicationDate: LocalDate? = null,
    /**
     * Versioned Package URLs identifying the release artifacts. Used as product identifiers in VEX documents.
     */
    val purls: List<PurlEntry> = emptyList(),
)
