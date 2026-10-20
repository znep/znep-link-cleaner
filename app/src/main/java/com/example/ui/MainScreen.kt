package com.example.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.CleanScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.RulesScreen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class MainTab(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    CLEAN("Clean", Icons.Filled.Link, Icons.Outlined.Link),
    HISTORY("History", Icons.Filled.History, Icons.Outlined.History),
    RULES("Protection", Icons.Filled.Shield, Icons.Outlined.Shield)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableStateOf(MainTab.CLEAN) }
    var copiedTextState by remember { mutableStateOf<String?>(null) }

    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val cleanResult by viewModel.cleanResult.collectAsStateWithLifecycle()
    val isExpanding by viewModel.isExpanding.collectAsStateWithLifecycle()
    val isFromShareIntent by viewModel.isFromShareIntent.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    val autoCopy by viewModel.autoCopyOnShare.collectAsStateWithLifecycle()
    val autoClose by viewModel.autoCloseAfterReshare.collectAsStateWithLifecycle()
    val keepAmazon by viewModel.keepAmazonAffiliate.collectAsStateWithLifecycle()
    val strictMode by viewModel.strictMode.collectAsStateWithLifecycle()
    val enableFacebookWebView by viewModel.enableFacebookWebViewExpansion.collectAsStateWithLifecycle()

    // Handle ViewModel events
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.LinkCopied -> {
                    copiedTextState = event.text
                    snackbarHostState.showSnackbar("Clean link copied to clipboard!")
                }
                is UiEvent.FinishActivity -> {
                    (context as? Activity)?.finish()
                }
            }
        }
    }

    // Handle back button on secondary tabs
    BackHandler(enabled = selectedTab != MainTab.CLEAN) {
        selectedTab = MainTab.CLEAN
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CleanLink",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                        ) {
                            Text(
                                text = "v${com.example.BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                MainTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                MainTab.CLEAN -> {
                    CleanScreen(
                        inputText = inputText,
                        cleanResult = cleanResult,
                        isExpanding = isExpanding,
                        isFromShareIntent = isFromShareIntent,
                        onInputChanged = { viewModel.updateInputText(it) },
                        onCleanClicked = { viewModel.processInputText(it) },
                        onClearClicked = { viewModel.clearInput() },
                        onCopyCleaned = { viewModel.copyToClipboard(it) },
                        onShareCleaned = { viewModel.shareLink(context, it) },
                        onOpenBrowser = { viewModel.openInBrowser(context, it) },
                        onLoadSample = { viewModel.loadSample(it) },
                        copiedTextState = copiedTextState,
                        onViewFacebookDetails = { selectedTab = MainTab.RULES }
                    )
                }
                MainTab.HISTORY -> {
                    HistoryScreen(
                        history = history,
                        stats = stats,
                        onCopy = { viewModel.copyToClipboard(it) },
                        onShare = { viewModel.shareLink(context, it) },
                        onDelete = { viewModel.deleteHistoryItem(it) },
                        onClearAll = { viewModel.clearAllHistory() }
                    )
                }
                MainTab.RULES -> {
                    RulesScreen(
                        autoCopy = autoCopy,
                        autoClose = autoClose,
                        keepAmazon = keepAmazon,
                        strictMode = strictMode,
                        enableFacebookWebView = enableFacebookWebView,
                        onAutoCopyChanged = { viewModel.setAutoCopy(it) },
                        onAutoCloseChanged = { viewModel.setAutoClose(it) },
                        onKeepAmazonChanged = { viewModel.setKeepAmazon(it) },
                        onStrictModeChanged = { viewModel.setStrictMode(it) },
                        onEnableFacebookWebViewChanged = { viewModel.setEnableFacebookWebViewExpansion(it) }
                    )
                }
            }
        }
    }
}
