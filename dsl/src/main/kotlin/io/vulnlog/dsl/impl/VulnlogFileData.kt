package io.vulnlog.dsl.impl

import io.vulnlog.dsl.VlBranchValue
import io.vulnlog.dsl.VlReleasePublishedValue
import io.vulnlog.dsl.VlReleaseValue
import io.vulnlog.dsl.VlReporterValue
import io.vulnlog.dsl.VlVariantValue
import io.vulnlog.dsl.VlVulnerabilityData

data class VulnlogFileData(
    val releases: Set<VlReleaseValue>,
    val publishedReleases: Set<VlReleasePublishedValue>,
    val branches: List<VlBranchValue>,
    val productVariants: Set<VlVariantValue>,
    val reporters: Set<VlReporterValue>,
    val vulnerabilities: List<VlVulnerabilityData>,
)
