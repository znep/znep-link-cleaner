package com.example.model

enum class AccessRestrictionType {
    NONE,
    PAYWALL,
    REGION_LOCKED,
    ACCESS_BLOCKED,
    REGISTRATION_WALL
}

enum class DetectionConfidence {
    HIGH,
    MEDIUM,
    OFFLINE_LIKELY
}

data class AccessAssessment(
    val targetUrl: String,
    val restrictionType: AccessRestrictionType = AccessRestrictionType.NONE,
    val confidence: DetectionConfidence = DetectionConfidence.OFFLINE_LIKELY,
    val signals: List<String> = emptyList(),
    val detectedTitle: String? = null,
    val isChecking: Boolean = false,
    val wasOfflineOnly: Boolean = false
) {
    val isRestricted: Boolean
        get() = restrictionType != AccessRestrictionType.NONE

    val bannerBadgeLabel: String
        get() = when (restrictionType) {
            AccessRestrictionType.PAYWALL ->
                if (confidence == DetectionConfidence.OFFLINE_LIKELY) "[ LIKELY PAYWALLED ]" else "[ PAYWALL DETECTED ]"
            AccessRestrictionType.REGION_LOCKED -> "[ REGION LOCKED ]"
            AccessRestrictionType.ACCESS_BLOCKED -> "[ ACCESS BLOCKED ]"
            AccessRestrictionType.REGISTRATION_WALL -> "[ REGISTRATION WALL ]"
            AccessRestrictionType.NONE -> "[ ACCESS CHECKED ]"
        }

    val clipboardRestrictionLabel: String
        get() = when (restrictionType) {
            AccessRestrictionType.PAYWALL -> "Paywalled"
            AccessRestrictionType.REGION_LOCKED -> "Region-Locked"
            AccessRestrictionType.ACCESS_BLOCKED -> "Blocked"
            AccessRestrictionType.REGISTRATION_WALL -> "Registration-Gated"
            AccessRestrictionType.NONE -> "Paywalled"
        }
}

enum class ArchiveProvider(
    val trustRank: Int,
    val displayName: String,
    val trustTierLabel: String,
    val providerDomain: String
) {
    WAYBACK_MACHINE(
        trustRank = 1,
        displayName = "Wayback Machine",
        trustTierLabel = "Tier 1 · Institutional Non-Profit",
        providerDomain = "web.archive.org"
    ),
    PERMA_CC(
        trustRank = 2,
        displayName = "Perma.cc",
        trustTierLabel = "Tier 1 · Harvard Law Library",
        providerDomain = "perma.cc"
    ),
    ARCHIVE_TODAY(
        trustRank = 3,
        displayName = "Archive.today",
        trustTierLabel = "Tier 2 · Independent Snapshot",
        providerDomain = "archive.today"
    ),
    GHOSTARCHIVE(
        trustRank = 4,
        displayName = "Ghostarchive",
        trustTierLabel = "Tier 3 · Community Archive",
        providerDomain = "ghostarchive.org"
    )
}

enum class ArchiveValidationStatus {
    CHECKING,
    VERIFIED_FULL_ARTICLE,
    SNAPSHOT_PARTIAL_OR_PAYWALLED,
    SNAPSHOT_UNVERIFIED,
    UNVALIDATED_CAPTCHA_OR_LOOKUP,
    NOT_ARCHIVED,
    ERROR
}

data class ArchiveCandidate(
    val provider: ArchiveProvider,
    val archiveUrl: String,
    val validationStatus: ArchiveValidationStatus,
    val capturedAtDisplay: String? = null,
    val estimatedWordCount: Int? = null,
    val extractedTitle: String? = null,
    val statusDetail: String = "",
    val isDirectLookupFallback: Boolean = false
) {
    val isActionable: Boolean
        get() = validationStatus == ArchiveValidationStatus.CHECKING ||
            validationStatus == ArchiveValidationStatus.VERIFIED_FULL_ARTICLE ||
            validationStatus == ArchiveValidationStatus.SNAPSHOT_PARTIAL_OR_PAYWALLED ||
            validationStatus == ArchiveValidationStatus.SNAPSHOT_UNVERIFIED ||
            validationStatus == ArchiveValidationStatus.UNVALIDATED_CAPTCHA_OR_LOOKUP
}

sealed class ArchiveLookupState {
    data object Idle : ArchiveLookupState()
    data class Searching(
        val targetUrl: String,
        val candidates: List<ArchiveCandidate>
    ) : ArchiveLookupState()
    data class Completed(
        val targetUrl: String,
        val candidates: List<ArchiveCandidate>
    ) : ArchiveLookupState()
}
