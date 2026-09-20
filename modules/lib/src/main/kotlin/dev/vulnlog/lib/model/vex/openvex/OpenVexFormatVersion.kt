// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

/** What every OpenVEX `@context` starts with, whatever version follows it. */
private const val CONTEXT_PREFIX = "https://openvex.dev/ns/v"

/**
 * A version of the OpenVEX specification this build knows, one entry per version, declared oldest first.
 *
 * A document is read and written in one version and never carried across two: the reader accepts the required
 * version only, and the writer emits the shape of the version the document carries. Every branch that shapes bytes
 * is an exhaustive `when` over these entries, so a new version does not compile until each of them handles it.
 */
enum class OpenVexFormatVersion(
    /** The version as the specification names it, without the `v` the `@context` prefixes it with. */
    val version: String,
) {
    VERSION_0_2_0("0.2.0"),
    ;

    /** The `@context` a document of this version carries. */
    val context: String get() = CONTEXT_PREFIX + version

    companion object {
        /** The newest version this build knows. What a run writes unless it is told otherwise. */
        val LATEST: OpenVexFormatVersion = entries.last()

        /**
         * The version [context] declares, written as it stands, or null when [context] is no OpenVEX context.
         *
         * A version this build does not know still comes back, so a caller can tell a document of another version
         * from a document of another format entirely.
         */
        fun declaredVersion(context: String): String? =
            context.takeIf { it.startsWith(CONTEXT_PREFIX) }?.removePrefix(CONTEXT_PREFIX)?.takeIf(String::isNotBlank)
    }
}
