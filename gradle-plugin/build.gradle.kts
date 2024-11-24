import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.2.1"
}

group = "dev.vulnlog.gradleplugin"
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
            id = "dev.vulnlog.kickstarter"
            displayName = "Vulnlog DSL"
            description =
                """
                A simple and easy to use DSL to describe vulnerabilities in software projects.
                """.trimIndent()
            tags = listOf("vulnerability", "logging", "security", "tracking", "DSL")
            implementationClass = "dev.vulnlog.gradleplugin.VulnLogPlugin"
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
