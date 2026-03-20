package dev.vulnlog.cli.model

data class Project(
    val organization: String,
    val name: String,
    val author: String,
    val contact: String? = null,
)
