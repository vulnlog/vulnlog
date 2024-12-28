package dev.vulnlog.dsl3

import dev.vulnlog.dsl3.ReleaseBranch.Factory.allReleases
import java.time.LocalDate
import kotlin.reflect.KProperty
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

data class ReportData(val scanner: String, val awareOfAt: LocalDate, val affectedReleases: List<ReleaseBranch>)

class ReportBuilder {
    var scannerName: String? = null
    var awareOfAt: LocalDate? = null
    val affectedReleases: MutableList<ReleaseBranch> = mutableListOf()

    fun build(): AnalysisBuilder {
        return AnalysisBuilder(ReportData(scannerName!!, awareOfAt!!, affectedReleases))
    }
}

class ReportInit(private val reportBuilder: ReportBuilder) {
    infix fun from(scanner: String): ScannerInit {
        reportBuilder.scannerName = scanner
        return ScannerInit(reportBuilder)
    }
}

class ScannerInit(private val reportBuilder: ReportBuilder) {
    infix fun at(date: String): ScannerSecond {
        reportBuilder.awareOfAt = LocalDate.parse(date)
        return ScannerSecond(reportBuilder)
    }
}

class ScannerSecond(private val reportBuilder: ReportBuilder) {
    infix fun on(releases: ClosedRange<ReleaseBranch>): AnalysisInit2 {
        val releaseList = allReleases.filter { it in releases }
        reportBuilder.affectedReleases += releaseList
        return AnalysisInit2(lazy { reportBuilder.build() })
    }
}

// ---

class AnalysisBuilder(val reportData: ReportData) {
    var analysedAt: LocalDate? = null
    var verdict: String? = null
    var reasoning: String? = null

    fun build(): TaskBuilder {
        return TaskBuilder(AnalysisData(analysedAt!!, verdict!!, reasoning!!))
    }
}

data class AnalysisData(val analysedAt: LocalDate, var verdict: String, var reasoning: String)

class AnalysisInit2(private val analysisBuilder: Lazy<AnalysisBuilder>) {
    infix fun analysedAt(date: String): AnalysisInit {
        analysisBuilder.value.analysedAt = LocalDate.parse(date)
        return AnalysisInit(analysisBuilder)
    }

    infix fun verdict(verdict: String): AnalysisSecond {
        analysisBuilder.value.analysedAt = analysisBuilder.value.reportData.awareOfAt
        analysisBuilder.value.verdict = verdict
        return AnalysisSecond(analysisBuilder)
    }
}

class AnalysisInit(private val analysisBuilder: Lazy<AnalysisBuilder>) {
    infix fun verdict(verdict: String): AnalysisSecond {
        analysisBuilder.value.verdict = verdict
        return AnalysisSecond(analysisBuilder)
    }
}

class AnalysisSecond(private val analysisBuilder: Lazy<AnalysisBuilder>) {
    infix fun because(reasoning: String): TaskInit2 {
        analysisBuilder.value.reasoning = reasoning
        return TaskInit2(lazy { analysisBuilder.value.build() })
    }
}

// ---

sealed interface ReleaseGroup

data object All : ReleaseGroup

data object AllOther : ReleaseGroup

val all = All
val allOther = AllOther

data class TaskData(val analysisData: AnalysisData, val tasks: List<Task>)

class TaskBuilder(val analysisData: AnalysisData) {
    var dependencyName: String? = null
    var action: TaskAction? = null
    val tasks: MutableList<Task> = mutableListOf()

    fun build(): ExecutionBuilder {
        return ExecutionBuilder(TaskData(analysisData, tasks))
    }
}

class TaskInit2(private val taskBuilder: Lazy<TaskBuilder>) {
    infix fun update(dependency: String): TaskUpdateInit {
        taskBuilder.value.dependencyName = dependency
        return TaskUpdateInit(this, taskBuilder.value)
    }

    infix fun waitOnAllFor(duration: Duration): ExecutionInit2 {
        taskBuilder.value.tasks += Task(WaitAction(duration), allReleases)
        return ExecutionInit2(lazy { taskBuilder.value.build() })
    }

    infix fun noActionOn(releaseGroup: ReleaseGroup): ExecutionInit2 {
        val releaseList: List<ReleaseBranch> =
            when (releaseGroup) {
                All -> allReleases
                AllOther ->
                    allReleases.filterNot { a ->
                        taskBuilder.value.tasks.flatMap { b -> b.releases }.contains(a)
                    }
            }
        taskBuilder.value.tasks += Task(NoActionAction, releaseList)
        return ExecutionInit2(lazy { taskBuilder.value.build() })
    }
}

