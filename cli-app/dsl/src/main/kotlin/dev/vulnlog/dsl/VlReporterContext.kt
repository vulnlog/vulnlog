package dev.vulnlog.dsl

import dev.vulnlog.dsl.definition.VlDslMarker

public interface VlReporterContext {
    public fun suppression(block: (@VlDslMarker VlSuppressContext).() -> Unit)
}
