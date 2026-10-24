package com.example.bridge

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppInfo(
    val name: String,
    val packageName: String,
    val isSystem: Boolean = false
)

enum class ActionType {
    OPEN_APP,
    SEARCH_GOOGLE,
    SEARCH_YOUTUBE,
    OPEN_URL,
    READ_DATE_TIME,
    SET_ALARM,
    SET_TIMER,
    VOLUME_CONTROL,
    TORCH_CONTROL,
    MAPS_SEARCH,
    DIAL_PHONE,
    WHATSAPP_MESSAGE
}

data class ActionRequest(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: ActionType,
    val parameters: Map<String, String> = emptyMap(),
    val requiresConfirmation: Boolean = false,
    val confirmationTitle: String? = null,
    val confirmationPrompt: String? = null
)

data class ActionResult(
    val success: Boolean,
    val message: String,
    val error: String? = null
)

class ActionBridge {

    companion object {
        private var isTorchOn = false

        // Common known app names and package aliases
        val KNOWN_PACKAGES = mapOf(
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "tiktok" to "com.zhiliaoapp.musically",
            "whatsapp" to "com.whatsapp",
            "telegram" to "org.telegram.messenger",
            "maps" to "com.google.android.apps.maps",
            "google maps" to "com.google.android.apps.maps",
            "camera" to "com.google.android.GoogleCamera",
            "gallery" to "com.google.android.apps.photos",
            "settings" to "com.android.settings",
            "clock" to "com.google.android.deskclock"
        )
    }

