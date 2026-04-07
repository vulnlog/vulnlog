package dev.vulnlog.cli.parse

import tools.jackson.databind.ObjectMapper
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.module.kotlin.kotlinModule

fun createYamlMapper(): ObjectMapper =
    YAMLMapper
        .builder()
        .addModule(kotlinModule())
        .build()
