package app.animalshelter.ApiService

import retrofit2.http.GET
import retrofit2.http.Path

interface User {
    @GET("/user/name/{id}")
    suspend fun getUserName(@Path("id") id: String): UserNameDto
}

data class UserNameDto(
    val userId: String,
    val username: String,
    val name: String,
) {}