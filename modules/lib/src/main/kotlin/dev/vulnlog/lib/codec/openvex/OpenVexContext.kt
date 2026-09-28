// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.openvex

import dev.vulnlog.lib.model.vex.openvex.OpenVexFormatVersion

/** What every OpenVEX `@context` starts with, whatever version follows it. */
private const val CONTEXT_PREFIX = "https://openvex.dev/ns/v"

/** The `@context` without a version, which the specification reads as [UNVERSIONED_VERSION]. */
private const val UNVERSIONED_CONTEXT = "https://openvex.dev/ns"

private const val UNVERSIONED_VERSION = "0.0.1"

/** The `@context` a document of [formatVersion] carries. */
internal fun openVexContext(formatVersion: OpenVexFormatVersion): String = CONTEXT_PREFIX + formatVersion.version

/**
 * The version [context] declares, written as it stands, or null when [context] is no OpenVEX context.
 *
 * A version this build does not know still comes back, so a caller can tell a document of another version from a
 * document of another format entirely. A context without a version declares 0.0.1.
 */
internal fun declaredOpenVexVersion(context: String): String? =
    when {
        context == UNVERSIONED_CONTEXT -> UNVERSIONED_VERSION
        context.startsWith(CONTEXT_PREFIX) -> context.removePrefix(CONTEXT_PREFIX).takeIf(String::isNotBlank)
        else -> null
    }
