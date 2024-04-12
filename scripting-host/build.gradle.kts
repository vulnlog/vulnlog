plugins {
    id("vulnlog.lib-convention")
}

dependencies {
    api(project(":dsl"))
    implementation(project(":scripting-definition"))
    implementation(libs.bundles.kotlinScript)
}
