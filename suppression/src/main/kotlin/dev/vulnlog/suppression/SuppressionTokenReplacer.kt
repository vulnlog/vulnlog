package dev.vulnlog.suppression

import java.util.regex.Pattern

data class SuppressionTokenData(
    val template: String,
    val tokenToReplacement: Map<String, String?>,
)

/**
 * A utility class designed to replace placeholder tokens within a template string.
 *
 * Tokens in the template are wrapped by start and end delimiters (e.g., `{{token}}`),
 * and the replacement values are provided in a map. The class supports matching and replacing
 * tokens line-by-line based on customizable token delimiters.
 */
class SuppressionTokenReplacer {
    private val startSearch = "{{"
    private val endSearch = "}}"
    private val pattern = Pattern.compile(".*(\\Q$startSearch\\E.*\\Q$endSearch\\E).*")

    /**
     * Replaces tokens within a template string using the provided token-to-replacement map.
     *
     * @param tokenData Data containing the template string and a map of tokens to their replacements.
     * The `template` in `tokenData` represents the raw string with placeholders,
     * and `tokenToReplacement` contains the mapping of tokens to their replacement values.
     * @return A processed string where all placeholders in the template have been replaced with the
     * respective replacement values or removed if no replacement is available.
     */
    fun replaceTokens(tokenData: SuppressionTokenData): String {
        return tokenData.template
            .lines().joinToString("\n") { line ->
                val matcher = pattern.matcher(line)
                if (matcher.matches()) {
                    replaceTokenInLine(line, matcher.group(1).trim(), tokenData.tokenToReplacement)
                } else {
                    line
                }
            }
    }

    private fun replaceTokenInLine(
        line: String,
        tokenInMatchingGroup: String,
        tokenToReplacement: Map<String, String?>,
    ): String {
        val token = tokenToReplacement.keys.firstOrNull { vlKey -> tokenInMatchingGroup.contains(vlKey) }
        val tokenValue = token?.let { tokenToReplacement[token] }
        return if (tokenValue == null) {
            removeTemplatePlaceholder(line)
        } else {
            replaceTokenWithValue(line, tokenInMatchingGroup, token, tokenValue)
        }
    }

    private fun replaceTokenWithValue(
        line: String,
        tokenInMatchingGroup: String,
        token: String,
        tokenValue: String,
    ): String {
        val lineBeforeTokenGroup = line.substringBefore(tokenInMatchingGroup)
        val lineAfterTokenGroup = line.substringAfter(tokenInMatchingGroup)
        val replacedToken =
            tokenInMatchingGroup.replace(token, tokenValue)
                .replace(Regex("\\Q$startSearch\\E\\s+"), "")
                .replace(Regex("\\s+$endSearch"), "")
        return lineBeforeTokenGroup + replacedToken + lineAfterTokenGroup
    }

    private fun removeTemplatePlaceholder(line: String) =
        line.substringBefore(startSearch) + line.substringAfter(endSearch)
}
