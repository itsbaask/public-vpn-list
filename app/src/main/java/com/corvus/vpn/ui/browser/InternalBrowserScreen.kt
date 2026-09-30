package com.corvus.vpn.ui.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.delay
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.corvus.vpn.R
import com.corvus.vpn.tor.TorManager
import com.corvus.vpn.tor.TorManager.TorState
import com.corvus.vpn.ui.theme.*

private const val SEARCH_URL = "https://www.startpage.com/sp/search?q="
private val ErrorRed = Color(0xFFE5484D)
private val JsOrange = Color(0xFFFF9800)

/**
 * [iconRes] is optional: drop a real logo into res/drawable and pass it here.
 * Without it, a clean letter badge is shown (no random generic icons).
 */
private data class QuickLink(
    val label: String,
    val host: String,
    val url: String,
    @DrawableRes val iconRes: Int,
    val onion: Boolean = false
)

private val privacyLinks = listOf(
    // Official V3 onion address of DuckDuckGo (verify before each release).
    QuickLink(
        "DuckDuckGo", "onion service",
        "http://duckduckgogg42xjoc72x3sjasowoarfbgcmvfimaftt6twagswzczad.onion",
        R.drawable.logo_duckduckgo,
        onion = true
    ),
    QuickLink("Tor Check", "check.torproject.org", "https://check.torproject.org", R.drawable.logo_tor),
    QuickLink("Temp Mail", "temp-mail.org", "https://temp-mail.org", R.drawable.logo_tempmail),
    QuickLink("Archive.org", "archive.org", "https://archive.org", R.drawable.logo_archive)
)

private val knowledgeLinks = listOf(
    QuickLink("Wikipedia", "wikipedia.org", "https://www.wikipedia.org", R.drawable.logo_wikipedia),
    QuickLink("GitHub", "github.com", "https://github.com", R.drawable.logo_github),
    QuickLink("OpenStreetMap", "openstreetmap.org", "https://www.openstreetmap.org", R.drawable.logo_openstreetmap),
    QuickLink("Project Gutenberg", "gutenberg.org", "https://www.gutenberg.org", R.drawable.logo_gutenberg)
)

/** Turns whatever the user typed into a loadable URL (or a search). */
private fun resolveInput(raw: String): String? {
    val input = raw.trim()
    if (input.isEmpty()) return null
    if (input.startsWith("http://", true) || input.startsWith("https://", true)) return input

    val host = input.substringBefore('/').substringBefore('?').substringBefore('#')
    return when {
        host.endsWith(".onion", ignoreCase = true) -> "http://$input"
        input.contains(' ') || !input.contains('.') -> SEARCH_URL + Uri.encode(input)
        else -> "https://$input"
    }
}

