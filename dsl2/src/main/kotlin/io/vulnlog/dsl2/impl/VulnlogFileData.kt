package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlBranchValue
import io.vulnlog.dsl2.VlReleasePublishedValue
import io.vulnlog.dsl2.VlReleaseValue
import io.vulnlog.dsl2.VlReporterValue
import io.vulnlog.dsl2.VlVariantValue
import io.vulnlog.dsl2.VlVulnerabilityData

data class VulnlogFileData(
    val releases: Set<VlReleaseValue>,
    val publishedReleases: Set<VlReleasePublishedValue>,
    val branches: List<VlBranchValue>,
    val productVariants: Set<VlVariantValue>,
    val reporters: Set<VlReporterValue>,
    val vulnerabilities: List<VlVulnerabilityData>,
)
