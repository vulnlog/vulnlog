// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app

import dev.vulnlog.lib.app.ValidationOutcome.Ok
import dev.vulnlog.lib.core.validation.v1DomainRules
import dev.vulnlog.lib.document.DomainMappingResult
import dev.vulnlog.lib.document.InputDocument
import dev.vulnlog.lib.document.dto.DtoVersion
import dev.vulnlog.lib.document.dto.VulnlogFileV1Dto
import dev.vulnlog.lib.document.mapToDomain
import dev.vulnlog.lib.document.validation.DocumentResult
import dev.vulnlog.lib.document.validation.DtoParseResult
import dev.vulnlog.lib.document.validation.NodeTreeResult
import dev.vulnlog.lib.document.validation.ParsedVulnlogProject
import dev.vulnlog.lib.document.validation.SchemaVersionResult
import dev.vulnlog.lib.document.validation.ValidVulnlogProject
import dev.vulnlog.lib.document.validation.bindToDto
import dev.vulnlog.lib.document.validation.constructDocument
import dev.vulnlog.lib.document.validation.locateFailures
import dev.vulnlog.lib.document.validation.parseToNodeTree
import dev.vulnlog.lib.document.validation.resolveSchemaVersion
import dev.vulnlog.lib.document.validation.v1DtoRules
import dev.vulnlog.lib.finding.FindingSeverity.ERROR
import dev.vulnlog.lib.finding.FindingSeverity.INFO
import dev.vulnlog.lib.finding.FindingSeverity.WARNING
import dev.vulnlog.lib.finding.ValidationFinding
import dev.vulnlog.lib.finding.highestSeverity
import dev.vulnlog.lib.model.SchemaVersion
import dev.vulnlog.lib.model.VulnlogFile

/**
 * Reads [document] to the Vulnlog DTO representation or returns a [ValidationOutcome] containing the details of why parsing and validation failed.
 */
fun parseDocument(
    document: InputDocument,
    config: ValidationConfig = ValidationConfig(),
): ValidationOutcome<ParsedVulnlogProject> {
    val nodeTree =
        when (val result = parseToNodeTree(document.content)) {
            is NodeTreeResult.Rejected -> return InputRejected.Unparsable(result.problems, emptyList())
            is NodeTreeResult.Valid -> result
        }

    val version =
        when (val result = resolveSchemaVersion(nodeTree.rootNode)) {
            is SchemaVersionResult.Rejected -> return InputRejected.Unparsable(result.problems, emptyList())
            is SchemaVersionResult.Recognized -> result.version
        }

    val values =
        when (val result = constructDocument(nodeTree.rootNode)) {
            is DocumentResult.Rejected -> return InputRejected.Unparsable(result.problems, emptyList())
            is DocumentResult.Built -> result.document
        }

    val dto =
        when (val result = bindToDto(values, version, nodeTree.rootNode)) {
            is DtoParseResult.Rejected -> return InputRejected.Unparsable(result.problems, emptyList())
            is DtoParseResult.Parsed -> result.dto
        }

    val findings = dtoFindings(dto)
    return outcomeOf(findings, config) { ParsedVulnlogProject(document, nodeTree, dto) }
}

/** Continues [parseDocument] into the domain model and runs the domain rules over it. */
fun validateDocument(
    document: InputDocument,
    config: ValidationConfig = ValidationConfig(),
): ValidationOutcome<ValidVulnlogProject> {
    val parsed =
        when (val outcome = parseDocument(document, config)) {
            is InputRejected -> return outcome
            is Ok -> outcome
        }

    val file =
        when (val result = mapToDomain(parsed.project.validatedDto)) {
            is DomainMappingResult.Rejected ->
                return InputRejected.Unparsable(
                    locateFailures(parsed.project.nodeTree.rootNode, result.problems),
                    parsed.findings,
                )

            is DomainMappingResult.Mapped -> result.vulnlogProjectFile
        }

    val findings = parsed.findings + domainFindings(file)
    return outcomeOf(findings, config) { ValidVulnlogProject(parsed.project, file) }
}

private fun dtoFindings(dto: DtoVersion): List<ValidationFinding> =
    when (dto) {
        is VulnlogFileV1Dto -> v1DtoRules.flatMap { rule -> rule(dto) }
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
        ERROR -> InputRejected.Invalid(findings)
        WARNING -> if (config.strict) InputRejected.Invalid(findings) else Ok(project(), findings)
        INFO -> Ok(project(), findings)
    }
