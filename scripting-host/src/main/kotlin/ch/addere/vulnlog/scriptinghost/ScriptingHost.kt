package ch.addere.vulnlog.scriptinghost

import ch.addere.vulnlog.scriptdefinition.VulnLogScript
import java.io.File
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.valueOrNull
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

class ScriptingHost {
    private val host = BasicJvmScriptingHost()

    fun evalScript(script: String): VulnLogScript =
        if (script.isEmpty()) {
            VulnLogScript()
        } else {
            vulnLogScript(script.toScriptSource())
        }

    fun evalScript(script: File): VulnLogScript = vulnLogScript(script.toScriptSource())

    private fun vulnLogScript(sourceCode: SourceCode): VulnLogScript {
        val result: EvaluationResult =
            host.evalWithTemplate<VulnLogScript>(sourceCode).valueOrNull()
                ?: error("Could not evaluate script")
        return result.returnValue.scriptInstance as VulnLogScript
    }
}
