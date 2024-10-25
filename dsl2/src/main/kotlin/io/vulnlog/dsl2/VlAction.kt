package io.vulnlog.dsl2

interface VlAction<out T> {
    /**
     * Apply an action for the vulnerability.
     *
     * @receiver context describing the vulnerability.
     * @return action to apply for this vulnerability.
     */
    fun action(context: VlActionContext<VlSuppressionBuilder>.() -> Unit): T
}
