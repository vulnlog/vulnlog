// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.cyclonedx

import com.github.packageurl.MalformedPackageURLException
import com.github.packageurl.PackageURL
import dev.vulnlog.lib.codec.cyclonedx.dto.CycloneDxAffectDto
import dev.vulnlog.lib.codec.cyclonedx.dto.CycloneDxAnalysisDto
import dev.vulnlog.lib.codec.cyclonedx.dto.CycloneDxBomDto
import dev.vulnlog.lib.codec.cyclonedx.dto.CycloneDxComponentDto
import dev.vulnlog.lib.codec.cyclonedx.dto.CycloneDxContactDto
import dev.vulnlog.lib.codec.cyclonedx.dto.CycloneDxExternalReferenceDto
import dev.vulnlog.lib.codec.cyclonedx.dto.CycloneDxMetadataDto
import dev.vulnlog.lib.codec.cyclonedx.dto.CycloneDxOrganizationDto
import dev.vulnlog.lib.codec.cyclonedx.dto.CycloneDxPropertyDto
import dev.vulnlog.lib.codec.cyclonedx.dto.CycloneDxReferenceDto
import dev.vulnlog.lib.codec.cyclonedx.dto.CycloneDxToolsDto
import dev.vulnlog.lib.codec.cyclonedx.dto.CycloneDxVulnerabilityDto
import dev.vulnlog.lib.codec.vex.remediationSentence
import dev.vulnlog.lib.codec.vex.vexJson
import dev.vulnlog.lib.codec.vex.vexJustificationToken
import dev.vulnlog.lib.core.vex.vexStatusKind
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.vex.VexAuthor
import dev.vulnlog.lib.model.vex.VexStatementTime
import dev.vulnlog.lib.model.vex.VexStatus
import dev.vulnlog.lib.model.vex.VexTooling
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexDocument
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexFormatVersion
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexProduct
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexVulnerability
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object CycloneDxVexEncoder {
    fun encode(document: CycloneDxVexDocument): String = vexJson.writeValueAsString(toDto(document)) + "\n"

    /** One branch per format version: a new version gets its own mapping rather than flags in this one. */
    internal fun toDto(document: CycloneDxVexDocument): CycloneDxBomDto =
        when (document.formatVersion) {
            CycloneDxVexFormatVersion.VERSION_1_7 -> toBomDtoV17(document)
        }
}

/**
 * The vulnerable packages are the top-level components, each referenced by its purl, so a consumer resolves every
 * `affects.ref` inside the document and matches the package by its purl.
 */
private fun toBomDtoV17(document: CycloneDxVexDocument): CycloneDxBomDto =
    CycloneDxBomDto(
        schema = cycloneDxSchema(document.formatVersion),
        bomFormat = BOM_FORMAT,
        specVersion = document.formatVersion.version,
        serialNumber = document.identity.serialNumber.value,
        version = document.identity.version.value,
        metadata =
            CycloneDxMetadataDto(
                timestamp = format(document.identity.timestamp),
                tools = document.tooling?.let(::toToolsDto),
                authors = listOf(toContactDto(document.author)),
                component = toProductDto(document.product),
                supplier = CycloneDxOrganizationDto(document.supplier.organization),
            ),
        components =
            document.vulnerabilities
                .flatMap(CycloneDxVexVulnerability::packages)
                .distinct()
                .sortedBy(Purl::value)
                .map { purl -> toComponentDto(LIBRARY_TYPE, purl, bomRef = purl.value) },
        vulnerabilities = document.vulnerabilities.map { vulnerability -> toVulnerabilityDto(vulnerability, document) },
    )

private fun toToolsDto(tooling: VexTooling): CycloneDxToolsDto =
    CycloneDxToolsDto(
        components =
            listOf(
                CycloneDxComponentDto(
                    type = APPLICATION_TYPE,
                    name = "Vulnlog ${tooling.platform}",
                    version = tooling.version,
                    externalReferences = listOf(CycloneDxExternalReferenceDto(WEBSITE_REFERENCE, VULNLOG_SITE)),
                ),
            ),
    )

