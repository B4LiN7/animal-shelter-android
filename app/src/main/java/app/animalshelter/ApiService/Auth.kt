package app.animalshelter.ApiService

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface Auth {

    @POST("/auth/register")
    suspend fun register(
        @Body data: RegisterDto
    ): Response<ResponseBody>

    @POST("/auth/login")
    suspend fun login(
        @Body data: LoginDto
    ): Response<ResponseBody>

    @GET("/auth/logout")
    suspend fun logout(): ResponseBody
}

data class LoginDto(
    val username: String,
    val password: String
)

data class RegisterDto(
    val username: String,
    val password: String,
    val email: String
)