// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.parse.suppression

import dev.vulnlog.cli.model.suppress.SuppressionOutput
import dev.vulnlog.cli.parse.suppression.generic.GenericSuppressionWriter
import dev.vulnlog.cli.parse.suppression.snyk.SnykSuppressionWriter
import dev.vulnlog.cli.parse.suppression.trivy.TrivySuppressionWriter

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
