package dev.hrx.libregadgets.api

import android.content.Context
import android.util.Log
import dev.hrx.libregadgets.api.types.ConnectionResponse
import dev.hrx.libregadgets.api.types.CountryResponse
import dev.hrx.libregadgets.api.types.LoginBody
import dev.hrx.libregadgets.api.types.LoginResponse
import dev.hrx.libregadgets.storage.SharedStorage
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
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
import java.lang.IllegalArgumentException

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
        private const val COUNTRIES_ENDPOINT: String = "llu/config/country?country="
    }

    private val storage: SharedStorage = SharedStorage(context)
    private val client: OkHttpClient = OkHttpClient.Builder()
        .addNetworkInterceptor(TokenInterceptor(context))
        .build()
    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    suspend fun login(email: String, password: String): LoginResponse? {
        val jsonBody = json.encodeToString(LoginBody(email, password))
        val body = jsonBody.toRequestBody(JSON)
        val req = Request.Builder()
            .url("${storage.apiUrl}/$LOGIN_ENDPOINT")
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

            val decodedResponse: LoginResponse? = tryDecode<LoginResponse.LoginErrorResponse>(responseBody)
                ?: tryDecode<LoginResponse.LoginRedirectResponse>(responseBody)
                ?: tryDecode<LoginResponse.LoginSuccessResponse>(responseBody)

            if(decodedResponse is LoginResponse.LoginRedirectResponse) {
                val countryInfo = getCountryInfo(decodedResponse.data.region) ?: return null
                storage.apiUrl = countryInfo.data.lslApi
                return login(email, password)
            }

            return decodedResponse
        } catch (e: IOException) {
            Log.e("LibreLinkApi", "Net call failed: $e")
            return null
        }
    }

    suspend fun getConnection(): ConnectionResponse? {
        val req = Request.Builder()
            .url("${storage.apiUrl}/${CONNECTIONS_ENDPOINT}")
            .headers(headers.toHeaders())
            .get()
            .build()

        try {
            val response = client.newCall(req).await()
            val responseBody = response.body?.string()

            if (responseBody == null) {
                Log.e("LibreLinkApi", "Login call returned no body")
                return null
            }

            Log.d("LibreLinkApi", responseBody)

            return json.decodeFromString<ConnectionResponse>(responseBody)
        } catch (e: IOException) {
            Log.e("LibreLinkApi", "Net call failed: $e")
            return null
        }
    }

    private suspend fun getCountryInfo(country: String): CountryResponse? {
        val req = Request.Builder()
            .url("${storage.apiUrl}/${COUNTRIES_ENDPOINT}$country")
            .headers(headers.toHeaders())
            .get()
            .build()

        try {
            val response = client.newCall(req).await()
            val responseBody = response.body?.string()

            if (responseBody != null) {
                Log.d("LibreLinkApi", responseBody)
                return json.decodeFromString<CountryResponse>(responseBody)
            }

            Log.e("LibreLinkApi", "Login call returned no body")
        } catch (e: IOException) {
            Log.e("LibreLinkApi", "Net call failed: $e")
        }

        return null
    }

    private inline fun <reified T>tryDecode(source: String): T? {
        return try {
            json.decodeFromString<T>(source)
        } catch(_: IllegalArgumentException) {
            null
        }
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