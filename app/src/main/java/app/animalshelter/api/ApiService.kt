package app.animalshelter.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log

class ApiService(context: Context) {
    private val retrofitService: RetrofitService = RetrofitService(context)

    // Create instances of the interfaces
    val petInterface: Pet = retrofitService.getRetrofitService().create(Pet::class.java)
    private val userInterface: User = retrofitService.getRetrofitService().create(User::class.java)
    val mediaInterface: Media = retrofitService.getRetrofitService().create(Media::class.java)
    val breedInterface: Breed = retrofitService.getRetrofitService().create(Breed::class.java)
    val authInterface: Auth = retrofitService.getRetrofitService().create(Auth::class.java)
    val adoptionInterface: Adoption = retrofitService.getRetrofitService().create(Adoption::class.java)

    suspend fun fetchAdoptions(): List<AdoptionDto> {
        var adoptionList: List<AdoptionDto> = emptyList()
        try {
            adoptionList = adoptionInterface.getAdoptions()
        } catch (e: Exception) {
            Log.e("AdoptionsFragment", "Error fetching adoptions", e)
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
                Log.e("AdoptionsFragment", "Error fetching username for user: [${adoption.userId}]")
            }
        }
        return usernameMap
    }
    suspend fun fetchPets(): List<PetDto> {
        val petList: List<PetDto>
        try {
            petList = petInterface.getPets()
        } catch (e: Exception) {
            Log.e("PetsFragment", "Error fetching pets", e)
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
                Log.e("PetsFragment", "Error fetching image for pet: [${pet.petId}] ${pet.name}")
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
            Log.e("PetsFragment", "Error fetching breeds", e)
        }
        return breedMap
    }
    suspend fun fetchBreed(breedId: Int): BreedDto {
        return try {
            val breed = breedInterface.getBreedById(breedId)
            breed
        } catch (e: Exception) {
            Log.e("PetsFragment", "Error fetching breed with ID $breedId", e)
            BreedDto(-1, "Unknown", "Unknown")
        }
    }

    fun printCookiesToLog() {
        retrofitService.printCookiesToLog()
    }
}