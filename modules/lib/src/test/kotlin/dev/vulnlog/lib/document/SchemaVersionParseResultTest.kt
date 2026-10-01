// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.document

import dev.vulnlog.lib.model.SchemaVersion
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SchemaVersionParseResultTest :
    FunSpec({

        context("parseSchemaVersion") {
            test("recognizes the supported version with or without a zero minor") {
                val values = listOf("1", "1.0")

                val results = values.map(::parseSchemaVersion)

                results shouldBe List(2) { SchemaVersionParseResult.Recognized(SchemaVersion.V1) }
            }

            test("reports a well-formed version this build does not support as unsupported") {
                val values = listOf("99", "1.2")

                val results = values.map(::parseSchemaVersion)

                results shouldBe values.map(SchemaVersionParseResult::Unsupported)
            }

            test("reports a blank or non-numeric version as malformed") {
                val values = listOf("abc", "")

                val results = values.map(::parseSchemaVersion)

                results shouldBe List(2) { SchemaVersionParseResult.Malformed }
            }
        }
    })
