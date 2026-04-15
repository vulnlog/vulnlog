// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.core

import dev.vulnlog.cli.model.SchemaVersion
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SchemaVersionTest :
    FunSpec({

        test("minor zero returns only major") {
            shortenSchemaVersion(SchemaVersion(1, 0)) shouldBe "1"
        }

        test("non-zero minor returns major.minor") {
            shortenSchemaVersion(SchemaVersion(1, 2)) shouldBe "1.2"
        }

        test("major zero with minor zero returns 0") {
            shortenSchemaVersion(SchemaVersion(0, 0)) shouldBe "0"
        }

        test("major zero with non-zero minor returns 0.minor") {
            shortenSchemaVersion(SchemaVersion(0, 1)) shouldBe "0.1"
        }

        test("large version numbers are formatted correctly") {
            shortenSchemaVersion(SchemaVersion(10, 20)) shouldBe "10.20"
        }
    })
