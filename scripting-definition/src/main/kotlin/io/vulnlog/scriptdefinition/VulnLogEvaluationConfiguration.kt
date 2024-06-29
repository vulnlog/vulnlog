package io.vulnlog.scriptdefinition

import io.vulnlog.dsl.VlVulnerabilitySetBlock
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.jvm.jvm

object VulnLogEvaluationConfiguration : ScriptEvaluationConfiguration({

    jvm {
        implicitReceivers(VlVulnerabilitySetBlock())
    }
})
