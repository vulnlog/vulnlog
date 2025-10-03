package dev.vulnlog.dsl

import kotlin.time.Duration

public sealed interface SuppressionSpecifier

public data object VlSuppressionPermanent : SuppressionSpecifier

/**
 * Permanently suppress a vulnerability.
 *
 * @since v0.5.0
 */
public val permanent: VlSuppressionPermanent = VlSuppressionPermanent

public data class VlSuppressionTemporarily(val duration: Duration) : SuppressionSpecifier

/**
 * Temporarily suppress a vulnerability.
 *
 * @since v0.5.0
 */
public val temporarily: VlSuppressionTemporarily = VlSuppressionTemporarily(0.days)

public data object VlSuppressionUntilNextPublication : SuppressionSpecifier

/**
 * Suppress a vulnerability until the successor release version is published.
 *
 * @since v0.5.0
 */
public val untilNextPublication: VlSuppressionUntilNextPublication = VlSuppressionUntilNextPublication
