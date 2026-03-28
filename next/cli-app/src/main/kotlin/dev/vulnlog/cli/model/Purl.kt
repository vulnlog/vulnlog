package dev.vulnlog.cli.model

sealed interface Purl {
    val value: String

    data class Cargo(override val value: String) : Purl

    data class Deb(override val value: String) : Purl

    data class Docker(override val value: String) : Purl

    data class Gem(override val value: String) : Purl

    data class Generic(override val value: String) : Purl

    data class Golang(override val value: String) : Purl

    data class Maven(override val value: String) : Purl

    data class Npm(override val value: String) : Purl

    data class Nuget(override val value: String) : Purl

    data class Pypi(override val value: String) : Purl

    data class Rpm(override val value: String) : Purl
}
