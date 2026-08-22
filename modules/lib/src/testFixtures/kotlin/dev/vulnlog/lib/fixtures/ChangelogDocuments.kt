// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.fixtures

/**
 * Builds a Vulnlog YAML with two shipped fixes and one accepted entry, so the changelog report has
 * something to group, order, and leave out.
 */
fun changelogDocument(projectName: String = "Acme Web App"): String =
    """
    ---
    schemaVersion: "1"

    project:
      organization: Acme Corp
      name: $projectName
      author: Acme Corp Security Team

    releases:
      - id: 1.0.0
        published_at: 2026-01-15
      - id: 1.1.0
        published_at: 2026-03-20
      - id: 1.2.0

    vulnerabilities:

      - id: CVE-2026-1111
        releases: [ 1.0.0 ]
        description: Authentication bypass in auth-middleware
        packages: [ "pkg:npm/auth-middleware@3.0.2" ]
        reports:
          - reporter: trivy
        analysis: Confirmed on every session-authenticated route.
        verdict: affected
        severity: critical
        resolution:
          in: 1.1.0
          ref: "https://issues.example.com/SEC-1"
          note: Updated auth-middleware from 3.0.2 to 3.1.0

      - id: CVE-2026-2222
        aliases: [ GHSA-aaaa-bbbb-cccc ]
        releases: [ 1.0.0 ]
        description: SQL injection in query-parser
        packages: [ "pkg:npm/query-parser@5.2.1" ]
        reports:
          - reporter: trivy
        analysis: Reachable through the search endpoint.
        verdict: affected
        severity: high
        resolution:
          in: 1.2.0
          note: Updated query-parser from 5.2.1 to 5.3.0

      - id: CVE-2026-3333
        releases: [ 1.0.0 ]
        description: Denial of service in thumbnail-gen
        packages: [ "pkg:npm/thumbnail-gen@2.0.0" ]
        reports:
          - reporter: trivy
        analysis: Only exploitable with dimensions the upload layer rejects.
        verdict: affected
        severity: low
        disposition: wont fix
    """.trimIndent()
