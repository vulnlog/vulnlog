// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app.report

import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.model.reporting.Changelog

sealed interface ChangelogOutcome {
    sealed interface Failed : ChangelogOutcome

    sealed interface Generated : ChangelogOutcome {
        val filter: ResolvedFilter
        val changelog: Changelog
        val content: String
    }

    data class Fixed(
        override val filter: ResolvedFilter,
        override val changelog: Changelog,
        override val content: String,
    ) : Generated

    /** No release in scope shipped a fix; whether to write the empty changelog anyway is the driver's call. */
    data class NothingFixed(
        override val filter: ResolvedFilter,
        override val changelog: Changelog,
        override val content: String,
    ) : Generated
}
