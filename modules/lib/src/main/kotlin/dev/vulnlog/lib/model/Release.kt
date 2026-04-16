// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.lib.model

data class Release(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Release value cannot be blank" }
    }
}
