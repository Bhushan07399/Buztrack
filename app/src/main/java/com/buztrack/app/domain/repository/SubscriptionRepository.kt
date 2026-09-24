package com.buztrack.app.domain.repository

import com.buztrack.app.data.remote.dto.SubscriptionDto
import com.buztrack.app.data.remote.network.NetworkResult
import kotlinx.coroutines.flow.StateFlow

interface SubscriptionRepository {
    val subscription: StateFlow<SubscriptionDto?>
    suspend fun fetchSubscriptionInfo(): NetworkResult<SubscriptionDto>
}
