package io.vulnlog.dsl2interpreter

import io.vulnlog.dsl2.data.VlData
import io.vulnlog.dsl2.definition.VulnLogCompilationConfiguration
import io.vulnlog.dsl2.impl.VlVulnlogContextImplValue
import java.io.File
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

class ScriptingHost {
    fun eval(script: File): VlData {
        val recipe = VlVulnlogContextImplValue()

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
