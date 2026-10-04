// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.resolution
import dev.vulnlog.lib.fixtures.vulnerability
import dev.vulnlog.lib.model.Disposition
import dev.vulnlog.lib.model.Resolution
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.reporting.WorkState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private val NOT_AFFECTED = Verdict.NotAffected(VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH)

private fun stateOf(
    verdict: Verdict,
    resolution: Resolution? = null,
): WorkState = findWorkState(vulnerability(id = cve("CVE-2026-0001"), verdict = verdict, resolution = resolution))

class WorkStatesTest :
    FunSpec({

        test("without a resolution, the verdict and the stated intent decide the state") {
            val verdicts =
                listOf(
                    Verdict.UnderInvestigation,
                    Verdict.Affected(Severity.HIGH),
                    Verdict.Affected(Severity.HIGH, Disposition.WILL_FIX),
                    Verdict.Affected(Severity.HIGH, Disposition.WONT_FIX),
                    NOT_AFFECTED,
                )

            val states = verdicts.map { stateOf(it) }

            states shouldContainExactly
                listOf(
                    WorkState.UNDER_INVESTIGATION,
                    WorkState.OPEN,
                    WorkState.OPEN,
                    WorkState.ACCEPTED,
                    WorkState.NOT_APPLICABLE,
                )
        }

        test("a resolution resolves every triaged entry, a wont fix one too") {
            val wontFix = Verdict.Affected(Severity.LOW, Disposition.WONT_FIX)
            val verdicts = listOf(Verdict.Affected(Severity.HIGH), wontFix, NOT_AFFECTED)

            val states = verdicts.map { stateOf(it, resolution("2.0.0")) }

            states shouldContainExactly List(3) { WorkState.RESOLVED }
        }

        test("an untriaged entry stays under investigation despite a resolution") {
            val state = stateOf(Verdict.UnderInvestigation, resolution("2.0.0"))

            state shouldBe WorkState.UNDER_INVESTIGATION
        }
    })
