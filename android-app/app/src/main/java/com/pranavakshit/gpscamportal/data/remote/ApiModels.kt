package com.pranavakshit.gpscamportal.data.remote

data class LoginRequest(
    val username: String,
    val passwordHash: String
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
