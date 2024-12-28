package dev.vulnlog.dsl.definition

import dev.vulnlog.dsl2.VlVulnlogContext
import java.io.File
import kotlin.script.experimental.api.RefineScriptCompilationConfigurationHandler
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptAcceptedLocation
import kotlin.script.experimental.api.ScriptCollectedData
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptConfigurationRefinementContext
import kotlin.script.experimental.api.acceptedLocations
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.foundAnnotations
import kotlin.script.experimental.api.ide
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.api.importScripts
import kotlin.script.experimental.api.refineConfiguration
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm

object Vulnlog2CompilationConfiguration : ScriptCompilationConfiguration(
    {
        defaultImports(Import::class)
        implicitReceivers(VlVulnlogContext::class)

        jvm {
            dependenciesFromCurrentContext(wholeClasspath = true)
            defaultImports("dev.vulnlog.dsl.*", "dev.vulnlog.dsl2.*")
        }

        refineConfiguration {
            onAnnotations(Import::class, handler = AnnotationRefinements())
        }

        ide {
            acceptedLocations(ScriptAcceptedLocation.Everywhere)
        }
    },
)

class AnnotationRefinements : RefineScriptCompilationConfigurationHandler {
    override operator fun invoke(
        context: ScriptConfigurationRefinementContext,
    ): ResultWithDiagnostics<ScriptCompilationConfiguration> = processAnnotations(context)

    private fun processAnnotations(
        context: ScriptConfigurationRefinementContext,
    ): ResultWithDiagnostics<ScriptCompilationConfiguration> {
        val annotations =
            context.collectedData?.get(ScriptCollectedData.foundAnnotations)?.takeIf { it.isNotEmpty() }
                ?: return context.compilationConfiguration.asSuccess()

        val scriptBaseDir = (context.script as? FileBasedScriptSource)?.file?.parentFile
        val importedSources =
            annotations.flatMap {
                (it as? Import)?.paths?.map { sourceName ->
                    FileScriptSource(scriptBaseDir?.resolve(sourceName) ?: File(sourceName))
                } ?: emptyList()
            }

        return ScriptCompilationConfiguration(context.compilationConfiguration) {
            if (importedSources.isNotEmpty()) importScripts.append(importedSources)
        }.asSuccess()
    }
}
