package dev.vulnlog.dslinterpreter.dsl.impl

import dev.vulnlog.dsl.VlBranchValue
import dev.vulnlog.dsl.VlReleasePublishedValue
import dev.vulnlog.dsl.VlReleaseValue
import dev.vulnlog.dsl.VlReporterValue
import dev.vulnlog.dsl.VlVariantValue
import dev.vulnlog.dslinterpreter.dsl.VlVulnerabilityData

data class VulnlogFileData(
    val releases: Set<VlReleaseValue>,
    val publishedReleases: Set<VlReleasePublishedValue>,
    val branches: List<VlBranchValue>,
    val productVariants: Set<VlVariantValue>,
    val reporters: Set<VlReporterValue>,
    val vulnerabilities: List<VlVulnerabilityData>,
)
