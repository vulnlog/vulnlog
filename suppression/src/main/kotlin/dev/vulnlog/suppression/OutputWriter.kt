package dev.vulnlog.suppression

import java.io.File

data class OutputData(val filename: String, val content: List<String>)

interface OutputWriter {
    fun writeText(data: OutputData)
}

class FileWriter(private val outputDir: File) : OutputWriter {
    override fun writeText(data: OutputData) {
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        outputDir.resolve(data.filename).writeText(data.content.joinToString("\n"))
    }
}

class ConsoleWriter(private val out: (String) -> Unit) : OutputWriter {
    override fun writeText(data: OutputData) {
        out("Suppression File: ${data.filename}")
        out(data.content.joinToString("\n"))
    }
}
