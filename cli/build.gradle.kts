plugins {
    id("vulnlog.app-convention")
}

dependencies {
    implementation(project(":scripting-host"))

    implementation(libs.bundles.kotlinScript)
    implementation(libs.clikt)
    implementation(libs.koinCore)

    testImplementation(testFixtures(project(":core")))

    testImplementation(libs.koinTest)
    testImplementation(libs.koinTestJoint5)
    testImplementation(libs.mockk)
    testImplementation(libs.kotestExtensionsKoin)
}

application {
    mainClass.set("ch.addere.vulnlog.cli.AppKt")
    applicationName = "vl"
}
