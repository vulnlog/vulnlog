package dev.vulnlog.cli.parse

import com.fasterxml.jackson.annotation.JsonInclude
import tools.jackson.databind.ObjectMapper
import tools.jackson.dataformat.yaml.YAMLMapper

fun createYamlMapper(): ObjectMapper =
    YAMLMapper.builder()
        .changeDefaultPropertyInclusion { it.withValueInclusion(JsonInclude.Include.NON_NULL) }
        .build()
