package dev.vulnlog.cli.service

import dev.vulnlog.common.AnalysisDataPerBranch
import dev.vulnlog.common.FixedExecutionPerBranch
import dev.vulnlog.common.SuppressionDateExecutionPerBranch
import dev.vulnlog.common.SuppressionEventExecutionPerBranch
import dev.vulnlog.common.SuppressionPermanentExecutionPerBranch
import dev.vulnlog.common.VulnerabilityDataPerBranch
import dev.vulnlog.dsl.ResultStatus
import dev.vulnlog.dsl.critical
import dev.vulnlog.dsl.high
import dev.vulnlog.dsl.low
import dev.vulnlog.dsl.moderate
import dev.vulnlog.dsl.notAffected
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate

class StatusRuleSetTest : FunSpec({

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
        listOf(
            SuppressionPermanentExecutionPerBranch(),
            SuppressionDateExecutionPerBranch(suppressUntilDate = LocalDate.now()),
            SuppressionEventExecutionPerBranch(),
        ).forEach { br ->
            test("result is affected when verdict is ${verdict.level} and x is $br") {
                val analysisData = mockk<AnalysisDataPerBranch>()
                every { analysisData.verdict } returns verdict
                val vulnerability = mockk<VulnerabilityDataPerBranch>()
                every { vulnerability.analysisData } returns analysisData
                every {
                    vulnerability.involvedReleaseVersions?.upcoming?.releaseDate
                } returns LocalDate.now().plusDays(42)
                every { vulnerability.executionData?.execution } returns br

                val result = ruleSet.handle(vulnerability)

                result shouldBe ResultStatus.AFFECTED
            }
        }
    }

    listOf(critical, high, moderate, low).forEach { verdict ->
        listOf(
            FixedExecutionPerBranch(fixDate = LocalDate.now()),
            SuppressionEventExecutionPerBranch(),
        ).forEach { br ->
            test("result is fixed when verdict is ${verdict.level} and the upcoming release date in the past") {
                val analysisData = mockk<AnalysisDataPerBranch>()
                every { analysisData.verdict } returns verdict
                val vulnerability = mockk<VulnerabilityDataPerBranch>()
                every { vulnerability.analysisData } returns analysisData
                every {
                    vulnerability.involvedReleaseVersions?.upcoming?.releaseDate
                } returns LocalDate.now().minusDays(42)
                every { vulnerability.executionData?.execution } returns br

                val result = ruleSet.handle(vulnerability)

                result shouldBe ResultStatus.FIXED
            }
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

    test("result is FIXED when verdict is high, fix in upcoming release, and past upcoming release date") {
        val analysisData = mockk<AnalysisDataPerBranch>()
        every { analysisData.verdict } returns high
        val vulnerability = mockk<VulnerabilityDataPerBranch>()
        every { vulnerability.analysisData } returns analysisData
        every { vulnerability.involvedReleaseVersions?.upcoming?.releaseDate } returns LocalDate.now().minusDays(1)
        every { vulnerability.executionData?.execution } returns SuppressionEventExecutionPerBranch()

        val result = ruleSet.handle(vulnerability)

        result shouldBe ResultStatus.FIXED
    }

    listOf(critical, high, moderate, low).forEach { verdict ->
        test("result is UNKNOWN when verdict is ${verdict.level}, upcoming release is today, execution is null") {
            val analysisData = mockk<AnalysisDataPerBranch>()
            every { analysisData.verdict } returns verdict
            val vulnerability = mockk<VulnerabilityDataPerBranch>()
            every { vulnerability.analysisData } returns analysisData
            every { vulnerability.involvedReleaseVersions?.upcoming?.releaseDate } returns LocalDate.now()
            every { vulnerability.executionData?.execution } returns null

            val result = ruleSet.handle(vulnerability)

            result shouldBe ResultStatus.UNKNOWN
        }
    }
})
