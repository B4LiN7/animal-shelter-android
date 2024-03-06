package app.animalshelter

import android.app.AlertDialog
import android.graphics.Bitmap
import android.net.Uri
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
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.animalshelter.ApiService.ApiService
import app.animalshelter.ApiService.PetDto
import app.animalshelter.ApiService.Sex
import app.animalshelter.ApiService.Status
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.time.LocalDate
import java.time.Period
import java.time.ZonedDateTime
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PetsFragment : Fragment() {

    // Form and RecyclerView
    private var recyclerView: RecyclerView? = null
    private var form: LinearLayout? = null

    // Dialog
    private var dialog: AlertDialog.Builder? = null

    // Camera
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService

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

    private lateinit var apiSrv: ApiService

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_pets, container, false)
        apiSrv = ApiService(requireContext())
        initViews(view)
        setEvent(currentEvent)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Select back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                imageCapture = ImageCapture.Builder().build()

                cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)

            } catch (exc: Exception) {
                Log.e("PetsFragment", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))

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
            try {
                val photoFile = File.createTempFile(currentPet?.name ?: "temp", ".jpg", context?.cacheDir)
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()), object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val savedUri = Uri.fromFile(photoFile)
                        lifecycleScope.launch {
                            val path = uploadImage(savedUri)
                            petImageUrl?.setText(path)
                        }
                    }
                    override fun onError(exception: ImageCaptureException) {
                        Log.e("PetsFragment", "Image capture failed", exception)
                    }
                })
            } catch (e: Exception) {
                Log.e("PetsFragment", "Error opening camera", e)
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
                            apiSrv.petInterface.createPet(dto)
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
                            apiSrv.petInterface.updatePet(dto.petId, dto)
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

    private suspend fun uploadImage(uri: Uri?): String {
        val file = File(uri?.path!!)
        val requestFile = file.asRequestBody("image/jpg".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val response = apiSrv.mediaInterface.postMedia(body)
        Log.i("PetsFragment", "Image uploaded: $response")
        return response.url
    }

    // Fetch and display pets using PetAdapter
    private suspend fun fetchAndDisplayPets() {
        Log.i("PetsFragment", "Start fetching and displaying pets")
        apiSrv.printCookiesToLog()

        Log.i("PetsFragment", "Fetching pets")
        val petList: List<PetDto> = apiSrv.fetchPets()

        Log.i("PetsFragment", "Fetching pet images")
        val imageMap: MutableMap<Int, Bitmap> = apiSrv.fetchImagesForPets(petList)

        Log.i("PetsFragment", "Fetching breeds")
        val breedMap: Map<Int, String> = apiSrv.fetchBreeds()

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
            val data: TextView = view.findViewById(R.id.PetItem_TextView_Data)
            val btnEdit: TextView = view.findViewById(R.id.PetItem_Button_Edit)
            val btnDelete: TextView = view.findViewById(R.id.PetItem_Button_Delete)
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
            holder.data.text = "Kor: ${age} | Faj: ${breeds[pet.breedId] ?: pet.breedId} | Állapot: ${pet.status}"

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
            return Period.between(birthDate, now).years
        }

        private suspend fun deletePet(pet: PetDto) {
            try {
                apiSrv.petInterface.deletePet(pet.petId)
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
        val breed = apiSrv.fetchBreed(breedId)
        petBreed?.setText(breed.name, false)
    }
    private suspend fun getFormValues(): PetDto {
        val name = petName?.text.toString()
        val description = petDescription?.text.toString()
        val birthDate = petBirthDate?.text.toString()
        val imageUrl = petImageUrl?.text.toString()

        val breedId = apiSrv.fetchBreeds().entries.find { it.value == petBreed?.text.toString() }?.key ?: 0
        return PetDto(
            petId = currentPet?.petId ?: 0,
            name = name,
            description = description,
            birthDate = birthDate,
            imageUrl = imageUrl,
            breedId = breedId,
            sex = Sex.entries.find { it.description == petSex?.text.toString() } ?: Sex.OTHER,
            status = Status.entries.find { it.description == petStatus?.text.toString() } ?: Status.UNKNOWN
        )
    }

    // Initialize views
    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.Pets_RecyclerView)
        form = view.findViewById(R.id.Pets_LinearLayout)

        dialog = AlertDialog.Builder(context)

        cameraExecutor = Executors.newSingleThreadExecutor()

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
        val breeds = apiSrv.fetchBreeds()
        breedAdapter = ArrayAdapter(requireContext(), R.layout.item_list, breeds.values.toList())
        petBreed?.setAdapter(breedAdapter)
    }
}