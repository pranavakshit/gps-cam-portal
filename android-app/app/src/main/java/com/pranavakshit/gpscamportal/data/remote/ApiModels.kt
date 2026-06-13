package com.pranavakshit.gpscamportal.data.remote

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val user: UserDto
)

data class UserDto(
    val id: Int,
    val username: String,
    val role: String
)

data class StateDto(val id: Int, val lgdCode: Int, val name: String)
data class DistrictDto(val id: Int, val lgdCode: Int, val name: String, val stateCode: Int)
data class SubDistrictDto(val id: Int, val lgdCode: Int, val name: String, val districtCode: Int)
data class VillageDto(val id: Int, val lgdCode: Int, val name: String, val subDistrictCode: Int)
data class UlbDto(val id: Int, val lgdCode: Int, val name: String, val districtCode: Int)
data class WardDto(val id: Int, val lgdCode: Int, val name: String, val ulbCode: Int)

data class OfflineBundleDto(
    val state: StateDto,
    val districts: List<DistrictDto>,
    val subDistricts: List<SubDistrictDto>,
    val villages: List<VillageDto>
)

data class SearchResultDto(
    val id: Int,
    val lgdCode: Int,
    val name: String,
    val type: String,
    val path: String,
    val stateId: Int?,
    val districtId: Int?,
    val subDistrictId: Int?
)

data class PhotoDto(
    val id: Int,
    val locationName: String,
    val stateCode: Int?,
    val districtCode: Int?,
    val latitude: Double,
    val longitude: Double,
    val timestamp: String,
    val imageUrl: String,
    val uploader: String,
    val deletionStatus: String?,
    val deletionReason: String?
)

data class DeletionRequest(
    val reason: String
)

data class UpdateRoleRequest(
    val role: String
)
