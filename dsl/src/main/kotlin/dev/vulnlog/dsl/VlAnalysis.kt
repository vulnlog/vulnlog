package dev.vulnlog.dsl

import java.time.LocalDate

public interface VulnlogAnalysisData {
    public val analysedAt: LocalDate
    public val verdict: VlVerdict
    public val reasoning: String
}

public sealed interface VlVerdict {
    public val level: String
}

internal data object VlVerdictCritical : VlVerdict {
    override val level: String = "critical"
}

/**
 * Vulnerability analysis resulted in critical impact to software project.
 *
 * @since v0.6.0
 */
public val critical: VlVerdict = VlVerdictCritical

internal data object VlVerdictHigh : VlVerdict {
    override val level: String = "high"
}

/**
 * Vulnerability analysis revealed high impact on software project.
 *
 * @since v0.6.0
 */
public val high: VlVerdict = VlVerdictHigh

internal data object VlVerdictModerate : VlVerdict {
    override val level: String = "moderate"
}

/**
 * Vulnerability analysis revealed moderate impact on software project.
 *
 * @since v0.6.0
 */
public val moderate: VlVerdict = VlVerdictModerate

internal data object VlVerdictLow : VlVerdict {
    override val level: String = "low"
}

/**
 * Vulnerability analysis revealed low impact on software project.
 *
 * @since v0.6.0
 */
public val low: VlVerdict = VlVerdictLow

public data object VlVerdictNotAffected : VlVerdict {
    override val level: String = "not affected"
}

/**
 * Vulnerability analysis revealed that the vulnerability does not affect the software project.
 *
 * @since v0.6.0
 */
public val notAffected: VlVerdict = VlVerdictNotAffected

public interface Verdict {
    /**
     * A [verdict] based on the analysis of the report on the software project.
     *
     * @since v0.6.0
     */
    public infix fun verdict(verdict: VlVerdict): VlAnalyseReasoningState
}

/**
 * Represents the initial analysis step of the vulnerability analysis process within the DSL.
 * Extends the Verdict interface.
 *
 * Allows specifying the date of the analysis.
 */
public interface VlAnalyseInitState : Verdict {
    /**
     * A date string in the format YYYY-MM-dd, e.g. `2025-03-07`. If not specified the date of the report is used.
     *
     * @since v0.5.0
     */
    public infix fun analysedAt(date: String): VlAnalyseVerdictState
}

/**
 * This interface represents a specific state within the vulnerability analysis DSL.
 * It extends the base `verdict` behavior and provides a focus on analysis-related
 * verdicts for software project reports.
 *
 * It serves as a part of the fluent interface design to chain analysis verdict
 * actions within the vulnerability reporting and task management DSL.
 */
public interface VlAnalyseVerdictState : Verdict

/**
 * Represents the reasoning state within the analysis DSL. This is used to specify the rationale behind
 * a verdict choice during the analysis process.
 */
public interface VlAnalyseReasoningState {
    /**
     * The reasoning why the verdict was chosen.
     *
     * @param reasoning description why the analysis leads to the specified verdict.
     * @since v0.5.0
     */
    public infix fun because(reasoning: String): VlTaskInitState
}
