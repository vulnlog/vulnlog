// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec

import java.nio.file.Files
import java.nio.file.Path

private const val GOLDEN_RESOURCE_DIR = "/report"
private val GOLDEN_SOURCE_DIR: Path = Path.of("src/test/resources/report")

/**
 * The golden file [name] from the test resources. With `UPDATE_GOLDEN` set, it writes [actual] as the new golden file
 * first and returns it, so the test passes and the change shows in the diff.
 */
internal fun readOrUpdateGolden(
    name: String,
    actual: String,
): String {
    if (System.getenv("UPDATE_GOLDEN") in listOf("1", "true")) {
        val source = GOLDEN_SOURCE_DIR.resolve(name)
        Files.createDirectories(GOLDEN_SOURCE_DIR)
        Files.writeString(source, actual)
        println("Golden file updated at $source")
        return actual
    }
    return object {}
        .javaClass
        .getResourceAsStream("$GOLDEN_RESOURCE_DIR/$name")
        ?.bufferedReader()
        ?.use { it.readText() }
        ?: error("Golden file missing at classpath $GOLDEN_RESOURCE_DIR/$name. Run with UPDATE_GOLDEN=1 to create it.")
}
