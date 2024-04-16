package app.animalshelter

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.icu.util.Calendar
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.animalshelter.api.ApiService
import app.animalshelter.api.BreedDto
import app.animalshelter.api.PetDto
import app.animalshelter.api.Sex
import app.animalshelter.api.Status
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.Period
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PetsFragment : Fragment(), DatePickerFragment.DatePickerCallback {

    // Form and RecyclerView
    private var recyclerView: RecyclerView? = null
    private var form: LinearLayout? = null
    private var textView: TextView? = null

    // Dialog
    private var dialog: AlertDialog.Builder? = null

    // Buttons (initially null, will be initialized in initViews)
    private var btnSubmit: androidx.appcompat.widget.AppCompatButton? = null
    private var btnCancel: androidx.appcompat.widget.AppCompatButton? = null
    private var btnAdd: androidx.appcompat.widget.AppCompatButton? = null
    private var btnMakeImg: androidx.appcompat.widget.AppCompatButton? = null
    private var btnDeleteImg: androidx.appcompat.widget.AppCompatButton? = null
    private var btnDatePicker: androidx.appcompat.widget.AppCompatButton? = null

    // Form fields (initially null, will be initialized in initViews)
    private var petName: EditText? = null
    private var petDescription: EditText? = null
    private var petBirthDate: EditText? = null
    private var petImageUrls: AutoCompleteTextView? = null
    private var petBreed: AutoCompleteTextView? = null
    private var petSex: AutoCompleteTextView? = null
    private var petStatus: AutoCompleteTextView? = null

    private var selectedImage: ImageView? = null

    // Form field adapters
    private var sexAdapter: ArrayAdapter<String>? = null
    private var statusAdapter: ArrayAdapter<String>? = null
    private var breedAdapter: ArrayAdapter<String>? = null
    private var imageUrlsAdapter: ArrayAdapter<String>? = null

    // Current event and pet for editing
    private enum class PetFragmentEvent { LIST_PETS, ADD_PET, EDIT_PET }
    private lateinit var currentEvent: PetFragmentEvent
    private var currentPet: PetDto? = null

    // API service
    private lateinit var apiSrv: ApiService

    // Override for DatePickerCallback
    override fun onDateSelected(year: Int, month: Int, day: Int) {
        val zonedDateTime = ZonedDateTime.of(year, month + 1, day, 0, 0, 0, 0, ZonedDateTime.now().zone)
        val localDate = zonedDateTime.toLocalDate()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val formattedDate = localDate.format(formatter)
        petBirthDate?.setText(formattedDate)
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        resultLauncher.launch(takePictureIntent)
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            val bundle: Bundle = Bundle(data?.extras)
            val imageBitmap = data?.extras?.get("data") as Bitmap
            val imageFile = File(context?.cacheDir, "${currentPet?.name}.jpg")
            val fileOutputStream = FileOutputStream(imageFile)
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
            val imageUri = Uri.fromFile(imageFile)
            lifecycleScope.launch {
                val imageUrl = apiSrv.uploadImage(imageUri)

                if (imageUrl == null) {
                    Toast.makeText(context, "Nem sikerült feltölteni a képet", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val updatedImageUrls = currentPet?.imageUrls?.toMutableList()
                updatedImageUrls?.add(imageUrl)
                currentPet?.imageUrls = updatedImageUrls
                setImageUrlsAdapter(currentPet?.imageUrls ?: listOf())
            }
        }
    }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_pets, container, false)
        apiSrv = ApiService(requireContext())
        initViews(view)
        currentEvent = PetFragmentEvent.LIST_PETS
        setEvent(currentEvent)

        // Cancel and add buttons (using setEvent)
        btnCancel?.setOnClickListener {
            currentEvent = PetFragmentEvent.LIST_PETS
            setEvent(currentEvent)
        }
        btnAdd?.setOnClickListener {
            currentEvent = PetFragmentEvent.ADD_PET
            setEvent(currentEvent)
        }

        // Make picture with camera and upload it
        btnMakeImg?.setOnClickListener {
            dispatchTakePictureIntent()
        }

        // Delete selected image url from list
        btnDeleteImg?.setOnClickListener {
            val imageUrl = petImageUrls?.text.toString()
            val updatedImageUrls = currentPet?.imageUrls?.toMutableList()
            updatedImageUrls?.remove(imageUrl)
            currentPet?.imageUrls = updatedImageUrls
            setImageUrlsAdapter(updatedImageUrls ?: listOf())
        }

        petImageUrls?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // Code here will execute before the text changes
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // Toast.makeText(context, "Text changed to $s", Toast.LENGTH_SHORT).show()
            }

            override fun afterTextChanged(s: Editable) {
                lifecycleScope.launch {
                    selectedImage?.setImageBitmap(apiSrv.fetchImage(s.toString()))
                }
            }
        })

        // Open date picker dialog
        btnDatePicker?.setOnClickListener {
            val datePicker = DatePickerFragment()
            datePicker.callback = this
            datePicker.show(parentFragmentManager, "datePicker")
        }

        // Submit form (add or edit pet)
        btnSubmit?.setOnClickListener {
            Log.i("PetsFragment", "Submit button clicked. Executing event: $currentEvent")
            when (currentEvent) {
                PetFragmentEvent.ADD_PET -> {
                    lifecycleScope.launch {
                        val newPet = apiSrv.createPet(getFormValues())
                        if (newPet != null) {
                            Toast.makeText(context, "Állat hozzáadva", Toast.LENGTH_SHORT).show()
                            setEvent(PetFragmentEvent.LIST_PETS)
                        } else {
                            Log.e("PetsFragment", "Error adding pet")
                            Toast.makeText(context, "Nem sikerült hozzáadni az állatot: $newPet.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                PetFragmentEvent.EDIT_PET -> {
                    lifecycleScope.launch {
                        val dto = getFormValues()
                        val updatedPet = apiSrv.updatePet(dto.petId, dto)
                        if (updatedPet != null) {
                            Log.i("PetsFragment", "Pet updated: [${updatedPet.petId}] ${updatedPet.name}")
                            Toast.makeText(context, "Állat szerkesztve", Toast.LENGTH_SHORT).show()
                            setEvent(PetFragmentEvent.LIST_PETS)
                        } else {
                            Log.e("PetsFragment", "Error updating pet")
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

        Log.i("PetsFragment", "Fetching pets")
        val petList: List<PetDto> = apiSrv.fetchPets()

        if (petList.isEmpty()) {
            textView?.text = "Nincsenek állatok."
            textView?.visibility = View.VISIBLE
            return
        }

        Log.i("PetsFragment", "Fetching pet images")
        val imageMap: MutableMap<String, Bitmap> = apiSrv.fetchImagesForPets(petList)

        Log.i("PetsFragment", "Fetching breeds")
        val breedList: List<BreedDto> = apiSrv.fetchBreeds()

        Log.i("PetsFragment", "Setting up RecyclerView")
        val adapter = PetAdapter(petList, imageMap, breedList)
        recyclerView?.layoutManager = LinearLayoutManager(context)
        recyclerView?.adapter = adapter
    }
    private inner class PetAdapter(private val pets: List<PetDto>, private val images: Map<String, Bitmap>, private val breeds: List<BreedDto>) : RecyclerView.Adapter<PetAdapter.PetViewHolder>() {
        inner class PetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.PetItem_TextView_Name)
            val image: ImageView = view.findViewById(R.id.PetItem_ImageView_Image)
            val description: TextView = view.findViewById(R.id.PetItem_TextView_Description)
            val data: TextView = view.findViewById(R.id.PetItem_TextView_Data)
            val btnEdit: Button = view.findViewById(R.id.PetItem_Button_Edit)
            val btnDelete: Button = view.findViewById(R.id.PetItem_Button_Delete)
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

            holder.description.text = pet.description
            val breedName = breeds.find { it.breedId == pet.breedId }?.name ?: "Unknown"
            holder.data.text = "Kor: ${age} | Faj: $breedName | Állapot: ${pet.status}"

            holder.btnEdit.setOnClickListener {
                Log.i("PetsFragment", "Edit button clicked for pet: [${pet.petId}] ${pet.name}")
                currentEvent = PetFragmentEvent.EDIT_PET
                currentPet = pet
                setEvent(currentEvent)
                lifecycleScope.launch {
                    setFormValues(pet)
                }
            }

            holder.btnDelete.setOnClickListener {
                Log.i("PetsFragment", "Delete button clicked for pet: [${pet.petId}] ${pet.name}")
                dialog?.setTitle("Törlés")?.setMessage("Biztosan törölni szeretné a kiválasztott, ${pet.name} nevű állatot?")
                    ?.setPositiveButton(R.string.btn_yes) { _, _ ->
                        lifecycleScope.launch {
                            val deletedPet = apiSrv.deletePet(pet.petId)
                            if (deletedPet != null) {
                                Log.i("PetsFragment", "Pet deleted: [${deletedPet.petId}] ${deletedPet.name}")
                                Toast.makeText(context, "Állat törölve", Toast.LENGTH_SHORT).show()
                                fetchAndDisplayPets()
                            } else {
                                Log.e("PetsFragment", "Error deleting pet")
                                Toast.makeText(
                                    context,
                                    "Nem sikerült törölni az állatot (${pet.name})",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    ?.setNegativeButton(R.string.btn_no) { _, _ -> }
                    ?.show()
            }
        }

        override fun getItemCount() = pets.size

        private fun getAgeFromBirthDate(birthDate: String): Int {
            val birthDateTime = ZonedDateTime.parse(birthDate)
            val birthDate = birthDateTime.toLocalDate()
            val now = LocalDate.now()
            return Period.between(birthDate, now).years
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
        petImageUrls?.setText("")
        petBreed?.setText("")
        petSex?.setText("")
        petStatus?.setText("")
    }
    private suspend fun setFormValues(pet: PetDto) {
        petName?.setText(pet.name)
        petDescription?.setText(pet.description)

        val zonedDateTime = ZonedDateTime.parse(pet.birthDate)
        val localDate = zonedDateTime.toLocalDate()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val formattedDate = localDate.format(formatter)
        petBirthDate?.setText(formattedDate)

        setImageUrlsAdapter(pet.imageUrls ?: listOf())

        petSex?.setText(pet.sex.description, false)
        petStatus?.setText(pet.status.description, false)

        val breedId = pet.breedId
        val breed = apiSrv.fetchBreed(breedId)
        petBreed?.setText(breed?.name ?: "", false)
    }
    private suspend fun getFormValues(petId: String = ""): PetDto {
        val name = petName?.text.toString()
        val description = petDescription?.text.toString()
        val birthDate = petBirthDate?.text.toString()

        val sex = Sex.entries.find { it.description == petSex?.text.toString() } ?: Sex.OTHER
        val status = Status.entries.find { it.description == petStatus?.text.toString() } ?: Status.UNKNOWN

        val breedId = apiSrv.fetchBreeds().find { it.name == petBreed?.text.toString() }?.breedId ?: ""

        return PetDto(
            petId = currentPet?.petId ?: petId,
            name = name,
            description = description,
            birthDate = birthDate,
            imageUrls = currentPet?.imageUrls ?: listOf(),
            breedId = breedId,
            sex = sex,
            status = status
        )
    }

    // Set image urls adapter
    private fun setImageUrlsAdapter(imageUrls: List<String>) {
        imageUrlsAdapter = ArrayAdapter(requireContext(), R.layout.item_list, imageUrls)
        petImageUrls?.setAdapter(imageUrlsAdapter)
        petImageUrls?.setText(imageUrls.lastOrNull() ?: "", false)
    }

    // Initialize views
    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.Pets_RecyclerView)
        form = view.findViewById(R.id.Pets_LinearLayout)
        textView = view.findViewById(R.id.Pets_TextView)

        dialog = AlertDialog.Builder(context)

        btnSubmit = view.findViewById(R.id.Pets_Button_Submit)
        btnCancel = view.findViewById(R.id.Pets_Button_BackToList)
        btnAdd = view.findViewById(R.id.Pets_Button_Add)
        btnMakeImg = view.findViewById(R.id.Pets_Button_MakeImage)
        btnDeleteImg = view.findViewById(R.id.Pets_Button_DeleteImage)
        btnDatePicker = view.findViewById(R.id.Pets_Button_DatePicker)

        petName = view.findViewById(R.id.Pets_EditText_Name)
        petDescription = view.findViewById(R.id.Pets_EditText_Description)
        petBirthDate = view.findViewById(R.id.Pets_EditText_BirthDate)
        petImageUrls = view.findViewById(R.id.Pets_AutoCompleteTextView_ImageUrl)
        petBreed = view.findViewById(R.id.Pets_AutoCompleteTextView_BreedId)
        petSex = view.findViewById(R.id.Pets_AutoCompleteTextView_Sex)
        petStatus = view.findViewById(R.id.Pets_AutoCompleteTextView_Status)

        selectedImage = view.findViewById(R.id.Pets_ImageView_Image)

        sexAdapter = ArrayAdapter(requireContext(), R.layout.item_list, Sex.entries.map { it.description })
        petSex?.setAdapter(sexAdapter)

        statusAdapter = ArrayAdapter(requireContext(), R.layout.item_list, Status.entries.filter { it != Status.ADOPTING && it != Status.ADOPTED }.map { it.description })
        petStatus?.setAdapter(statusAdapter)

        lifecycleScope.launch {
            initBreedAdapter()
        }
    }
    private suspend fun initBreedAdapter() {
        val breeds = apiSrv.fetchBreeds()
        val breedList = breeds.map { it.name }
        breedAdapter = ArrayAdapter(requireContext(), R.layout.item_list, breedList)
        petBreed?.setAdapter(breedAdapter)
    }
}
