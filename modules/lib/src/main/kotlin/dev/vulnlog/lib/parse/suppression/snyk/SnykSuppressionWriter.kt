// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.suppression.snyk

import dev.vulnlog.lib.model.suppress.SuppressionOutput
import dev.vulnlog.lib.parse.CanonicalYaml

object SnykSuppressionWriter {
    fun write(inputData: SuppressionOutput.SnykSuppression): String =
        CanonicalYaml.renderDocument(SnykMapper.toDto(inputData))
}
