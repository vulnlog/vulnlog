package dev.vulnlog.dsl2

interface VlResolution {
    fun resolution(resolutions: MyResolution): Task

    fun resolution(vararg resolutions: MyResolution): Array<Task>
}
