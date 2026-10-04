// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model

enum class Disposition {
    /** The project intends to remediate the vulnerability. */
    WILL_FIX,

    /** The project accepts the risk; no remediation is intended. */
    WONT_FIX,
}
