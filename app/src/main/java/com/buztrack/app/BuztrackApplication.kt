package com.buztrack.app

import android.app.Application
import com.buztrack.app.data.remote.network.ApiClient
import com.buztrack.app.data.repository.LocalBuztrackRepository
import com.buztrack.app.data.repository.impl.NetworkAuthRepository
import com.buztrack.app.data.repository.impl.NetworkCashbookRepository
import com.buztrack.app.data.repository.impl.NetworkCustomerRepository
import com.buztrack.app.data.repository.impl.NetworkSupplierRepository
import com.buztrack.app.data.repository.impl.NetworkTransactionRepository
import com.buztrack.app.data.session.SessionManager
import com.buztrack.app.domain.repository.AuthRepository
import com.buztrack.app.domain.repository.CashbookRepository
import com.buztrack.app.domain.repository.CustomerRepository
import com.buztrack.app.domain.repository.SupplierRepository
import com.buztrack.app.domain.repository.TransactionRepository

class BuztrackApplication : Application() {
    val sessionManager: SessionManager by lazy {
        SessionManager.getInstance(this)
    }

    val apiClient: ApiClient by lazy {
        ApiClient.getInstance(sessionManager)
    }

    val authRepository: AuthRepository by lazy {
        NetworkAuthRepository(apiClient.authApiService, sessionManager)
    }

    val transactionRepository: TransactionRepository by lazy {
        NetworkTransactionRepository(apiClient.transactionApiService)
    }

    val customerRepository: CustomerRepository by lazy {
        NetworkCustomerRepository(apiClient.customerApiService)
    }

    val supplierRepository: SupplierRepository by lazy {
        NetworkSupplierRepository(apiClient.supplierApiService)
    }

    val cashbookRepository: CashbookRepository by lazy {
        NetworkCashbookRepository(apiClient.cashbookApiService)
    }

    val repository: LocalBuztrackRepository by lazy {
        LocalBuztrackRepository.getInstance(
            context = this,
            transactionRepository = transactionRepository,
            customerRepository = customerRepository,
            supplierRepository = supplierRepository,
            cashbookRepository = cashbookRepository
        )
    }
}
