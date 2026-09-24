package com.buztrack.app.data.remote.network

import com.buztrack.app.config.AppConfig
import com.buztrack.app.data.remote.api.AuthApiService
import com.buztrack.app.data.remote.api.BusinessApiService
import com.buztrack.app.data.remote.api.CashbookApiService
import com.buztrack.app.data.remote.api.CustomerApiService
import com.buztrack.app.data.remote.api.SupplierApiService
import com.buztrack.app.data.remote.api.TransactionApiService
import com.buztrack.app.data.session.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient private constructor(sessionManager: SessionManager) {

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(sessionManager))
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(AppConfig.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(AppConfig.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(AppConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApiService: AuthApiService = retrofit.create(AuthApiService::class.java)
    val businessApiService: BusinessApiService = retrofit.create(BusinessApiService::class.java)
    val transactionApiService: TransactionApiService = retrofit.create(TransactionApiService::class.java)
    val customerApiService: CustomerApiService = retrofit.create(CustomerApiService::class.java)
    val supplierApiService: SupplierApiService = retrofit.create(SupplierApiService::class.java)
    val cashbookApiService: CashbookApiService = retrofit.create(CashbookApiService::class.java)

    companion object {
        @Volatile
        private var instance: ApiClient? = null

        fun getInstance(sessionManager: SessionManager): ApiClient {
            return instance ?: synchronized(this) {
                instance ?: ApiClient(sessionManager).also { instance = it }
            }
        }
    }
}
