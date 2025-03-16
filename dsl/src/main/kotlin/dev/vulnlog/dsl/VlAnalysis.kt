package dev.vulnlog.dsl

import java.time.LocalDate

public interface VulnlogAnalysisData {
    public val analysedAt: LocalDate
    public val verdict: VerdictSpecification
    public val reasoning: String
}

public sealed interface VerdictSpecification {
    public val level: String
}

internal data object VerdictCritical : VerdictSpecification {
    override val level: String = "critical"
}

/**
 * Vulnerability analysis resulted in critical impact to software project.
 *
 * @since v0.6.0
 */
public val critical: VerdictSpecification = VerdictCritical

internal data object VerdictHigh : VerdictSpecification {
    override val level: String = "high"
}

/**
 * Vulnerability analysis revealed high impact on software project.
 *
 * @since v0.6.0
 */
public val high: VerdictSpecification = VerdictHigh

internal data object VerdictModerate : VerdictSpecification {
    override val level: String = "moderate"
}

/**
 * Vulnerability analysis revealed moderate impact on software project.
 *
 * @since v0.6.0
 */
public val moderate: VerdictSpecification = VerdictModerate

internal data object VerdictLow : VerdictSpecification {
    override val level: String = "low"
}

/**
 * Vulnerability analysis revealed low impact on software project.
 *
 * @since v0.6.0
 */
public val low: VerdictSpecification = VerdictLow

public data object VerdictNotAffected : VerdictSpecification {
    override val level: String = "not affected"
}

/**
 * Vulnerability analysis revealed that the vulnerability does not affect the software project.
 *
 * @since v0.6.0
 */
public val notAffected: VerdictSpecification = VerdictNotAffected

public interface Verdict {
    /**
     * A verdict based on the analysis of the report on the software project.
     *
     * @since v0.5.0
     */
    @Deprecated(
        message = "The method verdict(String) is deprecated. Use the method verdict(VerdictSpecification) instead.",
    )
    public infix fun verdict(verdict: String): VlAnalyseReasoningStep

    /**
     * A verdict based on the analysis of the report on the software project.
     *
     * @since v0.6.0
     */
    public infix fun verdict(verdict: VerdictSpecification): VlAnalyseReasoningStep
}

public interface VlAnalyseInitStep : Verdict {
    /**
     * A date string in the format YYYY-MM-dd, e.g. `2025-03-07`. If not specified the date of the report is used.
     *
     * @since v0.5.0
     */
    public infix fun analysedAt(date: String): VlAnalyseVerdictStep
}

public interface VlAnalyseVerdictStep : Verdict

public interface VlAnalyseReasoningStep {
    /**
     * The reasoning why the verdict was chosen.
     *
     * @param reasoning description why the analysis lead to the specified verdict.
     * @since v0.5.0
     */
    public infix fun because(reasoning: String): VlTaskInitStep
}
