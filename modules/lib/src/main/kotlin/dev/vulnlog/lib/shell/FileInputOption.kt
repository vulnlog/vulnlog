// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.lib.shell

import java.nio.file.Path

sealed interface FileInputOption {
    data object Stdin : FileInputOption

    data class File(
        val path: Path,
    ) : FileInputOption
}
