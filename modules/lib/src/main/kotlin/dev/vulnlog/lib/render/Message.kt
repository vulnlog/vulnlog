// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

/**
 * What a run tells the person besides why it failed; the level decides when a driver shows it. Failures are
 * [Failure], because Gradle reports them in the exception rather than in the log.
 */
sealed interface Message {
    val text: String

    /** Unless quiet. */
    data class Status(
        override val text: String,
    ) : Message

    /** Always. */
    data class Warning(
        override val text: String,
    ) : Message

    /** With `-v`, or `--info` in Gradle. */
    data class Verbose(
        override val text: String,
    ) : Message

    /** With `-vv`, or `--debug` in Gradle. */
    data class Debug(
        override val text: String,
    ) : Message
}
