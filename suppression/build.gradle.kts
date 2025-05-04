plugins {
    id("vulnlog.lib-convention")
}

description = "The Vulnlog suppression file generation functionality."

dependencies {
    implementation(project(":common"))
}

group = "dev.vulnlog"
version = providers.gradleProperty("vlVersion").get()
