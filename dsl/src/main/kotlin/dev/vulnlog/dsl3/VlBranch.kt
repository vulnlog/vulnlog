package dev.vulnlog.dsl3

import kotlin.reflect.KProperty

class VlBranch private constructor(val name: String) {
    companion object Factory {
        val data = mutableMapOf<String, VlBranch>()

        fun createBranch(name: String): VlBranch {
            val release = VlBranch(name)
            data[name] = release
            return release
        }

        operator fun getValue(
            thisRef: Any?,
            property: KProperty<*>,
        ): VlBranch {
            return checkNotNull(data[property.name]) { "Branch not defined ${property.name}" }
        }
    }

    override fun toString(): String {
        return "Branch(name='$name')"
    }
}
