package dev.vulnlog.gradleplugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.get

class VulnLogPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val dslDependency = project.dependencies.create("dev.vulnlog.dsl:dsl:0.2.0")
        project.configurations["compileOnly"].dependencies.add(dslDependency)
    }
}
