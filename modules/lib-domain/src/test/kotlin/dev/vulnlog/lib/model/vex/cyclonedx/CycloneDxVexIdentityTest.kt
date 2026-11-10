// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.cyclonedx

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.lang.module.ModuleDescriptor

class CycloneDxVexIdentityTest :
    FunSpec({

        context("CycloneDxVexFormatVersion") {

            test("LATEST is the newest version, so a new entry must be declared last") {
                val newest = CycloneDxVexFormatVersion.entries.maxBy { ModuleDescriptor.Version.parse(it.version) }

                CycloneDxVexFormatVersion.LATEST shouldBe newest
            }
        }

        context("CycloneDxSerialNumber") {

            test("parses a lowercase urn:uuid") {
                val value = "urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79"

                val serialNumber = CycloneDxSerialNumber.parse(value)

                serialNumber shouldBe CycloneDxSerialNumber(value)
            }

            test("parses nothing that is no urn:uuid") {
                val values =
                    listOf(
                        "",
                        "3e671687-395b-41f5-a30f-a58921a69b79",
                        "urn:uuid:3E671687-395B-41F5-A30F-A58921A69B79",
                        "urn:uuid:3e671687-395b-41f5-a30f-a58921a69b7",
                        " urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79",
                    )

                val serialNumbers = values.map(CycloneDxSerialNumber::parse)

                serialNumbers.forEach { it.shouldBeNull() }
            }

            test("cannot be built from a value that is no urn:uuid") {
                shouldThrow<IllegalArgumentException> { CycloneDxSerialNumber("vex-1") }
            }
        }
    })
