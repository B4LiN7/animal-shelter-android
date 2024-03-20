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
    val speciesInterface: Species = retrofitService.getRetrofitService().create(Species::class.java)
    private val authInterface: Auth = retrofitService.getRetrofitService().create(Auth::class.java)
    val adoptionInterface: Adoption = retrofitService.getRetrofitService().create(Adoption::class.java)

    suspend fun apiTest(): Boolean {
        val apiTest: ApiTest = retrofitService.getRetrofitService().create(ApiTest::class.java)
        return try {
            val response = apiTest.testBaseURL()
            if (response.isSuccessful) {
                Log.i("ApiService", "Successfully reached base URL")
                true
            } else {
                Log.e("ApiService", "Failed to reach base URL, response code: ${response.code()}")
                true
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error reaching base URL", e)
            false
        }
    }

    suspend fun login(username: String, password: String): Boolean {
        return try {
            val loginDto = LoginDto(username, password)
            val response = authInterface.login(loginDto)
            if (response.isSuccessful) {
                Log.i("ApiService", "Login successful")
                true
            } else {
                Log.e("ApiService", "Login failed, response code: ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error logging in", e)
            false
        }
    }
    suspend fun logout() {
        try {
            authInterface.logout()
        } catch (e: Exception) {
            clearCookies()
            Log.e("ApiService", "Error logging out with /auth/logout. Delete cookies manually.")
        }
    }
    suspend fun register(username: String, password: String, email: String): Boolean {
        val registerDto = RegisterDto(username, password, email)
        return try {
            val response = authInterface.register(registerDto)
            if (response.isSuccessful) {
                Log.i("ApiService", "Register successful: ${response.body()?.string()}")
                true
            } else {
                Log.i("ApiService", "Register failed: ${response.errorBody()?.string()}")
                false
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error registering: ${e.message}")
            false
        }
    }
    suspend fun fetchCurrentUser(): UserDto? {
        return try {
            userInterface.getMe()
        } catch (e: Exception) {
            Log.e("ApiService", "Error fetching current user", e)
            null
        }
    }

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
    suspend fun fetchPetsArray(pets: List<Int>): List<PetDto> {
        var petList = mutableListOf<PetDto>()
        for (pet in pets) {
            try {
                val petDto = petInterface.getPetById(pet)
                petList = petList.plus(petDto).toMutableList()
            } catch (e: Exception) {
                Log.e("ApiService", "Error fetching pets", e)
            }
        }
        return petList
    }

    suspend fun fetchImagesForPets(petList: List<PetDto>): MutableMap<Int, Bitmap> {
        val imageMap: MutableMap<Int, Bitmap> = mutableMapOf()
        for (pet in petList) {
            try {
                if (pet.imageUrl == null) {
                    Log.e("ApiService", "Pet don't have image: [${pet.petId}] ${pet.name}")
                    continue
                }

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
    suspend fun fetchBreeds(): List<BreedDto> {
        var breedList: List<BreedDto> = mutableListOf()
        try {
            breedList = breedInterface.getBreeds()
        } catch (e: Exception) {
            Log.e("ApiService", "Error fetching breeds", e)
        }
        return breedList
    }
    suspend fun fetchBreed(breedId: Int): BreedDto? {
        return try {
            val breed = breedInterface.getBreedById(breedId)
            breed
        } catch (e: Exception) {
            Log.e("ApiService", "Error fetching breed with ID $breedId", e)
            return null
        }
    }
    suspend fun fetchSpecies(): List<SpeciesDto> {
        var speciesList: List<SpeciesDto> = mutableListOf()
        try {
            speciesList = speciesInterface.getSpecies()
        } catch (e: Exception) {
            Log.e("ApiService", "Error fetching species", e)
        }
        return speciesList
    }
    suspend fun fetchSpecies(speciesId: Int): SpeciesDto? {
        return try {
            val species = speciesInterface.getSpeciesById(speciesId)
            species
        } catch (e: Exception) {
            Log.e("ApiService", "Error fetching species with ID $speciesId", e)
            return null
        }
    }

    suspend fun uploadImage(uri: Uri): String? {
        try {
            val file = File(uri.path!!)
            val requestFile = file.asRequestBody("image/jpg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val response = mediaInterface.postMedia(body)
            Log.i("ApiService", "Image uploaded: $response")
            return response.url
        } catch (e: Exception) {
            Log.e("ApiService", "Error uploading image", e)
            return null
        }
    }

    fun printCookiesToLog() {
        retrofitService.printCookiesToLog()
    }
    fun clearCookies() {
        retrofitService.clearCookies()
    }
}