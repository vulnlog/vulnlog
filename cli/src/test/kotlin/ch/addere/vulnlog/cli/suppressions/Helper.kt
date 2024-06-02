package ch.addere.vulnlog.cli.suppressions

import ch.addere.vulnlog.core.model.reporter.VlOwaspReporter
import ch.addere.vulnlog.core.model.reporter.VlReporter
import ch.addere.vulnlog.core.model.reporter.VlReporterSet
import ch.addere.vulnlog.core.model.reporter.VlSnykReporter
import ch.addere.vulnlog.core.model.resolution.VlFixInResolution
import ch.addere.vulnlog.core.model.resolution.VlIgnoreResolution
import ch.addere.vulnlog.core.model.resolution.VlResolutionVersionSet
import ch.addere.vulnlog.core.model.resolution.VlSuppressResolution
import ch.addere.vulnlog.core.model.version.VlAffectedVersionSet
import ch.addere.vulnlog.core.model.version.VlReleaseGroup
import ch.addere.vulnlog.core.model.version.VlVersion
import ch.addere.vulnlog.core.model.vulnerability.VlVulnerability

fun versions(vararg versions: String): Set<VlVersion> = versions.map { VlVersion(it) }.toSet()

fun group(
    name: String,
    upcoming: String,
    vararg versions: String,
): VlReleaseGroup {
    return VlReleaseGroup(name, VlVersion(upcoming), versions.map { VlVersion(it) }.toList())
}

fun vulnerability(
    id: String,
    vararg reporters: VlReporter,
    ignore: VlIgnoreResolution? = null,
    suppress: VlSuppressResolution? = null,
    fixIn: VlFixInResolution? = null,
): VlVulnerability {
    return VlVulnerability(id, VlReporterSet(reporters.toSet()), ignore, suppress, fixIn)
}

fun owasp(vararg versions: String) = VlOwaspReporter(VlAffectedVersionSet((versions.map { VlVersion(it) }.toSet())))

fun snyk(
    snykId: String,
    vararg versions: String,
) = VlSnykReporter(snykId, VlAffectedVersionSet((versions.map { VlVersion(it) }.toSet())))

fun ignore(
    vararg versions: String,
    rationale: String,
): VlIgnoreResolution {
    return VlIgnoreResolution(VlResolutionVersionSet(versions.map { VlVersion(it) }.toSet()), rationale)
}

fun suppress(rationale: String): VlSuppressResolution {
    return VlSuppressResolution(VlResolutionVersionSet(emptySet()), rationale)
}
