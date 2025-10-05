plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.2.1"
}

group = "dev.vulnlog"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("de.undercouch:gradle-download-task:5.6.0")
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
