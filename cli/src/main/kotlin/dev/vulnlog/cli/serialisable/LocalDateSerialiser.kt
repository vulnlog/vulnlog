package dev.vulnlog.cli.serialisable

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object LocalDateSerialiser : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: LocalDate,
    ) {
        val result = value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        encoder.encodeString(result)
    }

    override fun deserialize(decoder: Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString())
    }
}
