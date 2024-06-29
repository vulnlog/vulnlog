package io.vulnlog.scriptdefinition

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.vulnlog.core.model.reporter.VlGenericReporter
import io.vulnlog.core.model.reporter.VlReporterSet
import io.vulnlog.core.model.resolution.VlMitigateResolution
import io.vulnlog.core.model.resolution.VlResolutionVersionSet
import io.vulnlog.core.model.resolution.VlSuppressResolution
import io.vulnlog.core.model.version.VlAffectedVersionSet
import io.vulnlog.core.model.version.VlVersion
import io.vulnlog.core.model.vulnerability.VlVulnerability

class VulnLogScriptTest :
    FunSpec(
        {
            test("test suppress implicit add affected versions") {
                val vulnLogScript = VulnLogScript()

                vulnLogScript.cve("cve1") {
                    affectedVersions(VlVersion("v1"), VlVersion("v2"))
                    suppress("rationale")
                }

                vulnLogScript.allVulnerabilities.size shouldBe 1
                vulnLogScript.allVulnerabilities.first() shouldBe vulnSuppresse()
            }

            test("test suppress explicit add affected versions") {
                val vulnLogScript = VulnLogScript()

                vulnLogScript.cve("cve1") {
                    affectedVersions(VlVersion("v1"), VlVersion("v2"))
                    suppress(VlVersion("v1"), VlVersion("v2"), rationale = "rationale")
                }

                vulnLogScript.allVulnerabilities.size shouldBe 1
                vulnLogScript.allVulnerabilities.first() shouldBe vulnSuppresse()
            }

            test("test mitigate explicit add affected versions") {
                val vulnLogScript = VulnLogScript()

                vulnLogScript.cve("cve1") {
                    affectedVersions(VlVersion("v1"), VlVersion("v2"))
                    mitigate(VlVersion("v1"), VlVersion("v2"), rationale = "rationale")
                }

                vulnLogScript.allVulnerabilities.size shouldBe 1
                vulnLogScript.allVulnerabilities.first() shouldBe vulnMitigate()
            }

            test("test mitigate implicit add affected versions") {
                val vulnLogScript = VulnLogScript()

                vulnLogScript.cve("cve1") {
                    affectedVersions(VlVersion("v1"), VlVersion("v2"))
                    mitigate("rationale")
                }

                vulnLogScript.allVulnerabilities.size shouldBe 1
                vulnLogScript.allVulnerabilities.first() shouldBe vulnMitigate()
            }
        },
    )

fun vulnSuppresse(): VlVulnerability =
    vuln(suppress = VlSuppressResolution(VlResolutionVersionSet(setOf(VlVersion("v1"), VlVersion("v2"))), "rationale"))

fun vulnMitigate(): VlVulnerability =
    vuln(mitigate = VlMitigateResolution(VlResolutionVersionSet(setOf(VlVersion("v1"), VlVersion("v2"))), "rationale"))

fun vuln(
    mitigate: VlMitigateResolution? = null,
    suppress: VlSuppressResolution? = null,
) = VlVulnerability(
    "cve1",
    VlReporterSet(setOf(VlGenericReporter(VlAffectedVersionSet(setOf(VlVersion("v1"), VlVersion("v2")))))),
    null,
    mitigate,
    suppress,
)
