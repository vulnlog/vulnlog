package ch.addere.cli.suppressions

import ch.addere.vulnlog.core.model.vulnerability.VlVulnerability
import java.io.File

abstract class Suppressor(
    private val suppressionFileTemplate: File,
    private val suppressionBlockMarker: String,
) {
    protected abstract val suppressionBlockTemplate: String

    init {
        require(suppressionFileTemplate.isFile) { "suppressionFileTemplate must be a file" }
        require(suppressionBlockMarker.isNotBlank()) { "suppressionBlockMarker cannot be blank" }
    }

    fun createSuppressions(vulnerabilities: Set<VlVulnerability>): SuppressionComposition {
        val filtered: Set<VlVulnerability> = filterRelevant(vulnerabilities)
        val suppressionBlocks: Set<SuppressionBlock> = transform(filtered)
        return generateSuppressionComposition(suppressionBlocks)
    }

    protected abstract fun filterRelevant(vulnerabilities: Set<VlVulnerability>): Set<VlVulnerability>

    protected abstract fun transform(filtered: Set<VlVulnerability>): Set<SuppressionBlock>

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
