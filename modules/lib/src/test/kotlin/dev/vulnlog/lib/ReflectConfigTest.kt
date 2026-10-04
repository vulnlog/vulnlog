// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib

import dev.vulnlog.lib.document.dto.VulnlogFileV1Dto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory

private const val REFLECT_CONFIG = "/META-INF/native-image/dev.vulnlog/lib/reflect-config.json"

/**
 * Jackson reaches the DTOs by reflection, so a DTO the native-image config does not list breaks only the native binary:
 * it writes an empty object or fails to read, while every JVM test passes. A package move has to update the config.
 */
class ReflectConfigTest :
    FunSpec({

        test("every DTO class is registered for reflection") {
            val registered = registeredClassNames()

            val unregistered = dtoClassNames().filterNot { it in registered }

            unregistered.shouldBeEmpty()
        }

        test("every lib class the config names exists") {
            val named = registeredClassNames().filter { it.startsWith("dev.vulnlog.") }

            val missing = named.filterNot(::classExists)

            missing.shouldBeEmpty()
        }
    })

private fun registeredClassNames(): Set<String> {
    val config =
        ReflectConfigTest::class.java
            .getResourceAsStream(REFLECT_CONFIG)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: error("Native-image config missing at classpath $REFLECT_CONFIG")
    return Regex(""""name"\s*:\s*"([^"]+)"""").findAll(config).map { it.groupValues[1] }.toSet()
}

/** Interfaces are left out: Jackson never instantiates one, so `DtoVersion` needs no entry. */
private fun dtoClassNames(): List<String> {
    val location =
        Path.of(
            VulnlogFileV1Dto::class.java.protectionDomain.codeSource.location
                .toURI(),
        )
    val classFiles = if (location.isDirectory()) classFilesIn(location) else classFilesIn(ZipFile(location.toFile()))
    val names =
        classFiles
            .filter { it.substringBeforeLast('/').endsWith("/dto") && '$' !in it }
            .map { it.removeSuffix(".class").replace('/', '.') }
            .filterNot { Class.forName(it, false, ReflectConfigTest::class.java.classLoader).isInterface }
            .sorted()
    check(names.isNotEmpty()) { "No DTO classes found under $location" }
    return names
}

private fun classFilesIn(directory: Path): List<String> =
    Files.walk(directory).use { paths ->
        paths
            .filter { it.toString().endsWith(".class") }
            .map { directory.relativize(it).invariantSeparatorsPathString }
            .toList()
    }

private fun classFilesIn(jar: ZipFile): List<String> =
    jar.use { zip ->
        zip
            .entries()
            .toList()
            .map { it.name }
            .filter { it.endsWith(".class") }
    }

private fun classExists(name: String): Boolean =
    runCatching { Class.forName(name, false, ReflectConfigTest::class.java.classLoader) }.isSuccess
