// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.shell

import dev.vulnlog.lib.core.VulnlogFilter
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Tag
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class FilterValidationTest :
    FunSpec({

        context("renderFilterResolution") {

            test("renders nothing for an empty filter") {
                val filter = VulnlogFilter(emptySet(), emptySet(), null)

                renderFilterResolution(filter) shouldBe emptyList()
            }

            test("lists the expanded releases") {
                val filter = VulnlogFilter(setOf(Release("1.0.0"), Release("1.1.0")), emptySet(), null)

                renderFilterResolution(filter) shouldBe
                    listOf("release filter expanded to releases: 1.0.0, 1.1.0")
            }

            test("lists the matched tags") {
                val filter = VulnlogFilter(emptySet(), setOf(Tag("internal")), null)

                renderFilterResolution(filter) shouldBe listOf("tag filter matched tags: internal")
            }

            test("renders the canonical reporter name") {
                val filter = VulnlogFilter(emptySet(), emptySet(), ReporterType.CARGO_AUDIT)

                renderFilterResolution(filter) shouldBe listOf("reporter filter: cargo-audit")
            }

            test("renders all active dimensions in order") {
                val filter = VulnlogFilter(setOf(Release("1.0.0")), setOf(Tag("internal")), ReporterType.TRIVY)

                renderFilterResolution(filter) shouldBe
                    listOf(
                        "release filter expanded to releases: 1.0.0",
                        "tag filter matched tags: internal",
                        "reporter filter: trivy",
                    )
            }
        }
    })
