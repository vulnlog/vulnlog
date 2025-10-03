package dev.vulnlog.dslinterpreter.service

import dev.vulnlog.common.model.VulnEntryPartialStep2
import dev.vulnlog.common.model.VulnStatus

class StatusService {
    private val ruleset =
        AbstractFindResultStatus.link(CheckUnderInvestigation, CheckNotAffected, CheckFixed, CheckAffected)

    fun calculateStatus(vulnEntry: VulnEntryPartialStep2): VulnStatus {
        return ruleset.handle(vulnEntry)
    }
}
