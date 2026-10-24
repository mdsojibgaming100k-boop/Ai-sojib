package com.example.api

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val actionSummary: String? = null,
    val isError: Boolean = false
)

sealed class ApiValidationResult {
    data class Success(val message: String, val model: String) : ApiValidationResult()
    data class InvalidKey(val details: String) : ApiValidationResult()
    data class QuotaExceeded(val details: String) : ApiValidationResult()
    data class NetworkError(val details: String) : ApiValidationResult()
    data class Timeout(val details: String) : ApiValidationResult()
    data class ServerError(val code: Int, val details: String) : ApiValidationResult()
    data class UnknownError(val details: String) : ApiValidationResult()
}
