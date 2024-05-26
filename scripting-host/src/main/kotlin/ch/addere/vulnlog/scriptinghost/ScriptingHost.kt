package ch.addere.vulnlog.scriptinghost

import ch.addere.vulnlog.scriptdefinition.VulnLogScript
import java.io.File
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.valueOrNull
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

class ScriptingHost {
    private val host = BasicJvmScriptingHost()

    fun evalScript(script: String): VulnLogScript {
        if (script.isEmpty()) {
            return VulnLogScript()
        }
        val result: EvaluationResult =
            host.evalWithTemplate<VulnLogScript>(script.toScriptSource()).valueOrNull()
                ?: error("Could not evaluate script")

        return result.returnValue.scriptInstance as VulnLogScript
    }

    fun evalScript(script: File): VulnLogScript {
        val result: EvaluationResult =
            host.evalWithTemplate<VulnLogScript>(script.toScriptSource()).valueOrNull()
                ?: error("Could not evaluate script")
        return result.returnValue.scriptInstance as VulnLogScript
    }
}
