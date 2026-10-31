package com.mas.store

import android.app.*
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.webkit.JavascriptInterface
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class NativeApi(private val a: MainActivity) {

    @JavascriptInterface fun isInstalled(pkg: String): Boolean =
        try { a.packageManager.getPackageInfo(pkg, 0); true } catch (e: Exception) { false }

    @JavascriptInterface fun open(pkg: String): Boolean {
        val i = a.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        a.startActivity(i); return true
    }

    @JavascriptInterface fun openExternal(url: String) {
        a.runOnUiThread {
            try { CustomTabsIntent.Builder().build().launchUrl(a, Uri.parse(url)) }
            catch (e: Exception) { a.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        }
    }

    @JavascriptInterface fun notify(title: String, body: String) {
        val nm = a.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel("mas", "MAS STORE", NotificationManager.IMPORTANCE_HIGH))
        val pi = PendingIntent.getActivity(a, 0, Intent(a, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(a, "mas").setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title).setContentText(body).setAutoCancel(true).setContentIntent(pi)
            .setDefaults(NotificationCompat.DEFAULT_ALL).build()
        nm.notify((System.currentTimeMillis() % 100000).toInt(), n)
    }

    @JavascriptInterface fun install(url: String, id: String) {
        if (!a.packageManager.canRequestPackageInstalls()) {
            a.runOnUiThread { a.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + a.packageName))) }
            Bridge.emit(JSONObject().put("type", "permission").put("id", id)); return
        }
        Thread {
            try {
                val f = File(a.cacheDir, "dl-$id.apk")
                val c = URL(url).openConnection() as HttpURLConnection
                c.connect()
                if (c.responseCode != 200) throw Exception("HTTP " + c.responseCode)
                val total = c.contentLengthLong; var got = 0L; var last = -1
                c.inputStream.use { inp -> f.outputStream().use { out ->
                    val buf = ByteArray(65536)
                    while (true) {
                        val n = inp.read(buf); if (n < 0) break
                        out.write(buf, 0, n); got += n
                        if (total > 0) { val pc = (got * 100 / total).toInt(); if (pc != last) { last = pc; Bridge.emit(JSONObject().put("type", "progress").put("id", id).put("pc", pc)) } }
                    }
                } }
                val pkg = a.packageManager.getPackageArchiveInfo(f.path, 0)?.packageName ?: ""
                val inst = a.packageManager.packageInstaller
                val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
                if (pkg.isNotEmpty()) params.setAppPackageName(pkg)
                val sid = inst.createSession(params)
                inst.openSession(sid).use { s ->
                    f.inputStream().use { i -> s.openWrite("app.apk", 0, f.length()).use { o -> i.copyTo(o); s.fsync(o) } }
                    val pend = PendingIntent.getBroadcast(a, sid,
                        Intent(a, InstallReceiver::class.java).putExtra("id", id).putExtra("pkg", pkg).setPackage(a.packageName),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
                    s.commit(pend.intentSender)
                }
                Bridge.emit(JSONObject().put("type", "installing").put("id", id).put("pkg", pkg))
                f.delete()
            } catch (e: Exception) {
                Bridge.emit(JSONObject().put("type", "error").put("id", id).put("msg", e.message ?: "خطأ"))
            }
        }.start()
    }
}
