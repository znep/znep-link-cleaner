package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleaner.AccessRestrictionDetector
import com.example.cleaner.ArchiveLookupEngine
import com.example.cleaner.ArchiveShareFormatter
import com.example.cleaner.FacebookWebViewExpander
import com.example.cleaner.LinkCleaner
import com.example.cleaner.UrlExpander
import com.example.cleaner.UrlSecurityValidator
import com.example.data.AppDatabase
import com.example.data.CleanStats
import com.example.data.CleanedLinkEntity
import com.example.data.PreferencesManager
import com.example.model.AccessAssessment
import com.example.model.AccessRestrictionType
import com.example.model.ArchiveCandidate
import com.example.model.ArchiveLookupState
import com.example.model.ArchiveProvider
import com.example.model.ArchiveValidationStatus
import com.example.model.CleanResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class LinkCopied(
        val text: String,
        val toastMessage: String = "Clean link copied to clipboard!"
    ) : UiEvent()
    data object FinishActivity : UiEvent()
}

enum class SampleType {
    PAYWALLED_ARTICLE,
    REDDIT,
    TIKTOK_SHARE,
    FACEBOOK_META,
    INSTAGRAM,
    AMAZON,
    YOUTUBE,
    MARKETING_UTM,
    TWITTER_X
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.cleanedLinkDao()
    private val prefs = PreferencesManager(application)

    val autoCopyOnShare = prefs.autoCopyOnShare
    val autoCloseAfterReshare = prefs.autoCloseAfterReshare
    val keepAmazonAffiliate = prefs.keepAmazonAffiliate
    val strictMode = prefs.strictMode
    val enableFacebookWebViewExpansion = prefs.enableFacebookWebViewExpansion
    val offlineOnlyMode = prefs.offlineOnlyMode

    val history: StateFlow<List<CleanedLinkEntity>> = dao.getAllLinks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val stats: StateFlow<CleanStats> = dao.getStats()
        .stateIn(viewModelScope, SharingStarted.Lazily, CleanStats(0, 0, 0))

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _cleanResult = MutableStateFlow<CleanResult?>(null)
    val cleanResult: StateFlow<CleanResult?> = _cleanResult.asStateFlow()

    private val _isExpanding = MutableStateFlow(false)
    val isExpanding: StateFlow<Boolean> = _isExpanding.asStateFlow()

    private val _isFromShareIntent = MutableStateFlow(false)
    val isFromShareIntent: StateFlow<Boolean> = _isFromShareIntent.asStateFlow()

    private val _accessAssessment = MutableStateFlow<AccessAssessment?>(null)
    val accessAssessment: StateFlow<AccessAssessment?> = _accessAssessment.asStateFlow()

    private val _archiveLookupState = MutableStateFlow<ArchiveLookupState>(ArchiveLookupState.Idle)
    val archiveLookupState: StateFlow<ArchiveLookupState> = _archiveLookupState.asStateFlow()

    private var accessCheckJob: Job? = null
    private var archiveLookupJob: Job? = null

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    fun updateInputText(text: String) {
        _inputText.value = text.take(UrlSecurityValidator.MAX_INPUT_LENGTH)
    }

    fun clearInput() {
        accessCheckJob?.cancel()
        archiveLookupJob?.cancel()
        _inputText.value = ""
        _cleanResult.value = null
        _isExpanding.value = false
        _isFromShareIntent.value = false
        _accessAssessment.value = null
        _archiveLookupState.value = ArchiveLookupState.Idle
    }

