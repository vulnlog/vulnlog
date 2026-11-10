// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.cyclonedx.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.JsonNode

// Property order is the emitted key order. Every DTO here is listed in the native-image reflect-config.json.

data class CycloneDxBomDto(
    @param:JsonProperty("\$schema")
    val schema: String,
    val bomFormat: String,
    val specVersion: String,
    val serialNumber: String,
    val version: Int,
    val metadata: CycloneDxMetadataDto,
    val components: List<CycloneDxComponentDto>,
    val vulnerabilities: List<CycloneDxVulnerabilityDto>,
)

data class CycloneDxMetadataDto(
    val timestamp: String,
    val tools: CycloneDxToolsDto? = null,
    val authors: List<CycloneDxContactDto>,
    val component: CycloneDxComponentDto,
    val supplier: CycloneDxOrganizationDto,
)

data class CycloneDxToolsDto(
    val components: List<CycloneDxComponentDto>,
)

data class CycloneDxContactDto(
    val name: String,
    val email: String? = null,
)

data class CycloneDxOrganizationDto(
    val name: String,
)

data class CycloneDxComponentDto(
    val type: String,
    @param:JsonProperty("bom-ref")
    val bomRef: String? = null,
    val group: String? = null,
    val name: String,
    val version: String? = null,
    val purl: String? = null,
    val externalReferences: List<CycloneDxExternalReferenceDto>? = null,
    val components: List<CycloneDxComponentDto>? = null,
)

data class CycloneDxExternalReferenceDto(
    val type: String,
    val url: String,
)

data class CycloneDxVulnerabilityDto(
    val id: String,
    val source: CycloneDxSourceDto,
    val references: List<CycloneDxReferenceDto>? = null,
    val description: String? = null,
    val recommendation: String? = null,
    val analysis: CycloneDxAnalysisDto,
    val affects: List<CycloneDxAffectDto>,
    val properties: List<CycloneDxPropertyDto>? = null,
)

data class CycloneDxSourceDto(
    val name: String,
    val url: String,
)

data class CycloneDxReferenceDto(
    val id: String,
    val source: CycloneDxSourceDto,
)

data class CycloneDxAnalysisDto(
    val state: String,
    val justification: String? = null,
    val response: List<String>? = null,
    val detail: String? = null,
    val firstIssued: String,
)

data class CycloneDxAffectDto(
    val ref: String,
)

data class CycloneDxPropertyDto(
    val name: String,
    val value: String,
)

/**
 * Every field is nullable, so a foreign document binds and is rejected afterwards. The nested parts stay trees,
 * because each vulnerability is read back leniently on its own.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class CycloneDxBaselineDto(
    val bomFormat: String? = null,
    val specVersion: String? = null,
    val serialNumber: String? = null,
    val version: Long? = null,
    val metadata: JsonNode? = null,
    val components: JsonNode? = null,
    val vulnerabilities: JsonNode? = null,
)
