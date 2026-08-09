// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.fixtures

/**
 * Builds a Vulnlog document. Every parameter defaults to a value that passes validation, so a test changes only what it is about.
 *
 * @param referencedRelease the release the vulnerability points at; defaults to the one defined.
 * @param extraTopLevel appended after the project block, for keys the schema does not define.
 * @param extraRelease appended to the release list, for a release nothing references.
 * @param reportsBlock the entry lines between `packages` and `verdict`.
 * @param verdictBlock the entry's closing lines, from `verdict` on.
 */
fun vulnlogDocument(
    schemaVersion: String = "1",
    organization: String = "Acme Corp",
    projectName: String = "Acme Web App",
    releaseId: String = "1.0.0",
    vulnId: String = "CVE-2026-1234",
    referencedRelease: String = releaseId,
    reporter: String = "trivy",
    extraTopLevel: String = "",
    extraRelease: String = "",
    reportsBlock: String =
        """
        |    reports:
        |      - reporter: $reporter
        """.trimMargin(),
    verdictBlock: String =
        """
        |    verdict: not affected
        |    justification: vulnerable code not in execute path
        """.trimMargin(),
): String =
    """
    |---
    |schemaVersion: "$schemaVersion"
    |
    |project:
    |  organization: $organization
    |  name: $projectName
    |  author: Acme Corp Security Team
    |$extraTopLevel
    |releases:
    |  - id: $releaseId
    |    published_at: 2026-01-15
    |$extraRelease
    |vulnerabilities:
    |  - id: $vulnId
    |    releases: [ $referencedRelease ]
    |    description: Remote code execution in example-lib
    |    packages: [ "pkg:npm/example-lib@2.3.0" ]
    |$reportsBlock
    |$verdictBlock
    """.trimMargin() + "\n"

/**
 * One Vulnlog document per validation stage, shared by the CLI and the Gradle plugin so both surfaces are driven into the same states. Each document reaches its stage and stops there.
 */
object ValidationDocuments {
    /** Passes every stage without a finding. */
    val CLEAN: String = vulnlogDocument()

    /** Not valid YAML: fails while building the node tree. */
    val MALFORMED_YAML: String = "schemaVersion: [unclosed"

    /** Declares a schema version this build does not support: fails while building the DTO. */
    val UNSUPPORTED_SCHEMA_VERSION: String = vulnlogDocument(schemaVersion = "99")

    /** Carries a key the schema does not define: fails while building the DTO. */
    val UNKNOWN_PROPERTY: String = vulnlogDocument(extraTopLevel = "bogus: true\n")

    /** Names a vulnerability ID with no domain representation: fails while mapping to the domain. */
    val UNMAPPABLE_VULN_ID: String = vulnlogDocument(vulnId = "UNKNOWN-2026-1234")

    /** References a release that is not defined: reaches the domain rules and errors. */
    val DANGLING_RELEASE: String = vulnlogDocument(referencedRelease = "9.9.9")

    /** Analyses a vulnerability before it was reported: reaches the domain rules and warns. */
    val ANALYZED_BEFORE_REPORTED: String =
        vulnlogDocument(
            reportsBlock =
                """
                |    analyzed_at: 2025-01-01
                |    reports:
                |      - reporter: trivy
                |        at: 2026-06-01
                """.trimMargin(),
        )

    /** Defines a release nothing points at: reaches the domain rules and informs. */
    val UNREFERENCED_RELEASE: String =
        vulnlogDocument(
            extraRelease =
                """
                |  - id: 2.0.0
                |    published_at: 2026-06-01
                """.trimMargin(),
        )
}
