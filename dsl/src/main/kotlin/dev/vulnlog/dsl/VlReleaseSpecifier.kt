package dev.vulnlog.dsl

sealed interface ReleaseGroup

data object All : ReleaseGroup

data object AllOther : ReleaseGroup

val all = All
val allOther = AllOther
