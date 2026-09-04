// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.fixtures

/**
 * Builds a Vulnlog YAML with one release that declares purls and one that does not, so an OpenVEX
 * document has both a product to anchor to and a release it must skip.
 */
fun openVexDocument(projectName: String = "Acme Web App"): String =
    """
    ---
    schemaVersion: "1"

    project:
      organization: Acme Corp
      name: $projectName
      author: Acme Corp Security Team
      contact: security@acme.example

    releases:
      - id: 1.0.0
        published_at: 2026-01-15
        purls:
          - purl: "pkg:maven/com.acme/acme-web-app@1.0.0"
      - id: 1.0.1

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
        resolution:
          in: 1.0.1
    """.trimIndent()

/**
 * Builds a Vulnlog YAML with four releases, tagged purls, and one entry per status, so a scoped OpenVEX document has
 * something to narrow.
 *
 * 0.9.0, 1.0.0 and 1.1.0 carry purls and 1.2.0 carries none. No entry references 0.9.0, so scoping to its `legacy`
 * tag leaves the document empty.
 */
fun openVexScopedDocument(): String =
    """
    ---
    schemaVersion: "1"

    project:
      organization: Acme Corp
      name: Acme Web App
      author: Acme Corp Security Team
      contact: security@acme.example

    tags:
      - id: container
        description: The container image artifact
      - id: library
        description: The published library artifact
      - id: legacy
        description: An artifact no current entry tracks

    releases:
      - id: 0.9.0
        published_at: 2025-11-01
        purls:
          - purl: "pkg:docker/acme/web-app@0.9.0"
            tags: [ legacy ]
      - id: 1.0.0
        published_at: 2026-01-15
        purls:
          - purl: "pkg:docker/acme/web-app@1.0.0"
            tags: [ container ]
          - purl: "pkg:maven/com.acme/acme-lib@1.0.0"
            tags: [ library ]
      - id: 1.1.0
        published_at: 2026-02-15
        purls:
          - purl: "pkg:docker/acme/web-app@1.1.0"
            tags: [ container ]
      - id: 1.2.0

    vulnerabilities:

      - id: CVE-2026-1111
        releases: [ 1.0.0 ]
        description: Remote code execution in example-lib
        packages: [ "pkg:npm/example-lib@2.3.0" ]
        reports:
          - reporter: trivy
        analysis: not reachable
        verdict: not affected
        justification: vulnerable code not in execute path

      - id: CVE-2026-2222
        releases: [ 1.0.0 ]
        description: Denial of service in example-parser
        packages: [ "pkg:npm/example-parser@1.0.0" ]
        reports:
          - reporter: trivy
        analysis: the parser is reachable
        verdict: affected
        severity: high
        disposition: will fix
        resolution:
          in: 1.1.0
          note: Bumped example-parser to 1.1.0.

      - id: CVE-2026-3333
        releases: [ 1.2.0 ]
        description: Path traversal in example-server
        packages: [ "pkg:npm/example-server@3.0.0" ]
        reports:
          - reporter: trivy
    """.trimIndent()
