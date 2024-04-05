package app.animalshelter

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.animalshelter.api.ApiService
import app.animalshelter.api.BreedDto
import app.animalshelter.api.SpeciesDto
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class BreedsFragment : Fragment() {

    private var recyclerView: RecyclerView? = null
    private var form: LinearLayout? = null
    private var addEditButtons: LinearLayout? = null
    private var textView: TextView? = null

    private var btnSubmit: Button? = null
    private var btnCancel: Button? = null
    private var btnAddBreed: Button? = null
    private var btnAddSpecies: Button? = null

    private var psName: EditText? = null
    private var psDescription: EditText? = null
    private var petSpecies: AutoCompleteTextView? = null
    private var petSpeciesLayout: TextInputLayout? = null

    private var speciesAdapter: ArrayAdapter<String>? = null

    private enum class BreedsFragmentEvent { LIST_BREEDS, ADD_BREED, EDIT_BREED, ADD_SPECIES, EDIT_SPECIES }
    private lateinit var currentEvent: BreedsFragmentEvent
    private var currentBreedOrSpeciesId: String = ""

    private lateinit var apiSrv: ApiService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_breeds, container, false)
        apiSrv = ApiService(requireContext())
        initViews(view)
        currentEvent = BreedsFragmentEvent.LIST_BREEDS
        setEvent(currentEvent)

        btnAddBreed?.setOnClickListener {
            currentEvent = BreedsFragmentEvent.ADD_BREED
            setEvent(currentEvent)
        }

        btnAddSpecies?.setOnClickListener {
            currentEvent = BreedsFragmentEvent.ADD_SPECIES
            setEvent(currentEvent)
        }

        btnCancel?.setOnClickListener {
            currentEvent = BreedsFragmentEvent.LIST_BREEDS
            setEvent(currentEvent)
        }

        btnSubmit?.setOnClickListener {
            currentEvent = BreedsFragmentEvent.LIST_BREEDS
            setEvent(currentEvent)
        }

        btnSubmit?.setOnClickListener {
            if (!checkInputs()) {
                return@setOnClickListener
            }
            lifecycleScope.launch {
                val breedOrSpecies = getFormValues()
                if (breedOrSpecies == null) {
                    Log.e("BreedsFragment", "Form values are null")
                }
                else {
                    when (currentEvent) {
                        BreedsFragmentEvent.ADD_BREED -> {
                            val breed = breedOrSpecies as BreedDto
                            val newBreed = apiSrv.createBreed(breed)
                            if (newBreed != null) {
                                Log.i("BreedsFragment", "Created breed: ${newBreed.breedId}")
                                return@launch
                            }
                            Log.i("BreedsFragment", "Created breed: ${newBreed?.breedId}")
                            currentEvent = BreedsFragmentEvent.LIST_BREEDS
                            setEvent(currentEvent)
                        }
                        /* BreedsFragmentEvent.EDIT_BREED -> {
                        val breed = breedOrSpecies as BreedDto
                        val updatedBreed = apiSrv.updateBreed(breed.breedId, breed)
                        Log.i("BreedsFragment", "Updated breed: ${updatedBreed.breedId}")
                    }*/
                    BreedsFragmentEvent.ADD_SPECIES -> {
                        val species = breedOrSpecies as SpeciesDto
                        val newSpecies = apiSrv.createSpecies(species)
                        if (newSpecies != null) {
                            Log.i("BreedsFragment", "Created breed: ${newSpecies.speciesId}")
                            return@launch
                        }
                        Log.i("BreedsFragment", "Created breed: ${newSpecies?.speciesId}")
                        currentEvent = BreedsFragmentEvent.LIST_BREEDS
                        setEvent(currentEvent)
                    }/*
                        BreedsFragmentEvent.EDIT_SPECIES -> {
                        val species = breedOrSpecies as SpeciesDto
                        val updatedSpecies = apiSrv.updateSpecies(species.speciesId, species)
                        Log.i("BreedsFragment", "Updated species: ${updatedSpecies.speciesId}")
                    }*/
                        else -> {
                            Log.e("BreedsFragment", "Unknown event: $currentEvent")
                        }
                    }
                }
            }
        }

        return view
    }

    private suspend fun fetchAndDisplayBreeds() {
        Log.i("BreedsFragment", "Fetching breeds...")
        val breedList = apiSrv.fetchBreeds()
        Log.i("BreedsFragment", "Fetched breeds: ${breedList.size}")

        Log.i("BreedsFragment", "Fetching species...")
        val speciesList = apiSrv.fetchSpecies()
        Log.i("BreedsFragment", "Fetched species: ${speciesList.size}")

        if (speciesList.isEmpty() || breedList.isEmpty()) {
            Log.e("BreedsFragment", "No breeds or species found")
            Toast.makeText(context, "Nem sikerült lekérni a fajokat", Toast.LENGTH_SHORT).show()
            textView?.text = "Nincsenek fajok."
            textView?.visibility = View.VISIBLE
            return
        }

        Log.i("BreedsFragment", "Setting up RecyclerView")
        val adapter = BreedAdapter(breedList, speciesList)
        recyclerView?.layoutManager = LinearLayoutManager(context)
        recyclerView?.adapter = adapter
    }

    class BreedAdapter(private val breeds: List<BreedDto>, private val species: List<SpeciesDto>) : RecyclerView.Adapter<BreedAdapter.BreedViewHolder>() {
        class BreedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.BreedItem_TextView_Name)
            val species: TextView = view.findViewById(R.id.BreedItem_TextView_Species)
            val description: TextView = view.findViewById(R.id.BreedItem_TextView_Description)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BreedViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_breed, parent, false)
            return BreedViewHolder(view)
        }

        override fun onBindViewHolder(holder: BreedViewHolder, position: Int) {
            val breed = breeds[position]
            Log.i("BreedsFragment", "Binding breed: ${breed.name} wit species: ${breed.speciesId}")
            holder.name.text = breed.name
            holder.species.text = species.find { it.speciesId == breed.speciesId }?.name ?: "Unknown"
            holder.description.text = breed.description
        }

        override fun getItemCount() = breeds.size
    }

    private fun setEvent(event: BreedsFragmentEvent) {
        Log.i("BreedsFragment", "Setting event to $event")
        when (event) {
            BreedsFragmentEvent.LIST_BREEDS -> {
                form?.visibility = View.GONE
                recyclerView?.visibility = View.VISIBLE
                addEditButtons?.visibility = View.VISIBLE

                lifecycleScope.launch {
                    fetchAndDisplayBreeds()
                }
            }
            BreedsFragmentEvent.ADD_BREED -> {
                form?.visibility = View.VISIBLE
                recyclerView?.visibility = View.GONE
                addEditButtons?.visibility = View.GONE
                petSpeciesLayout?.visibility = View.VISIBLE

                resetFormValues()

                btnSubmit?.text = "Breed hozzáadása"

                lifecycleScope.launch {
                    initSpeciesAdapter()
                }
            }
            BreedsFragmentEvent.EDIT_BREED -> {
                form?.visibility = View.VISIBLE
                recyclerView?.visibility = View.GONE
                addEditButtons?.visibility = View.GONE
                petSpeciesLayout?.visibility = View.VISIBLE

                btnSubmit?.text = "Breed módositása"

                lifecycleScope.launch {
                    initSpeciesAdapter()
                }
            }
            BreedsFragmentEvent.ADD_SPECIES -> {
                form?.visibility = View.VISIBLE
                recyclerView?.visibility = View.GONE
                addEditButtons?.visibility = View.GONE
                petSpeciesLayout?.visibility = View.GONE

                resetFormValues()

                btnSubmit?.text = "Species hozzáadása"
            }
            BreedsFragmentEvent.EDIT_SPECIES -> {
                form?.visibility = View.VISIBLE
                recyclerView?.visibility = View.GONE
                addEditButtons?.visibility = View.GONE
                petSpeciesLayout?.visibility = View.GONE

                btnSubmit?.text = "Species módositása"
            }
        }
    }

    private fun resetFormValues() {
        psName?.setText("")
        psDescription?.setText("")
        petSpecies?.setText("")
    }
    private suspend fun getFormValues(): Any? {
        val name = psName?.text.toString()
        val description = psDescription?.text.toString()
        val speciesId = apiSrv.fetchSpecies().find { it.name == petSpecies?.text.toString() }?.speciesId ?: ""

        return when (currentEvent) {
            BreedsFragmentEvent.ADD_BREED, BreedsFragmentEvent.EDIT_BREED -> BreedDto(currentBreedOrSpeciesId, name, description, speciesId)
            BreedsFragmentEvent.ADD_SPECIES, BreedsFragmentEvent.EDIT_SPECIES -> SpeciesDto(currentBreedOrSpeciesId, name, description)
            else -> null
        }
    }
    private fun setFormValues(breedOrSpecies: Any) {
        when (breedOrSpecies) {
            is BreedDto -> {
                psName?.setText(breedOrSpecies.name)
                psDescription?.setText(breedOrSpecies.description)
                petSpecies?.setText(breedOrSpecies.speciesId)
            }
            is SpeciesDto -> {
                psName?.setText(breedOrSpecies.name)
                psDescription?.setText(breedOrSpecies.description)
            }
        }
    }

    private fun checkInputs(): Boolean {
        var valid = true

        if (psName?.text.toString().isEmpty()) {
            psName?.error = "Név megadása kötelező"
            valid = false
        }

        if (psDescription?.text.toString().isEmpty()) {
            psDescription?.error = "Leírás megadása kötelező"
            valid = false
        }

        if (currentEvent == BreedsFragmentEvent.ADD_BREED || currentEvent == BreedsFragmentEvent.EDIT_BREED) {
            if (petSpecies?.text.toString().isEmpty()) {
                petSpecies?.error = "Faj megadása kötelező"
                valid = false
            }
        }

        return valid
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.Breeds_RecyclerView)
        form = view.findViewById(R.id.Breeds_LinearLayout_Form)
        addEditButtons = view.findViewById(R.id.Breeds_LinearLayout_AddButtons)
        textView = view.findViewById(R.id.Breeds_TextView)

        btnSubmit = view.findViewById(R.id.Breeds_Button_Submit)
        btnCancel = view.findViewById(R.id.Breeds_Button_BackToList)
        btnAddBreed = view.findViewById(R.id.Breeds_Button_AddBreed)
        btnAddSpecies = view.findViewById(R.id.Breeds_Button_AddSpecies)

        psName = view.findViewById(R.id.Breeds_EditText_Name)
        psDescription = view.findViewById(R.id.Breeds_EditText_Description)
        petSpecies = view.findViewById(R.id.Breeds_AutoCompleteTextView_Species)
        petSpeciesLayout = view.findViewById(R.id.Breeds_TextInputLayout_Species)
    }

    private suspend fun initSpeciesAdapter() {
        val speciesList = apiSrv.fetchSpecies()
        val speciesNames = speciesList.map { it.name }
        speciesAdapter = ArrayAdapter(requireContext(), R.layout.item_list, speciesNames)
        petSpecies?.setAdapter(speciesAdapter)
    }
}