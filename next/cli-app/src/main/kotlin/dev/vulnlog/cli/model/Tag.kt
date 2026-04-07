package dev.vulnlog.cli.model

data class Tag(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Tag value cannot be blank" }
    }
}
