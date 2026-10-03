// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import dev.vulnlog.lib.app.ChangelogOutcome
import dev.vulnlog.lib.app.FilterRejected
import dev.vulnlog.lib.app.ImpactReportOutcome
import dev.vulnlog.lib.app.InitOutcome
import dev.vulnlog.lib.app.ProjectsDiffer
import dev.vulnlog.lib.app.SuppressionOutcome
import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.document.InputRead
import dev.vulnlog.lib.model.OutputWrite
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.suppression.SuppressionCollection
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaselineRead
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class ExitsTest :
    FunSpec({

        test("a rejected suppression filter is a bad flag value, several reporters for one file an error") {
            val collection = SuppressionCollection(emptyMap(), emptyList())
            val failed: List<SuppressionOutcome.Failed> =
                listOf(
                    FilterRejected(emptyList()),
                    SuppressionOutcome.SeveralReporters(ResolvedFilter(), collection, listOf(ReporterType.TRIVY)),
                )

            val codes = failed.map { exitCode(it) }

            codes shouldContainExactly listOf(ExitCode.INVALID_FLAG_VALUE, ExitCode.GENERAL_ERROR)
        }

        test("in both reports, differing projects are a validation error and a rejected filter a bad flag value") {
            val failed = listOf(ProjectsDiffer(emptyList()), FilterRejected(emptyList()))
            val impact: List<ImpactReportOutcome.Failed> = failed
            val changelog: List<ChangelogOutcome.Failed> = failed

            val codes = impact.map { exitCode(it) } + changelog.map { exitCode(it) }

            codes shouldContainExactly
                listOf(
                    ExitCode.VALIDATION_ERROR,
                    ExitCode.INVALID_FLAG_VALUE,
                    ExitCode.VALIDATION_ERROR,
                    ExitCode.INVALID_FLAG_VALUE,
                )
        }

        test("an init target that exists is a general error") {
            val failed: InitOutcome.Failed = InitOutcome.AlreadyExists

            val code = exitCode(failed)

            code shouldBe ExitCode.GENERAL_ERROR
        }

        test("an absent baseline is a bad flag value, an unreadable one an I/O error") {
            val unavailable = listOf(OpenVexBaselineRead.Absent, OpenVexBaselineRead.Unreadable("Permission denied"))

            val codes = unavailable.map { exitCode(it) }

            codes shouldContainExactly listOf(ExitCode.INVALID_FLAG_VALUE, ExitCode.GENERAL_ERROR)
        }

        test("an input that cannot be read is an I/O error, whatever the reason") {
            val failed =
                listOf(
                    InputRead.Missing("a.vl.yaml"),
                    InputRead.Denied("a.vl.yaml"),
                    InputRead.Unreadable("a.vl.yaml", "Is a directory"),
                )

            val codes = failed.map { exitCode(it) }

            codes shouldContainExactly List(3) { ExitCode.GENERAL_ERROR }
        }

        test("an output that cannot be written is an I/O error, whatever the reason") {
            val failed =
                listOf(
                    OutputWrite.MissingDirectory("out/report.html"),
                    OutputWrite.Denied("out/report.html"),
                    OutputWrite.Unwritable("out/report.html", "Is a directory"),
                )

            val codes = failed.map { exitCode(it) }

            codes shouldContainExactly List(3) { ExitCode.GENERAL_ERROR }
        }
    })
