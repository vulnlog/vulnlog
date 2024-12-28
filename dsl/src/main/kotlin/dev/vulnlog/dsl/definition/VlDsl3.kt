package dev.vulnlog.dsl.definition

import kotlin.script.experimental.annotations.KotlinScript

@KotlinScript(
    displayName = "Vulnerability Logfile",
    fileExtension = "vl3.kts",
    compilationConfiguration = Vulnlog3CompilationConfiguration::class,
)
interface VlDsl3
