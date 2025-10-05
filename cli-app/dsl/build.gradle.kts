plugins {
    id("vulnlog.lib-convention")
    id("org.jetbrains.dokka") version "1.9.20"
    id("com.vanniktech.maven.publish") version "0.34.0"
}

description = "DLS definition for Vulnlog files."

group = "dev.vulnlog"

dependencies {
    implementation(libs.bundles.kotlinScriptDefinition)

    dokkaPlugin("org.jetbrains.dokka:versioning-plugin:1.9.20")
}

kotlin {
    // use explicit API mode for the DSL package to prevent unintended API changes.
    explicitApi()
}

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:versioning-plugin:1.9.20")
    }
}

tasks.dokkaHtml {
    outputDirectory = layout.buildDirectory.dir("apiDoc/")
    moduleName = "Vulnlog DSL"

    dokkaSourceSets {
        configureEach {
            perPackageOption {
                matchingRegex = "dev.vulnlog.dsl.definition.*"
                suppress = true
            }
            perPackageOption {
                matchingRegex = "dev.vulnlog.dsl.impl.*"
                suppress = true
            }
            perPackageOption {
                matchingRegex = "dev.vulnlog.dsl.util.*"
                suppress = true
            }
        }
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(kotlin.sourceSets.main.get().kotlin)
}

val javadocJar by tasks.registering(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Javadoc JAR"
    archiveClassifier.set("javadoc")
    from(tasks.named("dokkaHtml"))
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(project.group.toString(), project.name, project.version.toString())

    pom {
        name = "Vulnlog DSL"
        description = "A simple and easy to use DSL to describe vulnerabilities in software projects."
        inceptionYear = "2023"
        url = "https://github.com/vulnlog/vulnlog"

        licenses {
            license {
                name = "GPL-3.0 license"
                url = "https://www.gnu.org/licenses/gpl-3.0.txt"
                distribution = "https://www.gnu.org/licenses/gpl-3.0.txt"
            }
        }

        developers {
            developer {
                id = "pascalk"
                name = "Pascal Knecht"
                email = "pascal@addere.ch"
            }
        }

        scm {
            url = "https://github.com/vulnlog/vulnlog.git"
            connection = "scm:git:git://github.com/vulnlog/vulnlog.git"
            developerConnection = "scm:git:git://github.com/vulnlog/vulnlog.git"
            issueManagement {
                url = "https://github.com/vulnlog/vulnlog/issues"
            }
        }
    }
}
