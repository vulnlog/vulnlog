// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.suppression.trivy

import dev.vulnlog.lib.document.yaml.CanonicalYaml
import dev.vulnlog.lib.model.suppress.SuppressionOutput

object TrivySuppressionEncoder {
    fun encode(inputData: SuppressionOutput.TrivySuppression): String =
        CanonicalYaml.renderDocument(TrivyMapper.toDto(inputData))
}
