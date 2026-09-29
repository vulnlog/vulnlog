// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.fixtures

/** One release with a purl and one without, so a document has a product and a release to skip. */
fun openVexDocument(projectName: String = "Acme Web App"): String =
    """
    ---
    schemaVersion: "1"

    project:
      organization: Acme Corp
      name: $projectName
      author: Acme Corp Security Team
      contact: security@acme.example

    tags:
      - id: app
        description: The web application artifact

    releases:
      - id: 1.0.0
        published_at: 2026-01-15
        purls:
          - purl: "pkg:maven/com.acme/acme-web-app@1.0.0"
            tags: [ app ]
      - id: 1.0.1

    vulnerabilities:

      - id: CVE-2026-1234
        releases: [ 1.0.0 ]
        description: Remote code execution in example-lib
        packages: [ "pkg:npm/example-lib@2.3.0" ]
        reports:
          - reporter: trivy
            at: 2026-01-20
        tags: [ app ]
        analysis: not reachable
        verdict: not affected
        justification: vulnerable code not in execute path
        resolution:
          in: 1.0.1
    """.trimIndent()

/**
 * 1.0.5 is reached only through the range of the entries reported for 1.0.0, 1.2.0 has no purls, and no entry reaches
 * 0.9.0, so its `legacy` tag leaves the document empty.
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
      - id: 1.0.5
        published_at: 2026-02-01
        purls:
          - purl: "pkg:docker/acme/web-app@1.0.5"
            tags: [ container ]
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
        tags: [ container, library ]
        reports:
          - reporter: trivy
            at: 2026-01-20
        analysis: not reachable
        verdict: not affected
        justification: vulnerable code not in execute path

      - id: CVE-2026-2222
        releases: [ 1.0.0 ]
        description: Denial of service in example-parser
        packages: [ "pkg:npm/example-parser@1.0.0" ]
        tags: [ container, library ]
        reports:
          - reporter: trivy
            at: 2026-01-25
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
        tags: [ container ]
        reports:
          - reporter: trivy
            at: 2026-02-20
    """.trimIndent()