    fun processInputText(text: String, isSharedIntent: Boolean = false) {
        val trimmed = text.trim().take(UrlSecurityValidator.MAX_INPUT_LENGTH)
        if (trimmed.isEmpty()) return

        accessCheckJob?.cancel()
        archiveLookupJob?.cancel()
        _accessAssessment.value = null
        _archiveLookupState.value = ArchiveLookupState.Idle

        _inputText.value = trimmed
        _isFromShareIntent.value = isSharedIntent

        viewModelScope.launch {
            val isOfflineOnly = offlineOnlyMode.value
            // First immediate offline clean
            val initial = LinkCleaner.cleanText(
                inputText = trimmed,
                keepAmazonAffiliate = keepAmazonAffiliate.value,
                strictMode = strictMode.value,
                offlineOnlyMode = isOfflineOnly
            )
            _cleanResult.value = initial

            // If any URL in the text is a redirect / shortener link (including Reddit /r/<sub>/s/<id>) or Facebook share link,
            // expand it asynchronously unless Offline-Only Mode is active
            val extractedUrls = LinkCleaner.extractLinks(trimmed)
            val requiresNetworkExpansion = extractedUrls.any {
                UrlExpander.shouldExpand(it) || FacebookWebViewExpander.isFacebookShareLink(it)
            } || UrlExpander.shouldExpand(trimmed) || FacebookWebViewExpander.isFacebookShareLink(trimmed)

            if (!isOfflineOnly && requiresNetworkExpansion) {
                _isExpanding.value = true
                val expandedResult = LinkCleaner.cleanTextAsync(
                    inputText = trimmed,
                    context = getApplication(),
                    enableFacebookWebView = enableFacebookWebViewExpansion.value,
                    keepAmazonAffiliate = keepAmazonAffiliate.value,
                    strictMode = strictMode.value,
                    offlineOnlyMode = false
                )
                _cleanResult.value = expandedResult
                _isExpanding.value = false

                saveResultToDatabase(expandedResult)
                handleAutoCopy(expandedResult, isSharedIntent)
                startAccessAssessment(expandedResult.cleanedUrl, offlineOnlyMode = false)
            } else {
                saveResultToDatabase(initial)
                handleAutoCopy(initial, isSharedIntent)
                startAccessAssessment(initial.cleanedUrl, offlineOnlyMode = isOfflineOnly)
            }
        }
    }

    /**
     * Cleans selected text for Android's editable ACTION_PROCESS_TEXT write-back
     * so the highlighted link is replaced in-place inside the host app with zero UI switch.
     * If the selected link is an HTTP-redirect shortener (such as Reddit /r/<sub>/s/<id> or bit.ly)
     * and Offline-Only Mode is disabled, performs a fast bounded network expansion.
     */
    fun cleanSynchronouslyForInPlaceReplacement(text: String): CleanResult {
        val bounded = text.take(UrlSecurityValidator.MAX_INPUT_LENGTH)
        val isOfflineOnly = offlineOnlyMode.value
        val urls = LinkCleaner.extractLinks(bounded)
        val shouldExpandOverNetwork = !isOfflineOnly && urls.any { UrlExpander.shouldExpand(it) }

        val result = if (shouldExpandOverNetwork) {
            try {
                kotlinx.coroutines.runBlocking {
                    kotlinx.coroutines.withTimeout(3500L) {
                        LinkCleaner.cleanTextAsync(
                            inputText = bounded,
                            context = null,
                            enableFacebookWebView = false,
                            keepAmazonAffiliate = keepAmazonAffiliate.value,
                            strictMode = strictMode.value,
                            offlineOnlyMode = false
                        )
                    }
                }
            } catch (_: Exception) {
                LinkCleaner.cleanText(
                    inputText = bounded,
                    keepAmazonAffiliate = keepAmazonAffiliate.value,
                    strictMode = strictMode.value,
                    offlineOnlyMode = isOfflineOnly
                )
            }
        } else {
            LinkCleaner.cleanText(
                inputText = bounded,
                keepAmazonAffiliate = keepAmazonAffiliate.value,
                strictMode = strictMode.value,
                offlineOnlyMode = isOfflineOnly
            )
        }
        viewModelScope.launch {
            saveResultToDatabase(result)
        }
        return result
    }

