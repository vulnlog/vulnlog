// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.gradle

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.io.path.createTempDirectory

/**
 * Builds a syntactically valid Vulnlog YAML document. Override the parameters to vary project
 * metadata, releases, or reporters without restating the entire fixture.
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
        verdict: not affected
        justification: vulnerable code not in execute path
    """.trimIndent()

/**
 * A YAML document with a single vulnerability reported by both `trivy` and `grype`. Used to
 * exercise reporter-filter behaviour.
 */
internal val MULTI_REPORTER_VULNLOG_YAML: String =
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
          - reporter: grype
        analysis: vulnerable code not in execute path
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
 * A YAML document that parses cleanly but produces validation warnings (here: a report dated
 * after the analysis timestamp).
 */
internal val WARNING_VULNLOG_YAML: String =
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
        analyzed_at: 2025-01-01
        reports:
          - reporter: trivy
            at: 2026-06-01
        verdict: not affected
        justification: vulnerable code not in execute path
    """.trimIndent()

/**
 * Wraps build script content in the standard plugin block. [extra] is appended verbatim
 * after the plugin declaration.
 */
internal fun buildFile(extra: String = ""): String =
    """
    plugins {
        id("dev.vulnlog.plugin")
    }
    $extra
    """.trimIndent()

/**
 * Creates a temporary Gradle project directory containing a `build.gradle.kts`, an empty
 * `settings.gradle.kts`, and any [yamlFiles] given as `name to content` pairs.
 */
internal fun gradleProject(
    buildScript: String,
    vararg yamlFiles: Pair<String, String>,
): File {
    val dir = createTempDirectory("vulnlog-test").toFile()
    dir.resolve("build.gradle.kts").writeText(buildScript)
    dir.resolve("settings.gradle.kts").writeText("")
    for ((name, content) in yamlFiles) {
        dir.resolve(name).writeText(content)
    }
    return dir
}

/**
 * Configures a [GradleRunner] for [projectDir] with the plugin under test on the classpath.
 */
internal fun runner(
    projectDir: File,
    vararg args: String,
): GradleRunner =
    GradleRunner
        .create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(*args)
