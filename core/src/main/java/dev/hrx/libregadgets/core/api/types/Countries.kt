package dev.hrx.libregadgets.core.api.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CountryResponse(val status: Int, val data: CountryResponseData)

@Serializable
data class CountryResponseData(
    @SerialName("CaptureAnalytics")
    val captureAnalytics: String,
    @SerialName("CountryList")
    val countryList: CountryList,
    @SerialName("LSL-ServiceURL")
    val lslServiceUrl: String,
    @SerialName("LibreLinkResourceKey")
    val libreLinkResourceKey: String,
    @SerialName("PartnerApplicationKeys")
    val partnerApplicationKeys: List<String>,
    @SerialName("ShowAlert")
    val showAlert: Boolean,
    @SerialName("ShowAndroidBadges")
    val showAndroidBadges: String,
    @SerialName("SupportedLanguages")
    val supportedLanguages: List<String>,
    val alarmVersions: AlarmVersions,
    val alarmsEnabled: List<String>,
    val features: Features,
    val heartbeatAlarmVersions: String?,
    val heartbeatMilliseconds: Int,
    val lluPrivacyPolicyHtml: Map<String, String>,
    val lluPrivacyPolicyVersions: Map<String, String>,
    val lluSAM: String,
    val lluSupport: String,
    val lluSupportMain: String,
    val lluToUHtml: Map<String, String>,
    val lluToUVersions: Map<String, String>,
    val lslApi: String,
    val lv: String,
    val minorityAge: Int,
    val nameOrder: List<String>,
    val notificationService: String,
    val notificationTopics: List<String>,
    val passwordRules: PasswordRules,
    val pnDocId: String,
    val pushyApiEndpoint: String,
    val pushyMqttEndpoint: String,
    val regionMap: Map<String, Region> = mapOf(),
    val reviewConfig: ReviewConfig,
    val safetyBannerInterval: Int,
    val touDocId: String,
)

@Serializable
data class CountryList(
    val countries: List<CountryMember>,
)

@Serializable
data class CountryMember(
    @SerialName("DisplayMember")
    val displayMember: String,
    @SerialName("ValueMember")
    val valueMember: String,
)

@Serializable
data class AlarmVersions(
    val isfHigh: String,
    val isfHighDismissed: String,
    val isfLow: String,
    val isfLowDismissed: String,
    val isfUrgentLow: String,
    val isfUrgentLowDismissed: String,
    val lateJoined: String,
    val noData: String,
    val scan: String,
    val sensorExpired: String,
    val sensorStart: String,
    val sensorTerminated: String,
    val sharingResumed: String,
    val sharingStopped: String,
    val streamingHigh: String,
    val streamingLow: String,
    val streamingSensorStart: String,
    val streamingUrgentLow: String,
)

@Serializable
data class Features(
    val appSettings: Boolean,
    val connectionIsStreamingBanner: Boolean,
    val fsl2Streaming: Boolean,
    val heartbeat: Boolean,
    val iOSExitNotification: Boolean,
    val inAppReview: Boolean,
    val regulatoryUdiDom: Boolean,
    val repeatableAppReview: Boolean,
    val sensorEndAlarms: Boolean,
    val streamingAlarms: Boolean,
    val streamingSensorStartedAlarm: Boolean,
    val streamingTutorial: Boolean,
    val streamingUnavailableBanner: Boolean,
)

@Serializable
data class PasswordRules(
    @SerialName("PasswordRequirements")
    val passwordRequirements: List<Int>,
    val rules: PasswordRulesList,
)

@Serializable
data class PasswordRulesList(
    val max: PasswordRule,
    val min: PasswordRule,
    val noSpace: PasswordRule,
    val number: PasswordRule,
    val special: PasswordRule,
)

@Serializable
data class PasswordRule(val errorMsgKey: String, val value: String)

@Serializable
data class Region(val lslApi: String, val socketHub: String)

@Serializable
data class ReviewConfig(
    val daysBeforeReminding: Int,
    val daysUntilPrompt: Int,
    val significantUsesUntilPrompt: Int,
    val usesUntilPrompt: Int,
)