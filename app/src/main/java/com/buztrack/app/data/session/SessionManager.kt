package com.buztrack.app.data.session

import android.content.Context
import android.content.SharedPreferences

class SessionManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "buztrack_secure_session_prefs"
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_PHONE = "user_phone"
        private const val KEY_USER_NAME = "user_full_name"
        private const val KEY_BUSINESS_ID = "active_business_id"
        private const val KEY_BUSINESS_NAME = "active_business_name"

        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun saveSession(
        token: String,
        userId: String,
        phone: String,
        activeBusinessId: String,
        businessName: String,
        fullName: String? = null
    ) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_PHONE, phone)
            .putString(KEY_BUSINESS_ID, activeBusinessId)
            .putString(KEY_BUSINESS_NAME, businessName)
            .apply {
                if (!fullName.isNullOrEmpty()) {
                    putString(KEY_USER_NAME, fullName)
                }
            }
            .apply()
    }

    fun getAuthToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getActiveBusinessId(): String? = prefs.getString(KEY_BUSINESS_ID, null)

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun getUserPhone(): String? = prefs.getString(KEY_PHONE, null)

    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "Shop Owner") ?: "Shop Owner"

    fun getBusinessName(): String = prefs.getString(KEY_BUSINESS_NAME, "My Business") ?: "My Business"

    fun isLoggedIn(): Boolean {
        val token = getAuthToken()
        return !token.isNullOrBlank()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
