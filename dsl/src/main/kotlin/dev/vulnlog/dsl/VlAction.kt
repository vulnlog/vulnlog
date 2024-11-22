package dev.vulnlog.dsl

interface VlAction {
    /**
     * Apply an action for the vulnerability.
     *
     * @receiver context describing the vulnerability.
     * @return action to apply for this vulnerability.
     */
    fun action(context: VlActionContext.() -> Unit)
}
