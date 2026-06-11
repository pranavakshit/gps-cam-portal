package com.pranavakshit.gpscamportal.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    
    @Multipart
    @POST("/api/photos/upload")
    suspend fun uploadPhoto(
        @Part("uploader") uploader: RequestBody,
        @Part("location_name") locationName: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("timestamp") timestamp: RequestBody,
        @Part image: MultipartBody.Part
    ): Response<Unit>

    companion object {
        private const val BASE_URL = "http://localhost:5000/" // Uses adb reverse

        fun create(): ApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            
            return retrofit.create(ApiService::class.java)
        }
    }
}
