package dev.vulnlog.dsl2

import java.time.LocalDate

typealias MyAnalysis = () -> VlVulnerabilityRateContext

infix fun MyAnalysis.update(dependency: String): MyUpdateResolutionTask {
    return MyUpdateResolutionTask(invoke(), dependency)
}

class MyRatingReason(
    private val reportFor: VlReportFor,
    private val ratedAt: LocalDate,
    private val rating: Rating,
) {
    infix fun because(reasoning: String): VlVulnerabilityRateContext {
        return RatedVulnerabilityRateContext(reportFor, ratedAt, rating, reasoning)
    }
}

class MyAnalysedAt(private val reportFor: VlReportFor, private val at: LocalDate) {
    infix fun asRating(rating: Rating): MyRatingReason {
        return MyRatingReason(reportFor, at, rating)
    }
}

sealed class Rating {
    abstract val asText: String

    override fun toString(): String {
        return "Rating(asText='$asText')"
    }
}

object Low : Rating() {
    override val asText = "low"
}

object Moderate : Rating() {
    override val asText = "moderate"
}

object High : Rating() {
    override val asText = "high"
}

object Critical : Rating() {
    override val asText = "critical"
}

object NotAffected : Rating() {
    override val asText = "not affected"
}
