// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse

import dev.vulnlog.lib.model.SchemaVersion
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class VersionDetectorTest :
    FunSpec({

        context("parseSchemaVersion") {
            test("supported version is recognized") {
                parseSchemaVersion("1") shouldBe SchemaVersionParseResult.Recognized(SchemaVersion.V1)
            }

            test("explicit zero minor is recognized") {
                parseSchemaVersion("1.0") shouldBe SchemaVersionParseResult.Recognized(SchemaVersion.V1)
            }

            test("well-formed but unknown version is unsupported") {
                parseSchemaVersion("99") shouldBe SchemaVersionParseResult.Unsupported("99")
            }

            test("known major with non-zero minor is unsupported") {
                parseSchemaVersion("1.2") shouldBe SchemaVersionParseResult.Unsupported("1.2")
            }

            test("non-numeric version is malformed") {
                parseSchemaVersion("abc") shouldBe SchemaVersionParseResult.Malformed
            }

            test("blank version is malformed") {
                parseSchemaVersion("") shouldBe SchemaVersionParseResult.Malformed
            }
        }
    })
