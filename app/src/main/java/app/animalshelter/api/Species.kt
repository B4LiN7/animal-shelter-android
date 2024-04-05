package app.animalshelter.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface Species {
    @GET("/species")
    suspend fun getSpecies(): List<SpeciesDto>

    @GET("/species/{id}")
    suspend fun getSpeciesById(@Path("id") id: String): SpeciesDto

    @POST("/species")
    suspend fun createSpecies(@Body species: SpeciesDto): Response<ResponseBody>

    @PUT("/species/{id}")
    suspend fun updateSpecies(@Path("id") id: String, @Body species: SpeciesDto): Response<ResponseBody>

    @DELETE("/species/{id}")
    suspend fun deleteSpecies(@Path("id") id: String): Response<ResponseBody>
}

data class SpeciesDto(
    val speciesId: String,
    val name: String,
    val description: String,
)