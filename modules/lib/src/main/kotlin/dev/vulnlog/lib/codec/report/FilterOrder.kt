// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.report

import dev.vulnlog.lib.core.filter.ResolvedFilter

/**
 * The [selected] values in the order Vulnlog declares them, not in the order they were requested, so that equal
 * filters read the same in every view of the report.
 */
internal inline fun <reified T : Enum<T>> sortInDeclaredOrder(selected: Set<T>): List<T> =
    enumValues<T>().filter { it in selected }

/** Tags have no declared order, so every view lists them alphabetically. */
internal fun sortTagValues(filter: ResolvedFilter): List<String> = filter.tags.map { it.value }.sorted()
