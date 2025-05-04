package dev.vulnlog.dsl

import dev.vulnlog.dsl.util.toCamelCase

public interface VlReporter {
    public val name: String

    /**
     * Returns a providable representation of the release branch name.
     */
    public fun providerName(): String {
        return name.toCamelCase()
    }
}

public interface ReporterData {
    public val name: String
}

public data class VlReporterImpl(override val name: String, val config: VlReporterConfig?) : VlReporter
