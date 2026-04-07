package dev.vulnlog.cli.parse.suppression.trivy

import dev.vulnlog.cli.model.suppress.SuppressionOutput
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.dataformat.yaml.YAMLWriteFeature
import tools.jackson.module.kotlin.kotlinModule

object TrivySuppressionWriter {
    private val mapper =
        YAMLMapper
            .builder()
            .addModule(kotlinModule())
            .enable(YAMLWriteFeature.MINIMIZE_QUOTES)
            .enable(YAMLWriteFeature.LITERAL_BLOCK_STYLE)
            .build()

    fun write(inputData: SuppressionOutput.TrivySuppression): String {
        val dto = TrivyMapper.toDto(inputData)
        return mapper.writeValueAsString(dto)
    }
}