    /**
     * Explicitly expands the current shortlink on-demand (e.g., when Offline-Only Mode deferred automatic expansion).
     */
    fun expandCurrentLinkNow() {
        val currentInput = _inputText.value.trim()
        if (currentInput.isEmpty()) return

        viewModelScope.launch {
            _isExpanding.value = true
            val expandedResult = LinkCleaner.cleanTextAsync(
                inputText = currentInput,
                context = getApplication(),
                enableFacebookWebView = enableFacebookWebViewExpansion.value,
                keepAmazonAffiliate = keepAmazonAffiliate.value,
                strictMode = strictMode.value,
                offlineOnlyMode = false
            )
            _cleanResult.value = expandedResult
            _isExpanding.value = false
            saveResultToDatabase(expandedResult)
            startAccessAssessment(expandedResult.cleanedUrl, offlineOnlyMode = false)
        }
    }

    /**
     * Orthogonal access check: runs Stage A offline known-publisher detection immediately,
     * then Stage B bounded HTTP/HTML probing asynchronously unless Offline-Only Mode is active.
     */
    private fun startAccessAssessment(cleanedUrl: String, offlineOnlyMode: Boolean) {
        accessCheckJob?.cancel()
        archiveLookupJob?.cancel()
        _archiveLookupState.value = ArchiveLookupState.Idle

        if (cleanedUrl.isBlank() || !UrlSecurityValidator.isSafeHttpUrl(cleanedUrl)) {
            _accessAssessment.value = null
            return
        }

        val offlineResult = AccessRestrictionDetector.assessOffline(cleanedUrl, offlineOnlyMode)
        _accessAssessment.value = offlineResult

        if (!offlineOnlyMode) {
            accessCheckJob = viewModelScope.launch {
                val liveResult = AccessRestrictionDetector.assessLive(cleanedUrl, offlineOnlyMode = false)
                if (_cleanResult.value?.cleanedUrl == cleanedUrl) {
                    _accessAssessment.value = liveResult
                }
            }
        }
    }

    /**
     * User-triggered two-stage archival lookup across all 4 curated trust-ordered providers.
     * Never invoked automatically during cleaning to protect user privacy.
     */
    fun searchArchivesForCurrentLink() {
        val cleanedUrl = _cleanResult.value?.cleanedUrl?.trim().orEmpty()
        if (cleanedUrl.isEmpty() || !UrlSecurityValidator.isSafeHttpUrl(cleanedUrl)) {
            viewModelScope.launch {
                _uiEvents.emit(UiEvent.ShowToast("No valid HTTP/HTTPS link to search in archives"))
            }
            return
        }

        archiveLookupJob?.cancel()
        val initialPlaceholders = ArchiveProvider.entries
            .sortedBy { it.trustRank }
            .map { provider ->
                ArchiveCandidate(
                    provider = provider,
                    archiveUrl = "",
                    validationStatus = ArchiveValidationStatus.CHECKING,
                    statusDetail = "Querying ${provider.displayName}..."
                )
            }
        _archiveLookupState.value = ArchiveLookupState.Searching(cleanedUrl, initialPlaceholders)

        archiveLookupJob = viewModelScope.launch {
            val finalResults = ArchiveLookupEngine.lookupAllArchives(
                cleanedUrl = cleanedUrl,
                onProviderUpdate = { updatedCandidate ->
                    _archiveLookupState.update { current ->
                        if (current is ArchiveLookupState.Searching && current.targetUrl == cleanedUrl) {
                            val updatedList = current.candidates
                                .map { existing ->
                                    if (existing.provider == updatedCandidate.provider) updatedCandidate else existing
                                }
                                .sortedBy { it.provider.trustRank }
                            ArchiveLookupState.Searching(cleanedUrl, updatedList)
                        } else {
                            current
                        }
                    }
                }
            )
            if (_cleanResult.value?.cleanedUrl == cleanedUrl) {
                _archiveLookupState.value = ArchiveLookupState.Completed(cleanedUrl, finalResults)
            }
        }
    }

