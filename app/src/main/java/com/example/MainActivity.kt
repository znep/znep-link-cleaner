package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.cleaner.LinkCleaner
import com.example.cleaner.UrlSecurityValidator
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.CleanLinkTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (handleIncomingIntent(intent)) {
            return
        }
        setupShareShortcuts()

        setContent {
            CleanLinkTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    private fun setupShareShortcuts() {
        try {
            val shortcut = androidx.core.content.pm.ShortcutInfoCompat.Builder(this, "direct_clean_link")
                .setShortLabel("Clean & Purge Link")
                .setLongLabel("ZnepLinkCleaner: Instant Tracker Purging")
                .setIcon(androidx.core.graphics.drawable.IconCompat.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(Intent(Intent.ACTION_SEND).apply {
                    setClass(this@MainActivity, MainActivity::class.java)
                    type = "text/plain"
                })
                .setCategories(setOf("com.znep.linkcleaner.category.DIRECT_SHARE_TARGET"))
                .setRank(1)
                .build()
            androidx.core.content.pm.ShortcutManagerCompat.pushDynamicShortcut(this, shortcut)
        } catch (_: Exception) {
            // Graceful fallback if shortcuts not supported
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    /**
     * Returns true if the intent was handled as an immediate in-place text replacement
     * or partial-selection guard and the Activity is finishing without rendering the full UI.
     */
    private fun handleIncomingIntent(intent: Intent?): Boolean {
        if (intent == null) return false

        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("text/") == true || intent.hasExtra(Intent.EXTRA_TEXT)) {
                    val sharedText = (intent.getStringExtra(Intent.EXTRA_TEXT)
                        ?: intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString())
                        ?.take(UrlSecurityValidator.MAX_INPUT_LENGTH)

                    if (!sharedText.isNullOrBlank()) {
                        viewModel.processInputText(sharedText, isSharedIntent = true)
                    }
                }
                return false
            }

            Intent.ACTION_VIEW -> {
                val viewUrl = intent.dataString?.take(UrlSecurityValidator.MAX_INPUT_LENGTH)
                if (!viewUrl.isNullOrBlank()) {
                    viewModel.processInputText(viewUrl, isSharedIntent = true)
                }
                return false
            }

            Intent.ACTION_PROCESS_TEXT -> {
                val processedText = (intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
                    ?: intent.getCharSequenceExtra(Intent.EXTRA_TEXT))
                    ?.toString()
                    ?.take(UrlSecurityValidator.MAX_INPUT_LENGTH)
                val isReadOnly = intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)

                if (processedText.isNullOrBlank()) {
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                    return true
                }

                if (!LinkCleaner.containsLink(processedText)) {
                    val trimmedToken = processedText.trim().take(24)
                    val toastMsg = if (LinkCleaner.isPartialUrlSelection(processedText)) {
                        "Only '$trimmedToken' was selected — drag selection handles across the full URL or tap Select All"
                    } else {
                        "No complete URL in selection — highlight the full link first"
                    }
                    Toast.makeText(applicationContext, toastMsg, Toast.LENGTH_LONG).show()
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                    return true
                }

                if (!isReadOnly) {
                    val result = viewModel.cleanSynchronouslyForInPlaceReplacement(processedText)
                    val resultIntent = Intent().apply {
                        putExtra(Intent.EXTRA_PROCESS_TEXT, result.cleanedText)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    val toastMsg = if (result.isUnchanged) {
                        "Link already clean (unchanged)"
                    } else {
                        "Link cleaned in-place (-${result.bytesSaved}B)"
                    }
                    Toast.makeText(applicationContext, toastMsg, Toast.LENGTH_SHORT).show()
                    finish()
                    return true
                } else {
                    viewModel.processInputText(processedText, isSharedIntent = true)
                    return false
                }
            }
        }
        return false
    }
}
