package dev.vulnlog.dsl

import dev.vulnlog.dsl.definition.VlDsl
import dev.vulnlog.dsl.definition.VlDslMarker

interface VlVulnLogContextValue :
    VlDsl,
    VlDslMarker,
    VlRelease,
    VlLifeCycle,
    VlBranch,
    VlVariant,
    VlReporter,
    VlVulnerability
