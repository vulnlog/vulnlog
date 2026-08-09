// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.validation

import dev.vulnlog.lib.core.validation.ValidationOutcome.Ok
import dev.vulnlog.lib.core.validation.ValidationOutcome.Stopped
import dev.vulnlog.lib.model.SchemaVersion
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.finding.FindingSeverity.ERROR
import dev.vulnlog.lib.model.finding.FindingSeverity.INFO
import dev.vulnlog.lib.model.finding.FindingSeverity.WARNING
import dev.vulnlog.lib.model.finding.ValidationFinding
import dev.vulnlog.lib.model.finding.highestSeverity
import dev.vulnlog.lib.parse.DomainMappingResult
import dev.vulnlog.lib.parse.mapToDomain
import dev.vulnlog.lib.parse.validation.DocumentResult
import dev.vulnlog.lib.parse.validation.DtoParseResult
import dev.vulnlog.lib.parse.validation.NodeTreeResult
import dev.vulnlog.lib.parse.validation.ParsedVulnlogProject
import dev.vulnlog.lib.parse.validation.SchemaVersionResult
import dev.vulnlog.lib.parse.validation.ValidVulnlogProject
import dev.vulnlog.lib.parse.validation.bindToDto
import dev.vulnlog.lib.parse.validation.constructDocument
import dev.vulnlog.lib.parse.validation.locateFailures
import dev.vulnlog.lib.parse.validation.parseToNodeTree
import dev.vulnlog.lib.parse.validation.resolveSchemaVersion
import dev.vulnlog.lib.shell.InputDocument

/**
 * Reads [document] to the Vulnlog DTO representation or returns a [ValidationOutcome] containing the details of why parsing and validation failed.
 */
fun parseDocument(
    document: InputDocument,
    config: ValidationConfig = ValidationConfig(),
): ValidationOutcome<ParsedVulnlogProject> {
    val nodeTree =
        when (val result = parseToNodeTree(document.content)) {
            is NodeTreeResult.Rejected -> return Stopped.Unreadable(result.problems, emptyList())
            is NodeTreeResult.Valid -> result
        }

    val version =
        when (val result = resolveSchemaVersion(nodeTree.rootNode)) {
            is SchemaVersionResult.Rejected -> return Stopped.Unreadable(result.problems, emptyList())
            is SchemaVersionResult.Recognized -> result.version
        }

    val values =
        when (val result = constructDocument(nodeTree.rootNode)) {
            is DocumentResult.Rejected -> return Stopped.Unreadable(result.problems, emptyList())
            is DocumentResult.Built -> result.document
        }

    val dto =
        when (val result = bindToDto(values, version, nodeTree.rootNode)) {
            is DtoParseResult.Rejected -> return Stopped.Unreadable(result.problems, emptyList())
            is DtoParseResult.Parsed -> result.dto
        }

    return Ok(ParsedVulnlogProject(document, nodeTree, dto), emptyList())
}

/** Continues [parseDocument] into the domain model and runs the domain rules over it. */
fun validateDocument(
    document: InputDocument,
    config: ValidationConfig = ValidationConfig(),
): ValidationOutcome<ValidVulnlogProject> {
    val parsed =
        when (val outcome = parseDocument(document, config)) {
            is Stopped -> return outcome
            is Ok -> outcome
        }

    val file =
        when (val result = mapToDomain(parsed.project.validatedDto)) {
            is DomainMappingResult.Rejected ->
                return Stopped.Unreadable(
                    locateFailures(parsed.project.nodeTree.rootNode, result.problems),
                    parsed.findings,
                )

            is DomainMappingResult.Mapped -> result.vulnlogProjectFile
        }

    val findings = parsed.findings + domainFindings(file)
    return outcomeOf(findings, config) { ValidVulnlogProject(parsed.project, file) }
}

private fun domainFindings(file: VulnlogFile): List<ValidationFinding> =
    when (file.schemaVersion) {
        SchemaVersion.V1 -> v1DomainRules.flatMap { rule -> rule(file) }
    }

private fun <T> outcomeOf(
    findings: List<ValidationFinding>,
    config: ValidationConfig,
    project: () -> T,
): ValidationOutcome<T> =
    when (findings.highestSeverity) {
        ERROR -> Stopped.Rejected(findings)
        WARNING -> if (config.strict) Stopped.Rejected(findings) else Ok(project(), findings)
        INFO -> Ok(project(), findings)
    }
