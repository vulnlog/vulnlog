package dev.vulnlog.cli.core

import dev.vulnlog.cli.model.SchemaVersion

/**
 * Converts a schema version to a shortened string representation, omitting the minor version if it is zero.
 *
 * @param version The schema version to be shortened, containing major and minor version numbers.
 * @return A string representation of the schema version. If the minor version is zero, only the major version
 *         is included. Otherwise, both the major and minor versions are included, separated by a dot.
 */
fun shortenSchemaVersion(version: SchemaVersion): String {
    return if (version.minor == 0) {
        "${version.major}"
    } else {
        "${version.major}.${version.minor}"
    }
}
