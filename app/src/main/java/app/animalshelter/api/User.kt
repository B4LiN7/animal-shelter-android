package app.animalshelter.api

import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import java.util.Date

interface User {
    @GET("/user")
    suspend fun getUsers(): List<UserDto>

    @GET("/user/{id}")
    suspend fun getUser(@Path("id") id: String): UserDto

    @PUT("/user/{id}")
    suspend fun updateUser(@Path("id") id: String, user: UserDto): UserDto

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
    val profileImageUrl: String,
    val roles: List<String>,
    val createdAt: Date,
    val updatedAt: Date,
) {}

data class UserNameDto(
    val userId: String,
    val username: String,
    val name: String,
) {}