package app.animalshelter.api

import retrofit2.http.GET
import retrofit2.http.Path

interface Species {
    @GET("/species")
    suspend fun getSpecies(): List<SpeciesDto>

    @GET("/species/{id}")
    suspend fun getSpeciesById(@Path("id") id: Int): SpeciesDto
}

data class SpeciesDto(
    val speciesId: Int,
    val name: String,
    val description: String,
)