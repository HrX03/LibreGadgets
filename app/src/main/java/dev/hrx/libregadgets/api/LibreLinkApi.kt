package dev.hrx.libregadgets.api

import android.content.Context
import android.util.Log
import dev.hrx.libregadgets.api.types.ConnectionResponse
import dev.hrx.libregadgets.api.types.LoginBody
import dev.hrx.libregadgets.api.types.LoginSuccessResponse
import dev.hrx.libregadgets.storage.SharedStorage
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import okhttp3.Headers.Companion.toHeaders
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import ru.gildor.coroutines.okhttp.await
import java.io.IOException

val JSON: MediaType = "application/json".toMediaType()

class LibreLinkApi(context: Context) {
    companion object {
        private val headers = mapOf(
            "cache-control" to "no-cache",
            "connection" to "Keep-Alive",
            "content-type" to "application/json",
            "product" to "llu.android",
            "version" to "4.9.0",
        )

        private const val LOGIN_ENDPOINT: String = "llu/auth/login"
        private const val CONNECTIONS_ENDPOINT: String = "llu/connections"
        private const val COUNTRIES_ENDPOINT: String = "llu/config/country?country=DE"
        private const val BASE_API_URL: String = "https://api-eu.libreview.io"
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addNetworkInterceptor(TokenInterceptor(context))
        .build()
    @OptIn(ExperimentalSerializationApi::class)
    private val encoder = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private var reqURL: String = BASE_API_URL

    suspend fun login(email: String, password: String): LoginSuccessResponse? {
        val jsonBody = encoder.encodeToString(LoginBody(email, password))
        val body = jsonBody.toRequestBody(JSON)
        val req = Request.Builder()
            .url("$reqURL/$LOGIN_ENDPOINT")
            .headers(headers.toHeaders())
            .post(body)
            .build()

        try {
            val response = client.newCall(req).await()
            val responseBody = response.body?.string()

            if (responseBody == null) {
                Log.e("LibreLinkApi", "Login call returned no body")
                return null
            }

            Log.d("LibreLinkApi", responseBody)
            return encoder.decodeFromString<LoginSuccessResponse>(responseBody)
        } catch (e: IOException) {
            Log.e("LibreLinkApi", "Net call failed: $e")
            return null
        }
    }

    suspend fun getConnection(): ConnectionResponse? {
        val req = Request.Builder()
            .url("$reqURL/${CONNECTIONS_ENDPOINT}")
            .headers(headers.toHeaders())
            .get()
            .build()

        try {
            val response = client.newCall(req).await()
            val responseBody = response.body?.string()

            if (responseBody != null) {
                Log.d("LibreLinkApi", responseBody)
                return encoder.decodeFromString<ConnectionResponse>(responseBody)
            }

            Log.e("LibreLinkApi", "Login call returned no body")
        } catch (e: IOException) {
            Log.e("LibreLinkApi", "Net call failed: $e")
        }

        return null
    }
}

private class TokenInterceptor(context: Context) : Interceptor {
    private val storage = SharedStorage(context)

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        if(storage.jwtToken.isNotEmpty()) {
            request = request
                .newBuilder()
                .header("authorization", "Bearer ${storage.jwtToken}")
                .build()
        }

        return chain.proceed(request)
    }

}