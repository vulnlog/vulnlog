// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

/**
 * What a run tells the person besides why it failed; the level decides when a driver shows it. Failures are
 * [Failure], because Gradle reports them in the exception rather than in the log.
 *
 * The first matching level wins. Nothing is logged that the user can infer from the output or an existing message,
 * or that matches no level.
 *
 * Diagnostics are lowercase, verb first, past tense, one line per event, without a trailing full stop and ASCII only.
 * They name what the user would search for: the file, the vulnerability id or the reporter, for example
 * `skipped CVE-2026-1234 for .snyk: the snyk format requires SNYK ids`. Status lines use [StatusVerb].
 */
sealed interface Message {
    val text: String

    /** The command created or changed something the user asked for. Unless quiet. */
    data class Status(
        override val text: String,
    ) : Message

    /** Surprising but not fatal, such as data loss on rewrite or deprecated usage. Always. */
    data class Warning(
        override val text: String,
    ) : Message

    /**
     * What happened to the user's data: a file parsed, a filter resolved, a file written, an entry skipped and why.
     * With `-v`, or `--info` in Gradle.
     */
    data class Verbose(
        override val text: String,
    ) : Message

    /**
     * What only helps a maintainer diagnose, such as per-entry decisions or intermediate counts, for example
     * `included CVE-2026-1234 for reporter trivy (expires 2026-09-01)`. With `-vv`, or `--debug` in Gradle.
     */
    data class Debug(
        override val text: String,
    ) : Message
}
