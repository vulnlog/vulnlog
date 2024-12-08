package dev.vulnlog.dslinterpreter.splitter

import dev.vulnlog.dslinterpreter.dsl.VlVulnerabilityData
import dev.vulnlog.dslinterpreter.dsl.impl.VlBranchValueImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VlFixActionValueImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VlOverwriteValueImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VlRatingLowValueImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VlReleaseValueImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VlReportByValueImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VlReportForValueImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VlVariantValueImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VlVulnerabilityIdImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VlVulnerabilityValueImpl
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class SplitterKtTest : FunSpec({

    val v000 = VlReleaseValueImpl("0.0.0")
    val v100 = VlReleaseValueImpl("1.0.0")
    val v110 = VlReleaseValueImpl("1.1.0")
    val v111 = VlReleaseValueImpl("1.1.1")
    val v200 = VlReleaseValueImpl("2.0.0")
    val v210 = VlReleaseValueImpl("2.1.0")
    val v211 = VlReleaseValueImpl("2.1.1")
    val branch00 = VlBranchValueImpl("branch 00", v000, emptyList(), emptyList())
    val branch10 = VlBranchValueImpl("branch 10", v100, listOf(v110, v111), emptyList())
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

    test("should be empty when empty branches and empty vulnerabilities") {
        val result = vulnerabilityPerBranch(emptyList(), emptyList())

        result shouldBe emptyList()
    }

    test("should have default branch when empty branches") {

        val result = vulnerabilityPerBranch(emptyList(), listOf(vulnerabilityData))

        result.size shouldBe 1
        result[0] shouldBe VulnlogPerBranch(vulnerabilities = listOf(vulnerabilityData))
    }

    test("should have one vulnerability in one branch") {
        val data =
            VlVulnerabilityData(
                setOf(VlVulnerabilityIdImpl("CVE-1")),
                VlVulnerabilityValueImpl(
                    reportFor = setOf(reportFor100),
                    reportBy = emptySet(),
                    rating = null,
                    fixAction = null,
                    fixIn = setOf(v110),
                    overwrites = emptySet(),
                ),
            )
        val expected =
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

        val result = vulnerabilityPerBranch(listOf(branch10), listOf(data))

        result shouldHaveSize 1
        result[0].branch shouldBe branch10
        result[0].vulnerabilities shouldHaveSingleElement expected
    }

    test("should have one vulnerability with overwrites in one branch") {
        val data =
            VlVulnerabilityData(
                setOf(VlVulnerabilityIdImpl("CVE-1")),
                VlVulnerabilityValueImpl(
                    reportFor = setOf(reportFor100),
                    reportBy = emptySet(),
                    rating = null,
                    fixAction = null,
                    fixIn = setOf(v110),
                    overwrites =
                        setOf(
                            VlOverwriteValueImpl(
                                reportedFor = setOf(reportFor100),
                                reportBy = setOf(VlReportByValueImpl("reporter")),
                                rating = VlRatingLowValueImpl(LocalDate.parse("2024-12-15"), "for testing reasons"),
                                toFix = VlFixActionValueImpl("update some dependencies"),
                                fixIn = setOf(v111),
                                noAction = null,
                                suppressionValue = null,
                            ),
                        ),
                ),
            )
        val expected =
            VlVulnerabilityData(
                setOf(VlVulnerabilityIdImpl("CVE-1")),
                VlVulnerabilityValueImpl(
                    reportFor = setOf(VlReportForValueImpl(VlVariantValueImpl("abc"), v100)),
                    reportBy = setOf(VlReportByValueImpl("reporter")),
                    rating = VlRatingLowValueImpl(LocalDate.parse("2024-12-15"), "for testing reasons"),
                    fixAction = VlFixActionValueImpl("update some dependencies"),
                    fixIn = setOf(v111),
                    overwrites = emptySet(),
                ),
            )

        val result = vulnerabilityPerBranch(listOf(branch10), listOf(data))

        result shouldHaveSize 1
        result[0].branch shouldBe branch10
        result[0].vulnerabilities shouldHaveSingleElement expected
    }

    test("should split one vulnerability into two branch") {
        val data =
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
        val expected1 =
            VlVulnerabilityData(
                setOf(VlVulnerabilityIdImpl("CVE-1")),
                VlVulnerabilityValueImpl(
                    reportFor = setOf(reportFor100),
                    reportBy = emptySet(),
                    rating = null,
                    fixAction = null,
                    fixIn = setOf(v110),
                    overwrites = emptySet(),
                ),
            )
        val expected2 =
            VlVulnerabilityData(
                setOf(VlVulnerabilityIdImpl("CVE-1")),
                VlVulnerabilityValueImpl(
                    reportFor = setOf(reportFor210),
                    reportBy = emptySet(),
                    rating = null,
                    fixAction = null,
                    // no branch information available what is the successor of v210? therefor fixIn don't change.
                    fixIn = setOf(v211),
                    overwrites = emptySet(),
                ),
            )

        val result = vulnerabilityPerBranch(listOf(branch10, branch20), listOf(data))

        result shouldHaveSize 2
        result[0].branch shouldBe branch10
        result[0].vulnerabilities shouldHaveSingleElement expected1

        result[1].branch shouldBe branch20
        result[1].vulnerabilities shouldHaveSingleElement expected2
    }

    test("should split one vulnerability into a branch and a default branch") {
        val data =
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
        val expected1 =
            VlVulnerabilityData(
                setOf(VlVulnerabilityIdImpl("CVE-1")),
                VlVulnerabilityValueImpl(
                    reportFor = setOf(reportFor100),
                    reportBy = emptySet(),
                    rating = null,
                    fixAction = null,
                    fixIn = setOf(v110),
                    overwrites = emptySet(),
                ),
            )
        val expected2 =
            VlVulnerabilityData(
                setOf(VlVulnerabilityIdImpl("CVE-1")),
                VlVulnerabilityValueImpl(
                    reportFor = setOf(reportFor210),
                    reportBy = emptySet(),
                    rating = null,
                    fixAction = null,
                    // no branch information available what is the successor of v210? therefor fixIn don't change.
                    fixIn = setOf(v110, v211),
                    overwrites = emptySet(),
                ),
            )

        val result = vulnerabilityPerBranch(listOf(branch10), listOf(data))

        result shouldHaveSize 2
        result[0].branch shouldBe branch10
        result[0].vulnerabilities shouldHaveSingleElement expected1

        result[1].branch shouldBe DefaultBranch
        result[1].vulnerabilities shouldHaveSingleElement expected2
    }

    test("should have default branch with two vulnerabilities and a branch with one vulnerability") {
        val data1 =
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
        val data2 =
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
        val expected1 =
            VlVulnerabilityData(
                setOf(VlVulnerabilityIdImpl("CVE-1")),
                VlVulnerabilityValueImpl(
                    reportFor = setOf(reportFor100),
                    reportBy = emptySet(),
                    rating = null,
                    fixAction = null,
                    fixIn = setOf(v110),
                    overwrites = emptySet(),
                ),
            )
        val expected2 =
            VlVulnerabilityData(
                setOf(VlVulnerabilityIdImpl("CVE-1")),
                VlVulnerabilityValueImpl(
                    reportFor = setOf(reportFor210),
                    reportBy = emptySet(),
                    rating = null,
                    fixAction = null,
                    fixIn = setOf(v110, v211),
                    overwrites = emptySet(),
                ),
            )
        val expected3 =
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

        val result = vulnerabilityPerBranch(listOf(branch00, branch10), listOf(data1, data2))

        result.size shouldBe 2
        result[0].branch shouldBe branch10
        result[0].vulnerabilities shouldHaveSingleElement expected1

        result[1].branch shouldBe DefaultBranch
        result[1].vulnerabilities shouldContainExactly listOf(expected2, expected3)
    }
})
