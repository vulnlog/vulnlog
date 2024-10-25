package io.vulnlog.dsl2

interface VlRating<out T> {
    /**
     * Vulnerability has a critical impact on the product.
     *
     * @param dateOfAnalysing use the format YYYY-MM-dd to specify.
     * @param reasoning justifying the rating.
     */
    fun critical(
        dateOfAnalysing: String,
        reasoning: String,
    ): T

    /**
     * Vulnerability has a high impact on the product.
     *
     * @param dateOfAnalysing use the format YYYY-MM-dd to specify.
     * @param reasoning justifying the rating.
     */
    fun high(
        dateOfAnalysing: String,
        reasoning: String,
    ): T

    /**
     * Vulnerability has a moderate impact on the product.
     *
     * @param dateOfAnalysing use the format YYYY-MM-dd to specify.
     * @param reasoning justifying the rating.
     */
    fun moderate(
        dateOfAnalysing: String,
        reasoning: String,
    ): T

    /**
     * Vulnerability has a low impact on the product.
     *
     * @param dateOfAnalysing use the format YYYY-MM-dd to specify.
     * @param reasoning justifying the rating.
     */
    fun low(
        dateOfAnalysing: String,
        reasoning: String,
    ): T

    /**
     * Vulnerability has no impact on the product.
     *
     * @param dateOfAnalysing use the format YYYY-MM-dd to specify.
     * @param reasoning justifying the rating.
     */
    fun none(
        dateOfAnalysing: String,
        reasoning: String,
    ): T
}
