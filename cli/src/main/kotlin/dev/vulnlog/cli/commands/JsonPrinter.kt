package dev.vulnlog.cli.commands

import dev.vulnlog.cli.serialisable.Execution
import dev.vulnlog.cli.serialisable.FixExecution
import dev.vulnlog.cli.serialisable.PermanentSuppressionExecution
import dev.vulnlog.cli.serialisable.TemporarySuppressionExecution
import dev.vulnlog.cli.serialisable.UntilNextReleaseSuppressionExecution
import dev.vulnlog.cli.serialisable.Vulnlog
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

class JsonPrinter(private val printer: Output) {
    private val format =
        Json {
            allowStructuredMapKeys = true
            prettyPrint = true
            serializersModule =
                SerializersModule {
                    polymorphic(
                        Execution::class,
                        FixExecution::class,
                        FixExecution.serializer(),
                    )
                    polymorphic(
                        Execution::class,
                        PermanentSuppressionExecution::class,
                        PermanentSuppressionExecution.serializer(),
                    )
                    polymorphic(
                        Execution::class,
                        TemporarySuppressionExecution::class,
                        TemporarySuppressionExecution.serializer(),
                    )
                    polymorphic(
                        Execution::class,
                        UntilNextReleaseSuppressionExecution::class,
                        UntilNextReleaseSuppressionExecution.serializer(),
                    )
                }
        }

    fun print(vulnlog: Vulnlog) {
        printer.output(format.encodeToString(vulnlog))
    }

    fun translate(vulnlog: Vulnlog): String {
        return format.encodeToString(vulnlog)
    }
}
