package dev.vulnlog.dsl

import kotlin.time.Duration

sealed interface SuppressionSpecifier

data object SuppressionSpecifierPermanent : SuppressionSpecifier

val permanent = SuppressionSpecifierPermanent

data class SuppressionSpecifierTemporarily(val duration: Duration) : SuppressionSpecifier {
    companion object {
        val temporarily = SuppressionSpecifierTemporarily(0.days)
    }
}

data object SuppressionSpecifierUntilNextPublication : SuppressionSpecifier

val untilNextPublication = SuppressionSpecifierUntilNextPublication
