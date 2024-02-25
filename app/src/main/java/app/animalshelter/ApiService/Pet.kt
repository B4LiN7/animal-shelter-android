package app.animalshelter.ApiService

import retrofit2.Call
import retrofit2.http.GET

interface Pet {
    @GET("/pet")
    suspend fun getPets(): Call<List<PetDto>>
}

data class PetDto(
    val petId: Int,
    val name: String,
    val sex: String,
    val description: String,
    val birthDate: String,
    val breedId: Int,
    val status: String,
) {
    override fun toString(): String {
        return "[$petId] $name ($sex) (Born on: $birthDate) (Status: $status)"
    }
}