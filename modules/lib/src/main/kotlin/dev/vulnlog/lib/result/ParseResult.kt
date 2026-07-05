// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.result

import dev.vulnlog.lib.model.ParseValidationVersion
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.VulnlogFileRaw
import dev.vulnlog.lib.model.validation.ParseFailure
import dev.vulnlog.lib.parse.v1.dto.VulnlogFileV1Dto
import dev.vulnlog.lib.shell.FileInputOption
import org.snakeyaml.engine.v2.nodes.MappingNode

sealed interface ParseResult {
    /**
     * Every representation of the input from one single parse: the domain model for reading,
     * the DTO and node tree (with source positions and comments) for canonical rewriting and
     * format checking, and the verbatim text for byte-level comparison.
     */
    data class Ok(
        val validationVersion: ParseValidationVersion,
        val content: VulnlogFile,
        val dto: VulnlogFileV1Dto,
        val rootNode: MappingNode,
        val rawContent: VulnlogFileRaw,
    ) : ParseResult

    /** All problems the failing pipeline step found, at least one. */
    data class Error(
        val failures: List<ParseFailure>,
    ) : ParseResult
}

data class ParseResults(
    val success: Map<FileInputOption, ParseResult.Ok> = emptyMap(),
    val failure: Map<FileInputOption, ParseResult.Error> = emptyMap(),
)
