// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.validation.ParseFailure
import dev.vulnlog.lib.result.ParseResult
import dev.vulnlog.lib.result.Severity

/**
 * Renders each problem of a failed file as one line in the severity grammar, keeping the source
 * position and entry path when known: `error: <file>: <line>:<column>: <path>: <message>`.
 */
fun renderParseFailure(
    filename: String,
    failure: ParseResult.Error,
): List<String> = failure.failures.map { formatFinding(Severity.ERROR, filename, location(it), it.message) }

private fun location(failure: ParseFailure): String =
    listOfNotNull(
        failure.location?.let { "${it.line}:${it.column}" },
        failure.path,
    ).joinToString(": ")
