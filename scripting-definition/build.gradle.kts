plugins {
    id("vulnlog.lib-convention")
    `maven-publish`
}

dependencies {
    implementation(project(":dsl"))
    implementation(libs.bundles.kotlinScriptDefinition)
    implementation(libs.kotlinCoroutinesCore)
}

publishing {
    publications {
        create<MavenPublication>("VulnLogScriptLanguage") {
            from(components["kotlin"])
            groupId = "io.vulnlog"
            artifactId = "language"
        }
    }
}
