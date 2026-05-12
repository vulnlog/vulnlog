// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model

enum class Severity {
    /**
     * Low priority. Minimal impact or difficult to exploit.
     */
    LOW,

    /**
     * Action recommended. Moderate impact.
     */
    MEDIUM,

    /**
     * Action required. Significant impact if exploited.
     */
    HIGH,

    /**
     * Immediate action required. Exploitable with severe impact.
     */
    CRITICAL,
}