class TaskUpdateInit(private val taskInit2: TaskInit2, private val taskBuilder: TaskBuilder) {
    infix fun atLeastTo(version: String): TaskOn {
        taskBuilder.action = UpdateAction(taskBuilder.dependencyName!!, version)
        return TaskOn(taskInit2, taskBuilder)
    }
}

class TaskNext(private val taskInit2: TaskInit2, private val taskBuilder: TaskBuilder) {
    infix fun andUpdateAtLeastTo(version: String): TaskOn {
        taskBuilder.action = UpdateAction(taskBuilder.dependencyName!!, version)
        return TaskOn(taskInit2, taskBuilder)
    }

    infix fun andNoActionOn(releases: ClosedRange<ReleaseBranch>): ExecutionInit2 {
        val releaseList = allReleases.filter { it in releases }
        taskBuilder.tasks += Task(NoActionAction, releaseList)
        return ExecutionInit2(lazy { taskBuilder.build() })
    }

    infix fun andNoActionOn(releaseGroup: ReleaseGroup): ExecutionInit2 {
        val releaseList: List<ReleaseBranch> =
            when (releaseGroup) {
                All -> allReleases
                AllOther -> allReleases.filterNot { a -> taskBuilder.tasks.flatMap { b -> b.releases }.contains(a) }
            }
        taskBuilder.tasks += Task(NoActionAction, releaseList)
        return ExecutionInit2(lazy { taskBuilder.build() })
    }
}

class TaskOn(private val taskInit2: TaskInit2, private val taskBuilder: TaskBuilder) {
    infix fun on(release: ReleaseBranch): TaskNext {
        taskBuilder.tasks += Task(taskBuilder.action!!, listOf(release))
        return TaskNext(taskInit2, taskBuilder)
    }

    infix fun on(releases: ClosedRange<ReleaseBranch>): TaskNext {
        val releaseList = allReleases.filter { it in releases }
        taskBuilder.tasks += Task(taskBuilder.action!!, releaseList)
        return TaskNext(taskInit2, taskBuilder)
    }

    infix fun on(releaseGroup: ReleaseGroup): TaskNext {
        val releaseList: List<ReleaseBranch> =
            when (releaseGroup) {
                All -> allReleases
                AllOther ->
                    allReleases.filterNot { a ->
                        taskBuilder.tasks.flatMap { b -> b.releases }.contains(a)
                    }
            }
        taskBuilder.tasks += Task(taskBuilder.action!!, releaseList)
        return TaskNext(taskInit2, taskBuilder)
    }
}

sealed interface TaskAction

data object NoActionAction : TaskAction

data class UpdateAction(val dependency: String, val version: String) : TaskAction

data class WaitAction(val forAmountOfTime: Duration) : TaskAction

data class Task(val taskAction: TaskAction, val releases: List<ReleaseBranch>)

// ---

interface Publication

val nextPublication =
    object : Publication {
        override fun toString(): String {
            return "next publication"
        }
    }

val Int.days: Duration
    get() = this.toDuration(DurationUnit.DAYS)

class ExecutionData(val taskData: TaskData, execution: List<Execution>)

class ExecutionBuilder(val taskData: TaskData) {
    val executions: MutableList<Execution> = mutableListOf()
    var absolutDuration: Duration? = null
    var relativePublication: Publication? = null

    fun build(): ExecutionData {
        return ExecutionData(taskData, executions)
    }
}

class ExecutionInit2(private val executionBuilder: Lazy<ExecutionBuilder>) {
    infix fun suppressOn(releases: ClosedRange<ReleaseBranch>): ExecutionNext {
        val releaseList = allReleases.filter { it in releases }
        executionBuilder.value.executions += Execution("suppress", "permanent", releaseList)
        return ExecutionNext(this, executionBuilder.value)
    }
}

class ExecutionNext(private val executionInit2: ExecutionInit2, private val executionBuilder: ExecutionBuilder) {
    infix fun andSuppressFor(duration: Duration): ExecutionOnAbsolute {
        executionBuilder.absolutDuration = duration
        return ExecutionOnAbsolute(executionInit2, executionBuilder)
    }

    infix fun andSuppressUntil(publication: Publication): ExecutionOnRelative {
        executionBuilder.relativePublication = publication
        return ExecutionOnRelative(executionInit2, executionBuilder)
    }
}

class ExecutionOnAbsolute(private val executionInit2: ExecutionInit2, private val executionBuilder: ExecutionBuilder) {
    infix fun on(release: ReleaseBranch): ExecutionNext {
        executionBuilder.executions +=
            Execution(
                "suppress",
                executionBuilder.absolutDuration.toString(),
                listOf(release),
            )
        return ExecutionNext(executionInit2, executionBuilder)
    }

    infix fun on(releases: ClosedRange<ReleaseBranch>): ExecutionInit2 {
        val releaseList = allReleases.filter { it in releases }
        executionBuilder.executions += Execution("suppress", executionBuilder.absolutDuration.toString(), releaseList)
        return executionInit2
    }

