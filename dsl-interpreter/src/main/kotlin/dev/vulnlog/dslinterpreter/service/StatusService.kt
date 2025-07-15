package dev.vulnlog.dslinterpreter.service

import dev.vulnlog.common.model.VulnEntryNonIdData
import dev.vulnlog.common.model.VulnStatus

class StatusService {
    private val ruleset =
        AbstractFindResultStatus.link(CheckUnderInvestigation, CheckNotAffected, CheckFixed, CheckAffected)

    fun calculateStatus(vulnEntry: VulnEntryNonIdData): VulnStatus {
        return ruleset.handle(vulnEntry)
    }
}
