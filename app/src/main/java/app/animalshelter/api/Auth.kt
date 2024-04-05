package app.animalshelter.api

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
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

    @POST("/auth/refresh")
    suspend fun refresh(@Header("Authorization") refreshToken: String): Response<ResponseBody>

    @GET("/auth/logout")
    suspend fun logout(@Header("Authorization") refreshToken: String): Response<ResponseBody>
}

data class LoginDto(
    val username: String,
    val password: String
)

data class RegisterDto(
    val username: String,
    val password: String,
    val email: String,
)

data class AuthResponse(
    val message: String,
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String
)