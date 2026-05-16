// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VulnId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import java.time.LocalDate

class AddTest :
    FunSpec({

        test("createVulnerabilityEntry with only a vuln-id emits empty list fields and no verdict") {
            val yaml =
                createVulnerabilityEntry(
                    AddVulnerabilityOptions(
                        vulnId = VulnId.Cve("CVE-2026-1234"),
                        releases = emptySet(),
                        packages = emptySet(),
                        tags = emptySet(),
                        reporter = null,
                    ),
                )

            yaml shouldStartWith "  - id: \"CVE-2026-1234\""
            yaml shouldContain "releases: []"
            yaml shouldContain "packages: []"
            yaml shouldContain "reports: []"
            yaml shouldNotContain "verdict"
            yaml shouldNotContain "tags"
        }

        test("createVulnerabilityEntry serializes releases, packages, tags and reporter with today's date") {
            val yaml =
                createVulnerabilityEntry(
                    AddVulnerabilityOptions(
                        vulnId = VulnId.Cve("CVE-2026-5678"),
                        releases = setOf(Release("1.0.0")),
                        packages = setOf(Purl.Npm("pkg:npm/example-lib@2.3.0")),
                        tags = setOf(Tag("frontend")),
                        reporter = ReporterType.TRIVY,
                    ),
                )

            yaml shouldContain "CVE-2026-5678"
            yaml shouldContain "\"1.0.0\""
            yaml shouldContain "pkg:npm/example-lib@2.3.0"
            yaml shouldContain "frontend"
            yaml shouldContain "reporter: \"trivy\""
            yaml shouldContain "at: \"${LocalDate.now()}\""
        }
    })
