// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse

import dev.vulnlog.lib.model.ParseValidationVersion
import dev.vulnlog.lib.model.SchemaVersion
import dev.vulnlog.lib.model.VulnlogFileRaw
import dev.vulnlog.lib.model.validation.FailureLocation
import dev.vulnlog.lib.parse.v1.V1Mapper
import dev.vulnlog.lib.parse.v1.dto.VulnlogFileV1Dto
import dev.vulnlog.lib.result.ParseResult
import dev.vulnlog.lib.result.YamlParseDtoResult
import dev.vulnlog.lib.result.YamlParseResult
import org.snakeyaml.engine.v2.api.LoadSettings
import org.snakeyaml.engine.v2.api.lowlevel.Compose
import org.snakeyaml.engine.v2.exceptions.MarkedYamlEngineException
import org.snakeyaml.engine.v2.exceptions.YamlEngineException
import org.snakeyaml.engine.v2.nodes.MappingNode
import tools.jackson.databind.DatabindException
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.exc.UnrecognizedPropertyException
import tools.jackson.module.kotlin.readValue
import kotlin.jvm.optionals.getOrNull

private val syntaxSettings: LoadSettings =
    LoadSettings
        .builder()
        .setUseMarks(true)
        .setAllowDuplicateKeys(true) // key handling stays with Jackson (last wins), as before
        .build()

/** Step 1: the text is well-formed YAML with a schemaVersion declaration. */
fun parseToYaml(raw: VulnlogFileRaw): YamlParseResult {
    val root =
        try {
            Compose(syntaxSettings).composeString(raw.content).orElse(null)
        } catch (e: MarkedYamlEngineException) {
            val location = e.problemMark.getOrNull()?.let { FailureLocation(it.line + 1, it.column + 1) }
            return YamlParseResult.Invalid(e.problem ?: "Invalid YAML", location)
        } catch (e: YamlEngineException) {
            return YamlParseResult.Invalid(e.message ?: "Invalid YAML")
        }
    val mapping = root as? MappingNode ?: return YamlParseResult.Invalid("Missing or invalid schemaVersion")
    val schemaVersion =
        scalarValueOf(mapping, "schemaVersion")?.let(::parseSchemaVersion)
            ?: return YamlParseResult.Invalid("Missing or invalid schemaVersion")
    return YamlParseResult.Valid(mapping, schemaVersion)
}

/** Step 2b, first half: the YAML maps onto the versioned DTO. */
fun parseToVulnlogDto(
    mapper: ObjectMapper,
    validYaml: YamlParseResult.Valid,
    raw: VulnlogFileRaw,
): YamlParseDtoResult {
    val validationVersion =
        when (val major = validYaml.schemaVersion.major) {
            1 -> ParseValidationVersion.V1
            else -> return YamlParseDtoResult.Invalid("Unsupported schema version '$major'. Try updating vulnlog.")
        }
    return when (validationVersion) {
        ParseValidationVersion.V1 -> parseToV1Dto(mapper, validYaml.schemaVersion, validationVersion, raw)
    }
}

private fun parseToV1Dto(
    mapper: ObjectMapper,
    schemaVersion: SchemaVersion,
    validationVersion: ParseValidationVersion,
    raw: VulnlogFileRaw,
): YamlParseDtoResult =
    try {
        YamlParseDtoResult.Valid(mapper.readValue<VulnlogFileV1Dto>(raw.content), schemaVersion, validationVersion)
    } catch (e: UnrecognizedPropertyException) {
        YamlParseDtoResult.Invalid("Unknown property '${e.propertyName}'. Try updating vulnlog.")
    } catch (e: DatabindException) {
        YamlParseDtoResult.Invalid("YAML parse error: ${e.originalMessage}")
    }

/** Step 2b, second half: the DTO maps onto the domain model. */
fun parseToVulnlog(
    validDto: YamlParseDtoResult.Valid,
    raw: VulnlogFileRaw,
): ParseResult =
    when (validDto.validationVersion) {
        ParseValidationVersion.V1 ->
            V1Mapper.toDomain(validDto.validationVersion, validDto.schemaVersion, validDto.dto, raw)
    }

/** Full per-file pipeline; stops at the first failing step. */
fun parseVulnlogFile(
    mapper: ObjectMapper,
    raw: VulnlogFileRaw,
): ParseResult {
    val yaml =
        when (val result = parseToYaml(raw)) {
            is YamlParseResult.Invalid -> return ParseResult.Error(result.errorMessage, result.location)
            is YamlParseResult.Valid -> result
        }
    // Step 2a slot: validate yaml.rootNode against the JSON schema (/schema) in a future commit.
    val dto =
        when (val result = parseToVulnlogDto(mapper, yaml, raw)) {
            is YamlParseDtoResult.Invalid -> return ParseResult.Error(result.message)
            is YamlParseDtoResult.Valid -> result
        }
    return parseToVulnlog(dto, raw)
}
