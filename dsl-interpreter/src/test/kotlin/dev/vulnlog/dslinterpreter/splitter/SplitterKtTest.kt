package dev.vulnlog.dslinterpreter.splitter

import dev.vulnlog.dsl.VlBranchValue
import dev.vulnlog.dslinterpreter.dsl.VlVulnerabilityData
import dev.vulnlog.dslinterpreter.dsl.impl.VlReleaseValueImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VlReportForValueImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VlVariantValueImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VlVulnerabilityValueImpl
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class SplitterKtTest : FunSpec({

    val v100 = VlReleaseValueImpl("1.0.0")
    val v110 = VlReleaseValueImpl("1.1.0")
    val v200 = VlReleaseValueImpl("2.0.0")
    val v210 = VlReleaseValueImpl("2.1.0")
    val variant = VlVariantValueImpl("abc")
    val reportFor110 = VlReportForValueImpl(variant, v110)
    val reportFor200 = VlReportForValueImpl(variant, v200)
    val reportFor210 = VlReportForValueImpl(variant, v210)

    val branch10 = mockk<VlBranchValue>()
    val branch20 = mockk<VlBranchValue>()
    val vulnerabilityData1 = mockk<VlVulnerabilityData>()
    val vulnerabilityData1Modified = mockk<VlVulnerabilityData>()
    val vulnerabilityData2 = mockk<VlVulnerabilityData>()
    val vulnerabilityData2Modified = mockk<VlVulnerabilityData>()
    val vulnerability1 = mockk<VlVulnerabilityValueImpl>()
    val vulnerability1Modified = mockk<VlVulnerabilityValueImpl>()
    val vulnerability2 = mockk<VlVulnerabilityValueImpl>()
    val vulnerability2Modified = mockk<VlVulnerabilityValueImpl>()

    beforeEach {
        every { branch10.releases } returns listOf(v100, v110)
        every { branch20.releases } returns listOf(v200, v210)
        every { vulnerabilityData1.vulnerability } returns vulnerability1
        every { vulnerabilityData2.vulnerability } returns vulnerability2
        every { vulnerabilityData1.copy(vulnerability = vulnerability1Modified) } returns vulnerabilityData1Modified
        every { vulnerabilityData2.copy(vulnerability = vulnerability2Modified) } returns vulnerabilityData2Modified
    }

    test("should be empty when empty branches and empty vulnerabilities") {
        val result = vulnerabilityPerBranch(emptyList(), emptyList())

        result shouldBe emptyList()
    }

    test("should have default branch when empty when empty branches") {
        every { vulnerability1.reportFor } returns emptySet()
        every { vulnerability1.copy(reportFor = emptySet()) } returns vulnerability1Modified

        val result = vulnerabilityPerBranch(emptyList(), listOf(vulnerabilityData1))

        result.size shouldBe 1
        result[0] shouldBe VulnlogPerBranch(null, listOf(vulnerabilityData1))
    }

    test("should have empty vulnerabilities when branch has no releases") {
        every { branch10.releases } returns emptyList()

        val result = vulnerabilityPerBranch(listOf(branch10), emptyList())

        result shouldBe listOf(VulnlogPerBranch(branch10, emptyList()))
    }

    test("should have one vulnerability in one branch") {
        every { vulnerability1.reportFor } returns setOf(reportFor110)
        every { vulnerability1.copy(reportFor = setOf(reportFor110)) } returns vulnerability1Modified
        every { vulnerability2.reportFor } returns setOf(reportFor210)

        val result = vulnerabilityPerBranch(listOf(branch10), listOf(vulnerabilityData1))

        result.size shouldBe 1
        result[0] shouldBe VulnlogPerBranch(branch10, listOf(vulnerabilityData1Modified))
    }

    test("should have one vulnerability in each branch") {
        every { vulnerability1.reportFor } returns setOf(reportFor110)
        every { vulnerability1.copy(reportFor = setOf(reportFor110)) } returns vulnerability1Modified
        every { vulnerability2.reportFor } returns setOf(reportFor210)
        every { vulnerability2.copy(reportFor = setOf(reportFor210)) } returns vulnerability2Modified

        val result = vulnerabilityPerBranch(listOf(branch10, branch20), listOf(vulnerabilityData1, vulnerabilityData2))

        result.size shouldBe 2
        result[0] shouldBe VulnlogPerBranch(branch10, listOf(vulnerabilityData1Modified))
        result[1] shouldBe VulnlogPerBranch(branch20, listOf(vulnerabilityData2Modified))
    }

    test("should have one vulnerability in the first branch and two in the second branch") {
        every { vulnerability1.reportFor } returns setOf(reportFor110, reportFor200)
        every { vulnerability1.copy(reportFor = setOf(reportFor110)) } returns vulnerability1Modified
        every { vulnerability1.copy(reportFor = setOf(reportFor200)) } returns vulnerability1Modified
        every { vulnerability2.reportFor } returns setOf(reportFor210)
        every { vulnerability2.copy(reportFor = setOf(reportFor210)) } returns vulnerability2Modified

        val result = vulnerabilityPerBranch(listOf(branch10, branch20), listOf(vulnerabilityData1, vulnerabilityData2))

        result.size shouldBe 2
        result[0] shouldBe VulnlogPerBranch(branch10, listOf(vulnerabilityData1Modified))
        result[1] shouldBe VulnlogPerBranch(branch20, listOf(vulnerabilityData1Modified, vulnerabilityData2Modified))
    }

    test("should have default branch with one vulnerability and an empty branch") {
        every { vulnerability1.reportFor } returns emptySet()
        every { vulnerability1.copy(reportFor = emptySet()) } returns vulnerability1Modified
        every { vulnerability2.reportFor } returns setOf(reportFor210)
        every { vulnerability2.copy(reportFor = setOf(reportFor210)) } returns vulnerability2Modified

        val result = vulnerabilityPerBranch(listOf(branch10, branch20), listOf(vulnerabilityData1, vulnerabilityData2))

        result.size shouldBe 3
        result[0] shouldBe VulnlogPerBranch(branch10, emptyList())
        result[1] shouldBe VulnlogPerBranch(branch20, listOf(vulnerabilityData2Modified))
        result[2] shouldBe VulnlogPerBranch(null, listOf(vulnerabilityData1))
    }
})
