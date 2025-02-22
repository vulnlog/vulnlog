package dev.vulnlog.cli.commands

import dev.vulnlog.cli.serialisable.ReleaseBranches
import kotlinx.serialization.json.Json

class JsonPrinter(private val printer: (String) -> Unit) {
    private val format =
        Json {
            allowStructuredMapKeys = true
            prettyPrint = true
        }

    fun print(releaseBranches: ReleaseBranches) {
        printer(format.encodeToString(releaseBranches))
    }
}
