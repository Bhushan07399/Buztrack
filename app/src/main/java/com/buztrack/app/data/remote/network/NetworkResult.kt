package com.buztrack.app.data.remote.network

sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class Error(val code: Int? = null, val message: String) : NetworkResult<Nothing>()
    data object Loading : NetworkResult<Nothing>()
}
