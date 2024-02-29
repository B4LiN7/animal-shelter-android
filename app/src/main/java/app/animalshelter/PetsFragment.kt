package app.animalshelter

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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
import app.animalshelter.ApiService.Sex
import app.animalshelter.ApiService.Status
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.Period
import java.time.ZonedDateTime

class PetsFragment : Fragment() {

    // Form and RecyclerView
    private var recyclerView: RecyclerView? = null
    private var form: LinearLayout? = null

    // Dialog
    private var dialog: AlertDialog.Builder? = null

    // Buttons (initially null, will be initialized in initViews)
    private var btnSubmit: Button? = null
    private var btnCancel: Button? = null
    private var btnAdd: Button? = null
    private var btnMakeImg: Button? = null

    // Form fields (initially null, will be initialized in initViews)
    private var petName: EditText? = null
    private var petDescription: EditText? = null
    private var petBirthDate: EditText? = null
    private var petImageUrl: EditText? = null
    private var petBreed: AutoCompleteTextView? = null
    private var petSex: AutoCompleteTextView? = null
    private var petStatus: AutoCompleteTextView? = null

    // Form field adapters
    private var sexAdapter: ArrayAdapter<String>? = null
    private var statusAdapter: ArrayAdapter<String>? = null
    private var breedAdapter: ArrayAdapter<String>? = null

    // Current event and pet for editing
    private enum class PetFragmentEvent { LIST_PETS, ADD_PET, EDIT_PET }
    private var currentEvent: PetFragmentEvent = PetFragmentEvent.LIST_PETS
    private var currentPet: PetDto? = null

    // Services for fetching data
    private val petService = RetrofitService.getRetrofitService().create(Pet::class.java)
    private val breedService = RetrofitService.getRetrofitService().create(Breed::class.java)
    private val mediaService = RetrofitService.getRetrofitService().create(Media::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_pets, container, false)
        initViews(view)
        setEvent(currentEvent)

        // Return to list
        btnCancel?.setOnClickListener {
            currentEvent = PetFragmentEvent.LIST_PETS
            setEvent(currentEvent)
        }

        // Open form for adding new pet
        btnAdd?.setOnClickListener {
            currentEvent = PetFragmentEvent.ADD_PET
            setEvent(currentEvent)
        }

        btnMakeImg?.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val width = 100
                    val height = 100
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

                    // Convert bitmap to file
                    val file = File.createTempFile("temp", ".png", context?.cacheDir ?: File("/"))
                    file.deleteOnExit()
                    val fileOutputStream = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                    fileOutputStream.flush()
                    fileOutputStream.close()

