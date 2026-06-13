package com.pranavakshit.gpscamportal.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PUT
import retrofit2.http.DELETE
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

    @GET("/api/users")
    suspend fun getUsers(): Response<List<UserDto>>

    @PUT("/api/users/{id}/role")
    suspend fun updateUserRole(@retrofit2.http.Path("id") id: Int, @Body request: UpdateRoleRequest): Response<UserDto>

    @DELETE("/api/users/{id}")
    suspend fun deleteUser(@retrofit2.http.Path("id") id: Int): Response<Unit>

    @GET("/api/photos")
    suspend fun getPhotos(@retrofit2.http.Query("recycle_bin") recycleBin: Boolean = false): Response<List<PhotoDto>>

    @POST("/api/photos/{id}/request-delete")
    suspend fun requestDeletePhoto(@retrofit2.http.Path("id") id: Int, @Body request: DeletionRequest): Response<Unit>

    @POST("/api/photos/{id}/approve-delete")
    suspend fun approveDeletePhoto(@retrofit2.http.Path("id") id: Int): Response<Unit>

    @DELETE("/api/photos/{id}")
    suspend fun deletePhoto(@retrofit2.http.Path("id") id: Int): Response<Unit>

    @GET("/api/locations/states/{id}/districts")
    suspend fun getDistricts(@retrofit2.http.Path("id") stateId: Int): Response<List<DistrictDto>>

    @GET("/api/locations/districts/{id}/subdistricts")
    suspend fun getSubDistricts(@retrofit2.http.Path("id") districtId: Int): Response<List<SubDistrictDto>>

    @GET("/api/locations/subdistricts/{id}/villages")
    suspend fun getVillages(@retrofit2.http.Path("id") subDistrictId: Int): Response<List<VillageDto>>

    @GET("/api/locations/districts/{id}/ulbs")
    suspend fun getUlbs(@retrofit2.http.Path("id") districtId: Int): Response<List<UlbDto>>

    @GET("/api/locations/ulbs/{id}/wards")
    suspend fun getWards(@retrofit2.http.Path("id") ulbId: Int): Response<List<WardDto>>

    @DELETE("/api/locations/{id}")
    suspend fun deleteLocation(@retrofit2.http.Path("id") id: Int): Response<Unit>

    @Multipart
    @POST("/api/locations/import")
    suspend fun importLocations(@Part zipfile: MultipartBody.Part): Response<Unit>

    companion object {
        private const val BASE_URL = "https://api.pranavakshit.in/"

        fun create(context: android.content.Context): ApiService {
            val logging = okhttp3.logging.HttpLoggingInterceptor()
            logging.setLevel(okhttp3.logging.HttpLoggingInterceptor.Level.BODY)

            val authInterceptor = AuthInterceptor(context)

            val okHttpClient = okhttp3.OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(authInterceptor)
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
