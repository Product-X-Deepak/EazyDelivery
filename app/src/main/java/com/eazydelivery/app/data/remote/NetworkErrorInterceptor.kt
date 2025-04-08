package com.eazydelivery.app.data.remote

import com.eazydelivery.app.util.ConnectivityManager
import com.eazydelivery.app.util.ErrorHandler
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkErrorInterceptor @Inject constructor(
    private val connectivityManager: ConnectivityManager,
    private val errorHandler: ErrorHandler
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!connectivityManager.isNetworkAvailable()) {
            throw NoConnectivityException()
        }
        
        val request = chain.request()
        
        try {
            // Set timeouts for the request
            val newRequest = request.newBuilder().build()
            return chain.proceed(newRequest)
        } catch (e: Exception) {
            val wrappedException = when (e) {
                is SocketTimeoutException -> RequestTimeoutException()
                is UnknownHostException -> ServerUnreachableException()
                else -> e
            }
            
            errorHandler.handleException(
                "NetworkErrorInterceptor",
                wrappedException,
                "Error during network request: ${request.url}"
            )
            
            throw wrappedException
        }
    }
    
    class NoConnectivityException : IOException("No internet connection available")
    class RequestTimeoutException : IOException("Request timed out")
    class ServerUnreachableException : IOException("Server is unreachable")
}
