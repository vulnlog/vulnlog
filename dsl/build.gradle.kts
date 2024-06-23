plugins {
    id("vulnlog.lib-convention")
    `maven-publish`
}

dependencies {
    api(project(":core"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
        }
    }
}
