package app.animalshelter.ApiService

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path

interface Media {
    @GET("/media/{path}")
    suspend fun getMedia(@Path("path") path: String): ResponseBody
}