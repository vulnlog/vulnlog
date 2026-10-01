// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.suppression.snyk

import dev.vulnlog.lib.document.yaml.CanonicalYaml
import dev.vulnlog.lib.model.suppress.SuppressionOutput

object SnykSuppressionEncoder {
    fun encode(inputData: SuppressionOutput.SnykSuppression): String =
        CanonicalYaml.renderDocument(SnykMapper.toDto(inputData))
}
