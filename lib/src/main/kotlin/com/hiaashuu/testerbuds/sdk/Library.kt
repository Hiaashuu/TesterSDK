package com.hiaashuu.testerbuds.sdk

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

object TesterSdk {

    private const val PREFS_NAME = "testerbuds_sdk_prefs"
    private const val KEY_INSTALLATION_TOKEN = "installation_token"
    private const val KEY_PAIRING_CODE = "paired_code"
    private const val HEARTBEAT_INTERVAL_MS = 45000L
    const val SDK_VERSION = "1.0.0"

    private var appContext: Context? = null
    private var targetAppId: String = ""
    private var isForeground = false
    private var sessionStartTime = 0L
    private var lastHeartbeatTime = 0L
    private var activeActivityCount = 0

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO)
    private val eventFileLock = Any()

    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            if (isForeground) {
                recordHeartbeatEvent()
                handler.postDelayed(this, HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    fun init(application: Application, appId: String) {
        appContext = application.applicationContext
        targetAppId = appId

        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                activeActivityCount++
                if (activeActivityCount == 1) {
                    onAppForegrounded()
                }
            }

            override fun onActivityStopped(activity: Activity) {
                activeActivityCount--
                if (activeActivityCount <= 0) {
                    activeActivityCount = 0
                    onAppBackgrounded()
                }
            }

            override fun onActivityResumed(activity: Activity) {
                val intentCode = activity.intent?.getStringExtra("testerbuds_pair_code")
                if (!intentCode.isNullOrBlank()) {
                    setPairingCode(activity, intentCode)
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    fun setPairingCode(context: Context, code: String) {
        val trimmed = code.trim().uppercase()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentToken = prefs.getString(KEY_INSTALLATION_TOKEN, null)
        val token = if (currentToken.isNullOrEmpty()) "tok_" + UUID.randomUUID().toString() else currentToken

        prefs.edit()
            .putString(KEY_PAIRING_CODE, trimmed)
            .putString(KEY_INSTALLATION_TOKEN, token)
            .apply()

        recordEvent("paired", 0)
    }

    fun isPaired(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getString(KEY_PAIRING_CODE, null).isNullOrEmpty()
    }

    private fun onAppForegrounded() {
        isForeground = true
        sessionStartTime = System.currentTimeMillis()
        lastHeartbeatTime = sessionStartTime
        recordEvent("session_start", 0)
        handler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL_MS)
    }

    private fun onAppBackgrounded() {
        if (!isForeground) return
        isForeground = false
        handler.removeCallbacks(heartbeatRunnable)
        val now = System.currentTimeMillis()
        val durationSeconds = ((now - sessionStartTime) / 1000).toInt()
        recordEvent("session_end", durationSeconds)
    }

    private fun recordHeartbeatEvent() {
        val now = System.currentTimeMillis()
        val delta = ((now - lastHeartbeatTime) / 1000).toInt()
        lastHeartbeatTime = now
        recordEvent("heartbeat", delta)
    }

    private fun recordEvent(type: String, durationDeltaSeconds: Int) {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_INSTALLATION_TOKEN, "tok_local") ?: "tok_local"

        scope.launch {
            synchronized(eventFileLock) {
                val eventFile = File(ctx.filesDir, "testerbuds_sdk_events.json")
                val array = if (eventFile.exists()) {
                    try {
                        JSONArray(eventFile.readText())
                    } catch (_: Exception) {
                        JSONArray()
                    }
                } else {
                    JSONArray()
                }

                val eventObj = JSONObject().apply {
                    put("eventId", UUID.randomUUID().toString())
                    put("appId", targetAppId)
                    put("token", token)
                    put("type", type)
                    put("durationDeltaSeconds", durationDeltaSeconds)
                    put("clientTime", System.currentTimeMillis())
                    put("sdkVersion", SDK_VERSION)
                    put("deviceModel", Build.MODEL)
                    put("androidSdk", Build.VERSION.SDK_INT)
                }

                array.put(eventObj)
                while (array.length() > 500) {
                    array.remove(0)
                }
                eventFile.writeText(array.toString())
            }
        }
    }
}

class Library {
    fun sayMyName(ctx: Context): String {
        return ctx.getString(R.string.say_my_name)
    }
}