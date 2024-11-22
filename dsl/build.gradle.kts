plugins {
    id("vulnlog.lib-convention")
    `maven-publish`
    id("org.jetbrains.dokka") version "1.9.20"
}

group = "io.vulnlog.dsl"
version = "0.1.0"

dependencies {
    implementation(libs.bundles.kotlinScriptDefinition)
}

tasks.register<Jar>("dokkaHtmlJar") {
    dependsOn(tasks.dokkaHtml)
    from(tasks.dokkaHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("html-docs")
}

val javaDoc =
    tasks.register<Jar>("dokkaJavadocJar") {
        dependsOn(tasks.dokkaJavadoc)
        from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
        archiveClassifier.set("javadoc")
    }

val sources =
    tasks.register<Jar>("sourcesJar") {
        from(sourceSets.main.get().allSource)
        archiveClassifier.set("sources")
    }

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
            artifact(javaDoc)
            artifact(sources)
        }
    }
}
