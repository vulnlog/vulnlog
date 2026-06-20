// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.shell

/** The requested format for the suppression file. */
sealed interface SuppressionFormatRequest {
    /** Native format where the reporter has one, generic JSON otherwise. */
    data object Auto : SuppressionFormatRequest

    /** Generic Vulnlog JSON for every reporter that supports suppression. */
    data object Generic : SuppressionFormatRequest

    companion object {
        val byToken: Map<String, SuppressionFormatRequest> = mapOf("auto" to Auto, "generic" to Generic)

        fun fromToken(token: String): SuppressionFormatRequest =
            byToken[token.lowercase()]
                ?: throw IllegalArgumentException(
                    "Unknown suppression format '$token'. Valid values: ${byToken.keys.joinToString(", ")}.",
                )
    }
}
