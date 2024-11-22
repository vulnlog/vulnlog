package dev.vulnlog.dslinterpreter

import dev.vulnlog.dsl.VlVulnLogContextValueImpl
import dev.vulnlog.dsl.definition.VulnLogCompilationConfiguration
import dev.vulnlog.dsl.impl.VulnlogFileData
import java.io.File
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

class ScriptingHost {
    fun eval(script: File): VulnlogFileData {
        val recipe = VlVulnLogContextValueImpl()

        fun evalFile(scriptFile: SourceCode): ResultWithDiagnostics<EvaluationResult> {
            val compilationConfiguration = VulnLogCompilationConfiguration

            val evaluationConfiguration =
                ScriptEvaluationConfiguration {
                    implicitReceivers(recipe)
                }

            return BasicJvmScriptingHost().eval(scriptFile, compilationConfiguration, evaluationConfiguration)
        }

        val res =
            when (val result = evalFile(script.toScriptSource())) {
                is ResultWithDiagnostics.Failure -> {
                    result.reports.forEach {
                        println("Error : ${it.message}" + if (it.exception == null) "" else ": ${it.exception}")
                    }
                    recipe.build()
                }

                is ResultWithDiagnostics.Success -> {
                    recipe.build()
                }
            }
        return res
    }
}
