// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.fixtures

import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.PurlEntry
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReleaseEntry
import dev.vulnlog.lib.model.ReportEntry
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Resolution
import dev.vulnlog.lib.model.SchemaVersion
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.TagEntry
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import java.time.LocalDate

fun vulnlogFile(
    releases: List<ReleaseEntry> = emptyList(),
    vulnerabilities: List<VulnerabilityEntry> = emptyList(),
    tags: List<TagEntry> = emptyList(),
    project: Project = Project("org", "project", "author"),
    schemaVersion: SchemaVersion = SchemaVersion.V1,
): VulnlogFile =
    VulnlogFile(
        schemaVersion = schemaVersion,
        project = project,
        tags = tags,
        releases = releases,
        vulnerabilities = vulnerabilities,
    )

fun vulnerability(
    id: VulnId,
    releases: List<Release> = emptyList(),
    packages: List<Purl> = emptyList(),
    reports: List<ReportEntry> = emptyList(),
    aliases: List<VulnId> = emptyList(),
    tags: List<Tag> = emptyList(),
    analyzedAt: LocalDate? = null,
    resolution: Resolution? = null,
    verdict: Verdict = Verdict.UnderInvestigation,
): VulnerabilityEntry =
    VulnerabilityEntry(
        id = id,
        aliases = aliases,
        releases = releases,
        packages = packages,
        reports = reports,
        tags = tags,
        analyzedAt = analyzedAt,
        verdict = verdict,
        resolution = resolution,
    )

fun cve(id: String): VulnId.Cve = VulnId.Cve(id)

fun ghsa(id: String): VulnId.Ghsa = VulnId.Ghsa(id)

fun release(value: String): Release = Release(value)

fun releaseEntry(
    id: String,
    purls: List<PurlEntry> = emptyList(),
): ReleaseEntry = ReleaseEntry(id = Release(id), purls = purls)

fun tag(value: String): Tag = Tag(value)

fun tagEntry(id: String): TagEntry = TagEntry(Tag(id))

fun mavenPurlEntry(
    purl: String,
    tags: List<String> = emptyList(),
): PurlEntry = PurlEntry(Purl.Maven(purl), tags.map(::Tag))

fun report(
    reporter: ReporterType,
    at: LocalDate? = null,
    source: String? = null,
): ReportEntry = ReportEntry(reporter = reporter, at = at, source = source)

fun resolution(release: String): Resolution = Resolution(Release(release))
