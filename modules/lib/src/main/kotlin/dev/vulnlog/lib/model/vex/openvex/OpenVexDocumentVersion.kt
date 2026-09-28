// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

/** The `version` of a document: 1 for the first revision, one more for every revision after it. */
@JvmInline
value class OpenVexDocumentVersion(
    val value: Int,
) {
    init {
        require(value >= 1) { "OpenVEX document version must be at least 1: $value" }
    }

    /** The version of the next revision. [parse] never admits a version without a successor. */
    fun next(): OpenVexDocumentVersion = OpenVexDocumentVersion(Math.addExact(value, 1))

    companion object {
        val FIRST: OpenVexDocumentVersion = OpenVexDocumentVersion(1)

        /** The version [value] names when a revision can follow it, or null. For values read from a file. */
        fun parse(value: Long): OpenVexDocumentVersion? =
            value.takeIf { it in 1 until Int.MAX_VALUE }?.let { OpenVexDocumentVersion(it.toInt()) }
    }
}
