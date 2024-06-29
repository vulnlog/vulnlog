package io.vulnlog.cli.suppressions

typealias SuppressionHead = List<String>
typealias SuppressionTail = List<String>
typealias SuppressionBlock = List<String>

fun SuppressionBlock.pretty(indentation: String): String {
    return this.joinToString(separator = "\n") { "$indentation$it" }
}

data class SuppressionComposition(
    val outputFileName: String,
    val baseIndentation: String,
    val head: SuppressionHead,
    val tail: SuppressionTail,
    val suppressions: Set<SuppressionBlock>,
) {
    fun prettyString(): String =
        head.joinToString(separator = "\n", postfix = "\n") +
            suppressions.joinToString(separator = "\n") { it.pretty(baseIndentation) } +
            if (tail.isEmpty()) "\n" else tail.joinToString(separator = "\n", prefix = "\n", postfix = "\n")
}
