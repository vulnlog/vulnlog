package dev.vulnlog.cli.service

import dev.vulnlog.dsl.ResultStatus
import dev.vulnlog.dsl.critical
import dev.vulnlog.dsl.high
import dev.vulnlog.dsl.low
import dev.vulnlog.dsl.moderate
import dev.vulnlog.dsl.notAffected
import dev.vulnlog.dslinterpreter.splitter.AnalysisDataPerBranch
import dev.vulnlog.dslinterpreter.splitter.FixedExecutionPerBranch
import dev.vulnlog.dslinterpreter.splitter.VulnerabilityDataPerBranch
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate

class StatusRuleSet : FunSpec({

    test("result is under investigation when no analysis data defined") {
        val vulnerability = mockk<VulnerabilityDataPerBranch>()
        every { vulnerability.analysisData } returns null

        val result = ruleSet.handle(vulnerability)

        result shouldBe ResultStatus.UNDER_INVESTIGATION
    }

    test("result is not affected when verdict is not affected") {
        val analysisData = mockk<AnalysisDataPerBranch>()
        every { analysisData.verdict } returns notAffected
        val vulnerability = mockk<VulnerabilityDataPerBranch>()
        every { vulnerability.analysisData } returns analysisData

        val result = ruleSet.handle(vulnerability)

        result shouldBe ResultStatus.NOT_AFFECTED
    }

    listOf(critical, high, moderate, low).forEach { verdict ->
        test("result is affected when verdict is ${verdict.level} and no upcoming release is defined") {
            val analysisData = mockk<AnalysisDataPerBranch>()
            every { analysisData.verdict } returns verdict
            val vulnerability = mockk<VulnerabilityDataPerBranch>()
            every { vulnerability.analysisData } returns analysisData
            every { vulnerability.involvedReleaseVersions?.upcoming?.releaseDate } returns null
            every { vulnerability.executionData } returns null

            val result = ruleSet.handle(vulnerability)

            result shouldBe ResultStatus.AFFECTED
        }
    }

    listOf(critical, high, moderate, low).forEach { verdict ->
        test("result is affected when verdict is ${verdict.level} and upcoming release date is in the future") {
            val analysisData = mockk<AnalysisDataPerBranch>()
            every { analysisData.verdict } returns verdict
            val vulnerability = mockk<VulnerabilityDataPerBranch>()
            every { vulnerability.analysisData } returns analysisData
            every { vulnerability.involvedReleaseVersions?.upcoming?.releaseDate } returns LocalDate.now().plusDays(42)
            every { vulnerability.executionData } returns null

            val result = ruleSet.handle(vulnerability)

            result shouldBe ResultStatus.AFFECTED
        }
    }

    listOf(critical, high, moderate, low).forEach { verdict ->
        test("result is fixed when verdict is ${verdict.level} and the upcoming release date in the past") {
            val analysisData = mockk<AnalysisDataPerBranch>()
            every { analysisData.verdict } returns verdict
            val vulnerability = mockk<VulnerabilityDataPerBranch>()
            every { vulnerability.analysisData } returns analysisData
            every { vulnerability.involvedReleaseVersions?.upcoming?.releaseDate } returns LocalDate.now().minusDays(42)
            every { vulnerability.executionData } returns null

            val result = ruleSet.handle(vulnerability)

            result shouldBe ResultStatus.FIXED
        }
    }

    listOf(critical, high, moderate, low).forEach { verdict ->
        test("result is affected when verdict is ${verdict.level} and execution is fixed") {
            val analysisData = mockk<AnalysisDataPerBranch>()
            every { analysisData.verdict } returns verdict
            val vulnerability = mockk<VulnerabilityDataPerBranch>()
            every { vulnerability.analysisData } returns analysisData
            val fixedExecutionPerBranch = mockk<FixedExecutionPerBranch>()
            every { vulnerability.involvedReleaseVersions?.upcoming?.releaseDate } returns null
            every { vulnerability.executionData?.execution } returns fixedExecutionPerBranch

            val result = ruleSet.handle(vulnerability)

            result shouldBe ResultStatus.FIXED
        }
    }
})
