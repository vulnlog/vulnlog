package dev.vulnlog.dslinterpreter.impl

import dev.vulnlog.dsl.VlDslRoot
import dev.vulnlog.dsl.VlReleasesDslRoot
import dev.vulnlog.dsl.VlVulnerabilityDslRoot

class VlDslRootImpl :
    VlDslRoot,
    VlReleasesDslRoot by VlReleasesDslRootImpl(),
    VlVulnerabilityDslRoot by VlVulnerabilityDslRootImpl()
