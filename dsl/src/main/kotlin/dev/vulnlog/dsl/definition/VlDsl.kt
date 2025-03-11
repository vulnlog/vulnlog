package dev.vulnlog.dsl.definition

import kotlin.script.experimental.annotations.KotlinScript

@KotlinScript(
    displayName = "Vulnlog Script Definition",
    fileExtension = "vl.kts",
    compilationConfiguration = VulnlogCompilationConfiguration::class,
)
public interface VlDsl
