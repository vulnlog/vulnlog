// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.model

data class Tag(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Tag value cannot be blank" }
    }
}
