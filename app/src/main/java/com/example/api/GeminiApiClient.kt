package com.example.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class GeminiApiClient {

    companion object {
        const val DEFAULT_MODEL = "gemini-3.5-flash"
        val SUPPORTED_MODELS = listOf(
            "gemini-3.5-flash",
            "gemini-3.1-pro-preview",
            "gemini-3.1-flash-lite-preview"
        )
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun testConnection(apiKey: String, model: String = DEFAULT_MODEL): ApiValidationResult =
        withContext(Dispatchers.IO) {
            val trimmedKey = apiKey.trim()
            if (trimmedKey.isEmpty()) {
                return@withContext ApiValidationResult.InvalidKey("API key cannot be empty.")
            }

            val testPayload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", "ping") })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("maxOutputTokens", 5)
                    put("temperature", 0.0)
                })
            }

            val url = "$BASE_URL/$model:generateContent?key=$trimmedKey"
            val body = testPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    val code = response.code

                    if (response.isSuccessful) {
                        return@withContext ApiValidationResult.Success(
                            message = "Connection verified successfully with $model",
                            model = model
                        )
                    }

                    // Parse error response
                    val errorMsg = try {
                        val json = JSONObject(responseBody)
                        val err = json.optJSONObject("error")
                        err?.optString("message") ?: "HTTP $code: ${response.message}"
                    } catch (e: Exception) {
                        "HTTP $code: ${response.message}"
                    }

                    when {
                        code == 400 || errorMsg.contains("API_KEY_INVALID", ignoreCase = true) ||
                                errorMsg.contains("key not valid", ignoreCase = true) ||
                                errorMsg.contains("invalid api key", ignoreCase = true) -> {
                            ApiValidationResult.InvalidKey("Invalid API Key. Google rejected the credential: $errorMsg")
                        }
                        code == 429 || errorMsg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                                errorMsg.contains("quota", ignoreCase = true) -> {
                            ApiValidationResult.QuotaExceeded("Quota exceeded for this API key: $errorMsg")
                        }
                        code in 500..599 -> {
                            ApiValidationResult.ServerError(code, "Google Gemini server error (HTTP $code): $errorMsg")
                        }
                        else -> {
                            ApiValidationResult.UnknownError("Validation failed (HTTP $code): $errorMsg")
                        }
                    }
                }
            } catch (e: UnknownHostException) {
                ApiValidationResult.NetworkError("Network unreachable. Please check your internet connection.")
            } catch (e: ConnectException) {
                ApiValidationResult.NetworkError("Failed to connect to Google servers: ${e.message}")
            } catch (e: SocketTimeoutException) {
                ApiValidationResult.Timeout("Request timed out connecting to Gemini API.")
            } catch (e: Exception) {
                ApiValidationResult.UnknownError("Unexpected error during validation: ${e.localizedMessage ?: e.javaClass.simpleName}")
            }
        }

    suspend fun generateContent(
        apiKey: String,
        history: List<ChatMessage>,
        newPrompt: String,
        systemInstruction: String,
        model: String = DEFAULT_MODEL
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("API key is not configured."))
        }

        try {
            val payload = buildRequestJson(history, newPrompt, systemInstruction)
            val url = "$BASE_URL/$model:generateContent?key=$trimmedKey"
            val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errMsg = try {
                        val json = JSONObject(bodyStr)
                        json.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                    } catch (e: Exception) {
                        "HTTP ${response.code}: ${response.message}"
                    }
                    return@withContext Result.failure(Exception("Gemini error ($errMsg)"))
                }

                val json = JSONObject(bodyStr)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        return@withContext Result.success(text)
                    }
                }
                Result.failure(Exception("Empty response received from Gemini."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun streamGenerateContent(
        apiKey: String,
        history: List<ChatMessage>,
        newPrompt: String,
        systemInstruction: String,
        model: String = DEFAULT_MODEL
    ): Flow<String> = flow {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isEmpty()) {
            throw IllegalStateException("API key is not configured.")
        }

        val payload = buildRequestJson(history, newPrompt, systemInstruction)
        val url = "$BASE_URL/$model:streamGenerateContent?alt=sse&key=$trimmedKey"
        val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                val errMsg = try {
                    JSONObject(errBody).optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                } catch (e: Exception) {
                    "HTTP ${response.code}: ${response.message}"
                }
                throw Exception("Stream failed ($errMsg)")
            }

            val inputStream = response.body?.byteStream() ?: throw Exception("Empty stream body")
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val raw = line?.trim() ?: continue
                if (!raw.startsWith("data:")) continue
                val jsonStr = raw.removePrefix("data:").trim()
                if (jsonStr.isEmpty() || jsonStr == "[DONE]") continue

                try {
                    val chunkJson = JSONObject(jsonStr)
                    val candidates = chunkJson.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCand = candidates.getJSONObject(0)
                        val content = firstCand.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val chunkText = parts.getJSONObject(0).optString("text")
                            if (chunkText.isNotEmpty()) {
                                emit(chunkText)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore transient malformed SSE chunks
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun buildRequestJson(
        history: List<ChatMessage>,
        newPrompt: String,
        systemInstruction: String
    ): JSONObject {
        val contentsArray = JSONArray()

        // Include recent history (up to last 10 messages for context)
        val recentHistory = history.takeLast(10)
        for (msg in recentHistory) {
            val role = if (msg.isUser) "user" else "model"
            contentsArray.put(JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", msg.text) })
                })
            })
        }

        // Add the current prompt
        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", newPrompt) })
            })
        })

        return JSONObject().apply {
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("topP", 0.95)
                put("maxOutputTokens", 1024)
            })
            if (systemInstruction.isNotEmpty()) {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    })
                })
            }
        }
    }
}
