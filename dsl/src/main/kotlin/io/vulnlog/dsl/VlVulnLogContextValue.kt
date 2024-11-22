package io.vulnlog.dsl

import io.vulnlog.dsl.definition.VlDsl
import io.vulnlog.dsl.definition.VlDslMarker

interface VlVulnLogContextValue :
    VlDsl,
    VlDslMarker,
    VlRelease,
    VlLifeCycle,
    VlBranch,
    VlVariant,
    VlReporter,
    VlVulnerability
