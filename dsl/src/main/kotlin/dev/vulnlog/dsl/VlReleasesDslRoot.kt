package dev.vulnlog.dsl

import dev.vulnlog.dsl.definition.VlDslMarker

public interface VlReleasesDslRoot {
    /**
     * Defines a set of releases within the DSL context, allowing releases and their details to be specified.
     *
     * @param block A lambda with receiver of type [VlReleaseContext] that provides the DSL for defining releases.
     * @since v0.5.0
     */
    public fun releases(block: (@VlDslMarker VlReleaseContext).() -> Unit)

    /**
     * Define reporters in the context of the vulnerability reporting DSL.
     *
     * @param block A lambda with receiver of type [VlReportersContext] that provides the DSL for defining reporters.
     * @since v0.6.0
     */
    public fun reporters(block: (@VlDslMarker VlReportersContext).() -> Unit)
}
