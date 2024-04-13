package ch.addere.cli.suppressions

import ch.addere.dsl.Vulnerability
import java.io.File

abstract class Suppressor(
    private val suppressionFileTemplate: File,
    private val suppressionBlockMarker: String,
) {
    protected abstract val suppressionBlockTemplate: String

    init {
        if (!suppressionFileTemplate.isFile) throw IllegalArgumentException("suppressionFileTemplate must be a file")
        if (suppressionBlockMarker.isBlank()) throw IllegalArgumentException("suppressionBlockMarker cannot be blank")
    }

    fun createSuppressions(vulnerabilities: Set<Vulnerability>): SuppressionComposition {
        val filtered: Set<Vulnerability> = filterRelevant(vulnerabilities)
        val suppressionBlocks: Set<SuppressionBlock> = transform(filtered)
        return generateSuppressionComposition(suppressionBlocks)
    }

    protected abstract fun filterRelevant(vulnerabilities: Set<Vulnerability>): Set<Vulnerability>

    protected abstract fun transform(filtered: Set<Vulnerability>): Set<SuppressionBlock>

    private fun generateSuppressionComposition(suppressionBlocks: Set<SuppressionBlock>): SuppressionComposition {
        suppressionFileTemplate.useLines { lineSequence ->
            val head = mutableListOf<String>()
            val tail = mutableListOf<String>()
            val iterator = lineSequence.iterator()
            var readIntoHead = true
            while (iterator.hasNext()) {
                val line = iterator.next()
                if (line.contains(Regex("\\s+$suppressionBlockMarker"))) {
                    readIntoHead = false
                    continue
                }
                if (readIntoHead) {
                    head.add(line)
                } else {
                    tail.add(line)
                }
            }
            return SuppressionComposition(head, tail, suppressionBlocks)
        }
    }
}
