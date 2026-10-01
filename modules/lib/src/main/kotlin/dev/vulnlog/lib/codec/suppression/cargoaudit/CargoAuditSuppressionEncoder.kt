// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.suppression.cargoaudit

import dev.vulnlog.lib.model.suppress.SuppressionOutput

object CargoAuditSuppressionEncoder {
    fun encode(inputData: SuppressionOutput.CargoAuditSuppression): String {
        if (inputData.entries.isEmpty()) return "[advisories]\nignore = []\n"
        val ids = inputData.entries.joinToString(",\n") { "    \"${it.id.id}\"" }
        return "[advisories]\nignore = [\n$ids,\n]\n"
    }
}
