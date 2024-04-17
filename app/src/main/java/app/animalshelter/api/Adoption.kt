package app.animalshelter.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface Adoption {
    @GET("/adoption")
    suspend fun getAdoptions(): List<AdoptionResponse>

    @PUT("/adoption/{adoptionId}")
    suspend fun updateAdoption(@Path("adoptionId") id: String, @Body data: AdoptionDto): Response<ResponseBody>

    @DELETE("/adoption/{adoptionId}")
    suspend fun deleteAdoption(@Path("adoptionId") id: String): Response<ResponseBody>
}

data class AdoptionResponse(
    val adoptionId: String,
    val petId: String,
    val userId: String,
    val status: AdoptionStatus,
    val reason: String,
) {}

data class AdoptionDto(
    val status: AdoptionStatus,
    val reason: String?,
) {}

enum class AdoptionStatus {
    PENDING,
    REJECTED,
    CANCELLED,
    APPROVED,
    RETURNED,
}