plugins {
    id("vulnlog.lib-convention")
}

description = "The Vulnlog report generation functionality."

dependencies {
    implementation(project(":dsl"))
}

group = "dev.vulnlog"
version = providers.gradleProperty("vlVersion").get()