    fun discoverLaunchableApps(context: Context): List<AppInfo> {
        return try {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            resolveInfos.mapNotNull { resolveInfo ->
                val appName = resolveInfo.loadLabel(pm).toString()
                val pkgName = resolveInfo.activityInfo.packageName
                if (pkgName.isNotEmpty()) {
                    AppInfo(
                        name = appName,
                        packageName = pkgName,
                        isSystem = (resolveInfo.activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                    )
                } else null
            }.distinctBy { it.packageName }.sortedBy { it.name.lowercase() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun findApp(nameQuery: String, apps: List<AppInfo>): AppInfo? {
        val cleanQuery = nameQuery.trim().lowercase()

        // 1. Direct package match or alias
        val directPkg = KNOWN_PACKAGES[cleanQuery]
        if (directPkg != null) {
            val found = apps.find { it.packageName.equals(directPkg, ignoreCase = true) }
            if (found != null) return found
        }

        // 2. Exact name match
        val exact = apps.find { it.name.equals(cleanQuery, ignoreCase = true) }
        if (exact != null) return exact

        // 3. Name contains query
        val contains = apps.find { it.name.lowercase().contains(cleanQuery) }
        if (contains != null) return contains

        // 4. Package contains query
        return apps.find { it.packageName.lowercase().contains(cleanQuery) }
    }

    fun executeAction(context: Context, request: ActionRequest): ActionResult {
        return try {
            when (request.type) {
                ActionType.OPEN_APP -> handleOpenApp(context, request)
                ActionType.SEARCH_GOOGLE -> handleGoogleSearch(context, request)
                ActionType.SEARCH_YOUTUBE -> handleYouTubeSearch(context, request)
                ActionType.OPEN_URL -> handleOpenUrl(context, request)
                ActionType.READ_DATE_TIME -> handleReadDateTime()
                ActionType.SET_ALARM -> handleSetAlarm(context, request)
                ActionType.SET_TIMER -> handleSetTimer(context, request)
                ActionType.VOLUME_CONTROL -> handleVolumeControl(context, request)
                ActionType.TORCH_CONTROL -> handleTorchControl(context, request)
                ActionType.MAPS_SEARCH -> handleMapsSearch(context, request)
                ActionType.DIAL_PHONE -> handleDialPhone(context, request)
                ActionType.WHATSAPP_MESSAGE -> handleWhatsAppMessage(context, request)
            }
        } catch (e: Exception) {
            ActionResult(
                success = false,
                message = "Action execution failed: ${e.localizedMessage ?: "Unknown error"}",
                error = e.localizedMessage
            )
        }
    }

    private fun handleOpenApp(context: Context, request: ActionRequest): ActionResult {
        val appName = request.parameters["app"] ?: request.parameters["name"] ?: ""
        val targetPackage = request.parameters["package"]

        val pm = context.packageManager
        var launchIntent: Intent? = null

        if (!targetPackage.isNullOrEmpty()) {
            launchIntent = pm.getLaunchIntentForPackage(targetPackage)
        }

        if (launchIntent == null && appName.isNotEmpty()) {
            val apps = discoverLaunchableApps(context)
            val app = findApp(appName, apps)
            if (app != null) {
                launchIntent = pm.getLaunchIntentForPackage(app.packageName)
            }
        }

        // Special system fallbacks for Settings, Camera, Gallery
        if (launchIntent == null) {
            val lower = appName.lowercase()
            when {
                lower.contains("settings") -> launchIntent = Intent(Settings.ACTION_SETTINGS)
                lower.contains("camera") -> launchIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                lower.contains("gallery") || lower.contains("photos") -> {
                    launchIntent = Intent(Intent.ACTION_VIEW).apply {
                        type = "image/*"
                    }
                }
            }
        }

        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            ActionResult(true, "Opening $appName...")
        } else {
            ActionResult(
                false,
                "App '$appName' is not installed or cannot be launched.",
                "APP_NOT_INSTALLED"
            )
        }
    }

    private fun handleGoogleSearch(context: Context, request: ActionRequest): ActionResult {
        val query = request.parameters["query"] ?: ""
        if (query.isBlank()) {
            return ActionResult(false, "Search query is empty.")
        }

        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            ActionResult(true, "Searching Google for '$query'")
        } catch (e: Exception) {
            // Fallback to browser URL
            val url = "https://www.google.com/search?q=${Uri.encode(query)}"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
            ActionResult(true, "Searching Google for '$query'")
        }
    }

    private fun handleYouTubeSearch(context: Context, request: ActionRequest): ActionResult {
        val query = request.parameters["query"] ?: ""
        val uri = if (query.isNotBlank()) {
            Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
        } else {
            Uri.parse("https://www.youtube.com")
        }

        val appIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.youtube")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(appIntent)
            ActionResult(true, "Opening YouTube for '$query'")
        } catch (e: Exception) {
            // Fallback to web browser
            val webIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            ActionResult(true, "Searching YouTube for '$query'")
        }
    }

    private fun handleOpenUrl(context: Context, request: ActionRequest): ActionResult {
        var url = request.parameters["url"] ?: ""
        if (url.isBlank()) return ActionResult(false, "URL is empty.")
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ActionResult(true, "Opening $url")
    }

    private fun handleReadDateTime(): ActionResult {
        val now = Date()
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm a (z)", Locale.getDefault())
        val text = "Current time is ${timeFormat.format(now)} on ${dateFormat.format(now)}."
        return ActionResult(true, text)
    }

    private fun handleSetAlarm(context: Context, request: ActionRequest): ActionResult {
        val hour = request.parameters["hour"]?.toIntOrNull() ?: 7
        val minutes = request.parameters["minutes"]?.toIntOrNull() ?: 0
        val message = request.parameters["message"] ?: "SOJIB AI Alarm"

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minutes)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            ActionResult(true, "Alarm set for $hour:${String.format(Locale.US, "%02d", minutes)}: $message")
        } catch (e: Exception) {
            ActionResult(false, "Clock application not available to set alarm.", e.message)
        }
    }

    private fun handleSetTimer(context: Context, request: ActionRequest): ActionResult {
        val seconds = request.parameters["seconds"]?.toIntOrNull()
            ?: (request.parameters["minutes"]?.toIntOrNull()?.times(60) ?: 60)
        val message = request.parameters["message"] ?: "SOJIB AI Timer"

        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            ActionResult(true, "Timer set for $seconds seconds: $message")
        } catch (e: Exception) {
            ActionResult(false, "Clock application not available to set timer.", e.message)
        }
    }

    private fun handleVolumeControl(context: Context, request: ActionRequest): ActionResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ActionResult(false, "Audio system service unavailable.")

        val direction = request.parameters["direction"]?.lowercase() ?: "up"
        val adjustDir = when (direction) {
            "up", "raise", "increase" -> AudioManager.ADJUST_RAISE
            "down", "lower", "decrease" -> AudioManager.ADJUST_LOWER
            "mute" -> AudioManager.ADJUST_MUTE
            "unmute" -> AudioManager.ADJUST_UNMUTE
            else -> AudioManager.ADJUST_SAME
        }

        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            adjustDir,
            AudioManager.FLAG_SHOW_UI
        )

        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val pct = (currentVol * 100) / maxVol.coerceAtLeast(1)

        return ActionResult(true, "Media volume adjusted to $pct%")
    }

    private fun handleTorchControl(context: Context, request: ActionRequest): ActionResult {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return ActionResult(false, "Camera manager unavailable.")

        val targetState = request.parameters["state"]?.lowercase()
        val newState = when (targetState) {
            "on", "turn_on", "enable" -> true
            "off", "turn_off", "disable" -> false
            else -> !isTorchOn
        }

        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }

            if (cameraId == null) {
                return ActionResult(false, "Device does not have a supported camera flashlight.")
            }

            cameraManager.setTorchMode(cameraId, newState)
            isTorchOn = newState
            val msg = if (newState) "Flashlight turned ON" else "Flashlight turned OFF"
            ActionResult(true, msg)
        } catch (e: Exception) {
            ActionResult(false, "Failed to toggle flashlight: ${e.localizedMessage}")
        }
    }

    private fun handleMapsSearch(context: Context, request: ActionRequest): ActionResult {
        val query = request.parameters["query"] ?: ""
        val geoUri = if (query.isNotBlank()) {
            Uri.parse("geo:0,0?q=${Uri.encode(query)}")
        } else {
            Uri.parse("geo:0,0")
        }

        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(mapIntent)
            ActionResult(true, "Searching Maps for '$query'")
        } catch (e: Exception) {
            ActionResult(false, "No maps application available.")
        }
    }

    private fun handleDialPhone(context: Context, request: ActionRequest): ActionResult {
        val number = request.parameters["number"] ?: ""
        val dialUri = Uri.parse("tel:${Uri.encode(number)}")
        val intent = Intent(Intent.ACTION_DIAL, dialUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ActionResult(true, "Opening dialer with $number")
    }

    private fun handleWhatsAppMessage(context: Context, request: ActionRequest): ActionResult {
        val phone = request.parameters["phone"] ?: ""
        val message = request.parameters["message"] ?: ""

        val url = if (phone.isNotBlank()) {
            "https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(message)}"
        } else {
            "https://api.whatsapp.com/send?text=${Uri.encode(message)}"
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            ActionResult(true, "Opening WhatsApp with your draft message. Confirm to send in WhatsApp.")
        } catch (e: Exception) {
            // Try standard send intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(shareIntent)
                ActionResult(true, "Opening WhatsApp chat composer.")
            } catch (err: Exception) {
                ActionResult(false, "WhatsApp is not installed on this device.")
            }
        }
    }

    /**
     * Parses intent keywords from natural language (Bengali or English)
     */
    fun parseIntentOrCommand(input: String, apps: List<AppInfo>): ActionRequest? {
        val text = input.trim()
        val lower = text.lowercase()

        // 1. YouTube Search or open
        if (lower.startsWith("search youtube") || lower.contains("ইউটিউবে খুঁজুন") || lower.contains("youtube search")) {
            val query = text.replace(Regex("(?i)^(search youtube( for)?|youtube search|ইউটিউবে খুঁজুন)\\s*"), "").trim()
            return ActionRequest(type = ActionType.SEARCH_YOUTUBE, parameters = mapOf("query" to query))
        }

        // 2. Google Search
        if (lower.startsWith("search google") || lower.startsWith("google search") || lower.startsWith("search for") || lower.contains("গুগলে খুঁজুন")) {
            val query = text.replace(Regex("(?i)^(search google( for)?|google search|search for|গুগলে খুঁজুন)\\s*"), "").trim()
            return ActionRequest(type = ActionType.SEARCH_GOOGLE, parameters = mapOf("query" to query))
        }

        // 3. Time / Date query
        if (lower.contains("what time") || lower.contains("current time") || lower.contains("what's the time") ||
            lower.contains("আজকের তারিখ") || lower.contains("কয়টা বাজে") || lower.contains("সময় কত")) {
            return ActionRequest(type = ActionType.READ_DATE_TIME)
        }

        // 4. Torch / Flashlight
        if (lower.contains("flashlight") || lower.contains("torch") || lower.contains("টর্চ") || lower.contains("ফ্ল্যাশলাইট")) {
            val state = if (lower.contains("off") || lower.contains("বন্ধ")) "off" else "on"
            return ActionRequest(type = ActionType.TORCH_CONTROL, parameters = mapOf("state" to state))
        }

        // 5. Volume control
        if (lower.contains("volume") || lower.contains("সাউন্ড") || lower.contains("ভলিউম")) {
            val dir = when {
                lower.contains("up") || lower.contains("increase") || lower.contains("raise") || lower.contains("বাড়িয়ে") -> "up"
                lower.contains("down") || lower.contains("decrease") || lower.contains("lower") || lower.contains("কমিয়ে") -> "down"
                lower.contains("mute") || lower.contains("সাইলেন্ট") -> "mute"
                else -> "up"
            }
            return ActionRequest(type = ActionType.VOLUME_CONTROL, parameters = mapOf("direction" to dir))
        }

        // 6. Set Timer
        if (lower.contains("timer") || lower.contains("টাইমার")) {
            val num = Regex("\\d+").find(text)?.value?.toIntOrNull() ?: 60
            return ActionRequest(type = ActionType.SET_TIMER, parameters = mapOf("seconds" to num.toString()))
        }

        // 7. Set Alarm
        if (lower.contains("alarm") || lower.contains("অ্যালার্ম")) {
            val num = Regex("\\d+").find(text)?.value?.toIntOrNull() ?: 7
            return ActionRequest(type = ActionType.SET_ALARM, parameters = mapOf("hour" to num.toString(), "minutes" to "0"))
        }

        // 8. Open App (English & Bengali patterns: "open <app>", "<app> ওপেন করো", "চালু করো")
        if (lower.startsWith("open ") || lower.contains("ওপেন করো") || lower.contains("চালু করো") || lower.startsWith("launch ")) {
            val appQuery = text
                .replace(Regex("(?i)^(open|launch)\\s+"), "")
                .replace(Regex("\\s*(ওপেন করো|চালু করো)"), "")
                .trim()

            if (appQuery.isNotBlank()) {
                val matched = findApp(appQuery, apps)
                return ActionRequest(
                    type = ActionType.OPEN_APP,
                    parameters = mapOf(
                        "app" to appQuery,
                        "package" to (matched?.packageName ?: "")
                    )
                )
            }
        }

        // 9. Sensitive: Phone call / dial
        if (lower.startsWith("call ") || lower.startsWith("dial ") || lower.contains("কল করো")) {
            val number = text.replace(Regex("(?i)^(call|dial|কল করো)\\s*"), "").trim()
            return ActionRequest(
                type = ActionType.DIAL_PHONE,
                parameters = mapOf("number" to number),
                requiresConfirmation = true,
                confirmationTitle = "Confirm Phone Call",
                confirmationPrompt = "Do you want to dial $number?"
            )
        }

        // 10. Sensitive: WhatsApp
        if (lower.contains("whatsapp") || lower.contains("হোয়াটসঅ্যাপ")) {
            return ActionRequest(
                type = ActionType.WHATSAPP_MESSAGE,
                parameters = mapOf("message" to text),
                requiresConfirmation = true,
                confirmationTitle = "Open WhatsApp",
                confirmationPrompt = "Prepare WhatsApp message: \"$text\"?"
            )
        }

        return null
    }
}
