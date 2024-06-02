package ch.addere.vulnlog.cli.suppressions

typealias SuppressionHead = List<String>
typealias SuppressionTail = List<String>
typealias SuppressionBlock = List<String>

fun SuppressionBlock.pretty(indentation: String): String {
    return this.joinToString(separator = "\n") { "$indentation$it" }
}

data class SuppressionComposition(
    val head: SuppressionHead,
    val tail: SuppressionTail,
    val suppressions: Set<SuppressionBlock>,
) {
    fun pretty(
        before: String = "\n",
        between: String = "\n",
        after: String = "\n",
        indentation: String = "",
    ): List<String> {
        return head + before + suppressions.joinToString(separator = between) { it.pretty(indentation) } + after + tail
    }
}
