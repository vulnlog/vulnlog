package dev.vulnlog.dsl

import dev.vulnlog.dsl.util.toCamelCase

@Deprecated(message = "Default SCA scanner reporter is deprecated and will be removed in upcoming releases.")
public const val SCA_SCANNER: String = "SCANNER"

public interface VlReporter {
    public val name: String

    /**
     * Returns a providable representation of the release branch name.
     */
    public fun providerName(): String {
        return name.toCamelCase()
    }
}

public sealed interface ReporterData {
    public val name: String
}

public data class ReporterDataImpl(override val name: String) : ReporterData

public data object DefaultReporterDataImpl : ReporterData {
    override val name: String = "Default Reporter Data"
}

public data class VlReporterImpl(override val name: String) : VlReporter
