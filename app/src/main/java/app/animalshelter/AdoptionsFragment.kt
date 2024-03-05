package app.animalshelter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.animalshelter.ApiService.Adoption
import app.animalshelter.ApiService.AdoptionDto
import app.animalshelter.ApiService.AdoptionStatus
import app.animalshelter.ApiService.AdoptionSubmitDto
import app.animalshelter.ApiService.Media
import app.animalshelter.ApiService.Pet
import app.animalshelter.ApiService.PetDto
import app.animalshelter.ApiService.RetrofitService
import app.animalshelter.ApiService.User
import app.animalshelter.ApiService.UserNameDto
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import java.time.ZonedDateTime

class AdoptionsFragment : Fragment() {
    private var recyclerView: RecyclerView? = null

    private val petService = RetrofitService.getRetrofitService().create(Pet::class.java)
    private val mediaService = RetrofitService.getRetrofitService().create(Media::class.java)
    private val adoptionService = RetrofitService.getRetrofitService().create(Adoption::class.java)
    private val userService = RetrofitService.getRetrofitService().create(User::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_adoptions, container, false)
        initViews(view)
        lifecycleScope.launch {
            fetchAndDisplayAdoptions()
        }
        return view
    }

    private suspend fun fetchAndDisplayAdoptions() {
        Log.i("AdoptionsFragment", "Start fetching and displaying pets")
        RetrofitService.printCookiesToLog()

        Log.i("AdoptionsFragment", "Fetching adoptions")
        val adoptionList: List<AdoptionDto> = fetchAdoptions()

        Log.i("AdoptionsFragment", "Fetching user's names")
        val usernameMap: MutableMap<String, UserNameDto> = fetchUsernames(adoptionList)

        Log.i("AdoptionsFragment", "Fetching pets")
        val petList: List<PetDto> = fetchPets()

        Log.i("AdoptionsFragment", "Fetching pet images")
        val imageMap: MutableMap<Int, Bitmap> = fetchImagesForPets(petList)

        Log.i("AdoptionsFragment", "Setting up RecyclerView")
        val adapter = AdoptionAdapter(adoptionList, usernameMap, petList, imageMap)
        recyclerView?.layoutManager = LinearLayoutManager(context)
        recyclerView?.adapter = adapter
    }

    private inner class AdoptionAdapter(private val adoptions: List<AdoptionDto>, private val usernames: MutableMap<String, UserNameDto>, private val pets: List<PetDto>, private val images: Map<Int, Bitmap>) : RecyclerView.Adapter<AdoptionAdapter.AdoptionViewHolder>() {
        inner class AdoptionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val image = view.findViewById<ImageView>(R.id.AdoptionItem_ImageView_Image)
            val pet = view.findViewById<TextView>(R.id.AdoptionItem_TextView_Pet)
            val user = view.findViewById<TextView>(R.id.AdoptionItem_TextView_User)
            val status = view.findViewById<TextView>(R.id.AdoptionItem_TextView_Status)
            val btnFinish = view.findViewById<Button>(R.id.AdoptionItem_Button_Finish)
            val btnCancel = view.findViewById<Button>(R.id.AdoptionItem_Button_Cancel)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdoptionViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_adoption, parent, false)
            return AdoptionViewHolder(view)
        }

        override fun onBindViewHolder(holder: AdoptionViewHolder, position: Int) {
            val adoption = adoptions[position]
            val username = usernames[adoption.userId]
            val pet = pets.find { it.petId == adoption.petId }

            val image: Bitmap = images[pet?.petId] ?: Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            holder.image.setImageBitmap(image)

            holder.pet.text = pet?.name ?: "Unknown"
            holder.user.text = username?.username ?: "Unknown"
            holder.status.text = adoption.status.toString() ?: "Unknown"

            holder.btnFinish.setOnClickListener {
                lifecycleScope.launch {
                    val response = adoptionService.createAdoption(AdoptionSubmitDto(adoption.petId, adoption.userId, AdoptionStatus.ADOPTED))
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Adoption finished", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Error finishing adoption", Toast.LENGTH_SHORT).show()
                    }
                    fetchAndDisplayAdoptions()
                }
            }

            holder.btnCancel.setOnClickListener {
                lifecycleScope.launch {
                    val response = adoptionService.createAdoption(AdoptionSubmitDto(adoption.petId, adoption.userId, AdoptionStatus.CANCELLED))
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Adoption cancelled", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Error cancelling adoption", Toast.LENGTH_SHORT).show()
                    }
                    fetchAndDisplayAdoptions()
                }
            }
        }

        override fun getItemCount() = adoptions.size
    }


    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.Adoptions_RecyclerView)
    }

    // Fetch pets and images for pets
    private suspend fun fetchAdoptions(): List<AdoptionDto> {
        var adoptionList: List<AdoptionDto> = emptyList()
        try {
            adoptionList = adoptionService.getAdoptions()
        } catch (e: Exception) {
            Log.e("AdoptionsFragment", "Error fetching adoptions", e)
        }
        return adoptionList
    }
    private suspend fun fetchUsernames(adoptions: List<AdoptionDto>): MutableMap<String, UserNameDto> {
        var usernameMap: MutableMap<String, UserNameDto> = mutableMapOf()
        for (adoption in adoptions) {
            try {
                val username = userService.getUserName(adoption.userId)
                usernameMap[adoption.userId] = username
            } catch (e: Exception) {
                Log.e("AdoptionsFragment", "Error fetching username for user: [${adoption.userId}]")
            }
        }
        return usernameMap
    }
    private suspend fun fetchPets(): List<PetDto> {
        var petList: List<PetDto> = emptyList()
        try {
            petList = petService.getPets()
        } catch (e: Exception) {
            Log.e("AdoptionsFragment", "Error fetching pets", e)
            throw e
        }
        return petList
    }
    private suspend fun fetchImagesForPets(petList: List<PetDto>): MutableMap<Int, Bitmap> {
        val imageMap: MutableMap<Int, Bitmap> = mutableMapOf()
        for (pet in petList) {
            try {
                val fullUrl = pet.imageUrl
                val startIndex = fullUrl.indexOf("/uploads")
                val shortUrl = fullUrl.substring(startIndex)

                val image = mediaService.getMedia(shortUrl)

                val inputStream = image.byteStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                imageMap[pet.petId] = bitmap
            } catch (e: Exception) {
                Log.e("AdoptionsFragment", "Error fetching image for pet: [${pet.petId}] ${pet.name}")
            }
        }
        return imageMap
    }

    companion object {
        @JvmStatic
        fun newInstance() {
            AdoptionsFragment()
        }
    }
}