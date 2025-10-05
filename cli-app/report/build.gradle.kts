plugins {
    id("vulnlog.lib-convention")
}

description = "The Vulnlog report generation functionality."

dependencies {
    implementation(project(":common"))
    implementation(libs.kotlinxSerializationJson)
}

group = "dev.vulnlog"
