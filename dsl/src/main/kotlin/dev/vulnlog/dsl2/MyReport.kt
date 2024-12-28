package dev.vulnlog.dsl2

import dev.vulnlog.dsl2.impl.VlReportAtContextImpl
import java.time.LocalDate

typealias MyReport = () -> VlReportFor

infix fun MyReport.atDate(date: String): MyAnalysedAt {
    return MyAnalysedAt(this.invoke(), LocalDate.parse(date))
}

typealias MyEffectiveReporter = () -> VlReporter

/**
 * The date when this vulnerability report came to attention. Usually the date this vulnerability is added to this
 * Vulnlog file. This can be the release date of the vulnerability but may also be after it.
 *
 * [date] is expected in the format YYYY-MM-dd.
 */
infix fun MyEffectiveReporter.atDate(date: String): VlReportAtContext {
    return VlReportAtContextImpl(this.invoke(), LocalDate.parse(date))
}
