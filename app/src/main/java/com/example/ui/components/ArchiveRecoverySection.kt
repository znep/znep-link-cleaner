package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.cleaner.ArchiveShareFormatter
import com.example.model.AccessAssessment
import com.example.model.AccessRestrictionType
import com.example.model.ArchiveCandidate
import com.example.model.ArchiveLookupState
import com.example.model.ArchiveValidationStatus

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArchiveRecoverySection(
    cleanedUrl: String,
    accessAssessment: AccessAssessment?,
    archiveLookupState: ArchiveLookupState,
    copiedTextState: String?,
    onSearchArchives: () -> Unit,
    onCopyArchiveBundle: (ArchiveCandidate) -> Unit,
    onShareArchiveBundle: (ArchiveCandidate) -> Unit,
    onOpenArchive: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isRestricted = accessAssessment?.isRestricted == true
    val isActiveSearch = archiveLookupState !is ArchiveLookupState.Idle
    val showExpandedPanel = isRestricted || isActiveSearch

    val amberBorder = Color(0xFFF59E0B)
    val amberBg = Color(0xFF2B1C06)
    val amberText = Color(0xFFFDE68A)

    Column(modifier = modifier.fillMaxWidth()) {
        // Subtle single-row fallback when link is not flagged as paywalled/blocked
        AnimatedVisibility(visible = !showExpandedPanel) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSearchArchives() }
                    .heightIn(min = 48.dp)
                    .testTag("manual_archive_lookup_row"),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (accessAssessment?.isChecking == true) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = "Check web archives",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (accessAssessment?.isChecking == true) {
                                "Checking paywall/region status · Tap to search archives"
                            } else {
                                "[+] CHECK WEB ARCHIVES (4 TRUST-RANKED SOURCES)"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Expanded Access Advisory & Trust-Ordered Archive Recovery Panel
        AnimatedVisibility(visible = showExpandedPanel) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("archive_recovery_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    width = 1.5.dp,
                    color = if (isRestricted) amberBorder.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header row: Detection status badge + checking spinner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            val headerIcon = when (accessAssessment?.restrictionType) {
                                AccessRestrictionType.REGION_LOCKED -> Icons.Default.PublicOff
                                AccessRestrictionType.PAYWALL,
                                AccessRestrictionType.ACCESS_BLOCKED,
                                AccessRestrictionType.REGISTRATION_WALL -> Icons.Default.Lock
                                else -> Icons.Default.Archive
                            }
                            Icon(
                                imageVector = headerIcon,
                                contentDescription = null,
                                tint = if (isRestricted) amberText else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isRestricted) amberBg else MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(
                                    1.dp,
                                    if (isRestricted) amberBorder.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                            ) {
                                Text(
                                    text = if (isRestricted) {
                                        accessAssessment?.bannerBadgeLabel ?: "[ PAYWALL DETECTED ]"
                                    } else {
                                        "[ WEB ARCHIVE LOOKUP ]"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRestricted) amberText else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (accessAssessment?.isChecking == true) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = amberBorder
                            )
                        }
                    }

                    // Evidence chips explaining WHY it was flagged
                    val signals = accessAssessment?.signals.orEmpty()
                    if (signals.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (signal in signals) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = signal,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Show detected title preview if available and useful
                    val bestDetectedTitle = resolveDisplayTitle(accessAssessment, archiveLookupState)
                    if (!bestDetectedTitle.isNullOrBlank()) {
                        val willIncludeInBundle = !ArchiveShareFormatter.isTitleRepresentedInUrl(bestDetectedTitle, cleanedUrl)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                                Text(
                                    text = if (willIncludeInBundle) {
                                        "DETECTED TITLE (AUTO-INCLUDED ON COPY — NOT IN URL)"
                                    } else {
                                        "DETECTED TITLE (ALREADY IN URL SLUG)"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = bestDetectedTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // State-dependent content: Idle CTA vs Trust-Ordered Archive Cards
                    when (archiveLookupState) {
                        is ArchiveLookupState.Idle -> {
                            Text(
                                text = if (accessAssessment?.wasOfflineOnly == true) {
                                    "Offline-Only Mode prevented automatic probing. Tap below to explicitly query 4 curated archives for this cleaned link."
                                } else {
                                    "No archive service has been contacted yet. Tap to search Wayback Machine, Perma.cc, Archive.today & Ghostarchive and validate full-article copies."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )

                            Button(
                                onClick = onSearchArchives,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("search_archives_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "[ SEARCH WEB ARCHIVES (4 SOURCES) ]",
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        is ArchiveLookupState.Searching,
                        is ArchiveLookupState.Completed -> {
                            val candidates = when (archiveLookupState) {
                                is ArchiveLookupState.Searching -> archiveLookupState.candidates
                                is ArchiveLookupState.Completed -> archiveLookupState.candidates
                                else -> emptyList()
                            }.sortedBy { it.provider.trustRank }

                            val actionable = candidates.filter { it.isActionable }
                            val unavailable = candidates.filter { !it.isActionable }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (archiveLookupState is ArchiveLookupState.Searching) {
                                        "ARCHIVE SOURCES (ORDERED BY TRUST · VALIDATING...)"
                                    } else {
                                        "ARCHIVE SOURCES (ORDERED BY TRUST)"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                if (archiveLookupState is ArchiveLookupState.Completed) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onSearchArchives() }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                            .testTag("recheck_archives_button"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Re-check archives",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "RE-CHECK",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }

                            if (actionable.isEmpty() && archiveLookupState is ArchiveLookupState.Completed) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "No archived snapshots were confirmed across the 4 providers. You can still try opening a direct lookup below.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            for (candidate in actionable) {
                                val expectedBundle = ArchiveShareFormatter.formatClipboardBundle(
                                    originalUrl = cleanedUrl,
                                    archiveUrl = candidate.archiveUrl,
                                    providerName = candidate.provider.displayName,
                                    restrictionType = accessAssessment?.restrictionType ?: AccessRestrictionType.PAYWALL,
                                    title = ArchiveShareFormatter.selectBestTitle(
                                        candidate.extractedTitle,
                                        accessAssessment?.detectedTitle
                                    )
                                )
                                ArchiveCandidateCard(
                                    candidate = candidate,
                                    isBundleCopied = copiedTextState == expectedBundle,
                                    onCopyBundle = { onCopyArchiveBundle(candidate) },
                                    onShareBundle = { onShareArchiveBundle(candidate) },
                                    onOpen = { onOpenArchive(candidate.archiveUrl) }
                                )
                            }

                            // Compact muted footer for NOT_ARCHIVED / ERROR providers
                            if (unavailable.isNotEmpty()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("unavailable_archives_footer"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val names = unavailable.joinToString(", ") {
                                            "#${it.provider.trustRank} ${it.provider.displayName}"
                                        }
                                        Text(
                                            text = "[ ✕ NOT ARCHIVED / UNAVAILABLE ] $names",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        val directFallbacks = unavailable.filter {
                                            it.isDirectLookupFallback && it.archiveUrl.isNotBlank()
                                        }
                                        if (directFallbacks.isNotEmpty()) {
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                for (fallback in directFallbacks) {
                                                    Surface(
                                                        onClick = { onOpenArchive(fallback.archiveUrl) },
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.surface,
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                                                    ) {
                                                        Text(
                                                            text = "Search ${fallback.provider.displayName} ↗",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontFamily = FontFamily.Monospace,
                                                                fontSize = 10.sp
                                                            ),
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveCandidateCard(
    candidate: ArchiveCandidate,
    isBundleCopied: Boolean,
    onCopyBundle: () -> Unit,
    onShareBundle: () -> Unit,
    onOpen: () -> Unit
) {
    val emeraldBorder = Color(0xFF10B981)
    val emeraldBg = Color(0xFF062E22)
    val emeraldText = Color(0xFF6EE7B7)

    val amberBorder = Color(0xFFF59E0B)
    val amberBg = Color(0xFF2B1C06)
    val amberText = Color(0xFFFDE68A)

    val (badgeLabel, badgeBg, badgeBorder, badgeText) = when (candidate.validationStatus) {
        ArchiveValidationStatus.VERIFIED_FULL_ARTICLE ->
            Quad("[ ✓ VERIFIED FULL ARTICLE ]", emeraldBg, emeraldBorder, emeraldText)
        ArchiveValidationStatus.SNAPSHOT_PARTIAL_OR_PAYWALLED ->
            Quad("[ ◐ PARTIAL / PAYWALLED ]", amberBg, amberBorder, amberText)
        ArchiveValidationStatus.SNAPSHOT_UNVERIFIED ->
            Quad(
                "[ ◐ SNAPSHOT · UNVERIFIED ]",
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.primary
            )
        ArchiveValidationStatus.UNVALIDATED_CAPTCHA_OR_LOOKUP ->
            Quad("[ ◇ UNVALIDATED · DIRECT LOOKUP ]", amberBg, amberBorder, amberText)
        ArchiveValidationStatus.CHECKING ->
            Quad(
                "[ CHECKING... ]",
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.outline,
                MaterialTheme.colorScheme.onSurfaceVariant
            )
        else ->
            Quad(
                "[ ✕ NOT ARCHIVED ]",
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.outline,
                MaterialTheme.colorScheme.onSurfaceVariant
            )
    }

    val cardBorder = if (candidate.validationStatus == ArchiveValidationStatus.VERIFIED_FULL_ARTICLE) {
        emeraldBorder.copy(alpha = 0.65f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("archive_candidate_${candidate.provider.name.lowercase()}"),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Provider Trust Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "#${candidate.provider.trustRank} TRUST",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp
                                ),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = candidate.provider.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = candidate.provider.trustTierLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (candidate.validationStatus == ArchiveValidationStatus.CHECKING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Validation Status Badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = badgeBg,
                border = BorderStroke(1.dp, badgeBorder.copy(alpha = 0.75f))
            ) {
                Text(
                    text = badgeLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = badgeText,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }

            // Metadata / Verification details
            val metaParts = buildList {
                candidate.capturedAtDisplay?.let { add("Captured $it") }
                candidate.estimatedWordCount?.let { if (it > 0) add("~$it words") }
            }
            val detailLine = buildString {
                if (metaParts.isNotEmpty()) {
                    append(metaParts.joinToString(" · "))
                    if (candidate.statusDetail.isNotBlank()) append(" — ")
                }
                append(candidate.statusDetail)
            }
            if (detailLine.isNotBlank()) {
                Text(
                    text = detailLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            // Action Buttons (only once resolved and URL available)
            if (candidate.validationStatus != ArchiveValidationStatus.CHECKING && candidate.archiveUrl.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onCopyBundle,
                        modifier = Modifier
                            .weight(1.35f)
                            .height(48.dp)
                            .testTag("copy_archive_bundle_${candidate.provider.name.lowercase()}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (isBundleCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy paywalled and archive bundle",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isBundleCopied) "Copied!" else "Copy Bundle",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = onShareBundle,
                        modifier = Modifier
                            .weight(0.95f)
                            .height(48.dp)
                            .testTag("share_archive_bundle_${candidate.provider.name.lowercase()}"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share paywalled and archive bundle",
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
                        onClick = onOpen,
                        modifier = Modifier
                            .weight(0.9f)
                            .height(48.dp)
                            .testTag("open_archive_${candidate.provider.name.lowercase()}"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open archive in browser",
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
            }
        }
    }
}

private fun resolveDisplayTitle(
    accessAssessment: AccessAssessment?,
    archiveLookupState: ArchiveLookupState
): String? {
    val archiveTitle = when (archiveLookupState) {
        is ArchiveLookupState.Searching -> archiveLookupState.candidates.firstNotNullOfOrNull { it.extractedTitle }
        is ArchiveLookupState.Completed -> archiveLookupState.candidates.firstNotNullOfOrNull { it.extractedTitle }
        else -> null
    }
    return ArchiveShareFormatter.selectBestTitle(archiveTitle, accessAssessment?.detectedTitle)
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