    /**
     * Builds the structured clipboard/share text bundling the labeled original URL,
     * the archival URL, and the article title whenever it is not already represented in the URL.
     */
    fun buildArchiveBundleText(candidate: ArchiveCandidate): String {
        val cleanedUrl = _cleanResult.value?.cleanedUrl.orEmpty()
        val assessment = _accessAssessment.value
        val restrictionType = assessment?.restrictionType ?: AccessRestrictionType.PAYWALL
        val bestTitle = ArchiveShareFormatter.selectBestTitle(
            archiveTitle = candidate.extractedTitle,
            originalProbeTitle = assessment?.detectedTitle
        )
        return ArchiveShareFormatter.formatClipboardBundle(
            originalUrl = cleanedUrl,
            archiveUrl = candidate.archiveUrl,
            providerName = candidate.provider.displayName,
            restrictionType = restrictionType,
            title = bestTitle
        )
    }

    fun copyArchiveBundle(candidate: ArchiveCandidate) {
        if (!UrlSecurityValidator.isSafeHttpUrl(candidate.archiveUrl)) {
            viewModelScope.launch {
                _uiEvents.emit(UiEvent.ShowToast("No valid archive link available to copy"))
            }
            return
        }
        val bundle = buildArchiveBundleText(candidate)
        val context = getApplication<Application>()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("ZnepLinkCleaner Archive Bundle", bundle))

