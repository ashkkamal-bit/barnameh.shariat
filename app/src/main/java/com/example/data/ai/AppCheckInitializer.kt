package com.example.data.ai

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

object AppCheckInitializer {
    private const val TAG = "NocturneAppCheck"
    private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
    private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
    private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
    private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

    fun isFirebaseConfigured(context: Context): Boolean {
        val appIdRes = context.resources.getIdentifier("google_app_id", "string", context.packageName)
        if (appIdRes == 0) return false
        val appId = runCatching { context.getString(appIdRes) }.getOrDefault("")
        return appId.isNotBlank() && !appId.contains("placeholder", ignoreCase = true)
    }

    fun configureAppCheck(context: Context, intent: Intent?): Boolean {
        if (!isFirebaseConfigured(context)) {
            Log.i(TAG, "Firebase is not configured yet (google_app_id missing). Cloud AI stays in offline mode.")
            return false
        }

        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            if (FirebaseApp.getApps(context).isEmpty()) {
                Log.w(TAG, "FirebaseApp could not be initialized.")
                return false
            }

            if (BuildConfig.DEBUG) {
                val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
                if (!debugToken.isNullOrEmpty()) {
                    val persistenceKey = FirebaseApp.getInstance().persistenceKey
                    val prefs = context.getSharedPreferences(
                        "$DEBUG_STORE_PREFIX$persistenceKey",
                        Context.MODE_PRIVATE
                    )
                    // Synchronous commit() is required before DebugAppCheckProviderFactory reads it.
                    prefs.edit()
                        .putString(DEBUG_SECRET_KEY, debugToken)
                        .commit()
                    Log.d(TAG, "App Check debug secret persisted.")
                }
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
            } else {
                val resId = context.resources.getIdentifier(
                    RECAPTCHA_SITE_KEY_RES,
                    "string",
                    context.packageName
                )
                if (resId != 0) {
                    val siteKey = context.getString(resId)
                    if (siteKey.isNotBlank()) {
                        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                            RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                        )
                    } else {
                        Log.e(TAG, "recaptcha_site_key is blank in release build.")
                    }
                } else {
                    Log.e(TAG, "recaptcha_site_key string resource not found in release build.")
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase App Check", e)
            false
        }
    }
}