    infix fun on(releaseGroup: ReleaseGroup): ExecutionData {
        val releaseList: List<ReleaseBranch> =
            when (releaseGroup) {
                All -> allReleases
                AllOther ->
                    allReleases.filterNot { a ->
                        executionBuilder.executions.flatMap { it.releases }.contains(a)
                    }
            }
        executionBuilder.executions += Execution("suppress", executionBuilder.absolutDuration.toString(), releaseList)
        return executionBuilder.build()
    }
}

class ExecutionOnRelative(private val executionInit2: ExecutionInit2, private val executionBuilder: ExecutionBuilder) {
    infix fun on(release: ReleaseBranch): ExecutionNext {
        executionBuilder.executions +=
            Execution(
                "suppress",
                executionBuilder.relativePublication!!.toString(),
                listOf(release),
            )
        return ExecutionNext(executionInit2, executionBuilder)
    }

    infix fun on(releases: ClosedRange<ReleaseBranch>): ExecutionNext {
        val releaseList = allReleases.filter { it in releases }
        executionBuilder.executions +=
            Execution(
                "suppress",
                executionBuilder.relativePublication!!.toString(),
                releaseList,
            )
        return ExecutionNext(executionInit2, executionBuilder)
    }

    infix fun on(releaseGroup: ReleaseGroup): ExecutionData {
        val releaseList: List<ReleaseBranch> =
            when (releaseGroup) {
                All -> allReleases
                AllOther ->
                    allReleases.filterNot { a ->
                        executionBuilder.executions.flatMap { b -> b.releases }.contains(a)
                    }
            }
        executionBuilder.executions +=
            Execution(
                "suppress",
                executionBuilder.relativePublication!!.toString(),
                releaseList,
            )
        return executionBuilder.build()
    }
}

data class Execution(val action: String, val duration: String, val releases: List<ReleaseBranch>)

// ---

const val SCA_SCANNER = "Scanner"

class VulnContext {
    private val rBuilder = ReportBuilder()
    val aBuilder = lazy { rBuilder.build() }
    val tBuilder: Lazy<TaskBuilder> = lazy { aBuilder.value.build() }
    val eBuilder: Lazy<ExecutionBuilder> = lazy { tBuilder.value.build() }

    var r: ReportInit = ReportInit(rBuilder)
    var a: AnalysisInit2 = AnalysisInit2(aBuilder)
    var t: TaskInit2 = TaskInit2(tBuilder)
    var e: ExecutionInit2 = ExecutionInit2(eBuilder)
}

interface MyVuln {
    val data: List<VulnlogData>

    fun vuln(
        id: String,
        block: VulnContext.() -> Unit,
    ): VulnlogData

    fun vuln(
        vararg id: String,
        block: VulnContext.() -> Unit,
    ): VulnlogData
}

class MyVulnImpl : MyVuln {
    override val data = mutableListOf<VulnlogData>()

    override fun vuln(
        id: String,
        block: VulnContext.() -> Unit,
    ): VulnlogData = createVulnlogData(listOf(id), block)

    override fun vuln(
        vararg id: String,
        block: VulnContext.() -> Unit,
    ): VulnlogData = createVulnlogData(id.toList(), block)

    private fun createVulnlogData(
        id: List<String>,
        block: VulnContext.() -> Unit,
    ): VulnlogData =
        with(VulnContext()) {
            block()

            val reportData = aBuilder.value.reportData
            val analysisData = tBuilder.value.analysisData
            val taskData = eBuilder.value.taskData
            val executionData = eBuilder.value.build()

            val report: VulnlogReportData =
                if (reportData.scanner.isBlank()) {
                    VulnlogReportDataEmpty
                } else {
                    VulnlogReportDataImpl(
                        reportData.scanner,
                        reportData.awareOfAt,
                        reportData.affectedReleases.map { ReleaseBranchDataImpl(it.name) },
                    )
                }

            val analysis: VulnlogAnalysisData =
                if (analysisData.verdict.isBlank()) {
                    VulnlogAnalysisDataEmpty
                } else {
                    VulnlogAnalysisDataImpl(analysisData.analysedAt, analysisData.verdict, analysisData.reasoning)
                }

            val task: VulnlogTaskData =
                if (taskData.tasks.isEmpty()) {
                    VulnlogTaskDataEmpty
                } else {
                    val taskActionsToReleaseBranches: Map<TaskAction, List<ReleaseBranchData>> =
                        taskData.tasks.associate { task ->
                            task.taskAction to task.releases.map(ReleaseBranch::name).map(::ReleaseBranchDataImpl)
                        }
                    VulnlogTaskDataImpl(taskActionsToReleaseBranches)
                }

            val execution: VulnlogExecutionData =
                if (executionData.taskData.tasks.isEmpty()) {
                    VulnlogExecutionDataEmpty
                } else {
                    val taskData2 =
                        executionData.taskData.tasks
                            .map { t ->
                                TaskDataImpl(
                                    t.taskAction,
                                    t.releases.map(ReleaseBranch::name).map(::ReleaseBranchDataImpl),
                                )
                            }
                    VulnlogExecutionDataImpl(taskData2)
                }

            val vulnlogData = VulnlogData(id, report, analysis, task, execution)
            data += vulnlogData
            vulnlogData
        }
}

