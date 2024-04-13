package ch.addere.dsl

abstract class Scanner(val name: String, open val affected: Set<Version>)

abstract class ScannerBuilder {
    lateinit var name: String
    lateinit var affected: Set<Version>

    fun affected(block: VersionBuilder.() -> Unit) =
        with(VersionBuilder()) {
            block()
            affected = this.build()
        }

    abstract fun build(): Scanner
}
