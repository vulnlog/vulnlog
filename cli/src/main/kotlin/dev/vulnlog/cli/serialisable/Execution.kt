package dev.vulnlog.cli.serialisable

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
sealed interface Execution {
    val action: String
    val releases: List<String>
}

@Serializable
@SerialName("fix")
data class FixExecution(
    override val action: String,
    override val releases: List<String>,
    @Serializable(LocalDateSerialiser::class)
    val fixDate: LocalDate,
) : Execution

@Serializable
@SerialName("permanent_suppression")
data class PermanentSuppressionExecution(
    override val action: String,
    override val releases: List<String>,
) : Execution

@Serializable
@SerialName("temporary_suppression")
data class TemporarySuppressionExecution(
    override val action: String,
    override val releases: List<String>,
    @Serializable(LocalDateSerialiser::class)
    val untilDate: LocalDate,
) : Execution

@Serializable
@SerialName("until_next_release_suppression")
data class UntilNextReleaseSuppressionExecution(
    override val action: String,
    override val releases: List<String>,
    val nextReleaseName: String?,
    @Serializable(LocalDateSerialiser::class)
    val nextReleaseDate: LocalDate?,
) : Execution
