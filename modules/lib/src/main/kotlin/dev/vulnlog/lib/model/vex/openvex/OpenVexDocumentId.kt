// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

import java.net.URI
import java.net.URISyntaxException

/** The `@id` of a document: an absolute IRI, kept across every revision. */
@JvmInline
value class OpenVexDocumentId(
    val value: String,
) {
    init {
        require(isAbsoluteIri(value)) { "OpenVEX document id must be an absolute IRI: '$value'" }
    }

    companion object {
        /** The id [value] names, or null when it is no absolute IRI. For values read from a file. */
        fun parse(value: String): OpenVexDocumentId? = value.takeIf(::isAbsoluteIri)?.let(::OpenVexDocumentId)
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
