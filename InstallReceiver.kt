package com.mas.store

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import org.json.JSONObject

class InstallReceiver : BroadcastReceiver() {
    @Suppress("DEPRECATION")
    private fun confirmIntent(i: Intent): Intent? =
        if (Build.VERSION.SDK_INT >= 33) i.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java) else i.getParcelableExtra(Intent.EXTRA_INTENT)

    override fun onReceive(c: Context, i: Intent) {
        val id = i.getStringExtra("id") ?: ""
        val pkg = i.getStringExtra("pkg") ?: ""
        when (i.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val ui = confirmIntent(i)
                if (ui != null) { ui.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); c.startActivity(ui) }
            }
            PackageInstaller.STATUS_SUCCESS -> Bridge.emit(JSONObject().put("type", "done").put("id", id).put("pkg", pkg))
            else -> Bridge.emit(JSONObject().put("type", "error").put("id", id).put("msg", i.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "فشل التثبيت"))
        }
    }
}
