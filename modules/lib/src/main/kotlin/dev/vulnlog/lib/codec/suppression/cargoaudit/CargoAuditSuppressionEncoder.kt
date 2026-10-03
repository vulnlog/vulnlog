// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.suppression.cargoaudit

import dev.vulnlog.lib.model.suppression.SuppressionList

internal object CargoAuditSuppressionEncoder {
    fun encode(list: SuppressionList): String {
        if (list.entries.isEmpty()) return "[advisories]\nignore = []\n"
        val ids = list.entries.joinToString(",\n") { "    \"${it.id.id}\"" }
        return "[advisories]\nignore = [\n$ids,\n]\n"
    }
}
