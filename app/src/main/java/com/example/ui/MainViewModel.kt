package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleaner.LinkCleaner
import com.example.cleaner.UrlExpander
import com.example.data.AppDatabase
import com.example.data.CleanStats
import com.example.data.CleanedLinkEntity
import com.example.data.PreferencesManager
import com.example.model.CleanResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class LinkCopied(val text: String) : UiEvent()
    data object FinishActivity : UiEvent()
}

enum class SampleType {
    TIKTOK_SHARE,
    FACEBOOK_META,
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

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun clearInput() {
        _inputText.value = ""
        _cleanResult.value = null
        _isExpanding.value = false
        _isFromShareIntent.value = false
    }

    fun processInputText(text: String, isSharedIntent: Boolean = false) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        _inputText.value = trimmed
        _isFromShareIntent.value = isSharedIntent

        viewModelScope.launch {
            // First immediate offline clean
            val initial = LinkCleaner.cleanText(
                inputText = trimmed,
                keepAmazonAffiliate = keepAmazonAffiliate.value,
                strictMode = strictMode.value
            )
            _cleanResult.value = initial

            // If it's a redirect / shortener link or Facebook share link, expand it asynchronously
            if (UrlExpander.shouldExpand(trimmed) || com.example.cleaner.FacebookWebViewExpander.isFacebookShareLink(trimmed)) {
                _isExpanding.value = true
                val expandedResult = LinkCleaner.cleanTextAsync(
                    inputText = trimmed,
                    context = getApplication(),
                    enableFacebookWebView = enableFacebookWebViewExpansion.value,
                    keepAmazonAffiliate = keepAmazonAffiliate.value,
                    strictMode = strictMode.value
                )
                _cleanResult.value = expandedResult
                _isExpanding.value = false

                saveResultToDatabase(expandedResult)
                handleAutoCopy(expandedResult, isSharedIntent)
            } else {
                saveResultToDatabase(initial)
                handleAutoCopy(initial, isSharedIntent)
            }
        }
    }

    private suspend fun saveResultToDatabase(result: CleanResult) {
        if (result.cleanedUrl.isNotEmpty() && result.cleanedUrl.startsWith("http")) {
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
        val clip = ClipData.newPlainText("CleanLink", text)
        clipboard.setPrimaryClip(clip)

        viewModelScope.launch {
            if (showToast) {
                _uiEvents.emit(UiEvent.ShowToast("Copied clean link to clipboard!"))
            }
            _uiEvents.emit(UiEvent.LinkCopied(text))
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
        try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
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
            SampleType.TIKTOK_SHARE ->
                "https://www.tiktok.com/@creator/video/7345678901234567890?_r=1&_t=8Wxyz&is_from_webapp=1&sender_device=pc&share_app_id=1233&share_link_id=abc123xyz"
            SampleType.FACEBOOK_META ->
                "https://www.facebook.com/share/p/18V4XJ9abc/?mibextid=wwXIfr"
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
}
