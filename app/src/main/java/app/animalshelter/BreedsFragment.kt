package app.animalshelter

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.animalshelter.api.ApiService
import app.animalshelter.api.BreedDto
import app.animalshelter.api.SpeciesDto
import kotlinx.coroutines.launch

class BreedsFragment : Fragment() {

    private lateinit var apiSrv: ApiService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_breeds, container, false)
        apiSrv = ApiService(requireContext())

        lifecycleScope.launch {
            val breedList = apiSrv.fetchBreeds()
            val speciesList = apiSrv.fetchSpecies()
            if (speciesList.isEmpty() || breedList.isEmpty()) {
                Toast.makeText(context, "Nem sikerült lekérni a fajokat", Toast.LENGTH_SHORT).show()
                return@launch
            }

            Log.i("BreedsFragment", "Setting up RecyclerView")
            val adapter = BreedAdapter(breedList, speciesList)
            val recyclerView = view.findViewById<RecyclerView>(R.id.Breeds_RecyclerView)
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter
        }

        return view
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
            Log.i("BreedAdapter", "Binding breed: ${breed.name} wit species: ${breed.speciesId}")
            holder.name.text = breed.name
            holder.species.text = species.find { it.speciesId == breed.speciesId }?.name ?: "Unknown"
            holder.description.text = breed.description
        }

        override fun getItemCount() = breeds.size
    }
}