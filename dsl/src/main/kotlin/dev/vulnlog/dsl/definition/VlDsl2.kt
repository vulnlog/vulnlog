package dev.vulnlog.dsl.definition

import kotlin.script.experimental.annotations.KotlinScript

@KotlinScript(
    displayName = "Vulnerability Logfile",
    fileExtension = "vl2.kts",
    compilationConfiguration = Vulnlog2CompilationConfiguration::class,
)
interface VlDsl2
