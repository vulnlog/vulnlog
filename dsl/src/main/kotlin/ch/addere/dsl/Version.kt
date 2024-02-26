package ch.addere.dsl

data class Version(val major: Int, val minor: Int, val patch: Int)

class VersionBuilder {
    private val versions = mutableSetOf<Version>()
    operator fun Version.unaryPlus() = versions.add(this)
    fun build() = versions
}
