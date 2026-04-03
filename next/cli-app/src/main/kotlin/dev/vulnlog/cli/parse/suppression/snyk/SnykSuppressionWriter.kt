package dev.vulnlog.cli.parse.suppression.snyk

import dev.vulnlog.cli.model.suppress.SuppressionOutput
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.dataformat.yaml.YAMLWriteFeature
import tools.jackson.module.kotlin.kotlinModule

object SnykSuppressionWriter {
    private val mapper =
        YAMLMapper.builder()
            .addModule(kotlinModule())
            .enable(YAMLWriteFeature.MINIMIZE_QUOTES)
            .enable(YAMLWriteFeature.LITERAL_BLOCK_STYLE)
            .build()

    fun write(inputData: SuppressionOutput.SnykSuppression): String {
        val dto = SnykMapper.toDto(inputData)
        return mapper.writeValueAsString(dto)
    }
}
