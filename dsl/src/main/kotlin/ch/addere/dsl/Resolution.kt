package ch.addere.dsl

data class Ignore(val reason: String)

class IgnoreBuilder {
    lateinit var reason: String

    fun build() = Ignore(reason)
}

data class Suppression(val reason: String, val inVersion: Set<Version>, val untilVersion: Set<Version>)

class SuppressionBuilder {
    lateinit var reason: String
    private val inVersion = mutableSetOf<Version>()
    private val untilVersion = mutableSetOf<Version>()

    fun inVersion(block: VersionBuilder.() -> Unit) =
        with(VersionBuilder()) {
            block()
            inVersion.addAll(this.build())
        }

    fun untilVersion(block: VersionBuilder.() -> Unit) =
        with(VersionBuilder()) {
            block()
            untilVersion.addAll(this.build())
        }

    fun build() = Suppression(reason, inVersion, untilVersion)
}

data class Mitigation(val fixedIn: Set<Version>)

class MitigationBuilder {
    private val fixedVersions = mutableSetOf<Version>()

    operator fun Version.unaryPlus() = fixedVersions.add(this)

    fun build() = Mitigation(fixedVersions)
}

data class Resolution(val ignore: Ignore?, val suppress: Suppression?, val mitigate: Mitigation?)

class ResolutionBuilder {
    var ignore: Ignore? = null
    var suppress: Suppression? = null
    var mitigate: Mitigation? = null

    fun ignore(block: IgnoreBuilder.() -> Unit) =
        with(IgnoreBuilder()) {
            block()
            ignore = this.build()
        }

    fun suppress(block: SuppressionBuilder.() -> Unit) =
        with(SuppressionBuilder()) {
            block()
            suppress = this.build()
        }

    fun mitigate(block: MitigationBuilder.() -> Unit) =
        with(MitigationBuilder()) {
            block()
            mitigate = this.build()
        }

    fun build() = Resolution(ignore, suppress, mitigate)
}
