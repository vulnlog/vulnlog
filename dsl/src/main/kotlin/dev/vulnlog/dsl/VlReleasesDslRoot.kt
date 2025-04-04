package dev.vulnlog.dsl

import dev.vulnlog.dsl.definition.VlDslMarker

public interface VlReleasesDslRoot {
    /**
     * Define releases.
     *
     * @since v0.5.0
     */
    public fun releases(block: (@VlDslMarker VlReleaseContext).() -> Unit)

    /**
     * Define reporters.
     *
     * @since v0.6.0
     */
    public fun reporters(block: (@VlDslMarker VlReporterContext).() -> Unit)
}
