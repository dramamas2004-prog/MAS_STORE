package com.mas.store

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import org.json.JSONObject

// غيّر الرابط لو دومين متجرك اتغيّر (وغيّره كمان في AndroidManifest.xml)
const val STORE_URL = "https://mas-store-six.vercel.app/"

object Bridge {
    var web: WebView? = null
    fun emit(o: JSONObject) {
        web?.post { web?.evaluateJavascript("window.onNativeEvent&&window.onNativeEvent(" + JSONObject.quote(o.toString()) + ")", null) }
    }
}

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private val pkgReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) { Bridge.emit(JSONObject().put("type", "pkgchanged")) }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        setContentView(web)
        Bridge.web = web
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.userAgentString = web.settings.userAgentString + " MASStoreApp"
        CookieManager.getInstance().setAcceptCookie(true)
        web.addJavascriptInterface(NativeApi(this), "MASAndroid")
        web.webChromeClient = WebChromeClient()
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                val u = r.url
                if (u.host == Uri.parse(STORE_URL).host) return false
                try { CustomTabsIntent.Builder().build().launchUrl(this@MainActivity, u) }
                catch (e: Exception) { startActivity(Intent(Intent.ACTION_VIEW, u)) }
                return true
            }
        }
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED); addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, pkgReceiver, f, ContextCompat.RECEIVER_EXPORTED)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        web.loadUrl(urlFrom(intent))
    }

    private fun urlFrom(i: Intent?): String {
        val u = i?.data ?: return STORE_URL
        val q = u.encodedQuery; val fr = u.encodedFragment
        return STORE_URL + "?r=" + System.currentTimeMillis() + (if (q != null) "&$q" else "") + (if (fr != null) "#$fr" else "")
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); if (intent.data != null) web.loadUrl(urlFrom(intent)) }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { if (web.canGoBack()) web.goBack() else super.onBackPressed() }
    override fun onDestroy() { try { unregisterReceiver(pkgReceiver) } catch (e: Exception) {}; super.onDestroy() }
}
