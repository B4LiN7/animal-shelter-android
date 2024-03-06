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
import app.animalshelter.ApiService.ApiService
import app.animalshelter.ApiService.BreedDto
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
            apiSrv.printCookiesToLog()

            val breedList: List<BreedDto>
            Log.i("BreedsFragment", "Fetching pets")
            try {
                breedList = apiSrv.breedInterface.getBreeds()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to fetch breeds", Toast.LENGTH_SHORT).show()
                Log.e("BreedsFragment", "Failed to fetch breeds", e)
                return@launch
            }

            Log.i("BreedsFragment", "Setting up RecyclerView")
            val adapter = BreedAdapter(breedList)
            val recyclerView = view.findViewById<RecyclerView>(R.id.Breeds_RecyclerView)
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter
        }

        return view
    }

    class BreedAdapter(private val breeds: List<BreedDto>) : RecyclerView.Adapter<BreedAdapter.BreedViewHolder>() {
        class BreedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.BreedItem_TextView_Name)
            val description: TextView = view.findViewById(R.id.BreedItem_TextView_Description)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BreedViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_breed, parent, false)
            return BreedViewHolder(view)
        }

        override fun onBindViewHolder(holder: BreedViewHolder, position: Int) {
            val breed = breeds[position]
            holder.name.text = breed.name
            holder.description.text = breed.description
        }

        override fun getItemCount() = breeds.size
    }
}