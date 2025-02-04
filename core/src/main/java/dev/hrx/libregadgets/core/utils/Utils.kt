package dev.hrx.libregadgets.core.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.icu.text.DateFormat
import com.google.protobuf.Timestamp
import dev.hrx.libregadgets.core.storage.GlucoseMeasurement
import java.text.ParseException
import java.util.concurrent.TimeUnit

// Measured from real data, tends to be one minute but drifts after a while and adds a second after x events
const val defaultEventDelay: Long = 62000

fun isRunningOnWatch(context: Context): Boolean {
    return context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH);
}

fun getClickIntent(context: Context, forceAppActivity: Boolean = false): ComponentName {
    if (forceAppActivity) return ComponentName(
        context.packageName,
        "${context.packageName}.MainActivity"
    )

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
    } catch (_: ParseException) {
        null
    }

    return parsedDate?.time ?: System.currentTimeMillis()
}

fun formatGlucoseValue(value: Int?): String {
    return when {
        value == null -> "--"
        value < 40 -> "LO"
        value > 400 -> "HI"
        else -> value.toString()
    }
}

fun getDelayForNextEvent(currentTimestamp: Long, latestMeasurementTimestamp: Long): Long {
    val idealNextTime = (latestMeasurementTimestamp.floorDiv(1000) * 1000) + defaultEventDelay
    return idealNextTime - currentTimestamp
}

fun Long.toProtoTimestamp(): Timestamp {
    return Timestamp.newBuilder()
        .setSeconds(this / 1000)
        .setNanos((this % 1000).toInt() * 1000000)
        .build()
}