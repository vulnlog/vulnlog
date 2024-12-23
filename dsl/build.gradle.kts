import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("vulnlog.lib-convention")
    id("org.jetbrains.dokka") version "1.9.20"
    id("com.vanniktech.maven.publish") version "0.30.0"
}

description = "DLS definition for Vulnlog files."

group = "dev.vulnlog.dsl"
version = providers.gradleProperty("vlVersion").get()

dependencies {
    implementation(libs.bundles.kotlinScriptDefinition)

    dokkaPlugin("org.jetbrains.dokka:versioning-plugin:1.9.20")
}

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:versioning-plugin:1.9.20")
    }
}

tasks.dokkaHtml {
    val isSnapshotRelease = project.version.toString().endsWith("-SNAPSHOT")
    val outputPath = "apiDoc/" + if (isSnapshotRelease) "SNAPSHOT" else "latest"

    outputDirectory = layout.buildDirectory.dir(outputPath)
    moduleName = "Vulnlog DSL"

    dokkaSourceSets {
        configureEach {
            perPackageOption {
                matchingRegex = "dev.vulnlog.dsl.definition.*"
                suppress = true
            }
        }
    }

    if (!isSnapshotRelease) {
        doFirst {
            sync {
                from(layout.buildDirectory.dir(outputPath))
                into(layout.projectDirectory.dir("doc/oldVersions/$version"))
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
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
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
