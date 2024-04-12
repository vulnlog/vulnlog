plugins {
    id("vulnlog.lib-convention")
    `maven-publish`
}

dependencies {
    implementation(project(":dsl"))
    implementation(libs.bundles.kotlinScript)
    implementation(libs.kotlinCoroutinesCore)
}

// TODO used to publish DSL jar but makes problem with execution of scripting-host executable
//tasks.withType<Jar> {
//    val dependencies = configurations.runtimeClasspath.get().map(::zipTree)
//    from(dependencies)
//    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
        }
    }
}
