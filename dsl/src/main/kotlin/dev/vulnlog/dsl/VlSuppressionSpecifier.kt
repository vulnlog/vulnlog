package dev.vulnlog.dsl

import kotlin.time.Duration

sealed interface SuppressionSpecifier

data object SuppressionSpecifierPermanent : SuppressionSpecifier

/**
 * Permanently suppress a vulnerability.
 *
 * @since v0.5.0
 */
val permanent = SuppressionSpecifierPermanent

data class SuppressionSpecifierTemporarily(val duration: Duration) : SuppressionSpecifier {
    companion object {
        /**
         * Temporarily suppress a vulnerability.
         *
         * @since v0.5.0
         */
        val temporarily = SuppressionSpecifierTemporarily(0.days)
    }
}

data object SuppressionSpecifierUntilNextPublication : SuppressionSpecifier

/**
 * Suppress a vulnerability until the successor release version is published.
 *
 * @since v0.5.0
 */
val untilNextPublication = SuppressionSpecifierUntilNextPublication
