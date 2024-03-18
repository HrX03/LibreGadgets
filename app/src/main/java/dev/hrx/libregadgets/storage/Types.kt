package dev.hrx.libregadgets.storage

import kotlinx.serialization.Serializable

@Serializable
data class GlucoseThresholds(
    val high: Int,
    val low: Int,
)

@Serializable
data class GlucoseMeasurement(
    val value: Int,
    val evaluation: MeasurementEvaluation,
    val trend: MeasurementTrend,
    val timestamp: Long,
)

@Serializable
data class GraphMeasurements(
    val list: List<GraphMeasurement>,
    val timestamp: Long,
)

@Serializable
data class GraphMeasurement(
    val value: Int,
    val evaluation: MeasurementEvaluation,
)

enum class MeasurementTrend {
    Unknown,
    FallQuick,
    Fall,
    Normal,
    Rise,
    RiseQuick,
}

enum class MeasurementEvaluation {
    Normal,
    High,
    Low
}