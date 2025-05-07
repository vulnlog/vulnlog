package dev.vulnlog.cli.service

import dev.vulnlog.dsl.ResultStatus
import dev.vulnlog.dsl.VerdictSpecification
import dev.vulnlog.dsl.notAffected
import dev.vulnlog.dslinterpreter.splitter.FixedExecutionPerBranch
import dev.vulnlog.dslinterpreter.splitter.SuppressionEventExecutionPerBranch
import dev.vulnlog.dslinterpreter.splitter.VulnerabilityDataPerBranch
import java.time.LocalDate

interface FindResultStatus {
    fun handle(vulnerability: VulnerabilityDataPerBranch): ResultStatus
}

abstract class AbstractFindResultStatus : FindResultStatus {
    protected var next: AbstractFindResultStatus? = null

    companion object {
        fun link(
            first: AbstractFindResultStatus,
            vararg chain: AbstractFindResultStatus,
        ): AbstractFindResultStatus {
            var head: AbstractFindResultStatus = first
            for (nextInChain in chain) {
                head.next = nextInChain
                head = nextInChain
            }
            return first
        }
    }
}

object CheckUnderInvestigation : AbstractFindResultStatus() {
    override fun handle(vulnerability: VulnerabilityDataPerBranch): ResultStatus {
        return if (vulnerability.analysisData == null) {
            ResultStatus.UNDER_INVESTIGATION
        } else {
            next?.handle(vulnerability) ?: ResultStatus.UNKNOWN
        }
    }
}

object CheckNotAffected : AbstractFindResultStatus() {
    override fun handle(vulnerability: VulnerabilityDataPerBranch): ResultStatus {
        val verdict: VerdictSpecification = vulnerability.analysisData!!.verdict
        return if (verdict == notAffected) {
            ResultStatus.NOT_AFFECTED
        } else {
            next?.handle(vulnerability) ?: ResultStatus.UNKNOWN
        }
    }
}

object CheckFixed : AbstractFindResultStatus() {
    override fun handle(vulnerability: VulnerabilityDataPerBranch): ResultStatus {
        val verdict: VerdictSpecification = vulnerability.analysisData!!.verdict
        val upcomingReleaseDate: LocalDate? = vulnerability.involvedReleaseVersions?.upcoming?.releaseDate
        return if (verdict != notAffected && vulnerability.executionData?.execution is FixedExecutionPerBranch) {
            ResultStatus.FIXED
        } else if (verdict != notAffected &&
            vulnerability.executionData?.execution is SuppressionEventExecutionPerBranch &&
            upcomingReleaseDate?.isBefore(LocalDate.now()) == true
        ) {
            ResultStatus.FIXED
        } else {
            next?.handle(vulnerability) ?: ResultStatus.UNKNOWN
        }
    }
}

object CheckAffected : AbstractFindResultStatus() {
    override fun handle(vulnerability: VulnerabilityDataPerBranch): ResultStatus {
        val verdict: VerdictSpecification = vulnerability.analysisData!!.verdict
        val upcomingReleaseDate: LocalDate? = vulnerability.involvedReleaseVersions?.upcoming?.releaseDate
        return if (verdict != notAffected && upcomingReleaseDate == null) {
            ResultStatus.AFFECTED
        } else if (verdict != notAffected && upcomingReleaseDate?.isAfter(LocalDate.now()) == true) {
            ResultStatus.AFFECTED
        } else {
            next?.handle(vulnerability) ?: ResultStatus.UNKNOWN
        }
    }
}

val ruleSet: AbstractFindResultStatus =
    AbstractFindResultStatus.link(CheckUnderInvestigation, CheckNotAffected, CheckFixed, CheckAffected)
