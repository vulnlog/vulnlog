plugins {
    id("vulnlog.security-convention")
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
