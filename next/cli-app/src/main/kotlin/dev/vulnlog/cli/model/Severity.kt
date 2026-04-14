// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.model

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
