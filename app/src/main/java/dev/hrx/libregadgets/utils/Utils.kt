package dev.hrx.libregadgets.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.icu.text.DateFormat
import android.icu.text.SimpleDateFormat
import dev.hrx.libregadgets.gadgets.updateComplication
import dev.hrx.libregadgets.gadgets.updateWidget
import dev.hrx.libregadgets.storage.GlucoseMeasurement
import java.text.ParseException
import java.util.Date
import java.util.concurrent.TimeUnit

fun getClickIntent(context: Context): ComponentName {
    return canStartIntent(
        context,
        "com.freestylelibre.app.it",
        "com.librelink.app.ui.HomeActivity"
    )
        ?: canStartIntent(
            context,
            "org.nativescript.LibreLinkUp",
            "com.tns.NativeScriptActivity"
        )
        ?: ComponentName(context.packageName, "${context.packageName}.MainActivity")
}

fun canStartIntent(context: Context, pkg: String, cls: String): ComponentName? {
    val intent = Intent().setClassName(pkg, cls)
    val list: List<ResolveInfo> = context.packageManager.queryIntentActivities(
        intent,
        PackageManager.MATCH_DEFAULT_ONLY
    )

    return if (list.isNotEmpty()) ComponentName(pkg, cls) else null
}

fun isMeasurementStale(currentTimestamp: Long, measurement: GlucoseMeasurement): Boolean {
    val savedDate = measurement.timestamp
    val timeDiff = TimeUnit.MILLISECONDS.toMinutes(currentTimestamp - savedDate)

    return timeDiff >= 2 // considered stale if it's older than 2 minutes
}

fun getTimestamp(date: String): Long {
    val parsedDate = try {
        DateFormat.getInstanceForSkeleton("M/d/yyyy h:mm:ss a").parse(date)
    } catch(_: ParseException) {
        null
    }

    return parsedDate?.time ?: System.currentTimeMillis()
}