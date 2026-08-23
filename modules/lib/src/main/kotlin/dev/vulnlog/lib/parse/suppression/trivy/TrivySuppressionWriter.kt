// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.suppression.trivy

import dev.vulnlog.lib.model.suppress.SuppressionOutput
import dev.vulnlog.lib.parse.CanonicalYaml

object TrivySuppressionWriter {
    fun write(inputData: SuppressionOutput.TrivySuppression): String =
        CanonicalYaml.renderDocument(TrivyMapper.toDto(inputData))
}
