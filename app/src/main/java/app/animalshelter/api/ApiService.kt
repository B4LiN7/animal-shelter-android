package app.animalshelter.api

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File

class ApiService(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val baseUrl: String? = sharedPreferences.getString("base_url", "http://10.0.2.2:3001/")
    private val retrofitService: RetrofitService = RetrofitService(sharedPreferences)

    // Create instances of the interfaces
    private val petInterface: Pet = retrofitService.getRetrofitService().create(Pet::class.java)
    private val userInterface: User = retrofitService.getRetrofitService().create(User::class.java)
    private val mediaInterface: Media = retrofitService.getRetrofitService().create(Media::class.java)
    private val breedInterface: Breed = retrofitService.getRetrofitService().create(Breed::class.java)
    private val speciesInterface: Species = retrofitService.getRetrofitService().create(Species::class.java)
    private val authInterface: Auth = retrofitService.getRetrofitService().create(Auth::class.java)
    private val adoptionInterface: Adoption = retrofitService.getRetrofitService().create(Adoption::class.java)

    private val tokenRefresh: TokenRefresh = TokenRefresh(authInterface, sharedPreferences)


    /**
     * Simple test to check if the base URL is reachable.
     * @return true if the base URL is reachable, false otherwise.
     */
    suspend fun apiTest(): Boolean {
        val apiTest: ApiTest = retrofitService.getRetrofitService().create(ApiTest::class.java)
        return try {
            val response = apiTest.testBaseURL()
            if (response.isSuccessful) {
                Log.i("ApiService", "Successfully reached base URL")
                true
            } else {
                Log.e("ApiService", "Successfully reached base URL but with response code: ${response.code()}")
                true
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error reaching base URL", e)
            false
        }
    }

    /**
     * Logs in the user and saves the access and refresh tokens to the shared preferences.
     */
    suspend fun login(username: String, password: String): AuthResponse? {
        return try {
            val loginDto = LoginDto(username, password)
            val response = authInterface.login(loginDto)
            if (response.isSuccessful) {
                val responseBody = response.body()?.string()
                val authResponse: AuthResponse? = Gson().fromJson(responseBody, AuthResponse::class.java)
                authResponse?.let {
                    sharedPreferences.edit()
                        .putString("access_token", it.accessToken)
                        .putString("refresh_token", it.refreshToken)
                        .apply()
                }
                Log.i("ApiService", "Login successful (access_token: ${authResponse?.accessToken} refresh_token: ${authResponse?.refreshToken})")
                authResponse
            } else {
                Log.e("ApiService", "Login failed, response code: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error logging in", e)
            return null
        }
    }

    /**
     * Logs out the user and deletes the access and refresh tokens from the shared preferences.
     */
    suspend fun logout() {
        try {
            val refreshToken = sharedPreferences.getString("refresh_token", null)
            val response = authInterface.logout("Bearer $refreshToken")
            sharedPreferences.edit()
                .remove("access_token")
                .remove("refresh_token")
                .apply()
            if (response.isSuccessful) {
                Log.i("ApiService", "Logout successful. Local tokens deleted.")
            } else {
                Log.e("ApiService", "Logout failed with /auth/logout but local tokens deleted.")
            }
        } catch (e: Exception) {
            sharedPreferences.edit()
                .remove("access_token")
                .remove("refresh_token")
                .apply()
            Log.e("ApiService", "Logout request failed but local tokens deleted.", e)
        }
    }

    /**
     * Registers a new user. Not implemented in the app.
     */
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

    /**
     * Fetches the current user from the server.
     */
    suspend fun fetchCurrentUser(): UserDto? {
        tokenRefresh.refreshTokenIfNeeded()
        return try {
            userInterface.getMe()
        } catch (e: Exception) {
            Log.e("ApiService", "Error fetching current user", e)
            null
        }
    }

    suspend fun fetchAdoptions(): List<AdoptionResponse> {
        tokenRefresh.refreshTokenIfNeeded()
        var adoptionList: List<AdoptionResponse> = emptyList()
        try {
            adoptionList = adoptionInterface.getAdoptions()
        } catch (e: Exception) {
            Log.e("ApiService", "Error fetching adoptions", e)
        }
        return adoptionList
    }
    suspend fun updateAdoption(adoptionId: String, dto: AdoptionDto): Response<ResponseBody>? {
        tokenRefresh.refreshTokenIfNeeded()
        return try {
            adoptionInterface.updateAdoption(adoptionId, dto)
        } catch (e: Exception) {
            Log.e("ApiService", "Error updating adoption", e)
            null
        }
    }
    suspend fun deleteAdoption(adoptionId: String): Response<ResponseBody>? {
        tokenRefresh.refreshTokenIfNeeded()
        return try {
            adoptionInterface.deleteAdoption(adoptionId)
        } catch (e: Exception) {
            Log.e("ApiService", "Error deleting adoption", e)
            null
        }
    }


    suspend fun fetchUsernames(adoptions: List<AdoptionResponse>): MutableMap<String, UserNameDto> {
        tokenRefresh.refreshTokenIfNeeded()
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
    suspend fun fetchPetsArray(pets: List<String>): List<PetDto> {
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
    suspend fun createPet(pet: PetDto): Any? {
        return try {
            tokenRefresh.refreshTokenIfNeeded()
            val response = petInterface.createPet(pet)
            if (response.isSuccessful) {
                val responseBody = response.body()?.string()
                val petResponse: PetDto? = Gson().fromJson(responseBody, PetDto::class.java)
                Log.i("ApiService", "Pet added: $petResponse")
                petResponse
            } else {
                val responseBody = response.body()?.string()
                val petResponse: PetErrorResponse? = Gson().fromJson(responseBody, PetErrorResponse::class.java)
                Log.e("ApiService", "Error adding pet: ${petResponse.toString()}")
                petResponse
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error adding pet", e)
            null
        }
    }
    suspend fun updatePet(petId: String, pet: PetDto): Any? {
        return try {
            tokenRefresh.refreshTokenIfNeeded()
            val response = petInterface.updatePet(petId, pet)
            if (response.isSuccessful) {
                val responseBody = response.body()?.string()
                val petResponse: PetDto? = Gson().fromJson(responseBody, PetDto::class.java)
                Log.i("ApiService", "Pet updated: $petResponse")
                petResponse
            } else {
                val responseBody = response.body()?.string()
                val petResponse: PetErrorResponse? = Gson().fromJson(responseBody, PetErrorResponse::class.java)
                Log.e("ApiService", "Error updating pet: ${response.errorBody()?.string()}")
                petResponse
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error updating pet", e)
            null
        }
    }
    suspend fun deletePet(petId: String): PetDto? {
        return try {
            tokenRefresh.refreshTokenIfNeeded()
            val response = petInterface.deletePet(petId)
            if (response.isSuccessful) {
                val responseBody = response.body()?.string()
                val petResponse: PetDto? = Gson().fromJson(responseBody, PetDto::class.java)
                Log.i("ApiService", "Pet deleted: $petResponse")
                petResponse
            } else {
                Log.e("ApiService", "Error deleting pet: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error deleting pet", e)
            null
        }
    }

    suspend fun fetchImagesForPets(petList: List<PetDto>): MutableMap<String, Bitmap> {
        val imageMap: MutableMap<String, Bitmap> = mutableMapOf()
        for (pet in petList) {
            try {
                val imageUrls = pet.imageUrls?.toList()
                if (imageUrls != null) {
                    if (imageUrls.isEmpty()) {
                        Log.e("ApiService", "Pet ${pet.name} [${pet.petId}] don't have image")
                        continue
                    }
                }

                val fullUrl = imageUrls?.elementAt(0)
                val startIndex = fullUrl?.indexOf("/uploads")
                val shortUrl = fullUrl?.substring(startIndex ?:0 )

                val image = shortUrl?.let { mediaInterface.getMedia(it) }

                val inputStream = image?.byteStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                imageMap[pet.petId] = bitmap
            } catch (e: Exception) {
                Log.e("ApiService", "Error fetching pet ${pet.name} [${pet.petId}] image", e)
            }
        }
        return imageMap
    }
    suspend fun fetchImage(path: String): Bitmap? {
        return try {
            val startIndex = path.indexOf("/uploads")
            val shortUrl = path.substring(startIndex ?:0 )
            val image = mediaInterface.getMedia(shortUrl)
            val inputStream = image.byteStream()
            Log.i("ApiService", "Image fetched from $path")
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.e("ApiService", "Error fetching image from $path", e)
            null
        }
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
    suspend fun fetchBreed(breedId: String): BreedDto? {
        return try {
            val breed = breedInterface.getBreedById(breedId)
            breed
        } catch (e: Exception) {
            Log.e("ApiService", "Error fetching breed [$breedId]", e)
            return null
        }
    }
    suspend fun createBreed(breed: BreedDto): BreedDto? {
        return try {
            tokenRefresh.refreshTokenIfNeeded()
            val response = breedInterface.createBreed(breed)
            if (response.isSuccessful) {
                val responseBody = response.body()?.string()
                val breedResponse: BreedDto? = Gson().fromJson(responseBody, BreedDto::class.java)
                Log.i("ApiService", "Breed added: $breedResponse")
                breedResponse
            } else {
                Log.e("ApiService", "Error adding breed: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error adding breed", e)
            null
        }
    }
    suspend fun updateBreed(breedId: String, breed: BreedDto): BreedDto? {
        return try {
            tokenRefresh.refreshTokenIfNeeded()
            val response = breedInterface.updateBreed(breedId, breed)
            if (response.isSuccessful) {
                val responseBody = response.body()?.string()
                val breedResponse: BreedDto? = Gson().fromJson(responseBody, BreedDto::class.java)
                Log.i("ApiService", "Breed updated: $breedResponse")
                breedResponse
            } else {
                Log.e("ApiService", "Error updating breed: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error updating breed", e)
            null
        }
    }
    suspend fun deleteBreed(breedId: String): BreedDto? {
        return try {
            tokenRefresh.refreshTokenIfNeeded()
            val response = breedInterface.deleteBreed(breedId)
            if (response.isSuccessful) {
                val responseBody = response.body()?.string()
                val breedResponse: BreedDto? = Gson().fromJson(responseBody, BreedDto::class.java)
                Log.i("ApiService", "Breed deleted: $breedResponse")
                breedResponse
            } else {
                Log.e("ApiService", "Error deleting breed: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error deleting breed", e)
            null
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
    suspend fun fetchSpecies(speciesId: String): SpeciesDto? {
        return try {
            val species = speciesInterface.getSpeciesById(speciesId)
            species
        } catch (e: Exception) {
            Log.e("ApiService", "Error fetching species with ID $speciesId", e)
            return null
        }
    }
    suspend fun createSpecies(species: SpeciesDto): SpeciesDto? {
        return try {
            tokenRefresh.refreshTokenIfNeeded()
            val response = speciesInterface.createSpecies(species)
            if (response.isSuccessful) {
                val responseBody = response.body()?.string()
                val speciesResponse: SpeciesDto? = Gson().fromJson(responseBody, SpeciesDto::class.java)
                Log.i("ApiService", "Species added: $speciesResponse")
                speciesResponse
            } else {
                Log.e("ApiService", "Error adding species: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error adding species", e)
            null
        }
    }
    suspend fun updateSpecies(speciesId: String, species: SpeciesDto): SpeciesDto? {
        return try {
            tokenRefresh.refreshTokenIfNeeded()
            val response = speciesInterface.updateSpecies(speciesId, species)
            if (response.isSuccessful) {
                val responseBody = response.body()?.string()
                val speciesResponse: SpeciesDto? = Gson().fromJson(responseBody, SpeciesDto::class.java)
                Log.i("ApiService", "Species updated: $speciesResponse")
                speciesResponse
            } else {
                Log.e("ApiService", "Error updating species: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error updating species", e)
            null
        }
    }

    /**
     * Uploads an image to the server and returns the URL of the uploaded image.
     * @param uri - URI of the image to upload.
     * @return URL of the uploaded image or null if the upload failed.
     */
    suspend fun uploadImage(uri: Uri): String? {
        tokenRefresh.refreshTokenIfNeeded()
        var response: MediaPostResponse? = null
        return try {
            val file = File(uri.path!!)
            val requestFile = file.asRequestBody("image/jpg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            response = mediaInterface.postMedia(body)
            Log.i("ApiService", "Image uploaded: $response")
            response.url
        } catch (e: Exception) {
            Log.e("ApiService", "Error uploading image: $response", e)
            null
        }
    }
}