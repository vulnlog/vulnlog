// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.lib.parse.suppression

import dev.vulnlog.lib.model.suppress.SuppressionOutput
import dev.vulnlog.lib.parse.suppression.generic.GenericSuppressionWriter
import dev.vulnlog.lib.parse.suppression.snyk.SnykSuppressionWriter
import dev.vulnlog.lib.parse.suppression.trivy.TrivySuppressionWriter

data class SuppressionFile(
    val fileName: String,
    val content: String,
)

object SuppressionWriter {
    fun writeSuppressionOutput(output: SuppressionOutput): SuppressionFile =
        when (output) {
            is SuppressionOutput.GenericSuppression ->
                SuppressionFile(
                    fileName = output.fileName,
                    content = GenericSuppressionWriter.write(output),
                )

            is SuppressionOutput.TrivySuppression ->
                SuppressionFile(
                    fileName = output.fileName,
                    content = TrivySuppressionWriter.write(output),
                )

            is SuppressionOutput.SnykSuppression ->
                SuppressionFile(
                    fileName = output.fileName,
                    content = SnykSuppressionWriter.write(output),
                )
        }
}
