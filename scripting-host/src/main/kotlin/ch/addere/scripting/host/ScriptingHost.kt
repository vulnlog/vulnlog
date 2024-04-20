package ch.addere.scripting.host

import ch.addere.dsl.VulnLog
import ch.addere.scripting.definition.VulnLogScript
import java.io.File
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.valueOrNull
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

class ScriptingHost {
    private val host = BasicJvmScriptingHost()

    fun evalScript(script: String): VulnLog {
        if (script.isEmpty()) {
            return VulnLogScript()
        }
        val result: EvaluationResult =
            host.evalWithTemplate<VulnLogScript>(script.toScriptSource()).valueOrNull()
                ?: error("Could not evaluate script")
        return result.returnValue.scriptInstance as VulnLog
    }

    fun evalScript(script: File): VulnLog {
        val result: EvaluationResult =
            host.evalWithTemplate<VulnLogScript>(script.toScriptSource()).valueOrNull()
                ?: error("Could not evaluate script")
        return result.returnValue.scriptInstance as VulnLog
    }
}