                    // Create RequestBody and MultipartBody.Part
                    val requestFile = file.asRequestBody("image/png".toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

                    // Upload the file
                    val response = mediaService.postMedia(body)
                    Log.i("PetsFragment", "Image uploaded: ${response}")
                } catch (e: Exception) {
                    Log.e("PetsFragment", "Error uploading image", e)
                }
            }

        }

        // Submit form (add or edit pet)
        btnSubmit?.setOnClickListener {
            Log.i("PetsFragment", "Submit button clicked. Executing event: $currentEvent")
            when (currentEvent) {
                PetFragmentEvent.ADD_PET -> {
                    lifecycleScope.launch {
                        try {
                            val dto = getFormValues()
                            petService.createPet(dto)
                            Log.i("PetsFragment", "Pet added: [${dto.petId}] ${dto.name}")
                            Toast.makeText(context, "Állat hozzáadva", Toast.LENGTH_SHORT).show()
                            fetchAndDisplayPets()
                        } catch (e: Exception) {
                            Log.e("PetsFragment", "Error adding pet", e)
                            Toast.makeText(context, "Nem sikerült hozzáadni az állatot", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                PetFragmentEvent.EDIT_PET -> {
                    lifecycleScope.launch {
                        try {
                            val dto = getFormValues()
                            petService.updatePet(dto.petId, dto)
                            Log.i("PetsFragment", "Pet updated: [${dto.petId}] ${dto.name}")
                            Toast.makeText(context, "Állat szerkesztve", Toast.LENGTH_SHORT).show()
                            fetchAndDisplayPets()
                        } catch (e: Exception) {
                            Log.e("PetsFragment", "Error updating pet", e)
                            Toast.makeText(context, "Nem sikerült szerkeszteni az állatot", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                else -> {
                    Log.e("PetsFragment", "Invalid event: $currentEvent")
                }
            }
        }

        return view
    }

    // Fetch and display pets using PetAdapter
    private suspend fun fetchAndDisplayPets() {
        Log.i("PetsFragment", "Start fetching and displaying pets")
        RetrofitService.printCookiesToLog()

        Log.i("PetsFragment", "Fetching pets")
        val petList: List<PetDto> = fetchPets()

        Log.i("PetsFragment", "Fetching pet images")
        val imageMap: MutableMap<Int, Bitmap> = fetchImagesForPets(petList)

        Log.i("PetsFragment", "Fetching breeds")
        val breedMap: Map<Int, String> = fetchBreeds()

        Log.i("PetsFragment", "Setting up RecyclerView")
        val adapter = PetAdapter(petList, imageMap, breedMap)
        recyclerView?.layoutManager = LinearLayoutManager(context)
        recyclerView?.adapter = adapter
    }
    private inner class PetAdapter(private val pets: List<PetDto>, private val images: Map<Int, Bitmap>, private val breeds: Map<Int, String>) : RecyclerView.Adapter<PetAdapter.PetViewHolder>() {
        inner class PetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.PetItem_TextView_Name)
            val image: ImageView = view.findViewById(R.id.PetItem_ImageView_Image)
            val description: TextView = view.findViewById(R.id.PetItem_TextView_Description)
            val data = view.findViewById<TextView>(R.id.PetItem_TextView_Data)
            val editButton = view.findViewById<TextView>(R.id.PetItem_Button_Edit)
            val deleteButton = view.findViewById<TextView>(R.id.PetItem_Button_Delete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pet, parent, false)
            return PetViewHolder(view)
        }

        override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
            val pet = pets[position]
            val age = getAgeFromBirthDate(pet.birthDate)

            holder.name.text = pet.name

            val image: Bitmap = images[pet.petId] ?: Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            holder.image.setImageBitmap(image)

            holder.description.text = pet.description ?: "Nincs leírás"
            holder.data.text = "Kor: ${age ?: "Ismertelen"} | Faj: ${breeds[pet.breedId] ?: pet.breedId} | Állapot: ${pet.status ?: "Ismeretlen állapot"}"

            holder.editButton.setOnClickListener {
                Log.i("PetsFragment", "Edit button clicked for pet: [${pet.petId}] ${pet.name}")
                currentEvent = PetFragmentEvent.EDIT_PET
                currentPet = pet
                setEvent(currentEvent)
                lifecycleScope.launch {
                    setFormValues(pet)
                }
            }

            holder.deleteButton.setOnClickListener {
                Log.i("PetsFragment", "Delete button clicked for pet: [${pet.petId}] ${pet.name}")
                dialog?.setTitle("Törlés")?.setMessage("Biztosan törölni szeretné a kiválasztott, ${pet.name} nevű állatot?")
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

    // Set event
    private fun setEvent(event: PetFragmentEvent) {
        Log.i("PetsFragment", "Setting event to $event")
        when (event) {
            PetFragmentEvent.LIST_PETS -> {
                form?.visibility = View.GONE
                recyclerView?.visibility = View.VISIBLE

                btnAdd?.visibility = View.VISIBLE

                lifecycleScope.launch {
                    fetchAndDisplayPets()
                }
            }
            PetFragmentEvent.ADD_PET -> {
                form?.visibility = View.VISIBLE
                recyclerView?.visibility = View.GONE

                resetFormValues()

                btnAdd?.visibility = View.GONE
                btnSubmit?.text = "Felvétel"
            }
            PetFragmentEvent.EDIT_PET -> {
                form?.visibility = View.VISIBLE
                recyclerView?.visibility = View.GONE

                btnAdd?.visibility = View.GONE
                btnSubmit?.text = "Szerkesztés"
            }
        }
    }

    // Set and get form values
    private fun resetFormValues() {
        petName?.setText("")
        petDescription?.setText("")
        petBirthDate?.setText("")
        petImageUrl?.setText("")
        petBreed?.setText("")
        petSex?.setText("")
        petStatus?.setText("")
    }
    private suspend fun setFormValues(pet: PetDto) {
        petName?.setText(pet.name)
        petDescription?.setText(pet.description)
        petBirthDate?.setText(pet.birthDate)
        petImageUrl?.setText(pet.imageUrl)
        petSex?.setText(pet.sex.description, false)
        petStatus?.setText(pet.status.description, false)

        val breedId = pet.breedId
        val breed = fetchBreed(breedId)
        petBreed?.setText(breed, false)
    }
    private suspend fun getFormValues(): PetDto {
        val name = petName?.text.toString()
        val description = petDescription?.text.toString()
        val birthDate = petBirthDate?.text.toString()
        val imageUrl = petImageUrl?.text.toString()

        return PetDto(
            petId = currentPet?.petId ?: 0,
            name = name,
            description = description,
            birthDate = birthDate,
            imageUrl = imageUrl,
            breedId = fetchBreeds().entries.find { it.value == petBreed?.text.toString() }?.key ?: 0,
            sex = Sex.entries.find { it.description == petSex?.text.toString() } ?: Sex.OTHER,
            status = Status.entries.find { it.description == petStatus?.text.toString() } ?: Status.UNKNOWN
        )
    }

    // Initialize views
    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.Pets_RecyclerView)
        form = view.findViewById(R.id.Pets_LinearLayout)

        dialog = AlertDialog.Builder(context)

        btnSubmit = view.findViewById(R.id.Pets_Button_Submit)
        btnCancel = view.findViewById(R.id.Pets_Button_BackToList)
        btnAdd = view.findViewById(R.id.Pets_Button_Add)
        btnMakeImg = view.findViewById(R.id.Pets_Button_MakeImage)

        petName = view.findViewById(R.id.Pets_EditText_Name)
        petDescription = view.findViewById(R.id.Pets_EditText_Description)
        petBirthDate = view.findViewById(R.id.Pets_EditText_BirthDate)
        petImageUrl = view.findViewById(R.id.Pets_EditText_ImageUrl)
        petBreed = view.findViewById(R.id.Pets_AutoCompleteTextView_BreedId)
        petSex = view.findViewById(R.id.Pets_AutoCompleteTextView_Sex)
        petStatus = view.findViewById(R.id.Pets_AutoCompleteTextView_Status)

        sexAdapter = ArrayAdapter(requireContext(), R.layout.item_list, Sex.entries.map { it.description })
        petSex?.setAdapter(sexAdapter)

        statusAdapter = ArrayAdapter(requireContext(), R.layout.item_list, Status.entries.map { it.description })
        petStatus?.setAdapter(statusAdapter)

        lifecycleScope.launch {
            initBreedAdapter()
        }
    }
    private suspend fun initBreedAdapter() {
        val breeds = fetchBreeds()
        breedAdapter = ArrayAdapter(requireContext(), R.layout.item_list, breeds.values.toList())
        petBreed?.setAdapter(breedAdapter)
    }

    // Fetch pets, breeds and images for pets
    private suspend fun fetchPets(): List<PetDto> {
        var petList: List<PetDto> = emptyList()
        try {
            petList = petService.getPets()
        } catch (e: Exception) {
            Log.e("PetsFragment", "Error fetching pets", e)
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
                Log.e("PetsFragment", "Error fetching image for pet: [${pet.petId}] ${pet.name}")
            }
        }
        return imageMap
    }
    private suspend fun fetchBreeds(): Map<Int, String> {
        val breedMap: MutableMap<Int, String> = mutableMapOf()
        try {
            val breeds = breedService.getBreeds()
            for (breed in breeds) {
                breedMap[breed.breedId] = breed.name
            }
        } catch (e: Exception) {
            Log.e("PetsFragment", "Error fetching breeds", e)
        }
        return breedMap
    }
    private suspend fun fetchBreed(breedId: Int): String {
        try {
            val breed = breedService.getBreedById(breedId)
            return breed.name
        } catch (e: Exception) {
            Log.e("PetsFragment", "Error fetching breed with ID $breedId", e)
            return "Ismeretlen fajta"
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() {
            PetsFragment()
        }
    }
}