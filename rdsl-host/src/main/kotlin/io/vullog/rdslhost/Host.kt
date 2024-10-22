package io.vullog.rdslhost

import io.vulnlog.rdsl.definition.VulnLogRscript
import io.vulnlog.rdsl.dsl.Base
import java.io.File
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.valueOrNull
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

class Host {
    private val scriptingHost = BasicJvmScriptingHost()

    fun run() {
        val wholeScript = File("test.repscript.kts").readText(Charsets.UTF_8)
        evalScript(wholeScript)
//        evalScript("")
    }

    fun evalScript(script: String): VulnLogRscript =
        if (script.isEmpty()) {
            Base()
        } else {
            vulnLogScript(script.toScriptSource())
        }

    fun evalScript(script: File): VulnLogRscript = vulnLogScript(script.toScriptSource())

    private fun vulnLogScript(sourceCode: SourceCode): VulnLogRscript {
        val evalWithTemplate = scriptingHost.evalWithTemplate<VulnLogRscript>(sourceCode)
        val valueOrNull = evalWithTemplate.valueOrNull()
        val result: EvaluationResult =
            valueOrNull
                ?: error("Could not evaluate script")
        return result.returnValue.scriptInstance as VulnLogRscript
    }
}
