package dev.vulnlog.dslinterpreter.splitter

import dev.vulnlog.dslinterpreter.dsl.VlVulnerabilityData
import dev.vulnlog.dslinterpreter.dsl.impl.VlBranchValueImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VlReleaseValueImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VlReportForValueImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VlVariantValueImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VlVulnerabilityIdImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VlVulnerabilityValueImpl
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.shouldBe

class SplitterKtTest : FunSpec({

    val v000 = VlReleaseValueImpl("0.0.0")
    val v100 = VlReleaseValueImpl("1.0.0")
    val v110 = VlReleaseValueImpl("1.1.0")
    val v200 = VlReleaseValueImpl("2.0.0")
    val v210 = VlReleaseValueImpl("2.1.0")
    val v211 = VlReleaseValueImpl("2.1.1")
    val branch00 = VlBranchValueImpl("branch 00", v000, emptyList(), emptyList())
    val branch10 = VlBranchValueImpl("branch 10", v100, listOf(v110), emptyList())
    val branch20 = VlBranchValueImpl("branch 20", v200, listOf(v210, v211), emptyList())
    val variant = VlVariantValueImpl("abc")
    val reportFor100 = VlReportForValueImpl(variant, v100)
    val reportFor210 = VlReportForValueImpl(variant, v210)
    val vulnerabilityData =
        VlVulnerabilityData(
            setOf(VlVulnerabilityIdImpl("CVE-1")),
            VlVulnerabilityValueImpl(
                reportFor = setOf(reportFor100, reportFor210),
                reportBy = emptySet(),
                rating = null,
                fixAction = null,
                fixIn = setOf(v110, v211),
                overwrites = emptySet(),
            ),
        )
    val vulnerabilityData2 =
        VlVulnerabilityData(
            setOf(VlVulnerabilityIdImpl("CVE-2")),
            VlVulnerabilityValueImpl(
                reportFor = emptySet(),
                reportBy = emptySet(),
                rating = null,
                fixAction = null,
                fixIn = emptySet(),
                overwrites = emptySet(),
            ),
        )
    val vulnerabilityDataBranch10 =
        VlVulnerabilityData(
            setOf(VlVulnerabilityIdImpl("CVE-1")),
            VlVulnerabilityValueImpl(
                reportFor = setOf(VlReportForValueImpl(VlVariantValueImpl("abc"), v100)),
                reportBy = emptySet(),
                rating = null,
                fixAction = null,
                fixIn = setOf(v110),
                overwrites = emptySet(),
            ),
        )
    val vulnerabilityDataBranchDefault =
        VlVulnerabilityData(
            setOf(VlVulnerabilityIdImpl("CVE-2")),
            VlVulnerabilityValueImpl(
                reportFor = emptySet(),
                reportBy = emptySet(),
                rating = null,
                fixAction = null,
                fixIn = emptySet(),
                overwrites = emptySet(),
            ),
        )

    test("should be empty when empty branches and empty vulnerabilities") {
        val result = vulnerabilityPerBranch(emptyList(), emptyList())

        result shouldBe emptyList()
    }

    test("should have default branch when empty branches") {

        val result = vulnerabilityPerBranch(emptyList(), listOf(vulnerabilityData))

        result.size shouldBe 1
        result[0] shouldBe VulnlogPerBranch(null, listOf(vulnerabilityData))
    }

    test("should have one vulnerability in one branch") {
        val result = vulnerabilityPerBranch(listOf(branch10), listOf(vulnerabilityData))

        result.size shouldBe 1
        result[0] shouldBe VulnlogPerBranch(branch10, listOf(vulnerabilityDataBranch10))
    }

    test("should split one vulnerability into two branch") {

        val result = vulnerabilityPerBranch(listOf(branch10, branch20), listOf(vulnerabilityData))

        result.size shouldBe 2
        val branch10reportedFor = result[0].vulnerabilities[0].vulnerability.reportFor
        val expectedBranch10reportedFor = setOf(VlReportForValueImpl(VlVariantValueImpl("abc"), v100))
        branch10reportedFor shouldContainOnly expectedBranch10reportedFor
        val branch20reportFor = result[1].vulnerabilities[0].vulnerability.reportFor
        val expectedBranch20reportedFor = setOf(VlReportForValueImpl(VlVariantValueImpl("abc"), v210))
        branch20reportFor shouldContainOnly expectedBranch20reportedFor
    }

    test("should have default branch with one vulnerability and an empty branch") {

        val result = vulnerabilityPerBranch(listOf(branch00, branch10), listOf(vulnerabilityData, vulnerabilityData2))

        result.size shouldBe 3
        result[0].vulnerabilities.size shouldBe 0

        val branch10reportedFor = result[1].vulnerabilities[0].vulnerability.reportFor
        val expectedBranch10reportedFor = setOf(VlReportForValueImpl(VlVariantValueImpl("abc"), v100))
        branch10reportedFor shouldBe expectedBranch10reportedFor
        result[2] shouldBe VulnlogPerBranch(null, listOf(vulnerabilityDataBranchDefault))
    }
})