private fun isOnionUrl(url: String): Boolean =
    try { Uri.parse(url).host?.endsWith(".onion", ignoreCase = true) == true } catch (_: Exception) { false }

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InternalBrowserScreen(
    isPro: Boolean = false,
    onTriggerGate: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val torState by TorManager.torState.collectAsState()
    val bootstrap by TorManager.bootstrapProgress.collectAsState()
    val socksPort by TorManager.socksPort.collectAsState()
    val identity by TorManager.circuitId.collectAsState()

    var address by remember { mutableStateOf("") }
    var addressFocused by remember { mutableStateOf(false) }
    var pageProgress by remember { mutableIntStateOf(100) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var showStartPage by remember { mutableStateOf(true) }
    var proxyReady by remember { mutableStateOf(false) }
    var proxyError by remember { mutableStateOf<String?>(null) }
    var pendingUrl by remember { mutableStateOf<String?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var jsEnabled by remember { mutableStateOf(true) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2000)
            toastMessage = null
        }
    }

    val webView = remember(context) {
        WebView(context).apply {
            with(settings) {
                javaScriptEnabled = true
                domStorageEnabled = true           // wiped on "New identity" and on exit
                allowFileAccess = false
                allowContentAccess = false
                setGeolocationEnabled(false)
                setSupportMultipleWindows(false)
                mediaPlaybackRequiresUserGesture = true
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                cacheMode = WebSettings.LOAD_NO_CACHE
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                // Google Safe Browsing would send visited URLs outside of Tor.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) safeBrowsingEnabled = false
                // No unique suffix: a custom UA makes every Corvus user fingerprintable.
                userAgentString = "Mozilla/5.0 (Android 10; Mobile; rv:128.0) Gecko/128.0 Firefox/128.0"
            }
            WebView.setWebContentsDebuggingEnabled(false)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    pageProgress = newProgress
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val scheme = request.url.scheme?.lowercase()
                    return scheme != "http" && scheme != "https"
                }

                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    if (url == "about:blank") return
                    loadError = null
                    if (!addressFocused) address = url
                }

                override fun onPageFinished(view: WebView, url: String) {
                    if (url == "about:blank") return
                    if (!addressFocused) address = url
                    canGoBack = view.canGoBack()
                    canGoForward = view.canGoForward()
                }

                override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                    canGoBack = view.canGoBack()
                    canGoForward = view.canGoForward()
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    if (request.isForMainFrame) {
                        loadError = error.description?.toString() ?: "Connection failed"
                    }
                }
            }
        }
    }

    // 1) Start the Tor daemon when entering the screen, unless it is already up.
    LaunchedEffect(Unit) {
        val current = TorManager.torState.value
        if (current != TorState.CONNECTED && current != TorState.STARTING) {
            TorManager.startTor(context)
        }
    }

    // 2) Route WebView through Tor ONLY after Tor is fully bootstrapped.
    //    No addDirect(): if Tor is down, requests fail instead of leaking your real IP.
    //    socks5:// (not socks://, which Chromium treats as SOCKS4) so DNS is resolved by Tor too.
    LaunchedEffect(torState, socksPort) {
        val port = socksPort
        if (torState == TorState.CONNECTED && port != null) {
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
                proxyError = "This device's WebView doesn't support proxy override. Update Android System WebView."
                return@LaunchedEffect
            }
            try {
                val config = ProxyConfig.Builder()
                    .addProxyRule("socks5://127.0.0.1:$port")
                    .build()
                ProxyController.getInstance().setProxyOverride(
                    config,
                    { it.run() },
                    { proxyReady = true }
                )
            } catch (e: Exception) {
                proxyError = e.message ?: "Failed to enable Tor proxy"
            }
        } else {
            proxyReady = false
        }
    }

    // 3) Load a URL requested while Tor was still connecting.
    LaunchedEffect(proxyReady, pendingUrl) {
        val url = pendingUrl
        if (proxyReady && url != null) {
            pendingUrl = null
            webView.loadUrl(url)
        }
    }

    fun navigate(raw: String) {
        if (!isPro) {
            onTriggerGate()
            return
        }
        val url = resolveInput(raw) ?: return
        showStartPage = false
        loadError = null
        address = url
        if (proxyReady) webView.loadUrl(url) else pendingUrl = url
    }

    fun toggleJavaScript() {
        if (!isPro) {
            onTriggerGate()
            return
        }
        jsEnabled = !jsEnabled
        webView.settings.javaScriptEnabled = jsEnabled
        if (!showStartPage && webView.url != null && webView.url != "about:blank") webView.reload()
        toastMessage = if (jsEnabled) "JavaScript on" else "JavaScript off"
    }

    /** The crow is a real connect / disconnect button. */
    fun toggleTor() {
        when (torState) {
            TorState.CONNECTED, TorState.STARTING -> {
                webView.stopLoading()
                pendingUrl = null
                // NOTE: use the real "stop" function name of your TorManager.
                TorManager.stopTor()
            }
            else -> TorManager.startTor(context)
        }
    }

    fun wipeSession() {
        try {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.clearHistory()
            webView.clearCache(true)
            webView.clearFormData()
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
        } catch (_: Exception) {}
        address = ""
        canGoBack = false
        canGoForward = false
        loadError = null
        pendingUrl = null
    }

    fun newIdentity() {
        if (!isPro) {
            onTriggerGate()
            return
        }
        TorManager.requestNewCircuit()
        wipeSession()
        showStartPage = true
        toastMessage = "New Tor identity"
    }

    BackHandler {
        when {
            !showStartPage && webView.canGoBack() -> webView.goBack()
            !showStartPage -> showStartPage = true
            else -> onBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                webView.stopLoading()
                webView.clearHistory()
                webView.clearCache(true)
                CookieManager.getInstance().removeAllCookies(null)
                WebStorage.getInstance().deleteAllData()
                webView.destroy()
            } catch (_: Exception) {}
            // Don't leave the proxy override active for the rest of the app.
            try {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
                    ProxyController.getInstance().clearProxyOverride({ it.run() }, {})
                }
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CrowBlack)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (showStartPage) {
                StartPage(
                    torState = torState,
                    progress = bootstrap,
                    identity = identity,
                    jsEnabled = jsEnabled,
                    onToggleJs = { toggleJavaScript() },
                    onToggleTor = { toggleTor() },
                    onNewIdentity = { newIdentity() },
                    onNavigate = { navigate(it) }
                )
            } else {
                BrowserPage(
                    webView = webView,
                    address = address,
                    onAddressChange = { address = it },
                    onAddressFocus = { addressFocused = it },
                    onSubmit = { navigate(address) },
                    pageProgress = pageProgress,
                    torState = torState,
                    bootstrap = bootstrap,
                    proxyReady = proxyReady,
                    proxyError = proxyError,
                    loadError = loadError,
                    canGoBack = canGoBack,
                    canGoForward = canGoForward,
                    jsEnabled = jsEnabled,
                    onToggleJs = { toggleJavaScript() },
                    onReload = {
                        if (webView.url != null) webView.reload() else navigate(address)
                    },
                    onStop = { webView.stopLoading() },
                    onBackNav = { if (webView.canGoBack()) webView.goBack() },
                    onForwardNav = { if (webView.canGoForward()) webView.goForward() },
                    onHome = { showStartPage = true },
                    onNewIdentity = { newIdentity() },
                    onConnect = { TorManager.startTor(context) }
                )
            }
        }

        // Custom App-Themed Toast Overlay matching App Design
        if (toastMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = CrowSurface,
                    border = BorderStroke(1.dp, CrowAccent.copy(alpha = 0.6f)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(CrowAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.logo_browser),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(CrowAccent),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = toastMessage ?: "",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = CrowText
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Start page
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StartPage(
    torState: TorState,
    progress: Int,
    identity: Int,
    jsEnabled: Boolean,
    onToggleJs: () -> Unit,
    onToggleTor: () -> Unit,
    onNewIdentity: () -> Unit,
    onNavigate: (String) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.logo_browser),
                contentDescription = null,
                colorFilter = ColorFilter.tint(CrowLogoOn),
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text("Corvus", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = CrowText)
            Spacer(Modifier.weight(1f))
            JsButton(jsEnabled, onToggleJs)
            IconButton(onClick = onNewIdentity) {
                Icon(Icons.Filled.Autorenew, contentDescription = "New identity", tint = CrowAccent)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Corvus Browser",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = CrowText
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Private by design  •  Routed through Tor",
                fontSize = 13.sp,
                color = CrowMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))
            TorConnectSection(torState, progress, identity, onToggleTor)

            Spacer(Modifier.height(20.dp))
            StartSearchField(onSearch = onNavigate)

            Spacer(Modifier.height(24.dp))
            QuickAccessSection("PRIVACY & TOOLS", privacyLinks, onNavigate)
            Spacer(Modifier.height(14.dp))
            QuickAccessSection("KNOWLEDGE & DEV", knowledgeLinks, onNavigate)

            Spacer(Modifier.height(12.dp))
            Text(
                text = "No history is saved. Cookies and site data are erased when you leave or tap New identity.",
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = CrowMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Simple JS switch: neutral when JavaScript is on, orange when it is off. */
@Composable
private fun JsButton(enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Filled.Code,
            contentDescription = if (enabled) "Disable JavaScript" else "Enable JavaScript",
            tint = if (enabled) CrowTextSub else JsOrange
        )
    }
}

