import com.github.gradle.node.npm.task.NpxTask
import dev.vulnlog.gradle.VulnlogOpenVexTask
import dev.vulnlog.gradle.VulnlogOpenVexUpdateTask

plugins {
    id("dev.vulnlog.plugin") version "0.18.0"
    id("com.github.node-gradle.node") version "7.1.0"
}

node {
    download.set(true)
    version.set("22.11.0")
    workDir.set(layout.buildDirectory.dir("nodejs"))
    npmWorkDir.set(layout.buildDirectory.dir("npm"))
}

val gitHash: String = try {
    providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
    }.standardOutput.asText.get().trim()
} catch (_: Exception) {
    "unknown"
}

val snapshotVersion = "SNAPSHOT+$gitHash"
val appVersion: String = project.findProperty("appVersion")?.toString() ?: snapshotVersion

allprojects {
    version = appVersion
}

tasks.register("installGitHooks") {
    description = "Installs the ktlint pre-commit hook into .git/hooks/"
    notCompatibleWithConfigurationCache("Writes to .git which is outside the project")
    val hook = file(".git/hooks/pre-commit")
    doLast {
        hook.writeText(
            """
            #!/bin/sh

            # Auto-format staged Kotlin files via Gradle, re-stage any fixes, then verify.
            # Unfixable violations will still block the commit.

            if ! git diff --name-only --cached --relative -- '*.kt' '*.kts' | grep -q .; then
              exit 0
            fi

            ./gradlew ktlintFormat --quiet

            FORMATTED=${'$'}(git diff --name-only --relative -- '*.kt' '*.kts')
            if [ -n "${'$'}FORMATTED" ]; then
              echo "${'$'}FORMATTED" | xargs git add
            fi

            ./gradlew ktlintCheck --quiet
            """.trimIndent() + "\n",
        )
        hook.setExecutable(true)
        logger.lifecycle("Installed pre-commit hook at ${hook.path}")
    }
}

tasks.register<NpxTask>("docsBuild") {
    group = "documentation"
    description = "Builds the docs site locally into build/docs using Antora"
    command.set("antora")
    args.set(listOf("antora-local-playbook.yml"))
    inputs.dir("docs")
    inputs.file("antora-local-playbook.yml")
    outputs.dir(layout.buildDirectory.dir("docs"))
}

vulnlog {
    files = files.from("vulnlog.yaml")

    report {
        changelog {
            brief = true
            format = "markdown"
        }
    }
}

// VEX Hub crawls .vex/ and serves one document per package, so each artifact gets one covering all published releases.
val vexArtifactTaskSuffixByTag = mapOf("cli" to "Cli", "container" to "Container", "gradle plugin" to "GradlePlugin")

val vexDocumentUpdates = vexArtifactTaskSuffixByTag.map { (tag, taskSuffix) ->
    val fileName = "vulnlog-${tag.replace(' ', '-')}.openvex.json"
    val baselineFile = layout.projectDirectory.file(".vex/$fileName")

    val openVex = tasks.register<VulnlogOpenVexTask>("vulnlogOpenVex$taskSuffix") {
        group = "vulnlog"
        description = "Generates the OpenVEX document of the $tag artifact"
        files.from("vulnlog.yaml")
        tags = setOf(tag)
        baseline = baselineFile
        outputFile = layout.buildDirectory.file("vulnlog/vex/$fileName")
    }

    tasks.register<VulnlogOpenVexUpdateTask>("vulnlogOpenVex${taskSuffix}Update") {
        group = "vulnlog"
        description = "Updates the committed OpenVEX document of the $tag artifact"
        generatedFile = openVex.flatMap { it.outputFile }
        baseline = baselineFile
    }
}

tasks.register("updateVexDocuments") {
    group = "vulnlog"
    description = "Updates the committed VEX documents in .vex/ of all artifacts"
    dependsOn(vexDocumentUpdates)
}
