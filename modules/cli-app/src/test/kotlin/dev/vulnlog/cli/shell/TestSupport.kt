// Copyright the Vulnlog contributors
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
 * Builds a Vulnlog YAML with reports from multiple reporters, so that suppress would emit more
 * than one file unless filtered.
 */
internal fun vulnlogYamlMultiReporter(): String =
    """
    ---
    schemaVersion: "1"

    project:
      organization: Acme Corp
      name: Acme Web App
      author: Acme Corp Security Team

    releases:
      - id: 1.0.0
        published_at: 2026-01-15

    vulnerabilities:

      - id: CVE-2026-1234
        releases: [ 1.0.0 ]
        description: Remote code execution in example-lib
        packages: [ "pkg:npm/example-lib@2.3.0" ]
        reports:
          - reporter: trivy
          - reporter: snyk
        analysis: not reachable
        verdict: not affected
        justification: vulnerable code not in execute path
    """.trimIndent()

/**
 * Builds a Vulnlog YAML document for the "pending fix" scenario: an `Affected` CVE present in a
 * published release whose resolution targets a later release that has not been published yet
 * (no `published_at`).
 */
internal fun vulnlogYamlWithPendingFix(
    publishedRelease: String = "1.0.0",
    publishedAt: String = "2026-01-15",
    pendingRelease: String = "1.0.1",
    cveId: String = "CVE-2026-9999",
    reporter: String = "trivy",
): String =
    """
    ---
    schemaVersion: "1"

    project:
      organization: Acme Corp
      name: Acme Web App
      author: Acme Corp Security Team

    releases:
      - id: $publishedRelease
        published_at: $publishedAt
      - id: $pendingRelease

    vulnerabilities:

      - id: $cveId
        releases: [ $publishedRelease ]
        description: Remote code execution in tomcat
        packages: [ "pkg:maven/org.apache.tomcat/tomcat-core@10.1.0" ]
        reports:
          - reporter: $reporter
            suppress: { }
        analysis: Fix landed on dev but no release has been cut yet.
        analyzed_at: $publishedAt
        severity: high
        verdict: affected
        resolution:
          in: $pendingRelease
    """.trimIndent()

/**
 * Builds a Vulnlog YAML whose only report uses the `other` reporter, which is not suppressible
 * — used to exercise the empty-output path of suppress.
 */
internal fun vulnlogYamlOtherReporterOnly(): String =
    """
    ---
    schemaVersion: "1"

    project:
      organization: Acme Corp
      name: Acme Web App
      author: Acme Corp Security Team

    releases:
      - id: 1.0.0
        published_at: 2026-01-15

    vulnerabilities:

      - id: CVE-2026-1234
        releases: [ 1.0.0 ]
        description: Remote code execution in example-lib
        packages: [ "pkg:npm/example-lib@2.3.0" ]
        reports:
          - reporter: other
            source: in-house-scanner
        analysis: not reachable
        verdict: not affected
        justification: vulnerable code not in execute path
    """.trimIndent()

/**
 * A Vulnlog YAML with a release that is not referenced by any vulnerability. Triggers a single
 * INFO-severity validation finding (UNREFERENCED_RELEASE_ID) and no warnings or errors.
 */
internal fun vulnlogYamlWithInfoFinding(): String =
    """
    ---
    schemaVersion: "1"

    project:
      organization: Acme Corp
      name: Acme Web App
      author: Acme Corp Security Team

    releases:
      - id: 1.0.0
        published_at: 2026-01-15
      - id: 2.0.0
        published_at: 2026-06-01

    vulnerabilities:

      - id: CVE-2026-1234
        releases: [ 1.0.0 ]
        description: Remote code execution in example-lib
        packages: [ "pkg:npm/example-lib@2.3.0" ]
        reports:
          - reporter: trivy
        analysis: not reachable
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
 * Overrides the `user.dir` system property for the duration of [block]. The CLI commands read this
 * to resolve their default output directory, so tests that exercise the default destination must
 * set it to a temp path rather than polluting the real cwd.
 */
internal inline fun <R> withCwd(
    cwd: Path,
    block: () -> R,
): R {
    val original = System.getProperty("user.dir")
    return try {
        System.setProperty("user.dir", cwd.toAbsolutePath().toString())
        block()
    } finally {
        System.setProperty("user.dir", original)
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
