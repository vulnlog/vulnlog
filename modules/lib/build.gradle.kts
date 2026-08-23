plugins {
    id("vulnlog.common-convention")
    `java-library`
}

description = "Vulnlog core library"

group = "dev.vulnlog"

dependencies {
    // snakeyaml-engine is api: MappingNode surfaces on NodeTreeResult, which consumers read.
    api(libs.snakeyamlEngine)
    api(libs.packageUrl)
    implementation(libs.jacksonKotlin)

    testImplementation(libs.kotestAssertionsCoreJvm)
    testImplementation(libs.kotestRunnerJunit5Jvm)
}
