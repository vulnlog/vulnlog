package dev.vulnlog.suppression

import java.io.File

data class SuppressionConfig(
    val cliVersion: String,
    val templateDir: File,
)
