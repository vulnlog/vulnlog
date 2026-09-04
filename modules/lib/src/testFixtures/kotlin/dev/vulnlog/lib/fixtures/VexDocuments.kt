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
