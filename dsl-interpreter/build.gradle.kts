plugins {
    id("vulnlog.lib-convention")
}

description = "The Vulnlog DSL parsing interpreter parses Vulnlog DSL files."

group = "dev.vulnlog"
version = providers.gradleProperty("vlVersion").get()

dependencies {
    api(project(":dsl"))
    implementation(project(":common"))
    implementation(libs.bundles.kotlinScript)
}
