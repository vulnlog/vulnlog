// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.unique
import dev.vulnlog.lib.core.workStateTokens

class ImpactFilterOptions : OptionGroup() {
    val statesRequest: Set<String> by option(
        "--state",
        metavar = "<state>",
        help =
            """
            Filter on vulnerability state. Use multiple times to filter on multiple states.
            Supported states: ${workStateTokens()}
            """.trimIndent(),
    ).multiple()
        .unique()
}
