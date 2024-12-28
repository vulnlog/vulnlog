package dev.vulnlog.dsl2.impl

import dev.vulnlog.dsl2.MyEffectiveReporter
import dev.vulnlog.dsl2.VlReleaseBranch
import dev.vulnlog.dsl2.VlVuln

data class Vulnlog2FileData(
    val releases: Set<VlReleaseBranch>,
    val reporters: Set<MyEffectiveReporter>,
    val vulnerabilities: List<VlVuln>,
)
