package dev.hrx.libregadgets.api.types

import kotlinx.serialization.Serializable

@Serializable
data class LoginBody(
    val email: String,
    val password: String,
)

@Serializable
data class LoginRedirectResponse(val status: Long, val data: LoginRedirectData)

@Serializable
data class LoginRedirectData(val redirect: Boolean, val region: String)

@Serializable
data class LoginResponse(val status: Long, val data: LoginData)

@Serializable
data class LoginData(
    val user: User,
    val messages: DataMessages,
    val notifications: Notifications,
    val authTicket: AuthTicket,
    val invitations: List<String>?,
)

@Serializable
data class AuthTicket(val token: String, val expires: Long, val duration: Long)

@Serializable
data class DataMessages(val unread: Long)

@Serializable
data class Notifications(val unresolved: Long)

@Serializable
data class User(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val country: String,
    val uiLanguage: String,
    val communicationLanguage: String,
    val accountType: String,
    val uom: String,
    val dateFormat: String,
    val timeFormat: String,
    val emailDay: List<Long>,
    val system: System,
    //val details: Details,
    val created: Long,
    val lastLogin: Long,
    //val programs: Details,
    val dateOfBirth: Long,
    val practices: Map<String, Practice>,
    val devices: Map<String, Device>,
    val consents: Consents,
)

@Serializable
data class System(val messages: SystemMessages)

@Serializable
data class SystemMessages(
    val appReviewBanner: Long,
    val firstUsePhoenix: Long,
    val firstUsePhoenixReportsDataMerged: Long,
    val lluGettingStartedBanner: Long,
    val lluNewFeatureModal: Long,
    val lvWebPostRelease: String,
    val streamingTourMandatory: Long,
)

@Serializable
data class Consents(val realWorldEvidence: RealWorldEvidence)

@Serializable
data class RealWorldEvidence(val policyAccept: Long, val touAccept: Long)

@Serializable
data class Device(
    val id: String,
    val nickname: String,
    val sn: String,
    val type: Long,
    val uploadDate: Long,
)

@Serializable
data class Practice(
    val id: String,
    val practiceId: String,
    val name: String,
    val address1: String,
    val city: String,
    val state: String,
    val zip: String,
    val phoneNumber: String,
)