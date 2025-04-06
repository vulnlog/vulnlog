package dev.vulnlog.dsl

/**
 * Describes the status of a vulnerability.
 */
public enum class ResultStatus {
    /**
     * The software project is affected by this vulnerability.
     */
    AFFECTED,

    /**
     * The software project is not anymore affected by this vulnerability.
     */
    FIXED,

    /**
     * The software project is not affected by this vulnerability.
     */
    NOT_AFFECTED,

    /**
     * If the software project is affected by this vulnerability is currently  under investigation.
     */
    UNDER_INVESTIGATION,

    /**
     * The state of this vulnerability report is not known.
     */
    UNKNOWN,
}
