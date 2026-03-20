package dev.vulnlog.cli.model

data class VulnlogFileContext(
    val validationVersion: ParseValidationVersion,
    val fileName: String,
    val vulnlogFile: VulnlogFile,
)
