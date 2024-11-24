plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.2.1"
}

group = "dev.vulnlog.gradleplugin"
version = "0.3.0-SNAPSHOT"

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
