plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.2.1"
}

group = "io.vulnlog.gradleplugin"
version = "0.1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

gradlePlugin {
    website = "https://github.com/vulnlog/vulnlog"
    vcsUrl = "https://github.com/vulnlog/vulnlog.git"
    plugins {
        create("vulnlogPlugin") {
            id = "io.vulnlog.plugin"
            description =
                """
                Asdf
                """.trimIndent()
            implementationClass = "io.vulnlog.gradleplugin.VulnLogPlugin"
        }
    }
}
