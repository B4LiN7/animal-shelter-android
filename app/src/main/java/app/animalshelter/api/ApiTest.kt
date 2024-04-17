package app.animalshelter.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET

interface ApiTest {
    @GET("/")
    suspend fun testBaseURL(): Response<ResponseBody>
}