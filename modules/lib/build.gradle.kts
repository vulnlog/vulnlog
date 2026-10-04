plugins {
    id("vulnlog.common-convention")
    `java-library`
}

description = "Vulnlog library: the use cases, the document, the codecs, the messages and the file access"

group = "dev.vulnlog"

dependencies {
    api(project(":lib-domain"))
    // snakeyaml-engine is api: MappingNode surfaces on NodeTreeResult, which consumers read.
    api(libs.snakeyamlEngine)
    api(libs.packageUrl)
    implementation(libs.jacksonKotlin)

    testFixturesApi(testFixtures(project(":lib-domain")))

    testImplementation(libs.kotestAssertionsCoreJvm)
    testImplementation(libs.kotestRunnerJunit5Jvm)
}
