package dev.vulnlog.gradleplugin

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URI

abstract class DownloadTask : DefaultTask() {
    @get:Input
    abstract val version: Property<String>

    @get:OutputFile
    val output: Provider<RegularFile> = project.layout.buildDirectory.file("vl.zip")

    @TaskAction
    fun action() {
        val versionString = version.get()
        println("downloading version $versionString")
        val downloadUri = "https://vulnlog.dev/releases/v$versionString/vl-$versionString.zip"
        val input: InputStream = URI.create(downloadUri).toURL().openStream()
        val outputStream = FileOutputStream(output.get().asFile)
        copyInputStreamToFile(input, outputStream)
    }

    private fun copyInputStreamToFile(
        inputStream: InputStream,
        outputStream: FileOutputStream,
    ) {
        val buffer = ByteArray(8192)
        inputStream.use { input ->
            outputStream.use { fileOut ->

                while (true) {
                    val length = input.read(buffer)
                    if (length <= 0) {
                        break
                    }
                    fileOut.write(buffer, 0, length)
                }
                fileOut.flush()
                fileOut.close()
            }
        }
        inputStream.close()
    }
}
