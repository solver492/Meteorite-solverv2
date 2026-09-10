package com.example.wormhole.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    viewModel: EdgeCometViewModel,
    modifier: Modifier = Modifier
) {
    val tab = viewModel.activeTab
    var urlInput by remember(tab?.url) { mutableStateOf(tab?.url ?: "") }
    val focusManager = LocalFocusManager.current
    var showMenu by remember { mutableStateOf(false) }

    // Handle system back gesture
    BackHandler(enabled = tab?.canGoBack == true) {
        viewModel.currentWebView?.goBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0B18))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Unified Top Address & Search Bar
            Surface(
                color = Color(0xFF141226),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // SSL Lock or Incognito Icon
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Security",
                            tint = if (tab?.isIncognito == true) Color(0xFFBA68C8) else Color(0xFF81C784),
                            modifier = Modifier
                                .size(18.dp)
                                .padding(start = 2.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Address Input
                        TextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            placeholder = {
                                Text(
                                    "Search with DuckDuckGo or enter URL",
                                    color = Color(0x88FFFFFF),
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                focusManager.clearFocus()
                                viewModel.openUrl(urlInput)
                            }),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1F1C38),
                                unfocusedContainerColor = Color(0xFF1A182F),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color(0xDDFFFFFF),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("address_bar_input")
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        // Refresh / Stop Button
                        IconButton(
                            onClick = {
                                if (tab?.isLoading == true) {
                                    viewModel.currentWebView?.stopLoading()
                                } else {
                                    viewModel.currentWebView?.reload()
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (tab?.isLoading == true) Icons.Default.Close else Icons.Default.Refresh,
                                contentDescription = "Reload",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Tab Manager Badge Button
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.5.dp, Color(0x88FFFFFF), RoundedCornerShape(8.dp))
                                .clickable { viewModel.isTabManagerOpen = true }
                                .testTag("tab_manager_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${viewModel.tabs.size}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Overflow menu
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Menu",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(Color(0xFF1E1B38))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Bookmark Page", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.BookmarkBorder, null, tint = Color(0xFFFFD54F)) },
                                    onClick = {
                                        viewModel.bookmarkCurrentPage()
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("New Tab", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Add, null, tint = Color.White) },
                                    onClick = {
                                        viewModel.addTab()
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("On-Device Models", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Memory, null, tint = Color(0xFF64B5F6)) },
                                    onClick = {
                                        viewModel.isModelManagerOpen = true
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Python Automations", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Terminal, null, tint = Color(0xFF81C784)) },
                                    onClick = {
                                        viewModel.isAutomationsOpen = true
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Planificateur IA (Cron)", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Schedule, null, tint = Color(0xFFFFB74D)) },
                                    onClick = {
                                        viewModel.isTaskSchedulerOpen = true
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Settings & Privacy", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Settings, null, tint = Color(0xFFBA68C8)) },
                                    onClick = {
                                        viewModel.isSettingsOpen = true
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Progress bar
                    if (tab?.isLoading == true) {
                        LinearProgressIndicator(
                            color = Color(0xFFFFD54F),
                            trackColor = Color.Transparent,
                            modifier = Modifier.fillMaxWidth().height(2.5.dp)
                        )
                    }
                }
            }

            // WebView Main Viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                allowFileAccess = false
                                setSupportMultipleWindows(false)
                                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36 MeteoriteSolver/1.0"
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    url?.let {
                                        urlInput = it
                                        viewModel.onPageStarted(it)
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    if (view != null && url != null) {
                                        viewModel.onPageFinished(view, url)
                                    }
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    return false // Load inside this WebView
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)
                                    viewModel.activeTab?.progress = newProgress
                                }
                            }

                            tab?.url?.let { loadUrl(it) }
                            viewModel.currentWebView = this
                        }
                    },
                    update = { webView ->
                        viewModel.currentWebView = webView
                        if (tab != null && webView.url != tab.url && tab.url != "about:blank") {
                            webView.loadUrl(tab.url)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Bottom Navigation Toolbar
            Surface(
                color = Color(0xFF141226),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back
                    IconButton(
                        onClick = { viewModel.currentWebView?.goBack() },
                        enabled = tab?.canGoBack == true
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = if (tab?.canGoBack == true) Color.White else Color(0x44FFFFFF)
                        )
                    }

                    // Forward
                    IconButton(
                        onClick = { viewModel.currentWebView?.goForward() },
                        enabled = tab?.canGoForward == true
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Forward",
                            tint = if (tab?.canGoForward == true) Color.White else Color(0x44FFFFFF)
                        )
                    }

                    // Floating Agentic Comet AI Button
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFFD54F), Color(0xFFFFB300))
                                )
                            )
                            .clickable { viewModel.isChatSheetOpen = true }
                            .padding(horizontal = 16.dp)
                            .testTag("floating_comet_ai_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Meteorite AI",
                                tint = Color(0xFF0F0D20),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Ask Page",
                                color = Color(0xFF0F0D20),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Agent Mode quick button
                    IconButton(
                        onClick = {
                            viewModel.askFindKeyActions()
                        },
                        modifier = Modifier.testTag("quick_agent_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Agent Actions",
                            tint = Color(0xFF64B5F6)
                        )
                    }

                    // Bookmarks / History
                    IconButton(
                        onClick = { viewModel.isSettingsOpen = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
