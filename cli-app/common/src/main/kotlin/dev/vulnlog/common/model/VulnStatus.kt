package dev.vulnlog.common.model

sealed interface VulnStatus

data object VulnStatusAffected : VulnStatus

data object VulnStatusFixed : VulnStatus

data object VulnStatusNotAffected : VulnStatus

data object VulnStatusUnderInvestigation : VulnStatus

data object VulnStatusUnknown : VulnStatus
