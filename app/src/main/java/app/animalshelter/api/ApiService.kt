package app.animalshelter.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ApiService(context: Context) {
    private val retrofitService: RetrofitService = RetrofitService(context)

    // Create instances of the interfaces
    val petInterface: Pet = retrofitService.getRetrofitService().create(Pet::class.java)
    private val userInterface: User = retrofitService.getRetrofitService().create(User::class.java)
    private val mediaInterface: Media = retrofitService.getRetrofitService().create(Media::class.java)
    val breedInterface: Breed = retrofitService.getRetrofitService().create(Breed::class.java)
    val authInterface: Auth = retrofitService.getRetrofitService().create(Auth::class.java)
    val adoptionInterface: Adoption = retrofitService.getRetrofitService().create(Adoption::class.java)

    suspend fun fetchAdoptions(): List<AdoptionDto> {
        var adoptionList: List<AdoptionDto> = emptyList()
        try {
            adoptionList = adoptionInterface.getAdoptions()
        } catch (e: Exception) {
            Log.e("ApiService", "Error fetching adoptions", e)
        }
        return adoptionList
    }
    suspend fun fetchUsernames(adoptions: List<AdoptionDto>): MutableMap<String, UserNameDto> {
        val usernameMap: MutableMap<String, UserNameDto> = mutableMapOf()
        for (adoption in adoptions) {
            try {
                val username = userInterface.getUserName(adoption.userId)
                usernameMap[adoption.userId] = username
            } catch (e: Exception) {
                Log.e("ApiService", "Error fetching username for user: [${adoption.userId}]")
            }
        }
        return usernameMap
    }
    suspend fun fetchPets(): List<PetDto> {
        val petList: List<PetDto>
        try {
            petList = petInterface.getPets()
        } catch (e: Exception) {
            Log.e("ApiService", "Error fetching pets", e)
            throw e
        }
        return petList
    }
    suspend fun fetchImagesForPets(petList: List<PetDto>): MutableMap<Int, Bitmap> {
        val imageMap: MutableMap<Int, Bitmap> = mutableMapOf()
        for (pet in petList) {
            try {
                val fullUrl = pet.imageUrl
                val startIndex = fullUrl.indexOf("/uploads")
                val shortUrl = fullUrl.substring(startIndex)

                val image = mediaInterface.getMedia(shortUrl)

                val inputStream = image.byteStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                imageMap[pet.petId] = bitmap
            } catch (e: Exception) {
                Log.e("ApiService", "Error fetching image for pet: [${pet.petId}] ${pet.name}")
            }
        }
        return imageMap
    }
    suspend fun fetchBreeds(): Map<Int, String> {
        val breedMap: MutableMap<Int, String> = mutableMapOf()
        try {
            val breeds = breedInterface.getBreeds()
            for (breed in breeds) {
                breedMap[breed.breedId] = breed.name
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error fetching breeds", e)
        }
        return breedMap
    }
    suspend fun fetchBreed(breedId: Int): BreedDto {
        return try {
            val breed = breedInterface.getBreedById(breedId)
            breed
        } catch (e: Exception) {
            Log.e("ApiService", "Error fetching breed with ID $breedId", e)
            BreedDto(-1, "Unknown", "Unknown")
        }
    }

    suspend fun uploadImage(uri: Uri): String {
        try {
            val file = File(uri.path!!)
            val requestFile = file.asRequestBody("image/jpg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val response = mediaInterface.postMedia(body)
            Log.i("ApiService", "Image uploaded: $response")
            return response.url
        } catch (e: Exception) {
            Log.e("ApiService", "Error uploading image", e)
            throw e
        }
    }

    fun printCookiesToLog() {
        retrofitService.printCookiesToLog()
    }

    fun clearCookies() {
        retrofitService.clearCookies()
    }
}