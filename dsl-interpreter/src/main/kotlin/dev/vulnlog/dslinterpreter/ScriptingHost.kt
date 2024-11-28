package dev.vulnlog.dslinterpreter

import dev.vulnlog.dsl.definition.VulnLogCompilationConfiguration
import dev.vulnlog.dslinterpreter.dsl.VlVulnLogContextValueImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VulnlogFileData
import java.io.File
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.api.isError
import kotlin.script.experimental.api.onFailure
import kotlin.script.experimental.host.BasicScriptingHost
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

class ScriptingHost {
    private val host: BasicScriptingHost = BasicJvmScriptingHost()

    /**
     * Evaluate a Vulnlog DSL script file.
     *
     * @return parsed information in a [VulnlogFileData] or a [ScriptEvaluationException] in case of a parsing error.
     */
    fun eval(script: File): Result<VulnlogFileData> {
        val vulnlogContext = VlVulnLogContextValueImpl()

        fun evalFile(scriptFile: SourceCode): ResultWithDiagnostics<EvaluationResult> {
            val compilationConfiguration = VulnLogCompilationConfiguration
            val evaluationConfiguration = ScriptEvaluationConfiguration { implicitReceivers(vulnlogContext) }
            return host.eval(scriptFile, compilationConfiguration, evaluationConfiguration)
        }

        evalFile(script.toScriptSource())
            .onFailure { result ->
                return Result.failure(
                    ScriptEvaluationException(
                        result.reports.filter(ScriptDiagnostic::isError).map(ScriptDiagnostic::message).first(),
                    ),
                )
            }

        return Result.success(vulnlogContext.build())
    }
}
