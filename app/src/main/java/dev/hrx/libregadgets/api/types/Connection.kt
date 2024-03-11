package dev.hrx.libregadgets.api.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConnectionResponse(
    val status: Int,
    val data: List<Connection>,
    val ticket: AuthTicket,
)

@Serializable
data class Connection(
    val id: String,
    val patientId: String,
    val country: String,
    val status: Int,
    val firstName: String,
    val lastName: String,
    val targetLow: Int,
    val targetHigh: Int,
    val uom: Int,
    val sensor: Sensor,
    val alarmRules: AlarmRules,
    val glucoseMeasurement: GlucoseMeasurement,
    val glucoseItem: GlucoseMeasurement,
    val patientDevice: PatientDevice,
    val created: Long,
)

@Serializable
data class Sensor(
    val deviceId: String,
    val sn: String,
    val a: Long,
    val w: Int,
    val pt: Int,
    val s: Boolean,
    val lj: Boolean,
)

@Serializable
data class AlarmRules(
    val c: Boolean,
    val h: H,
    val f: F,
    val l: F,
    val nd: ND,
    val p: Int,
    val r: Int,
    //val std: Std,
)

@Serializable
data class GlucoseMeasurement(
    @SerialName("FactoryTimestamp")
    val factoryTimestamp: String,
    @SerialName("Timestamp")
    val timestamp: String,
    val type: Int,
    @SerialName("ValueInMgPerDl")
    val valueInMgPerDl: Int,
    @SerialName("TrendArrow")
    val trendArrow: Int,
    @SerialName("TrendMessage")
    val trendMessage: String?,
    @SerialName("MeasurementColor")
    val measurementColor: Int,
    @SerialName("GlucoseUnits")
    val glucoseUnits: Int,
    @SerialName("Value")
    val value: Int,
    val isHigh: Boolean,
    val isLow: Boolean,
)

@Serializable
data class PatientDevice(
    val did: String,
    val dtid: Int,
    val v: String,
    val l: Boolean,
    val ll: Int,
    val h: Boolean,
    val hl: Int,
    val u: Long,
    val fixedLowAlarmValues: FixedLowAlarmValues,
    val alarms: Boolean,
    val fixedLowThreshold: Int,
)

@Serializable
data class FixedLowAlarmValues(
    val mgdl: Int,
    val mmoll: Double,
)

@Serializable
data class H(
    val on: Boolean,
    val th: Int,
    val thmm: Double,
    val d: Int,
    val f: Double,
)

@Serializable
data class F(
    val on: Boolean?,
    val th: Int,
    val thmm: Double,
    val d: Int,
    val tl: Int,
    val tlmm: Double,
)

@Serializable
data class ND(
    val i: Int,
    val r: Int,
    val l: Int,
)