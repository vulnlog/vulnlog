// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.suppression.trivy

import dev.vulnlog.lib.document.yaml.CanonicalYaml
import dev.vulnlog.lib.model.suppression.SuppressionList

internal object TrivySuppressionEncoder {
    fun encode(list: SuppressionList): String = CanonicalYaml.renderDocument(TrivyMapper.toDto(list))
}
