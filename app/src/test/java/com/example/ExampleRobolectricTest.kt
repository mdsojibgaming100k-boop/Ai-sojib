package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.api.ChatMessage
import com.example.bridge.ActionBridge
import com.example.bridge.ActionType
import com.example.data.ChatRepository
import com.example.data.SettingsRepository
import com.example.security.SecureKeyStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var keyStorage: SecureKeyStorage
    private lateinit var actionBridge: ActionBridge
    private lateinit var chatRepository: ChatRepository
    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        keyStorage = SecureKeyStorage(context)
        actionBridge = ActionBridge()
        chatRepository = ChatRepository(context)
        settingsRepository = SettingsRepository(context)
    }

    @Test
    fun testAppNameResource() {
        val appName = context.getString(R.string.app_name)
        assertEquals("SOJIB AI", appName)
    }

    @Test
    fun testSecureKeyStoragePersistenceAndClear() {
        // Initially cleared
        keyStorage.clearApiKey()
        assertFalse(keyStorage.isValidationPassed())

        // Save key
        val testKey = "AIzaSyTestMockKeyForVerification123"
        val saved = keyStorage.saveApiKey(testKey)
        assertTrue(saved)

        // Retrieved key matches
        val retrieved = keyStorage.getApiKey()
        assertEquals(testKey, retrieved)

        // Validation starts false until tested
        assertFalse(keyStorage.isValidationPassed())
        keyStorage.setValidationPassed(true)
        assertTrue(keyStorage.isValidationPassed())

        // Clearing key invalidates access
        keyStorage.clearApiKey()
        assertNull(keyStorage.getApiKey())
        assertFalse(keyStorage.isValidationPassed())
    }

    @Test
    fun testActionBridgeCommandParsing() {
        val installed = emptyList<com.example.bridge.AppInfo>()

        // 1. YouTube Search
        val ytAction = actionBridge.parseIntentOrCommand("search youtube Kotlin Jetpack Compose", installed)
        assertNotNull(ytAction)
        assertEquals(ActionType.SEARCH_YOUTUBE, ytAction?.type)
        assertEquals("Kotlin Jetpack Compose", ytAction?.parameters?.get("query"))

        // 2. Google Search
        val googleAction = actionBridge.parseIntentOrCommand("search google artificial intelligence", installed)
        assertNotNull(googleAction)
        assertEquals(ActionType.SEARCH_GOOGLE, googleAction?.type)
        assertEquals("artificial intelligence", googleAction?.parameters?.get("query"))

        // 3. Current Time
        val timeAction = actionBridge.parseIntentOrCommand("what's the time", installed)
        assertNotNull(timeAction)
        assertEquals(ActionType.READ_DATE_TIME, timeAction?.type)

        // 4. Torch Control
        val torchAction = actionBridge.parseIntentOrCommand("turn on flashlight", installed)
        assertNotNull(torchAction)
        assertEquals(ActionType.TORCH_CONTROL, torchAction?.type)
        assertEquals("on", torchAction?.parameters?.get("state"))

        // 5. Sensitive Phone Call requires confirmation
        val dialAction = actionBridge.parseIntentOrCommand("call 9876543210", installed)
        assertNotNull(dialAction)
        assertEquals(ActionType.DIAL_PHONE, dialAction?.type)
        assertTrue(dialAction?.requiresConfirmation == true)
    }

    @Test
    fun testChatRepositoryPersistence() {
        chatRepository.clearHistory()
        assertEquals(0, chatRepository.messages.value.size)

        val msg1 = ChatMessage(text = "Hello Sojib", isUser = true)
        chatRepository.addMessage(msg1)
        assertEquals(1, chatRepository.messages.value.size)
        assertEquals("Hello Sojib", chatRepository.messages.value[0].text)

        val msg2 = ChatMessage(text = "Thinking...", isUser = false)
        chatRepository.addMessage(msg2)
        assertEquals(2, chatRepository.messages.value.size)

        chatRepository.updateLastMessage("Hello! I am ready to help.")
        assertEquals("Hello! I am ready to help.", chatRepository.messages.value[1].text)
    }

    @Test
    fun testSettingsRepositoryPersistence() {
        val current = settingsRepository.settings.value
        val updated = current.copy(
            wakePhrase = "Hey Sojib Assistant",
            speechRate = 1.2f,
            ttsEnabled = false
        )
        settingsRepository.updateSettings(updated)

        val reloaded = settingsRepository.settings.value
        assertEquals("Hey Sojib Assistant", reloaded.wakePhrase)
        assertEquals(1.2f, reloaded.speechRate, 0.01f)
        assertFalse(reloaded.ttsEnabled)
    }
}
