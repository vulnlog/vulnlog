// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.app.FilterRejected
import dev.vulnlog.lib.app.OpenVexOutcome
import dev.vulnlog.lib.core.filter.FilterProblem
import dev.vulnlog.lib.core.vex.openvex.collectOpenVexStatements
import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.mavenPurlEntry
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.tag
import dev.vulnlog.lib.fixtures.vulnerability
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaselineProblem
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexEmptyReason
import dev.vulnlog.lib.model.vex.openvex.OpenVexFormatVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexIdentityField
import dev.vulnlog.lib.model.vex.openvex.OpenVexScope
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private val file =
    vulnlogFile(
        releases =
            listOf(
                releaseEntry(
                    "1.0.0",
                    purls = listOf(mavenPurlEntry("pkg:maven/com.acme/app@1.0.0", tags = listOf("app"))),
                ),
                releaseEntry("1.0.1"),
            ),
        vulnerabilities =
            listOf(
                vulnerability(
                    id = cve("CVE-2026-1111"),
                    releases = listOf(release("1.0.0")),
                    tags = listOf(tag("app")),
                ),
                vulnerability(
                    id = cve("CVE-2026-2222"),
                    releases = listOf(release("1.0.1")),
                    tags = listOf(tag("app")),
                ),
                vulnerability(id = cve("CVE-2026-3333"), tags = listOf(tag("app"))),
                vulnerability(id = cve("CVE-2026-4444"), releases = listOf(release("1.0.0"))),
                vulnerability(
                    id = cve("CVE-2026-5555"),
                    releases = listOf(release("1.0.0")),
                    tags = listOf(tag("build")),
                ),
            ),
    )

private val taggedFile =
    vulnlogFile(
        releases =
            listOf(
                releaseEntry(
                    "1.0.0",
                    purls = listOf(mavenPurlEntry("pkg:maven/com.acme/app@1.0.0", tags = listOf("container"))),
                ),
                releaseEntry(
                    "1.0.1",
                    purls = listOf(mavenPurlEntry("pkg:maven/com.acme/lib@1.0.1", tags = listOf("library"))),
                ),
            ),
        vulnerabilities =
            listOf(
                vulnerability(
                    id = cve("CVE-2026-1111"),
                    releases = listOf(release("1.0.0"), release("1.0.1")),
                    tags = listOf(tag("container"), tag("library")),
                ),
            ),
    )

