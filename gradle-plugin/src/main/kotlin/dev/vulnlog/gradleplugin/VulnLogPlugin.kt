package dev.vulnlog.gradleplugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.get

class VulnLogPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val dslVersion = javaClass.getResource("/version.txt")?.readText()?.lines()?.first() ?: ""
        val dslDependency = project.dependencies.create("dev.vulnlog:dsl:$dslVersion")
        project.configurations["compileOnly"].dependencies.add(dslDependency)
    }
}
