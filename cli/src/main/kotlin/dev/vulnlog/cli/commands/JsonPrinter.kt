package dev.vulnlog.cli.commands

import dev.vulnlog.cli.serialisable.Vulnlog
import kotlinx.serialization.json.Json

class JsonPrinter(private val printer: (String) -> Unit) {
    private val format =
        Json {
            allowStructuredMapKeys = true
            prettyPrint = true
        }

    fun print(vulnlog: Vulnlog) {
        printer(format.encodeToString(vulnlog))
    }

    fun translate(vulnlog: Vulnlog): String {
        return format.encodeToString(vulnlog)
    }
}
