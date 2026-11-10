// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.cyclonedx

import dev.vulnlog.lib.model.vex.VexDocumentVersion
import java.time.Instant

/**
 * Declared oldest first. A document is read and written in one version only, and every branch that shapes bytes is an
 * exhaustive `when` over these entries, so a new version does not compile until each branch handles it.
 */
enum class CycloneDxVexFormatVersion(
    val version: String,
) {
    VERSION_1_7("1.7"),
    ;

    companion object {
        val LATEST: CycloneDxVexFormatVersion = entries.last()
    }
}

data class CycloneDxVexIdentity(
    val serialNumber: CycloneDxSerialNumber,
    val timestamp: Instant,
    val version: VexDocumentVersion,
)

/** A baseline is only continued in its own format version, so a revision takes the version from it. */
sealed interface CycloneDxVexRevision {
    val formatVersion: CycloneDxVexFormatVersion

    data class First(
        val serialNumber: CycloneDxSerialNumber,
        override val formatVersion: CycloneDxVexFormatVersion = CycloneDxVexFormatVersion.LATEST,
    ) : CycloneDxVexRevision

    data class Next(
        val baseline: CycloneDxVexBaseline,
    ) : CycloneDxVexRevision {
        override val formatVersion: CycloneDxVexFormatVersion get() = baseline.formatVersion
    }
}

/** The specification requires an RFC 4122 URN in lowercase, as `urn:uuid:<uuid>`. */
@JvmInline
value class CycloneDxSerialNumber(
    val value: String,
) {
    init {
        require(isSerialNumber(value)) { "CycloneDX serial number must be a urn:uuid: '$value'" }
    }

    companion object {
        fun parse(value: String): CycloneDxSerialNumber? = value.takeIf(::isSerialNumber)?.let(::CycloneDxSerialNumber)
    }
}

private val SERIAL_NUMBER = Regex("urn:uuid:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

private fun isSerialNumber(value: String): Boolean = SERIAL_NUMBER.matches(value)
