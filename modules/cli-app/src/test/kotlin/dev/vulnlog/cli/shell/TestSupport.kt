// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.shell

import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * Builds a syntactically valid Vulnlog YAML document. Override the parameters to vary project
 * metadata, releases, or vulnerability fields without restating the entire fixture.
 */
internal fun vulnlogYaml(
    projectName: String = "Acme Web App",
    organization: String = "Acme Corp",
    author: String = "Acme Corp Security Team",
    releaseId: String = "1.0.0",
    publishedAt: String = "2026-01-15",
    cveId: String = "CVE-2026-1234",
    reporter: String = "trivy",
): String =
    """
    # ${'$'}schema: https://vulnlog.dev/schema/vulnlog-v1.json
    ---
    schemaVersion: "1"

    project:
      organization: $organization
      name: $projectName
      author: $author

    releases:
      - id: $releaseId
        published_at: $publishedAt

    vulnerabilities:

      - id: $cveId
        releases: [ $releaseId ]
        description: Remote code execution in example-lib
        packages: [ "pkg:npm/example-lib@2.3.0" ]
        reports:
          - reporter: $reporter
        analysis: >
          The vulnerable code path is not reachable in our application
          because we only use the safe subset of the API.
        verdict: not affected
        justification: vulnerable code not in execute path
    """.trimIndent()

/**
 * A YAML document missing required fields — used to exercise parse-failure paths.
 */
internal val INVALID_VULNLOG_YAML: String =
    """
    ---
    project:
      organization: Acme Corp
      name: Acme Web App
      author: Acme Corp Security Team
    """.trimIndent()

/**
 * Creates a temporary file that is deleted after [block] returns. Pre-populates [content] when given.
 *
 * @param suffix Defaults to `.vl.yaml` so the file passes Vulnlog's name validation. Override
 *               (e.g. with `.txt`) to exercise the invalid-name path.
 */
internal inline fun <R> withTempFile(
    prefix: String = "vulnlog",
    suffix: String = ".vl.yaml",
    content: String? = null,
    block: (File) -> R,
): R {
    val file = Files.createTempFile(prefix, suffix).toFile()
    return try {
        if (content != null) file.writeText(content)
        block(file)
    } finally {
        file.delete()
    }
}

/**
 * Creates a temporary directory that is recursively deleted after [block] returns.
 */
internal inline fun <R> withTempDir(
    prefix: String = "vulnlog",
    block: (Path) -> R,
): R {
    val dir = Files.createTempDirectory(prefix)
    return try {
        block(dir)
    } finally {
        dir.toFile().deleteRecursively()
    }
}

/**
 * Replaces `System.in` with [content] for the duration of [block] and restores it afterwards.
 */
internal inline fun <R> withStdin(
    content: String,
    block: () -> R,
): R {
    val original = System.`in`
    return try {
        System.setIn(content.byteInputStream())
        block()
    } finally {
        System.setIn(original)
    }
}
