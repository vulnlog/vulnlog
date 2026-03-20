package dev.vulnlog.cli.parse

import dev.vulnlog.cli.model.ParseValidationVersion
import dev.vulnlog.cli.model.SchemaVersion
import dev.vulnlog.cli.parse.v1.V1Mapper
import dev.vulnlog.cli.parse.v1.dto.VulnlogFileV1Dto
import dev.vulnlog.cli.result.ParseResult
import tools.jackson.databind.DatabindException
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

class YamlParser(private val mapper: ObjectMapper) {
    fun parse(yaml: String): ParseResult {
        val schemaVersion: SchemaVersion =
            detectVersion(yaml)
                ?: return ParseResult.Error(listOf("Missing or invalid schemaVersion"))

        val parseAndValidationVersion =
            when (schemaVersion.major) {
                1 -> ParseValidationVersion.V1
                else -> return ParseResult.Error(
                    listOf("Unsupported schema version '$schemaVersion'. Try updating vulnlog."),
                )
            }

        return when (parseAndValidationVersion) {
            ParseValidationVersion.V1 -> parseV1(yaml, ParseValidationVersion.V1, schemaVersion)
        }
    }

    private fun detectVersion(yaml: String): SchemaVersion? {
        val tree = mapper.readTree(yaml)
        val raw = tree.get("schemaVersion")?.stringValue() ?: return null
        return parseSchemaVersion(raw)
    }

    private fun parseV1(
        yaml: String,
        validationVersion: ParseValidationVersion,
        schemaVersion: SchemaVersion,
    ): ParseResult {
        val dto: VulnlogFileV1Dto =
            try {
                mapper.readValue<VulnlogFileV1Dto>(yaml)
            } catch (e: DatabindException) {
                return ParseResult.Error(listOf("YAML parse error: ${e.originalMessage}"))
            }
        return V1Mapper.toDomain(validationVersion, schemaVersion, dto)
    }
}
