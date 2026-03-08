plugins {
    id("vulnlog.security-convention")
}

val gitHash: String = try {
    providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
    }.standardOutput.asText.get().trim()
} catch (e: Exception) {
    "unknown"
}

val snapshotVersion = "SNAPSHOT+$gitHash"
val appVersion: String = project.findProperty("appVersion")?.toString() ?: snapshotVersion

allprojects {
    version = appVersion
}
