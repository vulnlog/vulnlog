package dev.vulnlog.dsl2

import dev.vulnlog.dsl.definition.VlDsl2
import dev.vulnlog.dsl.definition.VlDslMarker

interface VlVulnlogContext :
    VlDsl2,
    VlDslMarker,
    VlCreateReporter,
    VlRelease,
    VlCreateVulnerability
