package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("clean_link_prefs", Context.MODE_PRIVATE)

    private val _autoCopyOnShare = MutableStateFlow(prefs.getBoolean(KEY_AUTO_COPY, true))
    val autoCopyOnShare: StateFlow<Boolean> = _autoCopyOnShare.asStateFlow()

    private val _autoCloseAfterReshare = MutableStateFlow(prefs.getBoolean(KEY_AUTO_CLOSE, false))
    val autoCloseAfterReshare: StateFlow<Boolean> = _autoCloseAfterReshare.asStateFlow()

    private val _keepAmazonAffiliate = MutableStateFlow(prefs.getBoolean(KEY_KEEP_AMAZON, false))
    val keepAmazonAffiliate: StateFlow<Boolean> = _keepAmazonAffiliate.asStateFlow()

    private val _strictMode = MutableStateFlow(prefs.getBoolean(KEY_STRICT_MODE, false))
    val strictMode: StateFlow<Boolean> = _strictMode.asStateFlow()

    // Option 1: Headless WebView Expansion for Facebook Share links (Enabled by default)
    private val _enableFacebookWebViewExpansion =
        MutableStateFlow(prefs.getBoolean(KEY_ENABLE_FB_WEBVIEW, true))
    val enableFacebookWebViewExpansion: StateFlow<Boolean> = _enableFacebookWebViewExpansion.asStateFlow()

    // Security Option: Offline-Only Mode (No automatic outbound network expansion)
    private val _offlineOnlyMode =
        MutableStateFlow(prefs.getBoolean(KEY_OFFLINE_ONLY_MODE, false))
    val offlineOnlyMode: StateFlow<Boolean> = _offlineOnlyMode.asStateFlow()

    fun setAutoCopyOnShare(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_COPY, enabled).apply()
        _autoCopyOnShare.value = enabled
    }

    fun setAutoCloseAfterReshare(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CLOSE, enabled).apply()
        _autoCloseAfterReshare.value = enabled
    }

    fun setKeepAmazonAffiliate(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_AMAZON, enabled).apply()
        _keepAmazonAffiliate.value = enabled
    }

    fun setStrictMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STRICT_MODE, enabled).apply()
        _strictMode.value = enabled
    }

    fun setEnableFacebookWebViewExpansion(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLE_FB_WEBVIEW, enabled).apply()
        _enableFacebookWebViewExpansion.value = enabled
    }

    fun setOfflineOnlyMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OFFLINE_ONLY_MODE, enabled).apply()
        _offlineOnlyMode.value = enabled
    }

    companion object {
        private const val KEY_AUTO_COPY = "auto_copy_on_share"
        private const val KEY_AUTO_CLOSE = "auto_close_after_reshare"
        private const val KEY_KEEP_AMAZON = "keep_amazon_affiliate"
        private const val KEY_STRICT_MODE = "strict_mode"
        private const val KEY_ENABLE_FB_WEBVIEW = "enable_fb_webview_expansion"
        private const val KEY_OFFLINE_ONLY_MODE = "offline_only_mode"
    }
}
