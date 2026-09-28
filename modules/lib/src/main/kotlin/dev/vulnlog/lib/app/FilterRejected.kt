// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app

import dev.vulnlog.lib.core.filter.FilterProblem

/*
 * Outcome cases that several use cases share. Each is declared once and joins every use-case outcome it belongs to, so
 * drivers render it in one place. Sealed interfaces only admit subtypes from the same package and module, which is why
 * every use case lives in dev.vulnlog.lib.app.
 */

/** A filter or scope names a release, tag, reporter or token the files do not define. */
data class FilterRejected(
    val problems: List<FilterProblem>,
) : OpenVexOutcome
