package ch.addere.vulnlog.scriptdefinition

import ch.addere.vulnlog.core.model.reporter.VlGenericReporter
import ch.addere.vulnlog.core.model.reporter.VlReporterSet
import ch.addere.vulnlog.core.model.resolution.VlMitigateResolution
import ch.addere.vulnlog.core.model.resolution.VlResolutionVersionSet
import ch.addere.vulnlog.core.model.resolution.VlSuppressResolution
import ch.addere.vulnlog.core.model.version.VlAffectedVersionSet
import ch.addere.vulnlog.core.model.version.VlVersion
import ch.addere.vulnlog.core.model.vulnerability.VlVulnerability
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

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