@Composable
private fun TorConnectSection(
    state: TorState,
    progress: Int,
    identity: Int,
    onToggle: () -> Unit
) {
    val starting = state == TorState.STARTING

    val accentColor = when (state) {
        TorState.CONNECTED -> CrowAccent
        TorState.STARTING -> CrowGold
        TorState.FAILED -> ErrorRed
        else -> CrowMuted
    }

    val title = when (state) {
        TorState.CONNECTED -> "Connected to Tor"
        TorState.STARTING -> "Connecting ($progress%)..."
        TorState.FAILED -> "Connection Failed"
        else -> "Tap to take flight"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Calm, serene, elegant fixed logo button (static, non-moving)
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.15f),
                            CrowSurface
                        )
                    )
                )
                .border(1.5.dp, accentColor.copy(alpha = 0.7f), CircleShape)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            // Optional progress ring when starting
            if (starting) {
                CircularProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.size(132.dp),
                    color = CrowGold,
                    strokeWidth = 2.5.dp,
                    trackColor = Color.Transparent
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_browser),
                    contentDescription = "Corvus Logo",
                    colorFilter = ColorFilter.tint(
                        when (state) {
                            TorState.CONNECTED -> CrowAccent
                            TorState.STARTING -> CrowGold
                            TorState.FAILED -> ErrorRed
                            else -> CrowTextSub
                        }
                    ),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = when (state) {
                        TorState.CONNECTED -> "CONNECTED"
                        TorState.STARTING -> "$progress%"
                        TorState.FAILED -> "TAP TO RETRY"
                        else -> "DISCONNECTED"
                    },
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = accentColor
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Calm status text
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = CrowText,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = when (state) {
                TorState.CONNECTED -> if (identity > 0) "Secure circuit active • ID #$identity" else "Tap logo to disconnect"
                TorState.STARTING -> "Building secure Tor circuits…"
                TorState.FAILED -> "Check connection and tap to retry"
                else -> "Your traffic is private and encrypted"
            },
            fontSize = 12.5.sp,
            color = CrowTextSub,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StartSearchField(onSearch: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val submit = {
        if (input.isNotBlank()) {
            focusManager.clearFocus()
            onSearch(input)
        }
    }

    OutlinedTextField(
        value = input,
        onValueChange = { input = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text("Search or enter address / .onion", color = CrowMuted, fontSize = 14.sp)
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { submit() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = CrowSurface,
            unfocusedContainerColor = CrowSurface,
            focusedBorderColor = CrowAccent,
            unfocusedBorderColor = CrowBorderMid,
            focusedTextColor = CrowText,
            unfocusedTextColor = CrowText,
            cursorColor = CrowAccent
        ),
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = null, tint = CrowMuted, modifier = Modifier.size(20.dp))
        },
        trailingIcon = {
            if (input.isNotBlank()) {
                IconButton(onClick = { submit() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Go",
                        tint = CrowAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    )
}

@Composable
private fun QuickAccessSection(title: String, links: List<QuickLink>, onNavigate: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 10.5.sp,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Bold,
            color = CrowMuted
        )
        Spacer(Modifier.height(8.dp))
        links.chunked(2).forEach { rowLinks ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowLinks.forEach { link ->
                    QuickLinkCard(link, Modifier.weight(1f)) { onNavigate(link.url) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun QuickLinkCard(link: QuickLink, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = CrowSurface,
        border = BorderStroke(0.5.dp, CrowBorderMid),
        modifier = modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CrowSurface2),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(link.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    link.label,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CrowText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    link.host,
                    fontSize = 9.sp,
                    color = if (link.onion) CrowAccent else CrowMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Browsing page
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ColumnScope.BrowserPage(
    webView: WebView,
    address: String,
    onAddressChange: (String) -> Unit,
    onAddressFocus: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    pageProgress: Int,
    torState: TorState,
    bootstrap: Int,
    proxyReady: Boolean,
    proxyError: String?,
    loadError: String?,
    canGoBack: Boolean,
    canGoForward: Boolean,
    jsEnabled: Boolean,
    onToggleJs: () -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
    onBackNav: () -> Unit,
    onForwardNav: () -> Unit,
    onHome: () -> Unit,
    onNewIdentity: () -> Unit,
    onConnect: () -> Unit
) {
    val isLoading = pageProgress in 0..99

    // Top: status dot + address pill + JS switch + load progress
    Surface(color = CrowSurface, border = BorderStroke(0.5.dp, CrowBorder)) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TorDot(torState)
                AddressPill(
                    modifier = Modifier.weight(1f),
                    value = address,
                    onValueChange = onAddressChange,
                    onFocusChange = onAddressFocus,
                    onSubmit = onSubmit,
                    isLoading = isLoading,
                    onReload = onReload,
                    onStop = onStop
                )
                JsButton(jsEnabled, onToggleJs)
            }
            if (isLoading) {
                LinearProgressIndicator(
                    progress = { pageProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = CrowAccent,
                    trackColor = Color.Transparent
                )
            } else {
                Spacer(Modifier.height(2.dp))
            }
        }
    }

    // Content
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
    ) {
        AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())

        when {
            proxyError != null -> StatusOverlay(
                icon = Icons.Filled.ErrorOutline,
                tint = ErrorRed,
                title = "Secure browsing unavailable",
                message = proxyError,
                actionLabel = "Back",
                onAction = onHome
            )
            torState == TorState.FAILED -> StatusOverlay(
                icon = Icons.Filled.ErrorOutline,
                tint = ErrorRed,
                title = "Couldn't connect to Tor",
                message = "Corvus never falls back to a direct connection. Check your network and retry.",
                actionLabel = "Retry",
                onAction = onConnect
            )
            torState == TorState.STOPPED -> StatusOverlay(
                icon = Icons.Filled.PowerSettingsNew,
                tint = CrowMuted,
                title = "Disconnected",
                message = "Tor is off. Nothing loads until you connect.",
                actionLabel = "Connect",
                onAction = onConnect
            )
            !proxyReady -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CrowBlack),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (bootstrap > 0) {
                        CircularProgressIndicator(
                            progress = { bootstrap / 100f },
                            color = CrowGold,
                            trackColor = CrowBorder
                        )
                    } else {
                        CircularProgressIndicator(color = CrowGold, trackColor = CrowBorder)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Connecting to Tor…  $bootstrap%", color = CrowText, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Your page will load automatically", color = CrowMuted, fontSize = 12.sp)
                }
            }
            loadError != null -> StatusOverlay(
                icon = Icons.Filled.ErrorOutline,
                tint = CrowGold,
                title = "Couldn't load this page",
                message = loadError,
                actionLabel = "Try again",
                onAction = onReload
            )
        }
    }

    // Bottom toolbar
    Surface(color = CrowSurface, border = BorderStroke(0.5.dp, CrowBorder)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", canGoBack, onBackNav)
            ToolbarButton(Icons.AutoMirrored.Filled.ArrowForward, "Forward", canGoForward, onForwardNav)
            ToolbarButton(Icons.Filled.Home, "Home", true, onHome, CrowAccent)
            ToolbarButton(Icons.Filled.Autorenew, "New identity", true, onNewIdentity, CrowAccent)
        }
    }
}

