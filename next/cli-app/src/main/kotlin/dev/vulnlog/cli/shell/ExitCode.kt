// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.shell

enum class ExitCode {
    /**
     * Command completed successfully.
     */
    SUCCESS,

    /**
     * Unexpected error (I/O failure, unhandled exception).
     */
    GENERAL_ERROR,

    /**
     * The Vulnlog file contains validation errors.
     */
    VALIDATION_ERROR,

    /**
     * The specified Vulnlog file does not exist.
     */
    FILE_NOT_FOUND,

    /**
     * The file has lint violations
     */
    LINT_ERROR,

    /**
     * A flag references an unknown entity or unsupported type.
     */
    INVALID_FLAG_VALUE,
}
