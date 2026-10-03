// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

/**
 * Renders one diagnostic line stating how many reporting entries the inputs produced and how many
 * remain after merging. Shared by the CLI and the Gradle plugin.
 */
fun renderReportingCounts(
    collected: Int,
    merged: Int,
): String = "collected $collected report entries, merged to $merged"