class OpenVexMessagesTest :
    FunSpec({

        context("renderOpenVexReport") {

            test("reports the scope, what is left out, the anchors and the counts, in print order") {
                val scope = OpenVexScope(releases = setOf(release("1.0.0")), tags = setOf(tag("container")))
                val collection = collectOpenVexStatements(taggedFile, scope)
                val outcome = OpenVexOutcome.Unchanged(collection, OpenVexDocumentVersion.FIRST, content = "")

                val lines = renderOpenVexReport(outcome)

                lines shouldContainExactly
                    listOf(
                        OpenVexLine.Verbose("release scope: 1.0.0"),
                        OpenVexLine.Verbose("tag scope matched tags: container"),
                        OpenVexLine.Verbose("anchored on 1 release with purls: '1.0.0' (1 purl)"),
                        OpenVexLine.Verbose("collected 1 statement: 1 under_investigation"),
                    )
            }

            test("warns about releases without purls and states why each entry is left out") {
                val collection = collectOpenVexStatements(file)
                val outcome = OpenVexOutcome.Unchanged(collection, OpenVexDocumentVersion.FIRST, content = "")

                val lines = renderOpenVexReport(outcome)

                lines shouldContainExactly
                    listOf(
                        OpenVexLine.Warning("releases without purls are not part of the document: '1.0.1'"),
                        OpenVexLine.Verbose("anchored on 1 release with purls: '1.0.0' (1 purl)"),
                        OpenVexLine.Debug("skipped CVE-2026-2222: no release it applies to declares purls in scope"),
                        OpenVexLine.Debug("skipped CVE-2026-3333: it references no release"),
                        OpenVexLine.Debug("skipped CVE-2026-4444: it has no tags to match a release purl"),
                        OpenVexLine.Debug("skipped CVE-2026-5555: no release purl in scope shares one of its tags"),
                        OpenVexLine.Verbose("collected 1 statement: 1 under_investigation"),
                    )
            }

            test("reports no count when no statement applies, and blames the scope for bare releases") {
                val onlyOnLibraryRelease =
                    taggedFile.vulnerabilities.map { it.copy(releases = listOf(release("1.0.1"))) }
                val file = taggedFile.copy(vulnerabilities = onlyOnLibraryRelease)
                val collection = collectOpenVexStatements(file, OpenVexScope(tags = setOf(tag("container"))))
                val outcome = OpenVexOutcome.NoStatementApplies(collection, OpenVexEmptyReason.NO_ENTRY_IN_TAG_SCOPE)

                val lines = renderOpenVexReport(outcome)

                lines shouldContainExactly
                    listOf(
                        OpenVexLine.Verbose("tag scope matched tags: container"),
                        OpenVexLine.Warning("releases without purls in scope are not part of the document: '1.0.1'"),
                        OpenVexLine.Verbose("anchored on 1 release with purls: '1.0.0' (1 purl)"),
                        OpenVexLine.Debug("skipped CVE-2026-1111: no release it applies to declares purls in scope"),
                    )
            }

            test("reports nothing for a run rejected before it collected") {
                val outcomes =
                    listOf(
                        OpenVexOutcome.BaselineRejected(OpenVexBaselineProblem.NotOpenVex),
                        FilterRejected(emptyList()),
                    )

                val lines = outcomes.flatMap(::renderOpenVexReport)

                lines shouldBe emptyList()
            }
        }

        test("renderOpenVexWritten names the target, the format, the version and the count") {
            val collection = collectOpenVexStatements(file)
            val outcome = OpenVexOutcome.Unchanged(collection, OpenVexDocumentVersion(3), content = "")

            val line = renderOpenVexWritten("vex.json", outcome)

            line shouldBe "wrote vex.json: openvex format, version 3, 1 statement"
        }

        test("renderOpenVexFailure words every filter problem of a rejected scope") {
            val problem = FilterProblem.UnknownTags(listOf(tag("binary")), listOf(tag("app")))

            val failures = renderOpenVexFailure(FilterRejected(listOf(problem)), "", "--baseline")

            failures shouldContainExactly listOf(Failure("Tag not found: binary", "Known tags: app"))
        }

        test("renderOpenVexFailure names the baseline, what is wrong with it and the option to omit") {
            val outcomes =
                listOf(
                    OpenVexBaselineProblem.NotOpenVex,
                    OpenVexBaselineProblem.OtherFormatVersion("0.1.0", OpenVexFormatVersion.VERSION_0_2_0),
                    OpenVexBaselineProblem.InvalidIdentity(OpenVexIdentityField.ID, "vex-1"),
                    OpenVexBaselineProblem.InvalidIdentity(OpenVexIdentityField.TIMESTAMP, null),
                    OpenVexBaselineProblem.InvalidIdentity(OpenVexIdentityField.VERSION, "0"),
                ).map(OpenVexOutcome::BaselineRejected)

            val failures = outcomes.flatMap { renderOpenVexFailure(it, "vex.json", "--baseline") }

            failures shouldContainExactly
                listOf(
                    "baseline 'vex.json' is not an OpenVEX document",
                    "baseline 'vex.json' is an OpenVEX 0.1.0 document, but this run writes OpenVEX 0.2.0",
                    "baseline 'vex.json' has an invalid '@id' 'vex-1', expected an absolute IRI",
                    "baseline 'vex.json' has no 'timestamp', expected an RFC 3339 timestamp",
                    "baseline 'vex.json' has an invalid 'version' '0', expected a whole number from 1 to 2147483646",
                ).map { message -> Failure(message, "omit --baseline to issue a new document") }
        }

        test("renderOpenVexFailure says that no statement applies and names what to change for every reason") {
            val collection = collectOpenVexStatements(file)
            val outcomes = OpenVexEmptyReason.entries.map { OpenVexOutcome.NoStatementApplies(collection, it) }

            val failures = outcomes.flatMap { renderOpenVexFailure(it, "", "--baseline") }

            failures shouldContainExactly
                listOf(
                    "declare 'purls' on the releases you want the document to cover",
                    "tag the vulnerability entries with the tags of the release purls they apply to",
                    "no vulnerability entry and release purl in scope share one of the requested tags",
                    "no vulnerability entry applies to the release in scope",
                    "no vulnerability entry references a release that declares purls",
                ).map { hint -> Failure("no statement applies", hint) }
        }
    })
