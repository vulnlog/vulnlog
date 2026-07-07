// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

/**
 * The exit codes of the CLI. They are documented API: the values are pinned explicitly so a
 * reordering can never silently change the contract.
 */
enum class ExitCode(
    val code: Int,
) {
    /**
     * Command completed successfully.
     */
    SUCCESS(0),

    /**
     * Unexpected error (I/O failure, unhandled exception).
     */
    GENERAL_ERROR(1),

    /**
     * The Vulnlog file contains validation errors.
     */
    VALIDATION_ERROR(2),

    /**
     * The file has formatting violations.
     */
    FORMAT_ERROR(3),

    /**
     * Reserved for a Vulnlog file that does not exist. Not raised yet: a missing file currently
     * surfaces as a usage error and exits with the [GENERAL_ERROR] value.
     */
    FILE_NOT_FOUND(4),

    /**
     * A flag references an unknown entity or unsupported type.
     */
    INVALID_FLAG_VALUE(5),
}
