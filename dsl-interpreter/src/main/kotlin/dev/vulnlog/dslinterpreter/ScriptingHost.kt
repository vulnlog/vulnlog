package dev.vulnlog.dslinterpreter

import dev.vulnlog.dsl.ReporterData
import dev.vulnlog.dsl.VlDslRoot
import dev.vulnlog.dsl.VulnerabilityData
import dev.vulnlog.dsl.definition.VulnlogCompilationConfiguration
import dev.vulnlog.dslinterpreter.impl.VlDslRootImpl
import dev.vulnlog.dslinterpreter.impl.VlReleasesDslRootImpl
import dev.vulnlog.dslinterpreter.impl.VlVulnerabilityDslRootImpl
import dev.vulnlog.dslinterpreter.repository.BranchRepositoryImpl
import dev.vulnlog.dslinterpreter.service.AffectedVersionsServiceImpl
import dev.vulnlog.dslinterpreter.service.VulnerabilityServiceImpl
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

    fun eval(scripts: List<File>): Result<VlDslRoot> {
        val branchRepository = BranchRepositoryImpl()
        val reporterRepository = mutableListOf<ReporterData>()
        val vulnerabilityDataRepository = mutableListOf<VulnerabilityData>()
        val affectedVersionsService = AffectedVersionsServiceImpl(branchRepository)
        val vulnerabilityService = VulnerabilityServiceImpl(affectedVersionsService, vulnerabilityDataRepository)

        val dslRoot =
            VlDslRootImpl(
                branchRepository,
                vulnerabilityDataRepository,
                VlReleasesDslRootImpl(branchRepository, reporterRepository),
                VlVulnerabilityDslRootImpl(vulnerabilityService),
            )

        fun evalFile(scriptFile: SourceCode): ResultWithDiagnostics<EvaluationResult> {
            val compilationConfiguration =
                VulnlogCompilationConfiguration.with {
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
                    implicitReceivers(dslRoot)
                    enableScriptsInstancesSharing()
                }
            return host.eval(scriptFile, compilationConfiguration, evaluationConfiguration)
        }

        scripts.forEach { script ->
            evalFile(script.toScriptSource())
                .onFailure { result ->
                    return Result.failure(
                        ScriptEvaluationException(
                            result.reports.filter(ScriptDiagnostic::isError).map(ScriptDiagnostic::message).first(),
                        ),
                    )
                }
        }
        return Result.success(dslRoot)
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
