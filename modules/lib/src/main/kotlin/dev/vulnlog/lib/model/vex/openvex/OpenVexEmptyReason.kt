// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

/** The most likely reason a run collected no statement, so the user learns what to change. */
enum class OpenVexEmptyReason {
    /** No release declares purls, so nothing can anchor a statement. */
    NO_RELEASE_DECLARES_PURLS,

    /** The tag scope left no release purl to anchor a statement. */
    NO_PURL_CARRIES_TAG,

    /** No vulnerability entry applies to the release in scope. */
    NO_ENTRY_IN_RELEASE_SCOPE,

    /** No vulnerability entry references a release that declares purls. */
    NO_ENTRY_ON_ANCHORED_RELEASE,
}
