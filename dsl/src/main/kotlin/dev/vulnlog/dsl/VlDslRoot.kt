package dev.vulnlog.dsl

import dev.vulnlog.dsl.definition.VlDslMarker

@VlDslMarker
public interface VlDslRoot : VlReleasesDslRoot, VlVulnerabilityDslRoot
