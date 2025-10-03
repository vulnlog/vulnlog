package dev.vulnlog.dslinterpreter.impl

import dev.vulnlog.dsl.VlSuppressContext

class VlSuppressContextImpl : VlSuppressContext {
    override var templateFilename: String = ""
    override var idMatcher: String = ""
    override var template: String = ""
}
