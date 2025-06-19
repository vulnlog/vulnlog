package dev.vulnlog.dslinterpreter.service

import dev.vulnlog.common.FixedExecutionPerBranch
import dev.vulnlog.common.SuppressionEventExecutionPerBranch
import dev.vulnlog.common.model.VulnEntryPartialStep2
import dev.vulnlog.common.model.VulnStatus
import dev.vulnlog.common.model.VulnStatusAffected
import dev.vulnlog.common.model.VulnStatusFixed
import dev.vulnlog.common.model.VulnStatusNotAffected
import dev.vulnlog.common.model.VulnStatusUnderInvestigation
import dev.vulnlog.common.model.VulnStatusUnknown
import dev.vulnlog.dsl.VlVerdict
import dev.vulnlog.dsl.notAffected
import java.time.LocalDate

interface FindResultStatus {
    fun handle(vulnEntry: VulnEntryPartialStep2): VulnStatus
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
    override fun handle(vulnEntry: VulnEntryPartialStep2): VulnStatus {
        return if (vulnEntry.analysis == null) {
            VulnStatusUnderInvestigation
        } else {
            next?.handle(vulnEntry) ?: VulnStatusUnknown
        }
    }
}

object CheckNotAffected : AbstractFindResultStatus() {
    override fun handle(vulnEntry: VulnEntryPartialStep2): VulnStatus {
        val verdict: VlVerdict = vulnEntry.analysis!!.verdict
        return if (verdict == notAffected) {
            VulnStatusNotAffected
        } else {
            next?.handle(vulnEntry) ?: VulnStatusUnknown
        }
    }
}

object CheckFixed : AbstractFindResultStatus() {
    override fun handle(vulnEntry: VulnEntryPartialStep2): VulnStatus {
        val verdict: VlVerdict = vulnEntry.analysis!!.verdict
        val upcomingReleaseDate: LocalDate? = vulnEntry.involved?.upcoming?.releaseDate
        return if (verdict != notAffected && vulnEntry.execution?.execution is FixedExecutionPerBranch) {
            VulnStatusFixed
        } else if (verdict != notAffected &&
            vulnEntry.execution?.execution is SuppressionEventExecutionPerBranch &&
            upcomingReleaseDate?.isBefore(LocalDate.now()) == true
        ) {
            VulnStatusFixed
        } else {
            next?.handle(vulnEntry) ?: VulnStatusUnknown
        }
    }
}

object CheckAffected : AbstractFindResultStatus() {
    override fun handle(vulnEntry: VulnEntryPartialStep2): VulnStatus {
        val verdict: VlVerdict = vulnEntry.analysis!!.verdict
        val upcomingReleaseDate: LocalDate? = vulnEntry.involved?.upcoming?.releaseDate
        return if (verdict != notAffected && upcomingReleaseDate == null) {
            VulnStatusAffected
        } else if (verdict != notAffected && upcomingReleaseDate?.isAfter(LocalDate.now()) == true) {
            VulnStatusAffected
        } else {
            next?.handle(vulnEntry) ?: VulnStatusUnknown
        }
    }
}
