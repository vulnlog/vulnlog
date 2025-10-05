import java.text.SimpleDateFormat
import java.util.Date

plugins {
    id("vulnlog.security-convention")
}

val gitHash: String by lazy {
    try {
        "git rev-parse --short HEAD".runCommand(project.rootDir).trim()
    } catch (e: Exception) {
        "unknown"
    }
}

fun String.runCommand(workingDir: File): String {
    return ProcessBuilder(*split(" ").toTypedArray())
        .directory(workingDir)
        .redirectErrorStream(true)
        .start()
        .inputStream.bufferedReader().readText()
}

val snapshotVersion = "SNAPSHOT-${SimpleDateFormat("yyyyMMdd-HHmmss").format(Date())}+$gitHash"
val appVersion: String = project.findProperty("appVersion")?.toString() ?: snapshotVersion

allprojects {
    version = appVersion
}
