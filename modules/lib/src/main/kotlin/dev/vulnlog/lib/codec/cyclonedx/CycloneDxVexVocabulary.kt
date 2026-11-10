// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.cyclonedx

import dev.vulnlog.lib.codec.cyclonedx.dto.CycloneDxSourceDto
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.vex.Remediation
import dev.vulnlog.lib.model.vex.VexStatusKind
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxSerialNumber
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexFormatVersion
import java.util.UUID

// Every CycloneDX string Vulnlog writes, next to its reverse where a baseline is read back.

internal const val BOM_FORMAT: String = "CycloneDX"

private const val SCHEMA_PREFIX = "http://cyclonedx.org/schema/bom-"
private const val SCHEMA_SUFFIX = ".schema.json"
private const val SERIAL_NUMBER_PREFIX = "urn:uuid:"

internal const val VULNLOG_SITE: String = "https://vulnlog.dev/"

internal const val APPLICATION_TYPE: String = "application"
internal const val LIBRARY_TYPE: String = "library"
internal const val WEBSITE_REFERENCE: String = "website"

/** The OpenVEX justification, kept because CycloneDX folds two of them into `code_not_present`. */
internal const val JUSTIFICATION_PROPERTY: String = "vulnlog:justification"

/** The caller draws the UUID, so the serial number stays deterministic in tests. */
fun cycloneDxSerialNumber(uuid: UUID): CycloneDxSerialNumber = CycloneDxSerialNumber(SERIAL_NUMBER_PREFIX + uuid)

internal fun cycloneDxSchema(formatVersion: CycloneDxVexFormatVersion): String =
    SCHEMA_PREFIX + formatVersion.version + SCHEMA_SUFFIX

internal fun cycloneDxState(kind: VexStatusKind): String =
    when (kind) {
        VexStatusKind.AFFECTED -> "exploitable"
        VexStatusKind.FIXED -> "resolved"
        VexStatusKind.NOT_AFFECTED -> "not_affected"
        VexStatusKind.UNDER_INVESTIGATION -> "in_triage"
    }

internal fun cycloneDxStateKind(token: String): VexStatusKind? =
    VexStatusKind.entries.firstOrNull { cycloneDxState(it) == token }

/** Not reversible: both "not present" justifications read as `code_not_present`, see [JUSTIFICATION_PROPERTY]. */
internal fun cycloneDxJustification(justification: VexJustification): String =
    when (justification) {
        VexJustification.COMPONENT_NOT_PRESENT -> "code_not_present"
        VexJustification.INLINE_MITIGATIONS_ALREADY_EXIST -> "protected_by_mitigating_control"
        VexJustification.VULNERABLE_CODE_CANNOT_BE_CONTROLLED_BY_ADVERSARY -> "protected_at_runtime"
        VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH -> "code_not_reachable"
        VexJustification.VULNERABLE_CODE_NOT_PRESENT -> "code_not_present"
    }

/** Null where a fix is planned: no CycloneDX response says "wait", and `update` would name a release that is not out. */
internal fun cycloneDxResponse(remediation: Remediation): String? =
    when (remediation) {
        is Remediation.RiskAccepted -> "will_not_fix"
        is Remediation.FixPlanned -> null
        is Remediation.UpdateTo -> "update"
        Remediation.NoneAvailable -> "can_not_fix"
    }

/** Named as Dependency-Track knows the sources, so it finds the vulnerability it already reported. */
internal fun cycloneDxSource(id: VulnId): CycloneDxSourceDto =
    when (id) {
        is VulnId.Cve -> CycloneDxSourceDto("NVD", "https://nvd.nist.gov/vuln/detail/${id.id}")
        is VulnId.Ghsa -> CycloneDxSourceDto("GITHUB", "https://github.com/advisories/${id.id}")
        is VulnId.RustSec -> CycloneDxSourceDto("RUSTSEC", "https://rustsec.org/advisories/${id.id}")
        is VulnId.Snyk -> CycloneDxSourceDto("SNYK", "https://security.snyk.io/vuln/${id.id}")
    }
