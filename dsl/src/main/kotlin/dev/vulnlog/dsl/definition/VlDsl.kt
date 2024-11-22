package dev.vulnlog.dsl.definition

import kotlin.script.experimental.annotations.KotlinScript

@KotlinScript(
    displayName = "Vulnerability Logfile",
    fileExtension = "vl.kts",
    compilationConfiguration = VulnLogCompilationConfiguration::class,
)
interface VlDsl
