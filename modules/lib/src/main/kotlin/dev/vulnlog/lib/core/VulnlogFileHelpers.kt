// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VulnlogFile

/**
 * Collects all defined and therefore known releaes in [vulnlog].
 */
fun knownReleases(vulnlog: VulnlogFile): Set<Release> = vulnlog.releases.map { it.id }.toSet()

/**
 * Collects all defined and therefore known tags in [vulnlog].
 */
fun knownTags(vulnlog: VulnlogFile): Set<Tag> = vulnlog.tags.map { it.id }.toSet()
