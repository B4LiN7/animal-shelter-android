package app.animalshelter.ApiService

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface Media {
    @GET("/media/{path}")
    suspend fun getMedia(@Path("path") path: String): ResponseBody

    @Multipart
    @POST("/media")
    suspend fun postMedia(@Part file: MultipartBody.Part): MediaPostResDto
}

data class MediaPostResDto(
    val message: String,
    val url: String,
    val name: String,
    val size: String
) {}