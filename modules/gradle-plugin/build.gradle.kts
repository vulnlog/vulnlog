import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.plugin.devel.tasks.PluginUnderTestMetadata

plugins {
    id("vulnlog.common-convention")
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "2.1.1"
    id("com.gradleup.shadow") version "9.4.1"
}

description = "Vulnlog Gradle plugin"

group = "dev.vulnlog"

val shaded: Configuration by configurations.creating

configurations {
    compileOnly.get().extendsFrom(shaded)
    testImplementation.get().extendsFrom(shaded)
}

dependencies {
    shaded(project(":lib"))

    testImplementation(gradleTestKit())
    testImplementation(libs.kotestAssertionsCoreJvm)
    testImplementation(libs.kotestRunnerJunit5Jvm)
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    configurations = listOf(shaded)
    relocate("tools.jackson", "dev.vulnlog.shaded.tools.jackson")
    relocate("com.github.packageurl", "dev.vulnlog.shaded.com.github.packageurl")
    mergeServiceFiles()
}

tasks.named<Jar>("jar") {
    enabled = false
    dependsOn(tasks.named("shadowJar"))
}

tasks.named<PluginUnderTestMetadata>("pluginUnderTestMetadata") {
    pluginClasspath.from(shaded)
}

gradlePlugin {
    website = "https://github.com/vulnlog/vulnlog"
    vcsUrl = "https://github.com/vulnlog/vulnlog.git"
    plugins {
        create("vulnlog") {
            id = "dev.vulnlog.plugin"
            displayName = "Vulnlog"
            description =
                """
                Manage vulnerability records as code. Validate Vulnlog YAML files,
                generate suppression files for downstream scanners (Trivy, Snyk,
                generic format), and produce self-contained HTML vulnerability reports.
                """.trimIndent()
            tags =
                listOf(
                    "vulnlog",
                    "vulnerability",
                    "security",
                    "cve",
                    "audit",
                    "suppression",
                    "sca",
                    "trivy",
                    "snyk",
                )
            implementationClass = "dev.vulnlog.gradle.VulnlogPlugin"
        }
    }
}
