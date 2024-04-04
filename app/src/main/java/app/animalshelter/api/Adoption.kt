package app.animalshelter.api

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface Adoption {
    @GET("/adoption")
    suspend fun getAdoptions(): List<AdoptionDto>

    @POST("/adoption")
    suspend fun createAdoption(@Body data: AdoptionSubmitDto): Response<ResponseBody>
}

data class AdoptionDto(
    val adoptionId: String,
    val petId: String,
    val userId: String,
    val status: Status,
    val reason: String,
) {}

data class AdoptionSubmitDto(
    val petId: String,
    val userId: String,
    val status: AdoptionStatus,
    val reason: String? = null,
    val adoptionId: String? = null,
) {}

enum class AdoptionStatus {
    PENDING,
    REJECTED,
    CANCELLED,
    APPROVED,
}