package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.CleanLinkTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingIntent(intent)
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
                .setLongLabel("CleanLink: Instant Tracker Purging")
                .setIcon(androidx.core.graphics.drawable.IconCompat.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(Intent(Intent.ACTION_SEND).apply {
                    setClass(this@MainActivity, MainActivity::class.java)
                    type = "text/plain"
                })
                .setCategories(setOf("com.example.cleanlink.category.DIRECT_SHARE_TARGET"))
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

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        if (Intent.ACTION_SEND == intent.action && intent.type?.startsWith("text/") == true) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                ?: intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()

            if (!sharedText.isNullOrBlank()) {
                viewModel.processInputText(sharedText, isSharedIntent = true)
            }
        } else if (Intent.ACTION_PROCESS_TEXT == intent.action && intent.type?.startsWith("text/") == true) {
            val processedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            if (!processedText.isNullOrBlank()) {
                viewModel.processInputText(processedText, isSharedIntent = true)
            }
        }
    }
}
