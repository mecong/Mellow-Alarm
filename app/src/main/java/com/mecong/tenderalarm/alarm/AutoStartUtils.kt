package com.mecong.tenderalarm.alarm

import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import com.mecong.tenderalarm.R
import com.mecong.tenderalarm.model.PropertyName
import com.mecong.tenderalarm.model.SQLiteDBHelper

class AutoStartUtils {

  companion object {
    fun runAutostartIfSupported(context: Context): Boolean {
      val autostartIntent = findAutoStartIntent(context)
      if (autostartIntent != null) {
        runAutostartDialog(context, autostartIntent)
        return true
      }
      return false
    }

    fun runAutostart(context: Context, intent: Intent) {
      try {
        val sqLiteDBHelper = SQLiteDBHelper.sqLiteDBHelper(context)!!
        sqLiteDBHelper.setPropertyString(PropertyName.AUTOSTART_TURNED_ON, "1")

        startActivity(context, intent, null)
      } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.cant_open_autostart), Toast.LENGTH_LONG).show()
      }
    }

    private fun runAutostartDialog(context: Context, intent: Intent) {
      val dialog = Dialog(context, R.style.UrlDialogCustom)
      dialog.setContentView(R.layout.autostart_dialog)

      val textView = dialog.findViewById<TextView>(R.id.textView)
      val buttonOk = dialog.findViewById<Button>(R.id.buttonOk)

      val appName = context.getString(R.string.app_name)
      textView.text = context.getString(R.string.dialog_autostart, appName)

      buttonOk.setOnClickListener {
        runAutostart(context, intent)
        dialog.dismiss()
      }

      val lp = WindowManager.LayoutParams()
      lp.copyFrom(dialog.window!!.attributes)
      lp.width = WindowManager.LayoutParams.MATCH_PARENT
      lp.height = WindowManager.LayoutParams.WRAP_CONTENT
      dialog.show()
      dialog.window!!.attributes = lp
    }

    fun findAutoStartIntent(context: Context): Intent? {
      return autoStartIntents.firstOrNull {
        context.packageManager.resolveActivity(it, PackageManager.MATCH_DEFAULT_ONLY) != null
      }
    }

    private val autoStartIntents = arrayOf(
      Intent().setComponent(
        ComponentName(
          "com.samsung.android.lool",
          "com.samsung.android.sm.ui.battery.BatteryActivity"
        )
      ),
      Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT),
      Intent().setComponent(
        ComponentName(
          "com.miui.securitycenter",
          "com.miui.permcenter.autostart.AutoStartManagementActivity"
        )
      ),
      Intent().setComponent(
        ComponentName(
          "com.letv.android.letvsafe",
          "com.letv.android.letvsafe.AutobootManageActivity"
        )
      ),
      Intent().setComponent(
        ComponentName(
          "com.huawei.systemmanager",
          "com.huawei.systemmanager.optimize.process.ProtectActivity"
        )
      ),
      Intent().setComponent(
        ComponentName(
          "com.coloros.safecenter",
          "com.coloros.safecenter.permission.startup.StartupAppListActivity"
        )
      ),
      Intent().setComponent(
        ComponentName(
          "com.coloros.safecenter",
          "com.coloros.safecenter.startupapp.StartupAppListActivity"
        )
      ),
      Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
      Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
      Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
      Intent().setComponent(
        ComponentName(
          "com.evenwell.powersaving.g3",
          "com.evenwell.powersaving.g3.exception.PowerSaverExceptionActivity"
        )
      ),
      Intent().setComponent(
        ComponentName(
          "com.vivo.permissionmanager",
          "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
        )
      ),
      Intent().setComponent(ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.entry.FunctionActivity"))
        .setData(Uri.parse("mobilemanager://function/entry/AutoStart"))
    )
  }
}