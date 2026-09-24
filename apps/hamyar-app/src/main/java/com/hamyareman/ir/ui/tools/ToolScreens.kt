package com.hamyareman.ir.ui.tools

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.ui.AppTypography
import kotlinx.coroutines.launch
import org.json.JSONObject

data class ToolCard(
    val id: String,
    val emoji: String,
    val title: String,
    val subtitle: String,
)

internal fun WebSettings.enableStudyPinchZoom() {
    setSupportZoom(true)
    builtInZoomControls = true
    displayZoomControls = false
    loadWithOverviewMode = true
    useWideViewPort = true
}

/** آزمایشگاه تمام‌صفحه: overview/pinch کل صفحه را به نوار باریک تبدیل می‌کرد. */
internal fun WebSettings.enableLabLayout() {
    setSupportZoom(false)
    builtInZoomControls = false
    displayZoomControls = false
    loadWithOverviewMode = false
    useWideViewPort = false
}

@Composable
fun ToolHubScreen(
    title: String,
    subtitle: String,
    items: List<ToolCard>,
    onBack: () -> Unit,
    onOpen: (ToolCard) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        AppTopBar(title, onBack)
        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            items.forEach { item ->
                Card(
                    onClick = { onOpen(item) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(item.emoji, fontSize = 28.sp)
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = AppTypography.cardTitle.family,
                                fontWeight = AppTypography.cardTitle.weight,
                                fontSize = AppTypography.cardTitle.size,
                            )
                            Text(
                                item.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = AppTypography.cardSub.family,
                                fontWeight = AppTypography.cardSub.weight,
                                fontSize = AppTypography.cardSub.size,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun GeneralToolkitScreen(onBack: () -> Unit, onOpen: (String) -> Unit) = ToolHubScreen(
    title = "جعبه‌ابزار عمومی",
    subtitle = "تقویم، ماشین‌حساب 120D و مبدل واحد — هرکدام کارت جدا.",
    items = listOf(
        ToolCard("calendar", "📅", "تقویم", "تقویم شمسی، تبدیل تاریخ و رویدادهای ذخیره‌شده"),
        ToolCard("dj120d", "🧮", "ماشین حساب 120D", "شبیه‌ساز DJ-120D Plus"),
        ToolCard("converter", "🔁", "مبدل", "مبدل همه‌کاره مهندسی"),
    ),
    onBack = onBack,
    onOpen = { onOpen(it.id) },
)

@Composable
fun MathToolkitScreen(onBack: () -> Unit, onOpen: (String) -> Unit) = ToolHubScreen(
    title = "جعبه‌ابزار ریاضی",
    subtitle = "ماشین‌حساب‌های مهندسی برای تمرین‌های ریاضی نهم.",
    items = listOf(
        ToolCard("ti_nspire", "📐", "TI-Nspire CX II-T CAS", "ماشین‌حساب نموداری تگزاس اینسترومنتس"),
        ToolCard("casio991", "🔢", "CASIO fx-991CW", "کاسیو ClassWiz نسل CW"),
    ),
    onBack = onBack,
    onOpen = { onOpen(it.id) },
)

@Composable
fun PhysicsLabScreen(onBack: () -> Unit) = ToolWebScreen("physics", "آزمایشگاه فیزیک", onBack)

@Composable
fun ChemistryLabScreen(onBack: () -> Unit) = ToolWebScreen("chemistry", "آزمایشگاه شیمی", onBack)

@Composable
fun BiologyLabScreen(onBack: () -> Unit) = ToolWebScreen("biology", "آزمایشگاه زیست‌شناسی", onBack)

internal fun toolTitle(id: String): String = when (id) {
    "calendar" -> "تقویم"
    "dj120d" -> "ماشین حساب 120D"
    "converter" -> "مبدل"
    "physics" -> "آزمایشگاه فیزیک"
    "chemistry" -> "آزمایشگاه شیمی"
    "biology" -> "آزمایشگاه زیست‌شناسی"
    "ti_nspire" -> "TI-Nspire CX II-T CAS"
    "casio991" -> "CASIO fx-991CW"
    else -> "ابزار"
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ToolWebScreen(toolId: String, title: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val webRef = remember { arrayOfNulls<WebView>(1) }
    val premium = StudentProfileState.isPaid()
    val isLab = toolId == "chemistry" || toolId == "physics" || toolId == "biology"
    val executeLocked = !premium && (toolId == "ti_nspire" || toolId == "casio991")
    val bridge = remember(toolId) {
        HamyarToolBridge(ctx.applicationContext, toolId) { json ->
            ToolSaveStore.put(ctx, toolId, json)
            scope.launch {
                val uid = container.auth.cachedUserId()
                    ?: runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
                if (uid.isNotBlank()) ToolSaveStore.push(ctx, container.tables, uid)
            }
        }
    }
    fun applySaved(view: WebView?) {
        if (view == null) return
        val saved = ToolSaveStore.get(ctx, toolId)
        val quoted = if (saved.isBlank()) "null" else JSONObject.quote(saved)
        view.evaluateJavascript(
            "(function(){" +
                "function go(){try{if(window.HamyarToolApply&&$quoted)HamyarToolApply($quoted);}catch(e){}}" +
                "if(window.HamyarToolApply){go();return;}" +
                "var s=document.createElement('script');" +
                "s.src='file:///android_asset/tools/hamyar-tool-persist.js';" +
                "s.onload=go;" +
                "document.documentElement.appendChild(s);" +
                "})();",
            null,
        )
    }
    LaunchedEffect(toolId) {
        val uid = container.auth.cachedUserId()
            ?: runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
        if (uid.isNotBlank()) runCatching { ToolSaveStore.pull(ctx, container.tables, uid) }
        applySaved(webRef[0])
    }
    Column(Modifier.fillMaxSize()) {
        AppTopBar(title, onBack)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = { c ->
                    WebView(c).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        if (isLab) settings.enableLabLayout() else settings.enableStudyPinchZoom()
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = false
                            override fun onPageFinished(view: WebView, url: String) {
                                if (isLab) {
                                    view.evaluateJavascript(
                                        "(function(){try{" +
                                            "var h=document.documentElement,b=document.body;" +
                                            "if(h){h.style.height='100%';h.style.minHeight='100%';}" +
                                            "if(b){b.style.height='100%';b.style.minHeight='100%';b.style.overflow='hidden';}" +
                                            "var w=document.querySelector('.workspace');" +
                                            "if(w){w.style.height='auto';w.style.flex='1 1 auto';w.style.minHeight='0';}" +
                                            "var g=document.getElementById('guideOverlay');" +
                                            "if(g){g.style.position='fixed';g.style.inset='0';}" +
                                            "}catch(e){}})();",
                                        null,
                                    )
                                    view.evaluateJavascript(labLockJs(premium), null)
                                }
                                applySaved(view)
                            }
                        }
                        addJavascriptInterface(bridge, "HamyarTool")
                        setBackgroundColor(if (isLab) android.graphics.Color.parseColor("#050912") else android.graphics.Color.TRANSPARENT)
                        webRef[0] = this
                        loadUrl("file:///android_asset/tools/$toolId.html")
                    }
                },
                modifier = Modifier.fillMaxSize(),
                onRelease = { webRef[0] = null; it.destroy() },
            )
            if (executeLocked) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.22f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) {},
                )
                Text(
                    "ماشین‌حساب را می‌بینی؛ اجرا با اشتراک فعال است.",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .background(Color(0xE67F1D1D), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
    }
}

private fun labLockJs(premium: Boolean): String {
    val flag = if (premium) "true" else "false"
    return """
    (function(){
      if (window.__hamyarLabLock) return;
      window.__hamyarLabLock = true;
      window.HamyarPremium = $flag;
      window.__labLinear = 0;
      function items(){ return Array.prototype.slice.call(document.querySelectorAll('.exp-item')); }
      function linear(){
        var a = document.querySelector('.exp-item.active');
        var i = items().indexOf(a);
        return i < 0 ? (window.__labLinear||0) : i;
      }
      function free(){ return !!window.HamyarPremium || linear() <= 1; }
      function banner(on){
        var el = document.getElementById('hamyar-sub-banner');
        if (!on) { if (el) el.style.display = 'none'; return; }
        if (!el) {
          el = document.createElement('div');
          el.id = 'hamyar-sub-banner';
          el.style.cssText = 'position:fixed;bottom:10px;left:10px;right:10px;z-index:2147483647;background:#7f1d1d;color:#fff;padding:10px 14px;border-radius:12px;font-family:Tahoma,sans-serif;text-align:center;font-size:13px;pointer-events:none';
          el.textContent = 'این آزمایش با اشتراک فعال اجرا می‌شود. محتوا را می‌بینی؛ اجرا قفل است.';
          document.body.appendChild(el);
        }
        el.style.display = 'block';
      }
      function decorate(){
        if (window.HamyarPremium) return;
        items().forEach(function(el, i){
          if (i <= 1) return;
          if (el.dataset.hyLock) return;
          el.dataset.hyLock = '1';
          el.style.opacity = '0.72';
          el.appendChild(document.createTextNode(' 🔒'));
        });
      }
      function wrap(name){
        var fn = window[name];
        if (typeof fn !== 'function' || fn.__hy) return;
        var wrapped = function(){
          if (name.indexOf('navigate') === 0) {
            var el = arguments[2];
            if (el) window.__labLinear = items().indexOf(el);
            var r = fn.apply(this, arguments);
            decorate();
            banner(!free());
            return r;
          }
          if (name === 'buildNavTree') {
            var r2 = fn.apply(this, arguments);
            decorate();
            banner(!free());
            return r2;
          }
          if (!free()) { banner(true); return; }
          return fn.apply(this, arguments);
        };
        wrapped.__hy = true;
        window[name] = wrapped;
      }
      ['buildNavTree','navigateChemLab','navigatePhysLab','navigateBioLab','toggleChemSim','togglePhysSim','toggleBioSim','startSim'].forEach(wrap);
      document.addEventListener('click', function(e){
        var t = e.target;
        if (!t || free()) return;
        var run = (t.id === 'btnRun') || (t.closest && t.closest('#btnRun,.btn-run'));
        if (run) { e.preventDefault(); e.stopPropagation(); banner(true); }
      }, true);
      decorate();
      banner(!free());
    })();
    """.trimIndent()
}

internal class HamyarToolBridge(
    private val appCtx: android.content.Context,
    private val toolId: String,
    private val onChanged: (String) -> Unit,
) {
    @JavascriptInterface
    fun onSave(json: String) {
        if (json.isBlank()) return
        ToolSaveStore.put(appCtx, toolId, json)
        onChanged(json)
    }

    @JavascriptInterface
    fun load(): String = ToolSaveStore.get(appCtx, toolId)
}
