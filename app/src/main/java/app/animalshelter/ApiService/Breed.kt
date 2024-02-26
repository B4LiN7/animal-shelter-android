package app.animalshelter.ApiService

import retrofit2.http.GET

interface Breed {
    @GET("/breed")
    suspend fun getBreeds(): List<BreedDto>
}

data class BreedDto(
    val breedId: Int,
    val name: String,
    val description: String,
) {}
