package app.animalshelter.api

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface Pet {
    @GET("/pet")
    suspend fun getPets(): List<PetDto>

    @GET("/pet/{id}")
    suspend fun getPetById(@Path("id") id: String): PetDto

    @POST("/pet")
    suspend fun createPet(@Body pet: PetDto): ResponseBody

    @PUT("/pet/{id}")
    suspend fun updatePet(@Path("id") id: String, @Body pet: PetDto): ResponseBody

    @DELETE("/pet/{id}")
    suspend fun deletePet(@Path("id") id: String): ResponseBody
}

data class PetDto(
    val petId: String,
    val name: String,
    val sex: Sex,
    val description: String,
    val birthDate: String,
    val breedId: String,
    val imageUrls: List<String>?,
    val status: Status,
) {
    override fun toString(): String {
        return "[$petId] $name ($sex) (Born on: $birthDate) (Status: $status)"
    }
}

enum class Sex(val description: String) {
    OTHER("Egyéb"),
    MALE("Him"),
    FEMALE("Nőstény");
}

enum class Status(val description: String) {
    UNKNOWN("Ismeretlen"),
    INCOMING("Menhelyre jön"),
    INSHELTER("Menhelyen van"),
    ILL("Beteg"),
    ADOPTED("Örökbe fogadva"),
    DECEASED("Elhunyt");
}