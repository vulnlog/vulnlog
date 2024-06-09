package ch.addere.vulnlog.scriptdefinition

import ch.addere.vulnlog.dsl.VlVulnerabilitySetBlock
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.jvm.jvm

object VulnLogEvaluationConfiguration : ScriptEvaluationConfiguration({

    jvm {
        implicitReceivers(VlVulnerabilitySetBlock())
    }
})