        val label = (_accessAssessment.value?.clipboardRestrictionLabel ?: "Paywalled").lowercase()
        viewModelScope.launch {
            _uiEvents.emit(
                UiEvent.LinkCopied(
                    text = bundle,
                    toastMessage = "Copied $label link + ${candidate.provider.displayName} archive!"
                )
            )
        }
    }

    fun shareArchiveBundle(context: Context, candidate: ArchiveCandidate) {
        if (!UrlSecurityValidator.isSafeHttpUrl(candidate.archiveUrl)) {
            viewModelScope.launch {
                _uiEvents.emit(UiEvent.ShowToast("No valid archive link available to share"))
            }
            return
        }
        val bundle = buildArchiveBundleText(candidate)
        shareLink(context, bundle)
    }

    private suspend fun saveResultToDatabase(result: CleanResult) {
        if (result.cleanedUrl.isNotEmpty() && UrlSecurityValidator.isSafeHttpUrl(result.cleanedUrl)) {
            val summary = if (result.expandedFromUrl != null) {
                "Expanded shortlink, " + result.strippedParams.joinToString(", ") { it.key }
            } else {
                result.strippedParams.joinToString(", ") { it.key }
            }
            dao.insertLink(
                CleanedLinkEntity(
                    originalUrl = result.originalUrl,
                    cleanedUrl = result.cleanedUrl,
                    domain = result.domain,
                    strippedCount = result.strippedParams.size + (if (result.expandedFromUrl != null) 1 else 0),
                    strippedParamsSummary = summary.trimEnd(',', ' '),
                    bytesSaved = result.bytesSaved
                )
            )
        }
    }

    private suspend fun handleAutoCopy(result: CleanResult, isSharedIntent: Boolean) {
        if (isSharedIntent && autoCopyOnShare.value && result.cleanedUrl.isNotEmpty()) {
            copyToClipboard(result.cleanedUrl, showToast = false)
            _uiEvents.emit(UiEvent.LinkCopied(result.cleanedUrl))
        }
    }

    fun copyToClipboard(text: String, showToast: Boolean = true) {
        val context = getApplication<Application>()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("ZnepLinkCleaner", text)
        clipboard.setPrimaryClip(clip)

        viewModelScope.launch {
            _uiEvents.emit(
                UiEvent.LinkCopied(
                    text = text,
                    toastMessage = if (showToast) "Copied clean link to clipboard!" else ""
                )
            )
        }
    }

    fun shareLink(context: Context, text: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Clean Link via")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)

        if (_isFromShareIntent.value && autoCloseAfterReshare.value) {
            viewModelScope.launch {
                _uiEvents.emit(UiEvent.FinishActivity)
            }
        }
    }

    fun openInBrowser(context: Context, url: String) {
        if (!UrlSecurityValidator.isSafeHttpUrl(url)) {
            viewModelScope.launch {
                _uiEvents.emit(UiEvent.ShowToast("Blocked unsafe non-HTTP/HTTPS URL scheme"))
            }
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url.trim())).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            viewModelScope.launch {
                _uiEvents.emit(UiEvent.ShowToast("Unable to open browser for this link"))
            }
        }
    }

    fun loadSample(type: SampleType) {
        val sample = when (type) {
            SampleType.PAYWALLED_ARTICLE ->
                "https://www.nytimes.com/2026/11/10/technology/ai-chips-energy-grid.html?smid=nytcore-ios-share&utm_source=twitter&utm_medium=social"
            SampleType.REDDIT ->
                "https://www.reddit.com/r/Seattle/comments/1abc234/sunset_over_puget_sound_from_kerry_park/?%24deep_link=true&correlation_id=9f8e7d6c-5b4a-3210&ref=share&ref_source=link&share_id=XyZ987654&shrid=ab12cd34&utm_source=share&utm_medium=android_app"
            SampleType.TIKTOK_SHARE ->
                "https://www.tiktok.com/@creator/video/7345678901234567890?_r=1&_t=8Wxyz&is_from_webapp=1&sender_device=pc&share_app_id=1233&share_link_id=abc123xyz"
            SampleType.FACEBOOK_META ->
                "https://www.facebook.com/share/p/18V4XJ9abc/?mibextid=wwXIfr"
            SampleType.INSTAGRAM ->
                "https://www.instagram.com/reel/DC123456789/?igsh=MTNmeXFuZzF3ejJveA==&exln=MTNmeXFuZzF3ejJveA==&obrf=1"
            SampleType.AMAZON ->
                "https://www.amazon.com/Apple-AirPods-Wireless-Earbuds-Charging/dp/B08N5WRWNW/ref=sr_1_1?crid=289EHQ99W&keywords=airpods&qid=1680000000&sprefix=airp%2Caps%2C120&sr=8-1&tag=affiliate-20&linkCode=ll1"
            SampleType.YOUTUBE ->
                "https://youtu.be/dQw4w9WgXcQ?si=7vO1v5X_8M9qPabc&feature=share&pp=ygUIcm9ja3JvbGw%3D"
            SampleType.MARKETING_UTM ->
                "https://techcrunch.com/2026/11/01/future-ai-chips/?utm_source=twitter&utm_medium=social&utm_campaign=fall2026_promo&utm_content=tech_breakdown&utm_term=ai_compute&gclid=Cj0KCQjwmICoBhC9ARIsAEkW64"
            SampleType.TWITTER_X ->
                "https://x.com/AndroidDev/status/1700000000000000000?s=20&t=abcdef1234567890&ref_src=twsrc%5Etfw"
        }
        processInputText(sample, isSharedIntent = false)
    }

    fun deleteHistoryItem(item: CleanedLinkEntity) {
        viewModelScope.launch {
            dao.deleteLink(item)
            _uiEvents.emit(UiEvent.ShowToast("Link removed from history"))
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            dao.clearAll()
            _uiEvents.emit(UiEvent.ShowToast("History cleared"))
        }
    }

    fun setAutoCopy(enabled: Boolean) = prefs.setAutoCopyOnShare(enabled)
    fun setAutoClose(enabled: Boolean) = prefs.setAutoCloseAfterReshare(enabled)
    fun setKeepAmazon(enabled: Boolean) {
        prefs.setKeepAmazonAffiliate(enabled)
        if (_inputText.value.isNotEmpty()) {
            processInputText(_inputText.value, _isFromShareIntent.value)
        }
    }
    fun setStrictMode(enabled: Boolean) {
        prefs.setStrictMode(enabled)
        if (_inputText.value.isNotEmpty()) {
            processInputText(_inputText.value, _isFromShareIntent.value)
        }
    }
    fun setEnableFacebookWebViewExpansion(enabled: Boolean) {
        prefs.setEnableFacebookWebViewExpansion(enabled)
        if (_inputText.value.isNotEmpty()) {
            processInputText(_inputText.value, _isFromShareIntent.value)
        }
    }
    fun setOfflineOnlyMode(enabled: Boolean) {
        prefs.setOfflineOnlyMode(enabled)
        if (_inputText.value.isNotEmpty()) {
            processInputText(_inputText.value, _isFromShareIntent.value)
        }
    }
}
