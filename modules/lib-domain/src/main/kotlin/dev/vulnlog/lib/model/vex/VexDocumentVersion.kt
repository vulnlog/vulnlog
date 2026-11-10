// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex

/** The revision count of a VEX document; every format starts at 1 and counts up by one. */
@JvmInline
value class VexDocumentVersion(
    val value: Int,
) {
    init {
        require(value >= 1) { "VEX document version must be at least 1: $value" }
    }

    fun next(): VexDocumentVersion = VexDocumentVersion(Math.addExact(value, 1))

    companion object {
        val FIRST: VexDocumentVersion = VexDocumentVersion(1)

        /** Rejects the last Int too: a baseline at that version could not be continued without overflow. */
        fun parse(value: Long): VexDocumentVersion? =
            value.takeIf { it in 1 until Int.MAX_VALUE }?.let { VexDocumentVersion(it.toInt()) }
    }
}
