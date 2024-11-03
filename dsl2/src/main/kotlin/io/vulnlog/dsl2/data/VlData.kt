package io.vulnlog.dsl2.data

import io.vulnlog.dsl2.VlBranchBuilder
import io.vulnlog.dsl2.VlLifeCycleValue
import io.vulnlog.dsl2.VlReleasePublishedValue
import io.vulnlog.dsl2.VlReleaseValue
import io.vulnlog.dsl2.VlReporterValue
import io.vulnlog.dsl2.VlVariantValue

data class VlData(
    val releases: Set<VlReleaseValue>,
    val publishedReleases: Set<VlReleasePublishedValue>,
    val lifeCyclePhaseData: Set<VlLifeCycleData>,
    val lifeCycles: Set<VlLifeCycleValue>,
    val branches: Set<VlBranchBuilder>,
    val productVariants: MutableSet<VlVariantValue>,
    val reporters: MutableSet<VlReporterValue>,
    val vulnerabilities: MutableList<VlVulnerabilityData>,
)
