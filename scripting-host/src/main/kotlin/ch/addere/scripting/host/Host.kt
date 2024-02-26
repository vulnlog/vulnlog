package ch.addere.scripting.host

import ch.addere.dsl.VulnLog
import ch.addere.scripting.definition.VulnLogScript
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.valueOrNull
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

class Host {
    private val host = BasicJvmScriptingHost()

    fun evalScript(script: String): VulnLog {
        if (script.isEmpty()) {
            return VulnLogScript()
        }
        val result: EvaluationResult = host.evalWithTemplate<VulnLogScript>(script.toScriptSource()).valueOrNull()
            ?: throw IllegalStateException("Could not evaluate script")
        return result.returnValue.scriptInstance as VulnLog
    }
}
