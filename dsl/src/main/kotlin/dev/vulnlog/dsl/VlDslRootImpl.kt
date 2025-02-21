package dev.vulnlog.dsl

import dev.vulnlog.dsl.impl.VlVulnerabilityDslRootImpl

class VlDslRootImpl :
    VlDslRoot,
    VlReleasesDslRoot by VlReleasesDslRootImpl(),
    VlVulnerabilityDslRoot by VlVulnerabilityDslRootImpl()
