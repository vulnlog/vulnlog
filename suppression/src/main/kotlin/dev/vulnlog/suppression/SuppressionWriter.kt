package dev.vulnlog.suppression

import dev.vulnlog.common.model.BranchName

private const val MARKER_KEYWORD = "vulnlogEntries"

/**
 * Handles the process of writing suppression data to output files or streams by combining provided templates
 * with generated suppression records.
 *
 * The class utilizes a provided implementation of `OutputWriter` to write structured output data.
 */
class SuppressionWriter(
    private val outputWriter: OutputWriter,
    private val releaseBranchesFiltered: Set<BranchName>,
) {
    /**
     * Writes suppression data into output files or streams by combining templates with generated suppression records.
     *
     * @param templateNameToContent a map where each key is an instance of `SuppressionFileInfo` representing a file's
     * metadata (e.g., filename and extension) and the value is the content of that file as a list of strings.
     * @param generatedSuppressions a set of `SuppressionRecord` instances representing the generated suppression data
     * to be inserted into the templates.
     */
    fun writeSuppression(
        templateNameToContent: Map<SuppressionFileInfo, List<String>>,
        generatedSuppressions: Set<SuppressionRecord>,
    ) {
        insertSuppressionsIntoTemplate(releaseBranchesFiltered, generatedSuppressions, templateNameToContent)
            .forEach(outputWriter::writeText)
    }

    private fun insertSuppressionsIntoTemplate(
        releaseBranchesFiltered: Set<BranchName>,
        generatedSuppressions: Set<SuppressionRecord>,
        templateNameToContent: Map<SuppressionFileInfo, List<String>>,
    ): List<OutputData> =
        generatedSuppressions.flatMap { record ->
            val templateName = record.templateFilename
            val templateEntry =
                templateNameToContent.entries
                    .firstOrNull { "${it.key.filename}.${it.key.fileExtension}" == templateName }
            if (templateEntry == null) {
                return emptyList()
            }

            val templateContent = templateEntry.value
            val whitespaceCount = countWhiteSpacesBeforeMarker(templateContent)
            val templateContentBeforeMarkerLine = extractContentBeforeMarkerLine(templateContent)
            val templateContentAfterMarkerLine = extractContentAfterMarkerLine(templateContent)
            createOutputData(
                releaseBranchesFiltered,
                record,
                templateEntry.key,
                whitespaceCount,
                templateContentBeforeMarkerLine,
                templateContentAfterMarkerLine,
            )
        }

    private fun countWhiteSpacesBeforeMarker(templateContent: List<String>) =
        templateContent.first { it.contains(MARKER_KEYWORD) }.takeWhile { it.isWhitespace() }.length

    private fun extractContentBeforeMarkerLine(templateContent: List<String>) =
        templateContent.takeWhile { !it.contains(MARKER_KEYWORD) }

    private fun extractContentAfterMarkerLine(templateContent: List<String>) =
        templateContent.dropWhile { !it.contains(MARKER_KEYWORD) }.drop(1)

    @Suppress("LongParameterList")
    private fun createOutputData(
        releaseBranchesFiltered: Set<BranchName>,
        record: SuppressionRecord,
        entry: SuppressionFileInfo,
        whitespaceCount: Int,
        beforeContent: List<String>,
        afterContent: List<String>,
    ): List<OutputData> {
        return releaseBranchesFiltered.map { branch ->
            val outputFileName = outputFilename(entry.filename, branch.name, entry.fileExtension)
            val transformedContent: List<String> =
                record.branchToSuppressions.filter { it.key == branch }.flatMap { (_, suppression) ->
                    val transformedContent =
                        suppression
                            .flatMap { it.split("\\n".toRegex()) }
                            .map { " ".repeat(whitespaceCount) + it }
                    transformedContent
                }
            val content: List<String> = beforeContent + transformedContent + afterContent
            OutputData(outputFileName, content)
        }
    }

    private fun outputFilename(
        reporterName: String,
        branchName: String,
        fileExtension: String,
    ): String =
        "$reporterName-$branchName.$fileExtension"
            .replace("\\s+".toRegex(), "-")
            .lowercase()
}
