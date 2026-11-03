package com.example.model

enum class ExpansionWarningType {
    NONE,
    RED_FACEBOOK_FAILED,
    ORANGE_UNEXPANDED_SHORTLINK,
    RED_SECURITY_BLOCKED
}

enum class SecuritySeverity {
    HIGH,
    MEDIUM,
    LOW
}

data class SecurityAdvisory(
    val code: String,
    val severity: SecuritySeverity,
    val title: String,
    val description: String
)

data class StrippedParam(
    val key: String,
    val value: String,
    val category: String,
    val description: String
)

data class CleanResult(
    val originalText: String,
    val cleanedText: String,
    val originalUrl: String,
    val cleanedUrl: String,
    val domain: String,
    val strippedParams: List<StrippedParam>,
    val bytesSaved: Int,
    val percentReduction: Int,
    val isEntireTextReplaced: Boolean,
    val expandedFromUrl: String? = null,
    val warningType: ExpansionWarningType = ExpansionWarningType.NONE,
    val warningMessage: String? = null,
    val isFacebookShareLink: Boolean = false,
    val securityAdvisories: List<SecurityAdvisory> = emptyList(),
    val isOfflineDeferredShortlink: Boolean = false
)