/** A contact that is no address goes with the name: the schema has no field for a URL or a phrase. */
private fun toContactDto(author: VexAuthor): CycloneDxContactDto {
    val contact = author.contact ?: return CycloneDxContactDto(author.name)
    return if ('@' in contact) {
        CycloneDxContactDto(author.name, email = contact)
    } else {
        CycloneDxContactDto("${author.name} ($contact)")
    }
}

/** The release purls are the parts of the product, as the document is about the release and not one artifact. */
private fun toProductDto(product: CycloneDxVexProduct): CycloneDxComponentDto =
    CycloneDxComponentDto(
        type = APPLICATION_TYPE,
        name = product.name,
        version = product.release.value,
        components = product.purls.map { purl -> toComponentDto(APPLICATION_TYPE, purl) }.takeIf { it.isNotEmpty() },
    )

/** Name, group and version are taken from the purl, so a consumer without purl matching still identifies it. */
private fun toComponentDto(
    type: String,
    purl: Purl,
    bomRef: String? = null,
): CycloneDxComponentDto {
    val parsed =
        try {
            PackageURL(purl.value)
        } catch (_: MalformedPackageURLException) {
            null
        }
    return CycloneDxComponentDto(
        type = type,
        bomRef = bomRef,
        group = parsed?.namespace,
        name = parsed?.name ?: purl.value,
        version = parsed?.version,
        purl = purl.value,
    )
}

/**
 * An issued analysis is written with the document's time rather than left out: the document time moves with every
 * revision, and a consumer would otherwise date the analysis by the revision it happens to read.
 */
private fun toVulnerabilityDto(
    vulnerability: CycloneDxVexVulnerability,
    document: CycloneDxVexDocument,
): CycloneDxVulnerabilityDto {
    val firstIssued =
        when (val time = vulnerability.firstIssued) {
            is VexStatementTime.Stated -> time.date.atStartOfDay(ZoneOffset.UTC).toInstant()
            is VexStatementTime.Carried -> time.at
            VexStatementTime.Issued -> document.identity.timestamp
        }.let(::format)
    val analysis =
        CycloneDxAnalysisDto(state = cycloneDxState(vexStatusKind(vulnerability.status)), firstIssued = firstIssued)
    val dto =
        CycloneDxVulnerabilityDto(
            id = vulnerability.id.id,
            source = cycloneDxSource(vulnerability.id),
            references =
                vulnerability.aliases
                    .map { alias -> CycloneDxReferenceDto(alias.id, cycloneDxSource(alias)) }
                    .takeIf { it.isNotEmpty() },
            description = vulnerability.description,
            analysis = analysis,
            affects = vulnerability.packages.map { purl -> CycloneDxAffectDto(purl.value) },
        )
    return when (val status = vulnerability.status) {
        is VexStatus.UnderInvestigation -> dto.copy(analysis = analysis.copy(detail = status.statusNotes))
        VexStatus.Fixed -> dto
        is VexStatus.NotAffected ->
            dto.copy(
                analysis =
                    analysis.copy(
                        justification = cycloneDxJustification(status.justification),
                        detail = status.impactStatement,
                    ),
                properties =
                    listOf(CycloneDxPropertyDto(JUSTIFICATION_PROPERTY, vexJustificationToken(status.justification))),
            )

        is VexStatus.Affected ->
            dto.copy(
                recommendation = remediationSentence(status.remediation),
                analysis =
                    analysis.copy(
                        response = cycloneDxResponse(status.remediation)?.let(::listOf),
                        detail = status.statusNotes,
                    ),
            )
    }
}

/** Formatted here rather than by Jackson, whose date handling differs between versions. */
private fun format(instant: Instant): String = DateTimeFormatter.ISO_INSTANT.format(instant)