@Composable
private fun AddressPill(
    modifier: Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    isLoading: Boolean,
    onReload: () -> Unit,
    onStop: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val shape = RoundedCornerShape(22.dp)

    val onion = isOnionUrl(value)
    val secureIcon: ImageVector
    val secureTint: Color
    when {
        onion -> { secureIcon = Icons.Filled.Security; secureTint = CrowAccent }
        value.startsWith("https://", true) -> { secureIcon = Icons.Filled.Lock; secureTint = CrowAccent }
        value.startsWith("http://", true) -> { secureIcon = Icons.Filled.LockOpen; secureTint = CrowGold }
        else -> { secureIcon = Icons.Filled.Search; secureTint = CrowMuted }
    }

    Row(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(CrowBlack)
            .border(0.5.dp, if (focused) CrowAccent else CrowBorderMid, shape)
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(secureIcon, contentDescription = null, tint = secureTint, modifier = Modifier.size(16.dp))

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = CrowText, fontSize = 14.sp),
            cursorBrush = SolidColor(CrowAccent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = {
                focusManager.clearFocus()
                onSubmit()
            }),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged {
                    focused = it.isFocused
                    onFocusChange(it.isFocused)
                }
        )

        if (onion) {
            Surface(shape = RoundedCornerShape(6.dp), color = CrowAccent.copy(alpha = 0.15f)) {
                Text(
                    "ONION",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrowAccent,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        IconButton(onClick = if (isLoading) onStop else onReload, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = if (isLoading) Icons.Filled.Close else Icons.Filled.Refresh,
                contentDescription = if (isLoading) "Stop" else "Reload",
                tint = CrowTextSub,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun TorDot(state: TorState) {
    val color = when (state) {
        TorState.CONNECTED -> CrowAccent
        TorState.FAILED -> ErrorRed
        TorState.STARTING -> CrowGold
        else -> CrowMuted
    }
    val blink by rememberInfiniteTransition(label = "dot").animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "dotAlpha"
    )
    val steady = state == TorState.CONNECTED || state == TorState.STOPPED
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = if (steady) 1f else blink))
    )
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tint: Color = CrowText
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled) tint else CrowMuted.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun StatusOverlay(
    icon: ImageVector,
    tint: Color,
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CrowBlack)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = CrowText, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(message, fontSize = 13.sp, color = CrowTextSub, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = CrowAccent, contentColor = CrowBlack),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(actionLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}