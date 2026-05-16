// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.shell

import java.nio.file.Path

sealed interface OutputOption

sealed interface FileOutputOption : OutputOption {
    data object Stdout : FileOutputOption

    data class File(
        val path: Path,
    ) : FileOutputOption
}

sealed interface DirectoryOutputOption : OutputOption {
    data class Directory(
        val path: Path,
    ) : DirectoryOutputOption
}
