package io.vulnlog.gradleplugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.get

class VulnLogPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val dslDependeny = project.dependencies.create("io.vulnlog.dsl2:dsl2:0.1.0")
        project.configurations["compileOnly"].dependencies.add(dslDependeny)
    }
}
