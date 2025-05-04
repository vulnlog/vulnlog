package dev.vulnlog.dslinterpreter.impl

import dev.vulnlog.dsl.VlReporterConfig
import dev.vulnlog.dsl.VlReporterContext
import dev.vulnlog.dsl.VlSuppressContext

class VlReporterContextImpl : VlReporterContext {
    var config: VlReporterConfig? = null

    override fun suppression(block: (VlSuppressContext).() -> Unit) {
        block.let { ctx ->
            VlSuppressContextImpl()
                .apply(ctx)
                .also { config = VlReporterConfig(it.templateFilename, it.idMatcher, it.template) }
        }
    }
}
