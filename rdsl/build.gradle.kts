plugins {
    id("vulnlog.lib-convention")
    `maven-publish`
}

group = "io.vulnlog.rdsl"
version = "0.1.0"

dependencies {
    implementation(libs.bundles.kotlinScriptDefinition)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
        }
    }
}
