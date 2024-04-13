package ch.addere.dsl

data class ReleaseBranch(val name: String, val upComing: Version?, val published: Set<Version>)

class ReleaseBranchBuilder {
    lateinit var name: String
    var upComing: Version? = null
    private val published = mutableSetOf<Version>()

    fun published(block: VersionBuilder.() -> Unit) =
        with(VersionBuilder()) {
            block()
            published.addAll(this.build())
        }

    fun build(): ReleaseBranch = ReleaseBranch(name, upComing, published)
}
