package dev.vulnlog.dsl

import kotlin.time.Duration

public sealed interface SuppressionSpecifier

public data object SuppressionSpecifierPermanent : SuppressionSpecifier

/**
 * Permanently suppress a vulnerability.
 *
 * @since v0.5.0
 */
public val permanent: SuppressionSpecifierPermanent = SuppressionSpecifierPermanent

public data class SuppressionSpecifierTemporarily(val duration: Duration) : SuppressionSpecifier

/**
 * Temporarily suppress a vulnerability.
 *
 * @since v0.5.0
 */
public val temporarily: SuppressionSpecifierTemporarily = SuppressionSpecifierTemporarily(0.days)

public data object SuppressionSpecifierUntilNextPublication : SuppressionSpecifier

/**
 * Suppress a vulnerability until the successor release version is published.
 *
 * @since v0.5.0
 */
public val untilNextPublication: SuppressionSpecifierUntilNextPublication = SuppressionSpecifierUntilNextPublication
