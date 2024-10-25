package io.vulnlog.dsl2.definition

import io.vulnlog.dsl2.impl.VlVulnlogContextImplValue
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.jvm.jvm

object VulnLogEvaluationConfiguration : ScriptEvaluationConfiguration({

    jvm {
        implicitReceivers(VlVulnlogContextImplValue())
    }
})
