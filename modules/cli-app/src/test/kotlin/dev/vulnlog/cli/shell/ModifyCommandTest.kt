// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.testing.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private val SOURCE_YAML = vulnlogYaml(releaseId = "2.0.0", cveId = "CVE-2026-1234")
private val TARGET_YAML = vulnlogYaml(releaseId = "1.0.0", cveId = "CVE-2026-0001")

class ModifyCommandTest :
    FunSpec({

        test("'modify copy' invokes the copy subcommand") {
            withTempFile(prefix = "source", content = SOURCE_YAML) { source ->
                withTempFile(prefix = "target", content = TARGET_YAML) { target ->
                    val result =
                        ModifyCommand().test(
                            "copy ${source.absolutePath} ${target.absolutePath} --vuln-id CVE-2026-1234",
                        )

                    result.statusCode shouldBe 0
                    result.stdout shouldContain "Copied to ${target.path}: CVE-2026-1234"
                    target.readText() shouldContain "CVE-2026-1234"
                }
            }
        }
    })
