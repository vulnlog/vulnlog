package dev.vulnlog.dsl

sealed interface ReleaseGroup

data object All : ReleaseGroup

data object AllOther : ReleaseGroup

/**
 * All releases that are defined in the report.
 *
 * @since v0.5.0
 */
val all = All

/**
 * All other releases that are not already specified in a statement.*
 *
 * @since v0.5.0
 */
val allOther = AllOther
