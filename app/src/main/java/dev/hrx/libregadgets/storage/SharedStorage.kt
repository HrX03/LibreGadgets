package dev.hrx.libregadgets.storage

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KProperty

private const val BASE_API_URL: String = "https://api-eu.libreview.io"
class SharedStorage (context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences("SHARED_CONTENT", Context.MODE_PRIVATE)

    var jwtToken by PreferenceDelegate("jwt_token", preferences)
    var apiUrl by PreferenceDelegate("api_url", preferences, defaultValue = BASE_API_URL)
    var latestMeasurement by createSerializablePreferenceDelegate<GlucoseMeasurement>("latest_measurement", preferences, synchronous = true)
    var glucoseThresholds by createSerializablePreferenceDelegate<GlucoseThresholds>("glucose_thresholds", preferences, synchronous = true)
    //var graphMeasurements by createSerializablePreferenceDelegate<GraphMeasurements>("graph_measurements", preferences, synchronous = true)
}

inline fun <reified T> createSerializablePreferenceDelegate(
    key: String,
    preferences: SharedPreferences,
    synchronous: Boolean = false,
): SerializablePreferenceDelegate<T> {
    return SerializablePreferenceDelegate(
        key,
        preferences,
        Json.serializersModule.serializer(),
        synchronous,
    )
}

class PreferenceDelegate(
    private val key: String,
    private val preferences: SharedPreferences,
    private val defaultValue: String = ""
) {
    operator fun getValue(self: Any?, property: KProperty<*>): String {
        return preferences.getString(key, defaultValue).toString()
    }

    operator fun setValue(self: Any?, property: KProperty<*>, value: String?) {
        val editor = preferences.edit()
        if(value == null) {
            editor.remove(key)
        } else {
            editor.putString(key, value)
        }
        editor.apply()
    }
}

class SerializablePreferenceDelegate<T>(
    private val key: String,
    private val preferences: SharedPreferences,
    private val serializer: KSerializer<T>,
    private val synchronous: Boolean,
) {
    operator fun getValue(self: Any?, property: KProperty<*>): T? {
        val contents = preferences.getString(key, "").toString()
        if(contents.isEmpty()) return null
        return Json.decodeFromString(serializer, contents)
    }

    @SuppressLint("ApplySharedPref")
    operator fun setValue(self: Any?, property: KProperty<*>, value: T?) {
        val editor = preferences.edit()
        if(value == null) {
            editor.remove(key)
        } else {
            editor.putString(key, Json.encodeToString(serializer, value))
        }

        if(synchronous) {
            editor.commit()
        } else {
            editor.apply()
        }
    }
}