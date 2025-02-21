package dev.vulnlog.dsl

import dev.vulnlog.dsl.definition.VlDslMarker

interface VlDslRoot : VlDslMarker, VlReleasesDslRoot, VlVulnerabilityDslRoot
