package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AccessAssessment
import com.example.model.AccessRestrictionType
import com.example.model.ArchiveCandidate
import com.example.model.ArchiveLookupState
import com.example.model.ArchiveValidationStatus
import com.example.model.CleanResult
import com.example.model.ExpansionWarningType
import com.example.model.RedirectHop
import com.example.model.SecurityAdvisory
import com.example.model.SecuritySeverity
import com.example.model.StrippedParam

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CleanResultCard(
    result: CleanResult,
    isCopied: Boolean,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onOpenBrowser: (String) -> Unit,
    onViewFacebookDetails: (() -> Unit)? = null,
    onExpandShortlinkNow: (() -> Unit)? = null,
    accessAssessment: AccessAssessment? = null,
    archiveLookupState: ArchiveLookupState = ArchiveLookupState.Idle,
    copiedTextState: String? = null,
    showArchiveSection: Boolean = false,
    onSearchArchives: () -> Unit = {},
    onCopyArchiveBundle: (ArchiveCandidate) -> Unit = {},
    onShareArchiveBundle: (ArchiveCandidate) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showDetails by remember { mutableStateOf(false) }
    val isUnchanged = result.isUnchanged
    val hasWarnings = result.warningType != ExpansionWarningType.NONE || result.securityAdvisories.isNotEmpty()
    val isRestricted = accessAssessment?.isRestricted == true
    val isArchiveActive = showArchiveSection && (isRestricted || archiveLookupState !is ArchiveLookupState.Idle)
    val hasVerifiedArchive = (archiveLookupState as? ArchiveLookupState.Completed)?.candidates?.any {
        it.validationStatus == ArchiveValidationStatus.VERIFIED_FULL_ARTICLE
    } == true

    val emeraldAccent = Color(0xFF10B981)
    val emeraldContainer = Color(0xFF062E22)
    val amberAccent = Color(0xFFF59E0B)
    val amberContainer = Color(0xFF2B1C06)
    val amberText = Color(0xFFFDE68A)

    val cardBorderColor = when {
        hasVerifiedArchive -> emeraldAccent.copy(alpha = 0.7f)
        isRestricted -> amberAccent.copy(alpha = 0.75f)
        isUnchanged && !hasWarnings -> emeraldAccent.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("clean_result_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(width = 1.5.dp, color = cardBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isArchiveActive) 15.dp else 18.dp)
        ) {
            // Unified HUD Status Header: shows Clean status + immediate Restriction status side-by-side
            if (isArchiveActive) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Clean status pill
                    if (isUnchanged) {
                        val badgeLabel = if (hasWarnings) "[ UNCHANGED // ADVISORY ]" else "[ ✓ ALREADY CLEAN ]"
                        val badgeBorder = if (hasWarnings) amberAccent else emeraldAccent
                        val badgeBg = if (hasWarnings) amberContainer else emeraldContainer
                        val badgeText = if (hasWarnings) amberText else Color(0xFF6EE7B7)

                        Surface(
                            modifier = Modifier.testTag("status_badge_unchanged"),
                            shape = RoundedCornerShape(8.dp),
                            color = badgeBg,
                            border = BorderStroke(1.dp, badgeBorder.copy(alpha = 0.8f))
                        ) {
                            Text(
                                text = badgeLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.Bold,
                                color = badgeText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        val cleanedLabel = if (result.percentReduction > 0) {
                            "[ CLEANED -${result.percentReduction}% ]"
                        } else {
                            "[ CLEANED ]"
                        }
                        Surface(
                            modifier = Modifier.testTag("status_badge_cleaned"),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = cleanedLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Immediate Restriction Badge (PAYWALLED / REGION LOCKED / BLOCKED / SIGN-IN WALL)
                    if (isRestricted && accessAssessment != null) {
                        val restrictionIcon = when (accessAssessment.restrictionType) {
                            AccessRestrictionType.REGION_LOCKED -> Icons.Default.PublicOff
                            else -> Icons.Default.Lock
                        }
                        Surface(
                            modifier = Modifier.testTag("status_badge_restricted"),
                            shape = RoundedCornerShape(8.dp),
                            color = amberContainer,
                            border = BorderStroke(1.dp, amberAccent.copy(alpha = 0.85f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = restrictionIcon,
                                    contentDescription = null,
                                    tint = amberText,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = accessAssessment.compactHudBadgeLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    fontWeight = FontWeight.Bold,
                                    color = amberText
                                )
                            }
                        }
                    }

                    if (result.domain.isNotEmpty()) {
                        Text(
                            text = result.domain,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(start = 2.dp)
                        )
                    }
                }
            } else {
                // Standard Unrestricted HUD Status Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        if (isUnchanged) {
                            val badgeLabel = if (hasWarnings) {
                                "[ UNCHANGED // SEE ADVISORY ]"
                            } else {
                                "[ ✓ UNCHANGED // ALREADY CLEAN ]"
                            }
                            val badgeBorder = if (hasWarnings) amberAccent else emeraldAccent
                            val badgeBg = if (hasWarnings) amberContainer else emeraldContainer
                            val badgeText = if (hasWarnings) amberText else Color(0xFF6EE7B7)

                            Surface(
                                modifier = Modifier.testTag("status_badge_unchanged"),
                                shape = RoundedCornerShape(8.dp),
                                color = badgeBg,
                                border = BorderStroke(1.dp, badgeBorder.copy(alpha = 0.8f))
                            ) {
                                Text(
                                    text = badgeLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    fontWeight = FontWeight.Bold,
                                    color = badgeText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Surface(
                                modifier = Modifier.testTag("status_badge_cleaned"),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "[ CLEANED ]",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (result.domain.isNotEmpty()) result.domain else "Clean Link",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isUnchanged) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            modifier = Modifier.testTag("delta_badge_unchanged"),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "0 B CHANGED",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else if (result.percentReduction > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "-${result.percentReduction}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Warnings: Red for Security Blocked or Facebook expansion failed, Orange for unexpanded/offline shortlink
            when (result.warningType) {
                ExpansionWarningType.RED_SECURITY_BLOCKED -> {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .testTag("security_blocked_banner"),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF380707),
                        border = BorderStroke(1.5.dp, Color(0xFFEF4444))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "[ 🛡️ SECURITY FIREWALL // UNSAFE REDIRECT BLOCKED ]",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFCA5A5)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.warningMessage
                                    ?: "ZnepLinkCleaner blocked a redirect to a non-public IP address or disallowed URI scheme.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFEE2E2),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
                ExpansionWarningType.RED_FACEBOOK_FAILED -> {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF380707),
                        border = BorderStroke(1.5.dp, Color(0xFFEF4444))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "[ ⚠️ EXPANSION FAILED // FACEBOOK SHARE SHIM ]",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFCA5A5)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.warningMessage
                                    ?: "Could not resolve Facebook share token to canonical post (Meta login wall or network timeout). Tracking parameters were still purged.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFEE2E2),
                                lineHeight = 16.sp
                            )
                            if (onViewFacebookDetails != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onViewFacebookDetails() }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ℹ️ How Facebook share tokens work (View Technical Details) →",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF87171)
                                    )
                                }
                            }
                        }
                    }
                }
                ExpansionWarningType.ORANGE_UNEXPANDED_SHORTLINK -> {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF331604),
                        border = BorderStroke(1.5.dp, Color(0xFFF97316))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (result.isOfflineDeferredShortlink) {
                                    "[ 🛡️ OFFLINE-ONLY MODE // SHORTLINK EXPANSION DEFERRED ]"
                                } else {
                                    "[ ⚠️ ADVISORY // UNEXPANDED SHORTLINK ]"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFDBA74)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.warningMessage
                                    ?: "This shortlink could not be expanded via network. Opaque tracking IDs may remain inside the shortener slug. Proceed with caution.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFEDD5),
                                lineHeight = 16.sp
                            )
                            if (result.isOfflineDeferredShortlink && onExpandShortlinkNow != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = onExpandShortlinkNow,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .testTag("expand_shortlink_now_button"),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFFF97316))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ElectricBolt,
                                        contentDescription = null,
                                        tint = Color(0xFFFDBA74),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "[ EXPAND SHORTLINK NOW ]",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFDBA74)
                                    )
                                }
                            }
                        }
                    }
                }
                ExpansionWarningType.NONE -> { /* No warning needed */ }
            }

            // Security Advisories Banner (Homograph, user@host, APK download, Cleartext HTTP, Raw IP)
            val displayedAdvisories = if (result.warningType == ExpansionWarningType.RED_SECURITY_BLOCKED) {
                result.securityAdvisories.filter { it.code != "BLOCKED_UNSAFE_REDIRECT" }
            } else {
                result.securityAdvisories
            }
            if (displayedAdvisories.isNotEmpty()) {
                val hasHighSeverity = displayedAdvisories.any { it.severity == SecuritySeverity.HIGH }
                val borderColor = if (hasHighSeverity) Color(0xFFEF4444) else amberAccent
                val bgColor = if (hasHighSeverity) Color(0xFF310B14) else amberContainer
                val titleColor = if (hasHighSeverity) Color(0xFFFCA5A5) else amberText

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .testTag("security_advisories_banner"),
                    shape = RoundedCornerShape(10.dp),
                    color = bgColor,
                    border = BorderStroke(1.5.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = titleColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "[ SECURITY & PHISHING ADVISORIES (${displayedAdvisories.size}) ]",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.Bold,
                                color = titleColor
                            )
                        }

                        for (advisory in displayedAdvisories) {
                            SecurityAdvisoryItem(advisory = advisory)
                        }
                    }
                }
            }

            val hopCount = (result.redirectHops.size - 1).coerceAtLeast(0)

            if (isArchiveActive) {
                // COMPACT INTEGRATED LAYOUT: keeps Clean URL + Copy/Share/Open + Inline Paywall/Archives above the fold
                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onCopy(result.cleanedUrl) },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        if (result.strippedParams.isNotEmpty() || hopCount > 0) {
                            val summary = buildList {
                                if (result.strippedParams.isNotEmpty()) add("${result.strippedParams.size} PURGED")
                                if (result.bytesSaved > 0) add("-${result.bytesSaved} B")
                                if (hopCount > 0) add(if (hopCount == 1) "1 HOP" else "$hopCount HOPS")
                            }.joinToString(" · ")
                            Text(
                                text = "CLEAN LINK // $summary",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.5.sp
                                ),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        Text(
                            text = result.cleanedUrl,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 17.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Single 48dp compact action row (Copy | Share | Open)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { onCopy(result.cleanedUrl) },
                        modifier = Modifier
                            .weight(1.1f)
                            .height(48.dp)
                            .testTag("copy_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f))
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy cleaned link",
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isCopied) "Copied!" else "Copy",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = { onShare(result.cleanedUrl) },
                        modifier = Modifier
                            .weight(0.95f)
                            .height(48.dp)
                            .testTag("share_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share cleaned link",
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Share",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = { onOpenBrowser(result.cleanedUrl) },
                        modifier = Modifier
                            .weight(0.95f)
                            .height(48.dp)
                            .testTag("open_browser_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open cleaned link in browser",
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Open",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            } else {
                // STANDARD UNRESTRICTED LAYOUT
                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onCopy(result.cleanedUrl) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isUnchanged && !hasWarnings) {
                            emeraldAccent.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = when {
                                isUnchanged && hasWarnings -> "UNCHANGED LINK // SEE ADVISORY ABOVE"
                                isUnchanged -> "VERIFIED CLEAN LINK // IDENTICAL TO INPUT"
                                else -> "PURGED LINK //"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isUnchanged && hasWarnings -> amberText
                                isUnchanged -> Color(0xFF6EE7B7)
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                        if (isUnchanged && !hasWarnings) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Link is already clean — no tracking parameters, redirect wrappers, or path bloat found.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = result.cleanedUrl,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { onCopy(result.cleanedUrl) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("copy_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCopied) "[ COPIED TO CLIPBOARD! ]" else "[ COPY CLEAN LINK ]",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onShare(result.cleanedUrl) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Share",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedButton(
                        onClick = { onOpenBrowser(result.cleanedUrl) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("open_browser_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Open",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Compact Retro Telemetry Pill HUD (shown in unrestricted mode)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TelemetryPill(
                        label = if (result.strippedParams.isNotEmpty()) {
                            "${result.strippedParams.size} ITEMS PURGED"
                        } else if (isUnchanged) {
                            "UNCHANGED (0 TRACKERS)"
                        } else {
                            "0 TRACKERS FOUND"
                        }
                    )
                    if (result.bytesSaved > 0) {
                        TelemetryPill(label = "-${result.bytesSaved} BYTES")
                    }
                    if (hopCount > 0) {
                        TelemetryPill(label = if (hopCount == 1) "1 HOP" else "$hopCount HOPS")
                    } else if (result.expandedFromUrl != null) {
                        TelemetryPill(label = "EXPANDED")
                    }
                    if (result.securityAdvisories.isNotEmpty()) {
                        TelemetryPill(label = "${result.securityAdvisories.size} SEC ALERT")
                    }
                }
            }

            // Collapsible Details Section (only shown when trackers/hops actually exist)
            if (result.strippedParams.isNotEmpty() || result.expandedFromUrl != null || result.redirectHops.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                val toggleLabel = when {
                    showDetails -> "[-] HIDE TELEMETRY & HOP TRACE"
                    hopCount > 0 && result.strippedParams.isNotEmpty() ->
                        "[+] VIEW ${result.strippedParams.size} PURGED ITEMS & $hopCount-HOP TRACE"
                    hopCount > 0 ->
                        "[+] VIEW $hopCount-HOP REDIRECT TRACE"
                    else ->
                        "[+] VIEW ${result.strippedParams.size} PURGED ITEMS"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showDetails = !showDetails }
                        .padding(vertical = 4.dp)
                        .testTag("toggle_telemetry_details"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = toggleLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Icon(
                        imageVector = if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(visible = showDetails) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (result.redirectHops.size > 1) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("redirect_hop_trace_card"),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "REDIRECT HOP CHAIN TRACE ($hopCount ${if (hopCount == 1) "HOP" else "HOPS"})",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    for (hop in result.redirectHops) {
                                        RedirectHopRow(hop = hop)
                                    }
                                }
                            }
                        } else if (result.expandedFromUrl != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "SHORTLINK RESOLVED",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = result.expandedFromUrl,
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        for (param in result.strippedParams) {
                            ParamDetailRow(param = param)
                        }
                    }
                }
            }

            // Inline Paywall & Web Archive Recovery inside the SAME unified card
            if (showArchiveSection) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(
                    thickness = 1.dp,
                    color = if (isRestricted) {
                        amberAccent.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    }
                )
                Spacer(modifier = Modifier.height(if (isArchiveActive) 10.dp else 4.dp))
                InlineArchiveRecoveryContent(
                    cleanedUrl = result.cleanedUrl,
                    accessAssessment = accessAssessment,
                    archiveLookupState = archiveLookupState,
                    copiedTextState = copiedTextState,
                    onSearchArchives = onSearchArchives,
                    onCopyArchiveBundle = onCopyArchiveBundle,
                    onShareArchiveBundle = onShareArchiveBundle,
                    onOpenArchive = onOpenBrowser
                )
            }
        }
    }
}

@Composable
private fun RedirectHopRow(hop: RedirectHop) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "#${hop.step} ${hop.host.ifEmpty { "link" }}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = hop.mechanism,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            Text(
                text = hop.url,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SecurityAdvisoryItem(advisory: SecurityAdvisory) {
    val badgeColor = when (advisory.severity) {
        SecuritySeverity.HIGH -> Color(0xFFEF4444)
        SecuritySeverity.MEDIUM -> Color(0xFFF59E0B)
        SecuritySeverity.LOW -> Color(0xFF38BDF8)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = badgeColor.copy(alpha = 0.22f),
                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.7f))
            ) {
                Text(
                    text = advisory.severity.name,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.sp),
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = advisory.title,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFEE2E2)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = advisory.description,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFFECACA),
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun TelemetryPill(label: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun ParamDetailRow(param: StrippedParam) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = param.key,
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = param.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp
                )
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            ) {
                Text(
                    text = param.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}
