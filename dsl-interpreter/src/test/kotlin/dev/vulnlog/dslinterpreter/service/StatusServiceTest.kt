package dev.vulnlog.dslinterpreter.service

import dev.vulnlog.common.AnalysisDataPerBranch
import dev.vulnlog.common.FixedExecutionPerBranch
import dev.vulnlog.common.SuppressionDateExecutionPerBranch
import dev.vulnlog.common.SuppressionEventExecutionPerBranch
import dev.vulnlog.common.SuppressionPermanentExecutionPerBranch
import dev.vulnlog.common.model.VulnEntryNonIdData
import dev.vulnlog.common.model.VulnStatusAffected
import dev.vulnlog.common.model.VulnStatusFixed
import dev.vulnlog.common.model.VulnStatusNotAffected
import dev.vulnlog.common.model.VulnStatusUnderInvestigation
import dev.vulnlog.common.model.VulnStatusUnknown
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

class StatusServiceTest : FunSpec({

    val statusService = StatusService()

    test("result is under investigation when no analysis data defined") {
        val vulnerability = mockk<VulnEntryNonIdData>()
        every { vulnerability.analysis } returns null

        val result = statusService.calculateStatus(vulnerability)

        result shouldBe VulnStatusUnderInvestigation
    }

    test("result is not affected when verdict is not affected") {
        val analysis = mockk<AnalysisDataPerBranch>()
        every { analysis.verdict } returns notAffected
        val vulnerability = mockk<VulnEntryNonIdData>()
        every { vulnerability.analysis } returns analysis

        val result = statusService.calculateStatus(vulnerability)

        result shouldBe VulnStatusNotAffected
    }

    listOf(critical, high, moderate, low).forEach { verdict ->
        test("result is affected when verdict is ${verdict.level} and no upcoming release is defined") {
            val analysis = mockk<AnalysisDataPerBranch>()
            every { analysis.verdict } returns verdict
            val vulnerability = mockk<VulnEntryNonIdData>()
            every { vulnerability.analysis } returns analysis
            every { vulnerability.involved?.upcoming?.releaseDate } returns null
            every { vulnerability.execution } returns null

            val result = statusService.calculateStatus(vulnerability)

            result shouldBe VulnStatusAffected
        }
    }

    listOf(critical, high, moderate, low).forEach { verdict ->
        test("result is affected when verdict is ${verdict.level} and upcoming release date is in the future") {
            val analysis = mockk<AnalysisDataPerBranch>()
            every { analysis.verdict } returns verdict
            val vulnerability = mockk<VulnEntryNonIdData>()
            every { vulnerability.analysis } returns analysis
            every { vulnerability.involved?.upcoming?.releaseDate } returns LocalDate.now().plusDays(42)
            every { vulnerability.execution } returns null

            val result = statusService.calculateStatus(vulnerability)

            result shouldBe VulnStatusAffected
        }
    }

    listOf(critical, high, moderate, low).forEach { verdict ->
        listOf(
            SuppressionPermanentExecutionPerBranch,
            SuppressionDateExecutionPerBranch(suppressUntilDate = LocalDate.now()),
            SuppressionEventExecutionPerBranch,
        ).forEach { br ->
            test("result is affected when verdict is ${verdict.level} and x is $br") {
                val analysis = mockk<AnalysisDataPerBranch>()
                every { analysis.verdict } returns verdict
                val vulnerability = mockk<VulnEntryNonIdData>()
                every { vulnerability.analysis } returns analysis
                every {
                    vulnerability.involved?.upcoming?.releaseDate
                } returns LocalDate.now().plusDays(42)
                every { vulnerability.execution?.execution } returns br

                val result = statusService.calculateStatus(vulnerability)

                result shouldBe VulnStatusAffected
            }
        }
    }

    listOf(critical, high, moderate, low).forEach { verdict ->
        listOf(FixedExecutionPerBranch(fixDate = LocalDate.now()), SuppressionEventExecutionPerBranch)
            .forEach { br ->
                test("result is fixed when verdict is ${verdict.level} and the upcoming release date in the past") {
                    val analysis = mockk<AnalysisDataPerBranch>()
                    every { analysis.verdict } returns verdict
                    val vulnerability = mockk<VulnEntryNonIdData>()
                    every { vulnerability.analysis } returns analysis
                    every {
                        vulnerability.involved?.upcoming?.releaseDate
                    } returns LocalDate.now().minusDays(42)
                    every { vulnerability.execution?.execution } returns br

                    val result = statusService.calculateStatus(vulnerability)

                    result shouldBe VulnStatusFixed
                }
            }
    }

    listOf(critical, high, moderate, low).forEach { verdict ->
        test("result is affected when verdict is ${verdict.level} and execution is fixed") {
            val analysis = mockk<AnalysisDataPerBranch>()
            every { analysis.verdict } returns verdict
            val vulnerability = mockk<VulnEntryNonIdData>()
            every { vulnerability.analysis } returns analysis
            val fixedExecutionPerBranch = mockk<FixedExecutionPerBranch>()
            every { vulnerability.involved?.upcoming?.releaseDate } returns null
            every { vulnerability.execution?.execution } returns fixedExecutionPerBranch

            val result = statusService.calculateStatus(vulnerability)

            result shouldBe VulnStatusFixed
        }
    }

    test("result is FIXED when verdict is high, fix in upcoming release, and past upcoming release date") {
        val analysis = mockk<AnalysisDataPerBranch>()
        every { analysis.verdict } returns high
        val vulnerability = mockk<VulnEntryNonIdData>()
        every { vulnerability.analysis } returns analysis
        every { vulnerability.involved?.upcoming?.releaseDate } returns LocalDate.now().minusDays(1)
        val execution = SuppressionEventExecutionPerBranch
        every { vulnerability.execution?.execution } returns execution

        val result = statusService.calculateStatus(vulnerability)

        result shouldBe VulnStatusFixed
    }

    listOf(critical, high, moderate, low).forEach { verdict ->
        test("result is UNKNOWN when verdict is ${verdict.level}, upcoming release is today, execution is null") {
            val analysis = mockk<AnalysisDataPerBranch>()
            every { analysis.verdict } returns verdict
            val vulnerability = mockk<VulnEntryNonIdData>()
            every { vulnerability.analysis } returns analysis
            every { vulnerability.involved?.upcoming?.releaseDate } returns LocalDate.now()
            every { vulnerability.execution?.execution } returns null

            val result = statusService.calculateStatus(vulnerability)

            result shouldBe VulnStatusUnknown
        }
    }
})
