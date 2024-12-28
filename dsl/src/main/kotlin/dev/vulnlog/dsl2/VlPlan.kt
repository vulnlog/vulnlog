package dev.vulnlog.dsl2

interface VlPlan {
    fun plan(task: MyTaskPlan)

    fun plan(vararg tasks: MyTaskPlan)
}