// ---

class ReleaseBranch private constructor(private val id: Int, val name: String) : Comparable<ReleaseBranch> {
    companion object Factory {
        private var counter = 0
        val allReleases = mutableListOf<ReleaseBranch>()

        fun create(name: String): ReleaseBranch {
            val releaseBranch = ReleaseBranch(counter++, name)
            allReleases.add(releaseBranch)
            return releaseBranch
        }

        operator fun getValue(
            thisRef: Any?,
            property: KProperty<*>,
        ): ReleaseBranch {
            return allReleases.first { it.name == property.name }
        }
    }

    override fun compareTo(other: ReleaseBranch): Int {
        return id.compareTo(other.id)
    }

    override fun toString(): String {
        return "ReleaseBranch(name='$name')"
    }
}

data class VulnlogData(
    val ids: List<String>,
    val reportData: VulnlogReportData = VulnlogReportDataEmpty,
    val analysisData: VulnlogAnalysisData = VulnlogAnalysisDataEmpty,
    val taskData: VulnlogTaskData = VulnlogTaskDataEmpty,
    val executionData: VulnlogExecutionData = VulnlogExecutionDataEmpty,
)

sealed interface ReleaseBranchData {
    val name: String
}

data class ReleaseBranchDataImpl(override val name: String) : ReleaseBranchData

data object DefaultReleaseBranchDataImpl : ReleaseBranchData {
    override val name: String = "Default Release Branch"
}

sealed interface ReleaseVersionData {
    val version: String
    val releaseDate: LocalDate?
}

data class ReleaseVersionDataImpl(override val version: String, override val releaseDate: LocalDate?) :
    ReleaseVersionData

sealed interface VulnlogReportData {
    val scanner: String
    val awareAt: LocalDate
    val affected: List<ReleaseBranchData>
}

sealed interface TaskData2 {
    val taskAction: TaskAction
    val releases: List<ReleaseBranchData>
}

data class TaskDataImpl(override val taskAction: TaskAction, override val releases: List<ReleaseBranchData>) : TaskData2

data class VulnlogReportDataImpl(
    override val scanner: String,
    override val awareAt: LocalDate,
    override val affected: List<ReleaseBranchData>,
) : VulnlogReportData

object VulnlogReportDataEmpty : VulnlogReportData {
    override val scanner: String = ""
    override val awareAt: LocalDate = LocalDate.MIN
    override val affected: List<ReleaseBranchData> = emptyList()

    override fun toString(): String {
        return "VulnlogReportDataEmpty()"
    }
}

sealed interface VulnlogAnalysisData {
    val analysedAt: LocalDate
    val verdict: String
    val reasoning: String
}

data class VulnlogAnalysisDataImpl(
    override val analysedAt: LocalDate,
    override val verdict: String,
    override val reasoning: String,
) : VulnlogAnalysisData

object VulnlogAnalysisDataEmpty : VulnlogAnalysisData {
    override val analysedAt: LocalDate = LocalDate.MIN
    override val verdict: String = ""
    override val reasoning: String = ""

    override fun toString(): String {
        return "VulnlogAnalysisDataEmpty()"
    }
}

sealed interface VulnlogTaskData {
    val taskOnReleaseBranch: Map<TaskAction, List<ReleaseBranchData>>
}

data class VulnlogTaskDataImpl(override val taskOnReleaseBranch: Map<TaskAction, List<ReleaseBranchData>>) :
    VulnlogTaskData

object VulnlogTaskDataEmpty : VulnlogTaskData {
    override val taskOnReleaseBranch: Map<TaskAction, List<ReleaseBranchData>> = emptyMap()

    override fun toString(): String {
        return "VulnlogAnalysisDataEmpty()"
    }
}

sealed interface VulnlogExecutionData {
    val tasks: List<TaskData2>
}

data class VulnlogExecutionDataImpl(override val tasks: List<TaskData2>) : VulnlogExecutionData

object VulnlogExecutionDataEmpty : VulnlogExecutionData {
    override val tasks: List<TaskData2> = emptyList()

    override fun toString(): String {
        return "VulnlogExecutionDataEmpty()"
    }
}
