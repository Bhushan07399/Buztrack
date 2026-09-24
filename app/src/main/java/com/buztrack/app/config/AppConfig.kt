package com.buztrack.app.config

object AppConfig {
    const val API_BASE_URL = "http://10.0.2.2:5000/api/v1/"
    const val DEFAULT_TIMEOUT_SECONDS = 30L
    
    // Switch between local offline demo mode and production network backend
    var isOfflineDemoMode: Boolean = true

    object Headers {
        const val AUTHORIZATION = "Authorization"
        const val TENANT_BUSINESS_ID = "x-business-id"
    }

    object SaaSDefaults {
        const val FREE_PLAN_MAX_STAFF = 1
        const val FREE_PLAN_MAX_OCR_SCANS = 10
    }
}
