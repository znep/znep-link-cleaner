package com.example.model

enum class ExpansionWarningType {
    NONE,
    RED_FACEBOOK_FAILED,
    ORANGE_UNEXPANDED_SHORTLINK
}

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
    val isFacebookShareLink: Boolean = false
)
