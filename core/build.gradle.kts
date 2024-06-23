plugins {
    id("vulnlog.lib-convention")
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
        }
    }
}
