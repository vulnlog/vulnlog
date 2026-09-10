// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.Tag

/**
 * What the document covers. Both dimensions are resolved against the Vulnlog file before they reach the collector.
 *
 * An empty [releases] covers every release, an empty [tags] every purl of the releases in scope.
 */
data class OpenVexScope(
    /**
     * The releases that may anchor a statement. `--release` narrows it to one; empty covers every release.
     */
    val releases: Set<Release> = emptySet(),
    /**
     * The tags a release purl must carry to become a product. Set by `--tag`, matched as a union.
     */
    val tags: Set<Tag> = emptySet(),
)
