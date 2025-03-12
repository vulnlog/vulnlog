package dev.vulnlog.dsl

@Deprecated(message = "Default SCA scanner reporter is deprecated and will be removed in upcoming releases.")
public const val SCA_SCANNER: String = "SCANNER"

public interface VlReporter {
    public val name: String
}

public data class VlDefaultReporter(override val name: String) : VlReporter

/**
 * Snyk Open Source default reporter.
 *
 * @since v0.6.0
 */
public val snykOpenSource: VlDefaultReporter = VlDefaultReporter("Snyk Open Source")

/**
 * OWASP Dependency Check default reporter.
 *
 * @since v0.6.0
 */
public val owaspDependencyCheck: VlDefaultReporter = VlDefaultReporter("OWASP Dependency Check")

/**
 * Semgrep Supply Chain default reporter.
 *
 * @since v0.6.0
 */
public val semgrepSupplyChain: VlDefaultReporter = VlDefaultReporter("Semgrep Supply Chain")
