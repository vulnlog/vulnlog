// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.finding

/** The domain rules a Vulnlog file is checked against. */
enum class Rule {
    ACCEPTED_CRITICAL_RISK,
    ANALYZED_BEFORE_REPORTED,
    DANGLING_RELEASE_REFERENCE,
    DANGLING_TAG_REFERENCE,
    DEPRECATED_VERDICT,
    DUPLICATE_RELEASE_ID,
    DUPLICATE_TAG_ID,
    DUPLICATE_VULNERABILITY_ID,
    MISSING_REPORTER_INFORMATION,
    UNREFERENCED_RELEASE_ID,
    UNREFERENCED_TAG_ID,
}
