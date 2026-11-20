// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.suppression.dependencycheck

import dev.vulnlog.lib.codec.suppression.SuppressionEncoder
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.suppression.SuppressionEntry
import dev.vulnlog.lib.model.suppression.SuppressionFormat
import dev.vulnlog.lib.model.suppression.SuppressionList
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DependencyCheckUnsupportedIdTest :
    FunSpec({
        listOf(VulnId.RustSec("RUSTSEC-2026-0001"), VulnId.Snyk("SNYK-JAVA-001")).forEach { id ->
            test("validation reports unsupported ${id::class.simpleName} before encoding") {
                val list = SuppressionList(SuppressionFormat.DependencyCheck, setOf(SuppressionEntry(id)))

                SuppressionEncoder.validate(list).map { it.id } shouldBe listOf(id)
            }

            test("direct encoding rejects unsupported ${id::class.simpleName} at validation") {
                val list = SuppressionList(SuppressionFormat.DependencyCheck, setOf(SuppressionEntry(id)))

                shouldThrow<IllegalArgumentException> { SuppressionEncoder.encode(list) }
            }
        }
    })
