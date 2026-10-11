package com.stillfeed.app.data.remote.supabase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Robust, lightweight HTTP client tailored for Supabase Auth and PostgREST endpoints
 * using standard Android networking and Coroutines.
 */
class SupabaseHttpClient(
    private val baseUrl: String = SupabaseConfig.PROJECT_URL,
    private val anonKey: String = SupabaseConfig.ANON_KEY,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Executes a POST request to an RPC endpoint (/rest/v1/rpc/{functionName}).
     */
    suspend fun executeRpc(
        functionName: String,
        params: JSONObject,
        accessToken: String? = null
    ): Result<String> = withContext(ioDispatcher) {
        val endpoint = "$baseUrl${SupabaseConfig.REST_PATH}/rpc/$functionName"
        sendRequest(
            endpoint = endpoint,
            method = "POST",
            body = params.toString(),
            accessToken = accessToken
        )
    }

    /**
     * Executes a POST request to insert records into a PostgREST table.
     */
    suspend fun executeInsert(
        tableName: String,
        jsonPayload: String,
        accessToken: String? = null
    ): Result<Unit> = withContext(ioDispatcher) {
        val endpoint = "$baseUrl${SupabaseConfig.REST_PATH}/$tableName"
        val result = sendRequest(
            endpoint = endpoint,
            method = "POST",
            body = jsonPayload,
            accessToken = accessToken,
            extraHeaders = mapOf("Prefer" to "return=minimal")
        )
        result.map { }
    }

    /**
     * Executes a GET query against a PostgREST table.
     */
    suspend fun executeSelect(
        tableName: String,
        queryParams: String = "select=*",
        accessToken: String? = null
    ): Result<String> = withContext(ioDispatcher) {
        val endpoint = "$baseUrl${SupabaseConfig.REST_PATH}/$tableName?$queryParams"
        sendRequest(
            endpoint = endpoint,
            method = "GET",
            body = null,
            accessToken = accessToken
        )
    }

    /**
     * Executes Supabase Auth user registration (/auth/v1/signup).
     */
    suspend fun authSignUp(
        email: String,
        password: CharSequence,
        displayName: String? = null
    ): Result<String> = withContext(ioDispatcher) {
        val endpoint = "$baseUrl${SupabaseConfig.AUTH_PATH}/signup"
        val payload = JSONObject().apply {
            put("email", email.trim().lowercase())
            put("password", password.toString())
            if (!displayName.isNullOrBlank()) {
                put("data", JSONObject().put("display_name", displayName.trim()))
            }
        }
        sendRequest(
            endpoint = endpoint,
            method = "POST",
            body = payload.toString(),
            accessToken = null
        )
    }

    /**
     * Executes Supabase Auth password sign-in (/auth/v1/token?grant_type=password).
     */
    suspend fun authSignIn(
        email: String,
        password: CharSequence
    ): Result<String> = withContext(ioDispatcher) {
        val endpoint = "$baseUrl${SupabaseConfig.AUTH_PATH}/token?grant_type=password"
        val payload = JSONObject().apply {
            put("email", email.trim().lowercase())
            put("password", password.toString())
        }
        sendRequest(
            endpoint = endpoint,
            method = "POST",
            body = payload.toString(),
            accessToken = null
        )
    }

    /**
     * Executes Supabase Auth sign-out (/auth/v1/logout).
     */
    suspend fun authSignOut(
        accessToken: String
    ): Result<Unit> = withContext(ioDispatcher) {
        val endpoint = "$baseUrl${SupabaseConfig.AUTH_PATH}/logout"
        val result = sendRequest(
            endpoint = endpoint,
            method = "POST",
            body = null,
            accessToken = accessToken
        )
        result.map { }
    }

    private fun sendRequest(
        endpoint: String,
        method: String,
        body: String?,
        accessToken: String?,
        extraHeaders: Map<String, String> = emptyMap()
    ): Result<String> {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(endpoint)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 15000
                readTimeout = 15000
                doInput = true

                // Supabase API authentication headers
                setRequestProperty("apikey", anonKey)
                val bearerToken = if (!accessToken.isNullOrBlank()) accessToken else anonKey
                setRequestProperty("Authorization", "Bearer $bearerToken")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")

                extraHeaders.forEach { (key, value) ->
                    setRequestProperty(key, value)
                }

                if (body != null) {
                    doOutput = true
                    outputStream.use { os ->
                        os.write(body.toByteArray(StandardCharsets.UTF_8))
                    }
                }
            }

            val responseCode = connection.responseCode
            val isSuccess = responseCode in 200..299

            val inputStream = if (isSuccess) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            val responseText = inputStream?.use { stream ->
                BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } ?: ""

            if (isSuccess) {
                Result.success(responseText)
            } else {
                val errorMessage = if (responseText.isNotBlank()) {
                    try {
                        val errorJson = JSONObject(responseText)
                        errorJson.optString("message", errorJson.optString("msg", responseText))
                    } catch (_: Exception) {
                        responseText
                    }
                } else {
                    "HTTP error $responseCode"
                }
                Result.failure(SupabaseApiException(responseCode, errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }
}

/**
 * Typed exception for Supabase API errors.
 */
class SupabaseApiException(
    val statusCode: Int,
    override val message: String
) : Exception("Supabase API Error ($statusCode): $message")
