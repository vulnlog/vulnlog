package dev.vulnlog.dsl

import dev.vulnlog.dsl.definition.VlDslMarker

public interface VlReportersContext {
    /**
     * Defines a reporter within the DSL context and executes the given block of code
     * within the scope of the `VlReporterContext`.
     *
     * @param reporter The name of the reporter being defined.
     * @param block A lambda with `VlReporterContext` receiver that describes the actions or configurations for the
     * reporter.
     *
     * @since v0.6.0
     */
    public fun reporter(
        reporter: String,
        block: ((@VlDslMarker VlReporterContext).() -> Unit)? = null,
    )
}
