package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.api.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class ChatRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sojib_chat_history", Context.MODE_PRIVATE)

    private val _messages = MutableStateFlow<List<ChatMessage>>(loadMessages())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private var isMemoryEnabled: Boolean = true

    fun setMemoryEnabled(enabled: Boolean) {
        isMemoryEnabled = enabled
        if (!enabled) {
            prefs.edit().clear().apply()
        } else {
            saveMessages(_messages.value)
        }
    }

    private fun loadMessages(): List<ChatMessage> {
        val rawJson = prefs.getString("messages_json", null) ?: return emptyList()
        return try {
            val array = JSONArray(rawJson)
            val list = mutableListOf<ChatMessage>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ChatMessage(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        text = obj.optString("text", ""),
                        isUser = obj.optBoolean("isUser", false),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        actionSummary = if (obj.has("actionSummary")) obj.getString("actionSummary") else null,
                        isError = obj.optBoolean("isError", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun addMessage(message: ChatMessage) {
        val updated = _messages.value.toMutableList().apply { add(message) }
        _messages.value = updated
        if (isMemoryEnabled) {
            saveMessages(updated)
        }
    }

    @Synchronized
    fun updateLastMessage(newText: String, isError: Boolean = false, actionSummary: String? = null) {
        val current = _messages.value
        if (current.isEmpty()) return
        val last = current.last()
        val updatedLast = last.copy(
            text = newText,
            isError = isError,
            actionSummary = actionSummary ?: last.actionSummary
        )
        val updated = current.toMutableList().apply {
            set(size - 1, updatedLast)
        }
        _messages.value = updated
        if (isMemoryEnabled) {
            saveMessages(updated)
        }
    }

    @Synchronized
    fun clearHistory() {
        _messages.value = emptyList()
        prefs.edit().clear().apply()
    }

    private fun saveMessages(list: List<ChatMessage>) {
        try {
            val array = JSONArray()
            // Keep up to last 100 messages to maintain speed
            val capped = list.takeLast(100)
            for (msg in capped) {
                val obj = JSONObject().apply {
                    put("id", msg.id)
                    put("text", msg.text)
                    put("isUser", msg.isUser)
                    put("timestamp", msg.timestamp)
                    msg.actionSummary?.let { put("actionSummary", it) }
                    put("isError", msg.isError)
                }
                array.put(obj)
            }
            prefs.edit().putString("messages_json", array.toString()).apply()
        } catch (e: Exception) {
            // Ignore write errors
        }
    }
}
