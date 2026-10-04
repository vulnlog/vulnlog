// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import java.io.File

/**
 * Enforces the package layers of the target architecture by scanning the imports and the fully qualified references of
 * the main sources of lib and lib-domain, and keeps the pure layers free of the clock, randomness, the environment and
 * the file system. No extra dependency. The module boundary already keeps the domain from the outer layers; the rules
 * inside each module stay this test's job.
 *
 * Packages in [strictPackages] already follow the rules and fail the build on a violation. Every other package is only
 * reported while the migration is in progress. A package that no layer names is not checked.
 */
class ArchitectureTest :
    FunSpec({
        test("lib packages only reach the layers they may use") {
            check(sources(), ::layerViolations)
        }

        test("pure layers reach no clock, randomness, environment or file system") {
            check(sources(), ::impurityViolations)
        }
    })

private const val LIB = "dev.vulnlog.lib"

/** Packages already in the target layout. */
private val strictPackages =
    listOf(
        "$LIB.model",
        "$LIB.finding",
        "$LIB.core.filter",
        "$LIB.core.reporting",
        "$LIB.core.suppression",
        "$LIB.core.validation",
        "$LIB.core.vex",
        "$LIB.document",
        "$LIB.codec",
        "$LIB.render",
        "$LIB.app",
        "$LIB.io",
    )

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

/** A missing root would yield no file and pass silently, so both must exist. */
private val sourceRoots = listOf(File("src/main/kotlin"), File("../lib-domain/src/main/kotlin"))

private fun sources(): List<Source> =
    sourceRoots
        .onEach { root -> check(root.isDirectory) { "Source root not found: $root" } }
        .flatMap { root -> root.walkTopDown().filter { it.extension == "kt" }.toList() }
        .map { file ->
            val text = file.readText()
            val pkg =
                Regex("^package (\\S+)", RegexOption.MULTILINE)
                    .find(text)
                    ?.groupValues
                    ?.get(1)
                    .orEmpty()
            Source(file.name, pkg, withoutComments(text))
        }

private val importLine = Regex("^import (\\S+)", RegexOption.MULTILINE)

/** A name written out in the code, such as `dev.vulnlog.lib.render.Message.Status(...)`, needs no import. */
private val qualifiedLibName = Regex("""\bdev\.vulnlog\.lib(?:\.\w+)+""")

private fun layerViolations(source: Source): List<String> {
    val layer = layerOf(source.pkg) ?: return emptyList()
    val body =
        source.code
            .lineSequence()
            .filterNot { line -> line.startsWith("package ") || line.startsWith("import ") }
            .joinToString("\n")
    val imports = importLine.findAll(source.code).map { "imports" to it.groupValues[1] }
    val references = qualifiedLibName.findAll(body).map { "references" to it.value }
    return (imports + references)
        .filterNot { (_, name) -> isAllowed(layer, name) }
        .map { (how, name) -> "${source.name}: ${source.pkg} $how $name" }
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
    name: String,
): Boolean =
    if (name.startsWith("$LIB.")) {
        allowed.getValue(layer).any { name.startsWith("$it.") }
    } else {
        externalAllowed[layer]?.any { name.startsWith(it) } ?: true
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
