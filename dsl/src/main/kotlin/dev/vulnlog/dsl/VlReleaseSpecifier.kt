package dev.vulnlog.dsl

public sealed interface ReleaseGroup

public data object All : ReleaseGroup

public data object AllOther : ReleaseGroup

/**
 * All releases that are defined in the report.
 *
 * @since v0.5.0
 */
public val all: All = All

/**
 * All other releases that are not already specified in a statement.*
 *
 * @since v0.5.0
 */
public val allOther: AllOther = AllOther
