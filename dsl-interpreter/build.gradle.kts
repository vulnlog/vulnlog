plugins {
    id("vulnlog.lib-convention")
}

group = "dev.vulnlog.dsl-interpreter"
version = providers.gradleProperty("vlVersion").get()

dependencies {
    implementation(project(":dsl"))
    implementation(libs.bundles.kotlinScript)
}
