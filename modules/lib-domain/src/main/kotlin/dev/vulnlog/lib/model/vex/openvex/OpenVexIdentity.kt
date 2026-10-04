// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

import java.net.URI
import java.net.URISyntaxException
import java.time.Instant

/**
 * Declared oldest first. A document is read and written in one version only, and every branch that shapes bytes is an
 * exhaustive `when` over these entries, so a new version does not compile until each branch handles it.
 */
enum class OpenVexFormatVersion(
    val version: String,
) {
    VERSION_0_2_0("0.2.0"),
    ;

    companion object {
        val LATEST: OpenVexFormatVersion = entries.last()
    }
}

data class OpenVexIdentity(
    val id: OpenVexDocumentId,
    val timestamp: Instant,
    val version: OpenVexDocumentVersion,
)

/** A baseline is only continued in its own format version, so a revision takes the version from it. */
sealed interface OpenVexRevision {
    val formatVersion: OpenVexFormatVersion

    data class First(
        val id: OpenVexDocumentId,
        override val formatVersion: OpenVexFormatVersion = OpenVexFormatVersion.LATEST,
    ) : OpenVexRevision

    data class Next(
        val baseline: OpenVexBaseline,
    ) : OpenVexRevision {
        override val formatVersion: OpenVexFormatVersion get() = baseline.formatVersion
    }
}

/** The specification requires an IRI; `java.net.URI` accepts the non-ASCII characters an IRI allows. */
@JvmInline
value class OpenVexDocumentId(
    val value: String,
) {
    init {
        require(isAbsoluteIri(value)) { "OpenVEX document id must be an absolute IRI: '$value'" }
    }

    companion object {
        fun parse(value: String): OpenVexDocumentId? = value.takeIf(::isAbsoluteIri)?.let(::OpenVexDocumentId)
    }
}

@JvmInline
value class OpenVexDocumentVersion(
    val value: Int,
) {
    init {
        require(value >= 1) { "OpenVEX document version must be at least 1: $value" }
    }

    fun next(): OpenVexDocumentVersion = OpenVexDocumentVersion(Math.addExact(value, 1))

    companion object {
        val FIRST: OpenVexDocumentVersion = OpenVexDocumentVersion(1)

        /** Rejects the last Int too: a baseline at that version could not be continued without overflow. */
        fun parse(value: Long): OpenVexDocumentVersion? =
            value.takeIf { it in 1 until Int.MAX_VALUE }?.let { OpenVexDocumentVersion(it.toInt()) }
    }
}

private fun isAbsoluteIri(value: String): Boolean =
    value.isNotBlank() &&
        value.trim() == value &&
        try {
            URI(value).isAbsolute
        } catch (_: URISyntaxException) {
            false
        }
