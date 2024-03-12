package app.animalshelter.api

import retrofit2.http.GET
import retrofit2.http.Path

interface Breed {
    @GET("/breed")
    suspend fun getBreeds(): List<BreedDto>

    @GET("/breed/{id}")
    suspend fun getBreedById(@Path("id") id: Int): BreedDto
}

data class BreedDto(
    val breedId: Int,
    val name: String,
    val description: String,
    val speciesId: Int,
) {}
