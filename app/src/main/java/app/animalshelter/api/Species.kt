package app.animalshelter.api

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
    suspend fun getSpeciesById(@Path("id") id: Int): SpeciesDto

    @POST("/species")
    suspend fun createSpecies(@Body species: SpeciesDto): SpeciesDto

    @PUT("/species/{id}")
    suspend fun updateSpecies(@Path("id") id: Int, @Body species: SpeciesDto): SpeciesDto

    @DELETE("/species/{id}")
    suspend fun deleteSpecies(@Path("id") id: Int): SpeciesDto
}

data class SpeciesDto(
    val speciesId: Int,
    val name: String,
    val description: String,
)