plugins {
    id("vulnlog.common-convention")
    `java-library`
}

description = "Vulnlog core library"

group = "dev.vulnlog"

dependencies {
    api(libs.jacksonKotlin)
    api(libs.jacksonYaml)
    api(libs.packageUrl)

    testImplementation(libs.kotestAssertionsCoreJvm)
    testImplementation(libs.kotestRunnerJunit5Jvm)
}
