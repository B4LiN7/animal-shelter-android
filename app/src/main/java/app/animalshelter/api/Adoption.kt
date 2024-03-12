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
    val petId: Int,
    val userId: String,
    val status: Status,
) {}

data class AdoptionSubmitDto(
    val petId: Int,
    val userId: String,
    val status: AdoptionStatus,
) {}

enum class AdoptionStatus {
    ADOPTING,
    ADOPTED,
    CANCELLED
}