package dev.vulnlog.cli.parse

import dev.vulnlog.cli.model.SchemaVersion
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class VersionDetectorTest :
    FunSpec({

        context("parseSchemaVersion") {
            test("major-only string parses with minor defaulting to zero") {
                parseSchemaVersion("1") shouldBe SchemaVersion(1, 0)
            }

            test("major.minor string parses both parts") {
                parseSchemaVersion("1.2") shouldBe SchemaVersion(1, 2)
            }

            test("zero major without minor") {
                parseSchemaVersion("0") shouldBe SchemaVersion(0, 0)
            }

            test("explicit zero minor") {
                parseSchemaVersion("2.0") shouldBe SchemaVersion(2, 0)
            }

            test("large version numbers are parsed correctly") {
                parseSchemaVersion("10.20") shouldBe SchemaVersion(10, 20)
            }

            test("non-numeric major returns null") {
                parseSchemaVersion("abc") shouldBe null
            }

            test("empty string returns null") {
                parseSchemaVersion("") shouldBe null
            }

            test("non-numeric minor defaults to zero") {
                parseSchemaVersion("1.abc") shouldBe SchemaVersion(1, 0)
            }
        }
    })
