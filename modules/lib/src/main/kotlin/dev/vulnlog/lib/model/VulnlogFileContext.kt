// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model

data class VulnlogFileContext(
    val validationVersion: ParseValidationVersion,
    val fileName: String,
    val vulnlogFile: VulnlogFile,
)
