package com.pranavakshit.gpscamportal.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

import retrofit2.http.Body
import retrofit2.http.GET

interface ApiService {
    
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("/api/locations/states")
    suspend fun getStates(): Response<List<StateDto>>

    @GET("/api/locations/states/{id}/offline-bundle")
    suspend fun getOfflineBundle(@retrofit2.http.Path("id") stateCode: Int): Response<OfflineBundleDto>

    @GET("/api/locations/search")
    suspend fun searchLocations(
        @retrofit2.http.Query("q") query: String,
        @retrofit2.http.Query("stateCode") stateCode: Int
    ): Response<List<SearchResultDto>>

    @Multipart
    @POST("/api/photos/upload")
    suspend fun uploadPhoto(
        @Part("uploader") uploader: RequestBody,
        @Part("locationName") locationName: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("timestamp") timestamp: RequestBody,
        @Part image: MultipartBody.Part
    ): Response<Unit>

    companion object {
        private const val BASE_URL = "https://api.pranavakshit.in/"

        fun create(): ApiService {
            val okHttpClient = okhttp3.OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val original = chain.request()
                    val request = original.newBuilder()
                        // Disguise as Chrome Mobile to bypass Cloudflare Bot Protection
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-G991U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36")
                        .build()
                    chain.proceed(request)
                }
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            
            return retrofit.create(ApiService::class.java)
        }
    }
}
