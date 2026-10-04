plugins {
    id("vulnlog.common-convention")
    `java-library`
}

description = "Vulnlog domain: the model, the validation findings and the rules"

group = "dev.vulnlog"

dependencies {
    // packageurl is api: core's parsePurl takes a PackageURL.
    api(libs.packageUrl)

    testImplementation(libs.kotestAssertionsCoreJvm)
    testImplementation(libs.kotestRunnerJunit5Jvm)
}
