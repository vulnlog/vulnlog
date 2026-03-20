package dev.vulnlog.cli.model

data class Release(val value: String) {
    init {
        require(value.isNotBlank()) { "Release value cannot be blank" }
    }
}
