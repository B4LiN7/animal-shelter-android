package app.animalshelter.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface Breed {
    @GET("/breed")
    suspend fun getBreeds(): List<BreedDto>

    @GET("/breed/{id}")
    suspend fun getBreedById(@Path("id") id: Int): BreedDto

    @POST("/breed")
    suspend fun createBreed(@Body breed: BreedDto): BreedDto

    @PUT("/breed/{id}")
    suspend fun updateBreed(@Path("id") id: Int, @Body breed: BreedDto): BreedDto

    @DELETE("/breed/{id}")
    suspend fun deleteBreed(@Path("id") id: Int): BreedDto
}

data class BreedDto(
    val breedId: Int,
    val name: String,
    val description: String,
    val speciesId: Int,
) {}
