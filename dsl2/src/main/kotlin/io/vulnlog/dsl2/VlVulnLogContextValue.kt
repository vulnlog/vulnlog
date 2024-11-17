package io.vulnlog.dsl2

import io.vulnlog.dsl2.definition.VlDsl
import io.vulnlog.dsl2.definition.VlDslMarker

interface VlVulnLogContextValue :
    VlDsl,
    VlDslMarker,
    VlRelease,
    VlLifeCycle,
    VlBranch,
    VlVariant,
    VlReporter,
    VlVulnerability
