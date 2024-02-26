package ch.addere.dsl

data class ResolutionUpdated(val versions: List<Version>)

class ResolutionUpdatedBuilder {
    val versions = mutableListOf<Version>()

    operator fun Version.unaryPlus() = versions.add(this)

    fun build(): ResolutionUpdated = ResolutionUpdated(versions)
}
