package app.animalshelter.api

import retrofit2.http.GET
import retrofit2.http.Path

interface User {
    @GET("/user/me")
    suspend fun getMe(): UserDto

    @GET("/user/name/{id}")
    suspend fun getUserName(@Path("id") id: String): UserNameDto
}

data class UserDto(
    val userId: String,
    val username: String,
    val name: String,
    val email: String,
) {}

data class UserNameDto(
    val userId: String,
    val username: String,
    val name: String,
) {}