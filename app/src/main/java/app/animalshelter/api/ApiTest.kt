package app.animalshelter.api

import retrofit2.http.GET

interface ApiTest {
    @GET("/")
    suspend fun testBaseURL(): retrofit2.Response<Unit>
}