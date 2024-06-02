plugins {
    id("vulnlog.lib-convention")
}

dependencies {
    api(project(":dsl"))
    api(project(":scripting-definition"))
    implementation(libs.bundles.kotlinScript)

    testImplementation(testFixtures(project(":core")))
}
