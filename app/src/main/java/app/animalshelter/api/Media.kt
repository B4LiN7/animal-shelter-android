package app.animalshelter.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface Media {
    @GET
    suspend fun getMediaOutsideBaseUrl(@retrofit2.http.Url url: String): ResponseBody

    @GET("/media/{path}")
    suspend fun getMedia(@Path("path") path: String): ResponseBody

    @Multipart
    @POST("/media")
    suspend fun postMedia(@Part file: MultipartBody.Part): MediaPostResponse
}

data class MediaPostResponse(
    val message: String,
    val url: String,
    val name: String,
    val size: String
) {}