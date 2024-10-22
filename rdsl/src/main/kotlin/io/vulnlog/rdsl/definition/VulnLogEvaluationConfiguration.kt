package io.vulnlog.rdsl.definition

import io.vulnlog.rdsl.dsl.Base
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.jvm.jvm

object VulnLogEvaluationConfiguration : ScriptEvaluationConfiguration({

    jvm {
        implicitReceivers(Base())
    }
})
