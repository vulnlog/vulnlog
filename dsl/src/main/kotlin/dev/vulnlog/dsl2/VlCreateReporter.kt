package dev.vulnlog.dsl2

interface VlCreateReporter {
    fun reporter(name: String): MyEffectiveReporter

    fun reporters(vararg names: String): Array<MyEffectiveReporter>
}
