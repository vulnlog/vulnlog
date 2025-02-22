import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.2.1"
}

group = "dev.vulnlog"
version = providers.gradleProperty("vlVersion").get()

repositories {
    mavenLocal()
    mavenCentral()
}

gradlePlugin {
    website = "https://github.com/vulnlog/vulnlog"
    vcsUrl = "https://github.com/vulnlog/vulnlog.git"
    plugins {
        create("vulnlogPlugin") {
            id = "dev.vulnlog.dslplugin"
            displayName = "Vulnlog DSL Plugin"
            description =
                """
                The Vulnlog DSL definition for describing and tracking SCA vulnerability reports for your software project.
                """.trimIndent()
            tags = listOf("vulnlog", "vl", "sca", "vulnerability", "logging", "security", "tracking", "dsl")
            implementationClass = "dev.vulnlog.gradleplugin.VulnlogPlugin"
        }
    }
}

tasks.named<Copy>("processResources") {
    val vlVersion = providers.gradleProperty("vlVersion")
    doFirst {
        filesMatching("version.txt") {
            filter(ReplaceTokens::class, "tokens" to mapOf("vlVersion" to vlVersion.get()))
        }
    }
}
