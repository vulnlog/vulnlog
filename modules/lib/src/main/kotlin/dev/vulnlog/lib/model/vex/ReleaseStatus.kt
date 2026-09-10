// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex

import dev.vulnlog.lib.model.Release
import java.time.LocalDate

/** The status of one vulnerability in one release, and the day that status became true. */
data class ReleaseStatus(
    /**
     * The release the status applies to.
     */
    val release: Release,
    /**
     * The status of the vulnerability in that release.
     */
    val status: VexStatus,
    /**
     * The day the status became true. Null when the entry records no date for it.
     */
    val since: LocalDate?,
)
