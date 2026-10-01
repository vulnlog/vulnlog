// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import java.io.File

/**
 * Enforces the package layers of the target architecture by scanning the imports of lib's main sources, and keeps the
 * pure layers free of the clock, randomness, the environment and the file system. No extra dependency.
 *
 * Packages in [strictPackages] already follow the rules and fail the build on a violation. Every other package is only
 * reported while the migration is in progress. A package that no layer names yet, such as `shell`, is not checked.
 */
class ArchitectureTest :
    FunSpec({
        test("lib packages only import the layers they may use") {
            check(sources(), ::layerViolations)
        }

        test("pure layers reach no clock, randomness, environment or file system") {
            check(sources(), ::impurityViolations)
        }
    })

private const val LIB = "dev.vulnlog.lib"

/** Packages migrated to the target layout: VEX, and the layers that so far hold VEX only. */
private val strictPackages =
    listOf("$LIB.model.vex", "$LIB.core.vex", "$LIB.codec", "$LIB.render", "$LIB.app", "$LIB.io")

/** Layer → the dev.vulnlog.lib packages it may import (itself included). */
private val allowed: Map<String, List<String>> =
    mapOf(
        "$LIB.model" to listOf("$LIB.model"),
        "$LIB.finding" to listOf("$LIB.finding", "$LIB.model"),
        "$LIB.core" to listOf("$LIB.core", "$LIB.model", "$LIB.finding"),
        "$LIB.document" to listOf("$LIB.document", "$LIB.core", "$LIB.model", "$LIB.finding"),
        "$LIB.codec" to listOf("$LIB.codec", "$LIB.document.yaml", "$LIB.core", "$LIB.model", "$LIB.finding"),
        "$LIB.render" to listOf("$LIB.render", "$LIB.app", "$LIB.document", "$LIB.core", "$LIB.model", "$LIB.finding"),
        "$LIB.app" to listOf("$LIB.app", "$LIB.codec", "$LIB.document", "$LIB.core", "$LIB.model", "$LIB.finding"),
        "$LIB.io" to listOf("$LIB.io", "$LIB.app.port", "$LIB.document", "$LIB.model", "$LIB.finding"),
    )

/** Layers that must stay free of third-party libraries. */
private val externalAllowed: Map<String, List<String>> =
    mapOf(
        "$LIB.model" to listOf("java.", "kotlin."),
        "$LIB.finding" to listOf("java.", "kotlin."),
        "$LIB.core" to listOf("java.", "kotlin.", "com.github.packageurl."),
    )

/** Layers whose functions are deterministic: time and ids arrive as data. */
private val pureLayers = listOf("$LIB.model", "$LIB.finding", "$LIB.core", "$LIB.codec", "$LIB.render", "$LIB.app")

/** Calls that read the clock, draw randomness, read the environment or touch the file system. */
private val impureCalls =
    listOf(
        "UUID.randomUUID(",
        "Instant.now(",
        "LocalDate.now(",
        "LocalDateTime.now(",
        "Clock.system",
        "System.currentTimeMillis(",
        "System.getenv(",
        "Files.",
        "java.io.File",
        "kotlin.io.path.",
    )

private data class Source(
    val name: String,
    val pkg: String,
    val code: String,
)

private fun sources(): List<Source> =
    File("src/main/kotlin")
        .walkTopDown()
        .filter { it.extension == "kt" }
        .map { file ->
            val text = file.readText()
            val pkg =
                Regex("^package (\\S+)", RegexOption.MULTILINE)
                    .find(text)
                    ?.groupValues
                    ?.get(1)
                    .orEmpty()
            Source(file.name, pkg, withoutComments(text))
        }.toList()

private fun layerViolations(source: Source): List<String> {
    val layer = layerOf(source.pkg) ?: return emptyList()
    return Regex("^import (\\S+)", RegexOption.MULTILINE)
        .findAll(source.code)
        .map { it.groupValues[1] }
        .filterNot { import -> isAllowed(layer, import) }
        .map { import -> "${source.name}: ${source.pkg} imports $import" }
        .toList()
}

private fun impurityViolations(source: Source): List<String> {
    val layer = layerOf(source.pkg) ?: return emptyList()
    if (layer !in pureLayers) return emptyList()
    return impureCalls
        .filter { call -> call in source.code }
        .map { call -> "${source.name}: ${source.pkg} calls $call" }
}

private fun layerOf(pkg: String): String? = allowed.keys.firstOrNull { isWithin(pkg, it) }

private fun isAllowed(
    layer: String,
    import: String,
): Boolean =
    if (import.startsWith("$LIB.")) {
        allowed.getValue(layer).any { import.startsWith("$it.") }
    } else {
        externalAllowed[layer]?.any { import.startsWith(it) } ?: true
    }

/** The code without its comments, so a KDoc that names a forbidden call does not count as one. */
private fun withoutComments(text: String): String =
    text
        .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("//[^\\n]*"), "")

/** Fails on a violation in a strict package and prints the others. */
private fun check(
    sources: List<Source>,
    violationsOf: (Source) -> List<String>,
) {
    val (strict, reported) = sources.partition { source -> strictPackages.any { isWithin(source.pkg, it) } }
    reported.flatMap(violationsOf).forEach(::println)
    strict.flatMap(violationsOf).shouldBeEmpty()
}

private fun isWithin(
    pkg: String,
    root: String,
): Boolean = pkg == root || pkg.startsWith("$root.")
