package dev.vulnlog.suppression

import Filtered
import java.io.File

data class SuppressionConfig(
    val cliVersion: String,
    val releaseBranches: Filtered?,
    val templateDir: File?,
)
