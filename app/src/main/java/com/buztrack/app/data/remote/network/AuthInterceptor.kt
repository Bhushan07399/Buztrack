package com.buztrack.app.data.remote.network

import com.buztrack.app.config.AppConfig
import com.buztrack.app.data.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()

        val token = sessionManager.getAuthToken()
        if (!token.isNullOrBlank()) {
            builder.header(AppConfig.Headers.AUTHORIZATION, "Bearer $token")
        }

        val businessId = sessionManager.getActiveBusinessId()
        if (!businessId.isNullOrBlank()) {
            builder.header(AppConfig.Headers.TENANT_BUSINESS_ID, businessId)
        }

        return chain.proceed(builder.build())
    }
}
