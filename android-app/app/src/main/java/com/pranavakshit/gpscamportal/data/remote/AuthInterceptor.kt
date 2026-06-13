package com.pranavakshit.gpscamportal.data.remote

import android.content.Context
import com.pranavakshit.gpscamportal.util.UserPreferences
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val userPreferences = UserPreferences(context)
        val token = userPreferences.getToken()

        val originalRequest = chain.request()
        
        if (token.isNullOrBlank()) {
            return chain.proceed(originalRequest)
        }

        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}
