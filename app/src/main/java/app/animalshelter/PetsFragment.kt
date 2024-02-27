package app.animalshelter

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.animalshelter.ApiService.Breed
import app.animalshelter.ApiService.Media
import app.animalshelter.ApiService.Pet
import app.animalshelter.ApiService.PetDto
import app.animalshelter.ApiService.RetrofitService
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import java.time.ZonedDateTime

class PetsFragment : Fragment() {

    private var recyclerView: RecyclerView? = null
    private var form: LinearLayout? = null

    private var btnSubmit: Button? = null
    private var btnCancel: Button? = null

    private var petName: EditText? = null
    private var petDescription: EditText? = null
    private var petBirthDate: EditText? = null
    private var petImageUrl: EditText? = null
    private var petBreed: AutoCompleteTextView? = null
    private var petSex: AutoCompleteTextView? = null
    private var petStatus: AutoCompleteTextView? = null

    private var dialog: AlertDialog.Builder? = null

    private var currentEvent: PetFragmentEvent = PetFragmentEvent.LIST_PETS
    private var currentPet: PetDto? = null

    private val petService = RetrofitService.getRetrofitService().create(Pet::class.java)
    private val breedService = RetrofitService.getRetrofitService().create(Breed::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pets, container, false)
        initViews(view)
        setEvent(currentEvent)

        btnCancel?.setOnClickListener {
            currentEvent = PetFragmentEvent.LIST_PETS
            setEvent(currentEvent)
        }

