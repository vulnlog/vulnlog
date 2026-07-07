// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.testing.test
import dev.vulnlog.lib.shell.DiagnosticLevel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private class ProbeCommand : CliktCommand(name = "probe") {
    var seen: Verbosity? = null

    override fun run() {
        seen = diagnostics().verbosity
    }
}

class VulnlogCliTest :
    FunSpec({

        fun cliWithProbe(): Pair<VulnlogCli, ProbeCommand> {
            val probe = ProbeCommand()
            return VulnlogCli().subcommands(probe) to probe
        }

        context("verbosity flags") {

            test("defaults to no diagnostics and status lines on") {
                val (cli, probe) = cliWithProbe()

                cli.test("probe").statusCode shouldBe 0

                probe.seen shouldBe Verbosity(level = 0, quiet = false)
            }

            test("-v enables verbose") {
                val (cli, probe) = cliWithProbe()

                cli.test("-v probe").statusCode shouldBe 0

                probe.seen shouldBe Verbosity(level = 1, quiet = false)
            }

            test("-vv enables debug") {
                val (cli, probe) = cliWithProbe()

                cli.test("-vv probe").statusCode shouldBe 0

                probe.seen shouldBe Verbosity(level = 2, quiet = false)
            }

            test("--verbose is the long form of -v") {
                val (cli, probe) = cliWithProbe()

                cli.test("--verbose probe").statusCode shouldBe 0

                probe.seen shouldBe Verbosity(level = 1, quiet = false)
            }

            test("-q sets quiet") {
                val (cli, probe) = cliWithProbe()

                cli.test("-q probe").statusCode shouldBe 0

                probe.seen shouldBe Verbosity(level = 0, quiet = true)
            }

            test("-q with -v is a usage error") {
                val (cli, _) = cliWithProbe()

                val result = cli.test("-q -v probe")

                result.statusCode shouldBe 1
                result.stderr shouldContain "Option --quiet cannot be combined with --verbose."
            }

            test("-q with -vv is a usage error") {
                val (cli, _) = cliWithProbe()

                val result = cli.test("-q -vv probe")

                result.statusCode shouldBe 1
                result.stderr shouldContain "Option --quiet cannot be combined with --verbose."
            }
        }

        context("Verbosity") {

            test("level 0 enables nothing") {
                Verbosity(level = 0).enables(DiagnosticLevel.VERBOSE).shouldBeFalse()
                Verbosity(level = 0).enables(DiagnosticLevel.DEBUG).shouldBeFalse()
            }

            test("level 1 enables verbose only") {
                Verbosity(level = 1).enables(DiagnosticLevel.VERBOSE).shouldBeTrue()
                Verbosity(level = 1).enables(DiagnosticLevel.DEBUG).shouldBeFalse()
            }

            test("level 2 enables verbose and debug") {
                Verbosity(level = 2).enables(DiagnosticLevel.VERBOSE).shouldBeTrue()
                Verbosity(level = 2).enables(DiagnosticLevel.DEBUG).shouldBeTrue()
            }

            test("stack traces require level 2") {
                Verbosity(level = 1).stackTraces.shouldBeFalse()
                Verbosity(level = 2).stackTraces.shouldBeTrue()
            }

            test("quiet disables status lines") {
                Verbosity(quiet = false).statusEnabled.shouldBeTrue()
                Verbosity(quiet = true).statusEnabled.shouldBeFalse()
            }
        }

        context("CliDiagnostics sink") {

            test("level 0 emits nothing") {
                val lines = mutableListOf<String>()

                val diagnostics = CliDiagnostics(Verbosity(level = 0), lines::add)
                diagnostics.sink.verbose("parsed")
                diagnostics.sink.debug("timing")

                lines shouldBe emptyList()
            }

            test("level 1 emits verbose only") {
                val lines = mutableListOf<String>()

                val diagnostics = CliDiagnostics(Verbosity(level = 1), lines::add)
                diagnostics.sink.verbose("parsed")
                diagnostics.sink.debug("timing")

                lines shouldBe listOf("verbose: parsed")
            }

            test("level 2 emits verbose and debug") {
                val lines = mutableListOf<String>()

                val diagnostics = CliDiagnostics(Verbosity(level = 2), lines::add)
                diagnostics.sink.verbose("parsed")
                diagnostics.sink.debug("timing")

                lines shouldBe listOf("verbose: parsed", "debug: timing")
            }
        }

        test("subcommand without a parent context falls back to defaults") {
            val probe = ProbeCommand()

            probe.test("").statusCode shouldBe 0

            probe.seen shouldBe Verbosity(level = 0, quiet = false)
        }

        context("quiet mode") {

            test("validate stays silent on success") {
                withTempFile(content = vulnlogYaml()) { file ->
                    val result = vulnlogCommand().test("-q validate ${file.absolutePath}")

                    result.statusCode shouldBe 0
                    result.stdout shouldBe ""
                    result.stderr shouldBe ""
                }
            }

            test("validate without quiet prints the status line") {
                withTempFile(content = vulnlogYaml()) { file ->
                    val result = vulnlogCommand().test("validate ${file.absolutePath}")

                    result.statusCode shouldBe 0
                    result.stderr shouldContain "Validation OK"
                }
            }

            test("suppress writes the file but no status line") {
                withTempFile(content = vulnlogYaml()) { file ->
                    withTempDir { dir ->
                        val result =
                            vulnlogCommand().test(
                                "-q suppress ${file.absolutePath} --output-dir ${dir.toAbsolutePath()}",
                            )

                        result.statusCode shouldBe 0
                        result.stdout shouldBe ""
                        result.stderr shouldBe ""
                        dir.resolve(".trivyignore.yaml").shouldExist()
                    }
                }
            }

            test("errors still print") {
                withTempFile(content = INVALID_VULNLOG_YAML) { file ->
                    val result = vulnlogCommand().test("-q validate ${file.absolutePath}")

                    result.statusCode shouldBe ExitCode.VALIDATION_ERROR.code
                    result.stderr shouldContain file.name
                }
            }
        }

        context("verbose mode") {

            test("suppress reports parsed inputs and written outputs on stderr") {
                withTempFile(content = vulnlogYaml()) { file ->
                    withTempDir { dir ->
                        val result =
                            vulnlogCommand().test(
                                "-v suppress ${file.absolutePath} --output-dir ${dir.toAbsolutePath()}",
                            )

                        result.statusCode shouldBe 0
                        result.stderr shouldContain
                            "verbose: parsed ${file.name}: schema version 1, releases: 1, tags: 0, vulnerabilities: 1"
                        result.stderr shouldContain
                            "verbose: wrote ${dir.resolve(".trivyignore.yaml")}: trivy format, 1 entry"
                    }
                }
            }

            test("without -v no verbose lines appear") {
                withTempFile(content = vulnlogYaml()) { file ->
                    withTempDir { dir ->
                        val result =
                            vulnlogCommand().test(
                                "suppress ${file.absolutePath} --output-dir ${dir.toAbsolutePath()}",
                            )

                        result.statusCode shouldBe 0
                        result.stderr shouldNotContain "verbose:"
                    }
                }
            }

            test("report shows the filter expansion") {
                withTempFile(content = vulnlogYaml()) { file ->
                    withTempDir { dir ->
                        val output = dir.resolve("report.html")
                        val result =
                            vulnlogCommand().test(
                                "-v report ${file.absolutePath} --release 1.0.0 -o ${output.toAbsolutePath()}",
                            )

                        result.statusCode shouldBe 0
                        result.stderr shouldContain "verbose: release filter expanded to releases: 1.0.0"
                        result.stderr shouldContain "verbose: wrote ${output.toAbsolutePath()}"
                    }
                }
            }

            test("suppress reports entries skipped for the output format") {
                withTempFile(content = vulnlogYaml(reporter = "snyk")) { file ->
                    withTempDir { dir ->
                        val result =
                            vulnlogCommand().test(
                                "-v suppress ${file.absolutePath} --output-dir ${dir.toAbsolutePath()}",
                            )

                        result.statusCode shouldBe 0
                        result.stderr shouldContain
                            "verbose: skipped CVE-2026-1234 for .snyk: the snyk format requires SNYK ids"
                    }
                }
            }

            test("skipped entries stay silent without -v") {
                withTempFile(content = vulnlogYaml(reporter = "snyk")) { file ->
                    withTempDir { dir ->
                        val result =
                            vulnlogCommand().test(
                                "suppress ${file.absolutePath} --output-dir ${dir.toAbsolutePath()}",
                            )

                        result.statusCode shouldBe 0
                        result.stderr shouldNotContain "skipped"
                    }
                }
            }

            test("suppress reports expired suppressions") {
                val expiredYaml =
                    """
                    ---
                    schemaVersion: "1"

                    project:
                      organization: Acme Corp
                      name: Acme Web App
                      author: Acme Corp Security Team

                    releases:
                      - id: 1.0.0
                        published_at: 2026-01-15

                    vulnerabilities:

                      - id: CVE-2026-1234
                        releases: [ 1.0.0 ]
                        description: Remote code execution in example-lib
                        packages: [ "pkg:npm/example-lib@2.3.0" ]
                        reports:
                          - reporter: trivy
                            suppress: { expires_at: 2026-02-01 }
                        analysis: not reachable
                        verdict: under_investigation
                    """.trimIndent()
                withTempFile(content = expiredYaml) { file ->
                    withTempDir { dir ->
                        val result =
                            vulnlogCommand().test(
                                "-v suppress ${file.absolutePath} --output-dir ${dir.toAbsolutePath()}",
                            )

                        result.statusCode shouldBe 0
                        result.stderr shouldContain
                            "verbose: skipped CVE-2026-1234 for reporter trivy: suppression expired on 2026-02-01"
                    }
                }
            }

            test("validate shows a per-file validation summary") {
                withTempFile(content = vulnlogYaml()) { file ->
                    val result = vulnlogCommand().test("-v validate ${file.absolutePath}")

                    result.statusCode shouldBe 0
                    result.stderr shouldContain "verbose: validated ${file.name}: no findings"
                }
            }

            test("verbosity never touches stdout") {
                withTempFile(content = vulnlogYaml()) { file ->
                    val plain = vulnlogCommand().test("suppress ${file.absolutePath} -o -")
                    val verbose = vulnlogCommand().test("-vv suppress ${file.absolutePath} -o -")

                    plain.statusCode shouldBe 0
                    verbose.statusCode shouldBe 0
                    verbose.stdout shouldBe plain.stdout
                }
            }
        }

        context("debug mode") {

            test("suppress lists included entries only at -vv") {
                withTempFile(content = vulnlogYaml()) { file ->
                    withTempDir { dir ->
                        val args = "suppress ${file.absolutePath} --output-dir ${dir.toAbsolutePath()}"
                        val verbose = vulnlogCommand().test("-v $args")
                        val debug = vulnlogCommand().test("-vv $args")

                        verbose.stderr shouldNotContain "included CVE-2026-1234"
                        debug.stderr shouldContain "debug: included CVE-2026-1234 for reporter trivy"
                    }
                }
            }

            test("report counts the collected and merged entries at -vv") {
                withTempFile(content = vulnlogYaml()) { file ->
                    withTempDir { dir ->
                        val output = dir.resolve("report.html")
                        val result =
                            vulnlogCommand().test(
                                "-vv report ${file.absolutePath} -o ${output.toAbsolutePath()}",
                            )

                        result.statusCode shouldBe 0
                        result.stderr shouldContain "debug: collected 1 report entries, merged to 1"
                    }
                }
            }
        }
    })
