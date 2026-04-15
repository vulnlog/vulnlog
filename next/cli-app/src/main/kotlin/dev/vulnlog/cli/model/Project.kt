// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.model

data class Project(
    /**
     * Name of the organization or vendor.
     */
    val organization: String,
    /**
     * Name of the software project.
     */
    val name: String,
    /**
     * Name of the responsible security team or author. Used as the author in VEX documents.
     */
    val author: String,
    /**
     * Contact email for the security team. Used as the author contact in VEX documents.
     */
    val contact: String? = null,
)
