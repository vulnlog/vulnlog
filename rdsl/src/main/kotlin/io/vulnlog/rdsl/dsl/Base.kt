package io.vulnlog.rdsl.dsl

import io.vulnlog.rdsl.definition.VulnLogRscript

open class Base : VulnLogRscript() {
    val myValues = mutableListOf<String>()

    /**
     * This is a very helpful documentation.
     */
    fun testFu1() = println("output of testFu1")

    /**
     * Add [input] into [myValues].
     */
    fun testFu2(input: String) {
        myValues += input
    }
}
