package dev.vulnlog.dslinterpreter

import dev.vulnlog.dsl.definition.VulnLogCompilationConfiguration
import dev.vulnlog.dslinterpreter.dsl.VlVulnLogContextValueImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VulnlogFileData
import java.io.File
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.enableScriptsInstancesSharing
import kotlin.script.experimental.api.hostConfiguration
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.api.isError
import kotlin.script.experimental.api.onFailure
import kotlin.script.experimental.api.with
import kotlin.script.experimental.host.BasicScriptingHost
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.compilationCache
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.CompiledScriptJarsCache

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
            val compilationConfiguration =
                VulnLogCompilationConfiguration.with {
                    hostConfiguration(
                        ScriptingHostConfiguration {
                            jvm {
                                val potPath = Path.of(System.getProperty("java.io.tmpdir")).resolve("vulnlog-cache")
                                val cacheBaseDir =
                                    if (!potPath.exists()) potPath.createDirectory().toFile() else potPath.toFile()
                                compilationCache(
                                    CompiledScriptJarsCache { script, scriptCompilationConfiguration ->
                                        val filename =
                                            compiledScriptUniqueName(script, scriptCompilationConfiguration) + ".jar"
                                        File(cacheBaseDir, filename)
                                    },
                                )
                            }
                        },
                    )
                }
            val evaluationConfiguration =
                ScriptEvaluationConfiguration {
                    implicitReceivers(vulnlogContext)
                    enableScriptsInstancesSharing()
                }
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

private fun compiledScriptUniqueName(
    script: SourceCode,
    scriptCompilationConfiguration: ScriptCompilationConfiguration,
): String {
    val digestWrapper = MessageDigest.getInstance("MD5")
    digestWrapper.update(script.text.toByteArray())
    scriptCompilationConfiguration.notTransientData.entries
        .sortedBy { it.key.name }
        .forEach {
            digestWrapper.update(it.key.name.toByteArray())
            digestWrapper.update(it.value.toString().toByteArray())
        }
    return digestWrapper.digest().toHexString()
}

private fun ByteArray.toHexString(): String = joinToString("", transform = { "%02x".format(it) })
