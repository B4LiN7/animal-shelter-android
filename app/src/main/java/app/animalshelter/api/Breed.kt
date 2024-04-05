package app.animalshelter.api

import okhttp3.ResponseBody
import retrofit2.Response
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
    suspend fun getBreedById(@Path("id") id: String): BreedDto

    @POST("/breed")
    suspend fun createBreed(@Body breed: BreedDto): Response<ResponseBody>

    @PUT("/breed/{id}")
    suspend fun updateBreed(@Path("id") id: String, @Body breed: BreedDto): Response<ResponseBody>

    @DELETE("/breed/{id}")
    suspend fun deleteBreed(@Path("id") id: String): Response<ResponseBody>
}

data class BreedDto(
    val breedId: String,
    val name: String,
    val description: String,
    val speciesId: String,
) {}
