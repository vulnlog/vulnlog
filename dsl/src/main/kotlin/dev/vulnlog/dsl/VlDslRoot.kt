package dev.vulnlog.dsl

import dev.vulnlog.dsl.definition.VlDslMarker

@VlDslMarker
interface VlDslRoot : VlReleasesDslRoot, VlVulnerabilityDslRoot
