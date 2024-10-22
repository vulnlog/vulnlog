package io.vulnlog.rdsl.definition

import kotlin.script.experimental.annotations.KotlinScript

@KotlinScript(
    displayName = "Vulnerability Log Reporting",
    fileExtension = "repscript.kts",
    compilationConfiguration = VulnLogCompilationConfiguration::class,
    evaluationConfiguration = VulnLogEvaluationConfiguration::class,
)
abstract class VulnLogRscript
