// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.vex.openvex

import dev.vulnlog.lib.parse.vex.openvex.dto.OpenVexDocumentDto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension

private const val DTO_PACKAGE = "dev.vulnlog.lib.parse.vex.openvex.dto"
private const val REFLECT_CONFIG = "/META-INF/native-image/dev.vulnlog/lib/reflect-config.json"

/**
 * Jackson reaches the DTOs by reflection, so a DTO the native-image config does not list serializes
 * to an empty object in the native binary while staying correct on the JVM. This test fails the
 * moment a DTO is added without registering it.
 */
class OpenVexReflectConfigTest :
    FunSpec({

        test("every OpenVEX DTO is registered for reflection") {
            val config =
                OpenVexReflectConfigTest::class.java
                    .getResourceAsStream(REFLECT_CONFIG)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: error("Native-image config missing at classpath $REFLECT_CONFIG")

            val unregistered = dtoClassNames().filterNot { name -> config.contains("\"$name\"") }

            unregistered.shouldBeEmpty()
        }
    })

/** The compiled DTO classes, read from whatever this test runs against: a classes directory or a jar. */
private fun dtoClassNames(): List<String> {
    val location =
        Path.of(
            OpenVexDocumentDto::class.java.protectionDomain.codeSource.location
                .toURI(),
        )
    val packagePath = DTO_PACKAGE.replace('.', '/')
    val simpleNames =
        if (location.isDirectory()) {
            location.resolve(packagePath).listDirectoryEntries("*.class").map { it.nameWithoutExtension }
        } else {
            ZipFile(location.toFile()).use { jar -> classEntriesUnder(jar, packagePath) }
        }
    return simpleNames.filterNot { it.contains('$') }.map { "$DTO_PACKAGE.$it" }.sorted()
}

private fun classEntriesUnder(
    jar: ZipFile,
    packagePath: String,
): List<String> =
    jar
        .entries()
        .asSequence()
        .map { it.name }
        .filter { it.startsWith("$packagePath/") && it.endsWith(".class") }
        .map { it.removePrefix("$packagePath/").removeSuffix(".class") }
        .filterNot { it.contains('/') }
        .toList()
