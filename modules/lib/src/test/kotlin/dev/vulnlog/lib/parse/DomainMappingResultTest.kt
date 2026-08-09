// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse

import dev.vulnlog.lib.fixtures.v1Dto
import dev.vulnlog.lib.fixtures.vulnerabilityDto
import dev.vulnlog.lib.model.SchemaVersion
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class DomainMappingResultTest :
    FunSpec({

        context("mapToDomain") {

            test("a valid document maps onto the domain model") {
                val dto = v1Dto()

                val result = mapToDomain(dto)

                val mapped = result.shouldBeInstanceOf<DomainMappingResult.Mapped>()
                mapped.vulnlogProjectFile.schemaVersion shouldBe SchemaVersion.V1
                mapped.vulnlogProjectFile.project.organization shouldBe "acme"
            }

            test("vulnerability entries keep their order") {
                val dto =
                    v1Dto(vulnerabilities = listOf(vulnerabilityDto("CVE-2021-1"), vulnerabilityDto("CVE-2021-2")))

                val result = mapToDomain(dto)

                val mapped = result.shouldBeInstanceOf<DomainMappingResult.Mapped>()
                mapped.vulnlogProjectFile.vulnerabilities.map { it.id.id } shouldBe
                    listOf("CVE-2021-1", "CVE-2021-2")
            }

            test("a value without a domain representation is reported with its path") {
                val dto = v1Dto(vulnerabilities = listOf(vulnerabilityDto("UNKNOWN-2021-0001")))

                val result = mapToDomain(dto)

                val rejected = result.shouldBeInstanceOf<DomainMappingResult.Rejected>()
                rejected.problems shouldHaveSize 1
                rejected.problems.single().path shouldBe "vulnerabilities[UNKNOWN-2021-0001].id"
            }
        }
    })
