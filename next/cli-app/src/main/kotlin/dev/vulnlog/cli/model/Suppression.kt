package dev.vulnlog.cli.model

import java.time.LocalDate

data class Suppression(
    /**
     * Expiration date. After this date, the entry is excluded from the generated suppression file. When omitted, the suppression is permanent.
     */
    val expiresAt: LocalDate? = null,
)
