// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.suppression.snyk

import dev.vulnlog.lib.document.yaml.CanonicalYaml
import dev.vulnlog.lib.model.suppression.SuppressionList

internal object SnykSuppressionEncoder {
    fun encode(list: SuppressionList): String = CanonicalYaml.renderDocument(SnykMapper.toDto(list))
}
