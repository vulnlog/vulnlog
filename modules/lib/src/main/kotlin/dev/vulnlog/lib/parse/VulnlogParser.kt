// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse

import dev.vulnlog.lib.model.ParseValidationVersion
import dev.vulnlog.lib.model.VulnlogFileRaw
import dev.vulnlog.lib.model.validation.FailureLocation
import dev.vulnlog.lib.model.validation.ParseFailure
import dev.vulnlog.lib.parse.v1.V1Mapper
import dev.vulnlog.lib.parse.v1.dto.VulnlogFileV1Dto
import dev.vulnlog.lib.result.DomainMappingResult
import dev.vulnlog.lib.result.ParseResult
import dev.vulnlog.lib.result.YamlParseDtoResult
import dev.vulnlog.lib.result.YamlParseResult
import org.snakeyaml.engine.v2.api.LoadSettings
import org.snakeyaml.engine.v2.api.lowlevel.Compose
import org.snakeyaml.engine.v2.constructor.StandardConstructor
import org.snakeyaml.engine.v2.exceptions.MarkedYamlEngineException
import org.snakeyaml.engine.v2.exceptions.YamlEngineException
import org.snakeyaml.engine.v2.nodes.MappingNode
import org.snakeyaml.engine.v2.nodes.Node
import org.snakeyaml.engine.v2.nodes.ScalarNode
import org.snakeyaml.engine.v2.nodes.SequenceNode
import tools.jackson.core.JacksonException
import tools.jackson.databind.DatabindException
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.exc.UnrecognizedPropertyException
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

private val syntaxSettings: LoadSettings =
    LoadSettings
        .builder()
        .setUseMarks(true)
        .setAllowDuplicateKeys(true) // key handling stays last-wins, as before
        .setParseComments(true) // rewrite flows detect comments on the same node tree
        .build()

/** Step 1: the text is well-formed YAML with a schemaVersion declaration. */
fun parseToYaml(raw: VulnlogFileRaw): YamlParseResult {
    val root =
        try {
            Compose(syntaxSettings).composeString(raw.content).orElse(null)
        } catch (e: MarkedYamlEngineException) {
            return YamlParseResult.Invalid(e.problem ?: "Invalid YAML", locationOf(e))
        } catch (e: YamlEngineException) {
            return YamlParseResult.Invalid(e.message ?: "Invalid YAML")
        }
    val mapping = root as? MappingNode ?: return YamlParseResult.Invalid("Missing or invalid schemaVersion")
    val versionNode =
        valueNodeOf(mapping, "schemaVersion")
            ?: return YamlParseResult.Invalid("Missing or invalid schemaVersion")
    val schemaVersion = (versionNode as? ScalarNode)?.value?.let(::parseSchemaVersion)
    return when (schemaVersion) {
        null -> YamlParseResult.Invalid("Missing or invalid schemaVersion", locationOf(versionNode))
        else -> YamlParseResult.Valid(mapping, schemaVersion)
    }
}

/**
 * Step 2b, first half: the YAML maps onto the versioned DTO. Works off the already composed node
 * tree, so the text is never parsed a second time and failures point at the offending node.
 */
fun parseToVulnlogDto(
    mapper: ObjectMapper,
    validYaml: YamlParseResult.Valid,
): YamlParseDtoResult {
    val validationVersion =
        when (val major = validYaml.schemaVersion.major) {
            1 -> ParseValidationVersion.V1
            else -> return YamlParseDtoResult.Invalid("Unsupported schema version '$major'. Try updating vulnlog.")
        }
    return when (validationVersion) {
        ParseValidationVersion.V1 -> parseToV1Dto(mapper, validYaml, validationVersion)
    }
}

private fun parseToV1Dto(
    mapper: ObjectMapper,
    validYaml: YamlParseResult.Valid,
    validationVersion: ParseValidationVersion,
): YamlParseDtoResult {
    val document =
        try {
            StandardConstructor(syntaxSettings).constructSingleDocument(Optional.of(validYaml.rootNode))
        } catch (e: MarkedYamlEngineException) {
            return YamlParseDtoResult.Invalid("YAML parse error: ${e.problem}", locationOf(e))
        } catch (e: YamlEngineException) {
            return YamlParseDtoResult.Invalid("YAML parse error: ${e.message}")
        }
    return try {
        val dto = mapper.convertValue(document, VulnlogFileV1Dto::class.java)
        YamlParseDtoResult.Valid(dto, validYaml.schemaVersion, validationVersion)
    } catch (e: UnrecognizedPropertyException) {
        YamlParseDtoResult.Invalid(
            "Unknown property '${e.propertyName}'. Try updating vulnlog.",
            locationOf(validYaml.rootNode, e.path),
        )
    } catch (e: DatabindException) {
        YamlParseDtoResult.Invalid("YAML parse error: ${e.originalMessage}", locationOf(validYaml.rootNode, e.path))
    }
}

/** Full per-file pipeline; parses the text once and stops at the first failing step. */
fun parseVulnlogFile(
    mapper: ObjectMapper,
    raw: VulnlogFileRaw,
): ParseResult {
    val yaml =
        when (val result = parseToYaml(raw)) {
            is YamlParseResult.Invalid ->
                return ParseResult.Error(listOf(ParseFailure(result.errorMessage, location = result.location)))

            is YamlParseResult.Valid -> result
        }
    // Step 2a slot: validate yaml.rootNode against the JSON schema (/schema) in a future commit.
    val dto =
        when (val result = parseToVulnlogDto(mapper, yaml)) {
            is YamlParseDtoResult.Invalid ->
                return ParseResult.Error(listOf(ParseFailure(result.message, location = result.location)))

            is YamlParseDtoResult.Valid -> result
        }
    // Step 2b, second half: the DTO maps onto the domain model.
    val domain =
        when (dto.validationVersion) {
            ParseValidationVersion.V1 -> V1Mapper.toDomain(dto.schemaVersion, dto.dto)
        }
    val content =
        when (domain) {
            is DomainMappingResult.Invalid -> return ParseResult.Error(locate(yaml.rootNode, domain.failures))
            is DomainMappingResult.Valid -> domain.file
        }
    return ParseResult.Ok(dto.validationVersion, content, dto.dto, yaml.rootNode, raw)
}

/** Resolves each failure's path against the node tree so messages can point at line and column. */
private fun locate(
    root: MappingNode,
    failures: List<ParseFailure>,
): List<ParseFailure> {
    val nodesByPath = walkValues(root).associate { it.path to it.node }
    return failures.map { failure ->
        failure.copy(location = failure.path?.let { nodesByPath[it] }?.let(::locationOf))
    }
}

private fun locationOf(e: MarkedYamlEngineException): FailureLocation? =
    e.problemMark.getOrNull()?.let { FailureLocation(it.line + 1, it.column + 1) }

private fun locationOf(node: Node): FailureLocation? =
    node.startMark.getOrNull()?.let {
        FailureLocation(
            it.line + 1,
            it.column + 1,
        )
    }

/** Location of the node a Jackson failure path points at; the deepest resolvable node wins. */
private fun locationOf(
    root: MappingNode,
    path: List<JacksonException.Reference>,
): FailureLocation? {
    var node: Node = root
    for (reference in path) {
        node = when {
            reference.propertyName != null -> (node as? MappingNode)?.let { valueNodeOf(it, reference.propertyName) }
            reference.index >= 0 -> (node as? SequenceNode)?.value?.getOrNull(reference.index)
            else -> null
        } ?: return locationOf(node)
    }
    return locationOf(node)
}