        return view
    }

    private suspend fun fetchAndDisplayPets() {
        RetrofitService.cookieJar.printCookiesToLog()

        Log.i("PetsFragment", "Fetching pets")
        var petList: List<PetDto> = emptyList()
        try {
            petList = petService.getPets()
        } catch (e: Exception) {
            Toast.makeText(context, "Error fetching pets", Toast.LENGTH_SHORT).show()
            Log.e("PetsFragment", "Error fetching pets", e)
            return
        }

        Log.i("PetsFragment", "Fetching pet images")
        val imageMap: MutableMap<Int, Bitmap> = fetchImagesForPets(petList)

        Log.i("PetsFragment", "Fetching breeds")
        val breedMap: Map<Int, String> = fetchBreedList()

        Log.i("PetsFragment", "Setting up RecyclerView")
        val adapter = PetAdapter(petList, imageMap, breedMap)
        recyclerView?.layoutManager = LinearLayoutManager(context)
        recyclerView?.adapter = adapter
    }

    private suspend fun fetchImagesForPets(petList: List<PetDto>): MutableMap<Int, Bitmap> {
        val imageMap: MutableMap<Int, Bitmap> = mutableMapOf()
        for (pet in petList) {
            try {
                val fullUrl = pet.imageUrl
                val startIndex = fullUrl.indexOf("/uploads")
                val shortUrl = fullUrl.substring(startIndex)

                val mediaService = RetrofitService.getRetrofitService().create(Media::class.java)
                val image = mediaService.getMedia(shortUrl)

                val inputStream = image.byteStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                imageMap[pet.petId] = bitmap
            } catch (e: Exception) {
                Log.e("PetsFragment", "Error fetching image for pet: [${pet.petId}] ${pet.name}")
            }
        }
        return imageMap
    }

    private suspend fun fetchBreedList(): Map<Int, String> {
        val breedMap: MutableMap<Int, String> = mutableMapOf()
        try {
            val breeds = breedService.getBreeds()
            for (breed in breeds) {
                breedMap[breed.breedId] = breed.name
            }
        } catch (e: Exception) {
            Log.e("PetsFragment", "Error fetching breeds", e)
            Toast.makeText(context, "Hiba a fajok lekérése közben", Toast.LENGTH_SHORT).show()
        }
        return breedMap
    }

    private inner class PetAdapter(private val pets: List<PetDto>, private val images: Map<Int, Bitmap>, private val breeds: Map<Int, String>) : RecyclerView.Adapter<PetAdapter.PetViewHolder>() {
        inner class PetViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.PetItem_TextView_Name)
            val image: ImageView = view.findViewById(R.id.PetItem_ImageView_Image)
            val otherData = view.findViewById<TextView>(R.id.PetItem_TextView_Data)
            val editButton = view.findViewById<TextView>(R.id.PetItem_Button_Edit)
            val deleteButton = view.findViewById<TextView>(R.id.PetItem_Button_Delete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.pet_item, parent, false)
            return PetViewHolder(view)
        }

        override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
            val pet = pets[position]
            val age = getAgeFromBirthDate(pet.birthDate)

            holder.name.text = pet.name

            val image: Bitmap = images[pet.petId] ?: Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            holder.image.setImageBitmap(image)

            holder.otherData.text = "${pet.description ?: "No description"} | Age: ${age ?: "Unknown"} | Breed: ${breeds[pet.breedId] ?: pet.breedId} | Status: ${pet.status ?: "Unknown status"}"

            holder.editButton.setOnClickListener {
                Log.i("PetsFragment", "Edit button clicked for pet: [${pet.petId}] ${pet.name}")
                currentEvent = PetFragmentEvent.EDIT_PET
                currentPet = pet
                setEvent(currentEvent)
            }

            holder.deleteButton.setOnClickListener {
                Log.i("PetsFragment", "Delete button clicked for pet: [${pet.petId}] ${pet.name}")
                dialog?.setTitle("Törlés")?.setMessage("Biztosan törölni szeretné a kiválasztott állatot?")
                    ?.setPositiveButton("Igen") { _, _ ->
                        lifecycleScope.launch {
                            deletePet(pet)
                        }
                    }
                    ?.setNegativeButton("Nem") { _, _ -> }
                    ?.show()
            }
        }

        override fun getItemCount() = pets.size

        private fun getAgeFromBirthDate(birthDate: String): Int {
            val birthDateTime = ZonedDateTime.parse(birthDate)
            val birthDate = birthDateTime.toLocalDate()
            val now = LocalDate.now()
            val age = Period.between(birthDate, now).years
            return age
        }

        private suspend fun deletePet(pet: PetDto) {
            try {
                petService.deletePet(pet.petId)
                Log.i("PetsFragment", "Pet deleted: [${pet.petId}] ${pet.name}")
                Toast.makeText(context, "Állat törölve", Toast.LENGTH_SHORT).show()
                fetchAndDisplayPets()
            } catch (e: Exception) {
                Log.e("PetsFragment", "Error deleting pet", e)
                Toast.makeText(context, "Nem sikerült törölni az állatot (${pet.name})", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setEvent(event: PetFragmentEvent) {
        when (event) {
            PetFragmentEvent.LIST_PETS -> {
                Log.i("PetsFragment", "Setting event to $event")

                form?.visibility = View.GONE
                recyclerView?.visibility = View.VISIBLE

                lifecycleScope.launch {
                    fetchAndDisplayPets()
                }
            }
            PetFragmentEvent.ADD_PET -> {
                Log.i("PetsFragment", "Setting event to $event")

                form?.visibility = View.VISIBLE
                recyclerView?.visibility = View.GONE

                btnSubmit?.text = "Felvétel"
            }
            PetFragmentEvent.EDIT_PET -> {
                Log.i("PetsFragment", "Setting event to $event")

                form?.visibility = View.VISIBLE
                recyclerView?.visibility = View.GONE

                btnSubmit?.text = "Szerkesztés"
            }
        }
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.Pets_RecyclerView)
        form = view.findViewById(R.id.Pets_LinearLayout)

        btnSubmit = view.findViewById(R.id.Pets_Button_Submit)
        btnCancel = view.findViewById(R.id.Pets_Button_BackToList)

        petName = view.findViewById(R.id.Pets_EditText_Name)
        petDescription = view.findViewById(R.id.Pets_EditText_Description)
        petBirthDate = view.findViewById(R.id.Pets_EditText_BirthDate)
        petImageUrl = view.findViewById(R.id.Pets_EditText_ImageUrl)
        petBreed = view.findViewById(R.id.Pets_AutoCompleteTextView_BreedId)
        petSex = view.findViewById(R.id.Pets_AutoCompleteTextView_Sex)
        petStatus = view.findViewById(R.id.Pets_AutoCompleteTextView_Status)

        dialog = AlertDialog.Builder(context)
    }

    private enum class PetFragmentEvent {
        LIST_PETS, ADD_PET, EDIT_PET
    }

    companion object {
        @JvmStatic
        fun newInstance() {
            PetsFragment()
        }
    }
}