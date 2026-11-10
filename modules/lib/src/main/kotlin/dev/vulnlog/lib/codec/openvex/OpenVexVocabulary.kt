// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.openvex

import dev.vulnlog.lib.codec.vex.remediationOf
import dev.vulnlog.lib.codec.vex.remediationSentence
import dev.vulnlog.lib.codec.vex.vexJustificationOf
import dev.vulnlog.lib.codec.vex.vexJustificationToken
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.vex.Remediation
import dev.vulnlog.lib.model.vex.VexAuthor
import dev.vulnlog.lib.model.vex.VexStatusKind
import dev.vulnlog.lib.model.vex.VexTooling
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentId
import dev.vulnlog.lib.model.vex.openvex.OpenVexFormatVersion
import java.util.UUID

// Every OpenVEX string Vulnlog writes, next to its reverse where a baseline is read back.

private const val OPEN_VEX_ID_PREFIX = "https://vulnlog.dev/vex/"

internal const val OPEN_VEX_ROLE: String = "Document Creator"

private const val CONTEXT_PREFIX = "https://openvex.dev/ns/v"
private const val UNVERSIONED_CONTEXT = "https://openvex.dev/ns"
private const val UNVERSIONED_VERSION = "0.0.1"
private const val VULNLOG_SITE = "https://vulnlog.dev/"

/** The caller draws the UUID, so the id stays deterministic in tests. */
fun openVexDocumentId(uuid: UUID): OpenVexDocumentId = OpenVexDocumentId(OPEN_VEX_ID_PREFIX + uuid)

internal fun openVexContext(formatVersion: OpenVexFormatVersion): String = CONTEXT_PREFIX + formatVersion.version

/**
 * An unknown version still comes back, so a document of another version is told apart from another format. The
 * specification reads a context without a version as 0.0.1.
 */
internal fun declaredOpenVexVersion(context: String): String? =
    when {
        context == UNVERSIONED_CONTEXT -> UNVERSIONED_VERSION
        context.startsWith(CONTEXT_PREFIX) -> context.removePrefix(CONTEXT_PREFIX).takeIf(String::isNotBlank)
        else -> null
    }

internal fun openVexStatus(kind: VexStatusKind): String =
    when (kind) {
        VexStatusKind.AFFECTED -> "affected"
        VexStatusKind.FIXED -> "fixed"
        VexStatusKind.NOT_AFFECTED -> "not_affected"
        VexStatusKind.UNDER_INVESTIGATION -> "under_investigation"
    }

internal fun openVexStatusKind(token: String): VexStatusKind? =
    VexStatusKind.entries.firstOrNull {
        openVexStatus(it) ==
            token
    }

/** The OpenVEX vocabulary is the one Vulnlog stores, so its token is shared with every other format. */
internal fun openVexJustification(justification: VexJustification): String = vexJustificationToken(justification)

internal fun openVexJustificationOf(token: String): VexJustification? = vexJustificationOf(token)

internal fun openVexVulnerabilityUrl(id: VulnId): String =
    when (id) {
        is VulnId.Cve -> "https://nvd.nist.gov/vuln/detail/${id.id}"
        is VulnId.Ghsa -> "https://github.com/advisories/${id.id}"
        is VulnId.RustSec -> "https://rustsec.org/advisories/${id.id}"
        is VulnId.Snyk -> "https://security.snyk.io/vuln/${id.id}"
    }

internal fun openVexAuthor(author: VexAuthor): String =
    author.contact?.let { contact -> "${author.name} ($contact)" } ?: author.name

internal fun openVexTooling(tooling: VexTooling): String =
    "Vulnlog ${tooling.platform} version ${tooling.version}, $VULNLOG_SITE"

internal fun openVexActionStatement(remediation: Remediation): String = remediationSentence(remediation)

/** Null for a text this writer never writes, so a baseline statement worded elsewhere is not matched. */
internal fun openVexRemediation(actionStatement: String): Remediation? = remediationOf(actionStatement)
