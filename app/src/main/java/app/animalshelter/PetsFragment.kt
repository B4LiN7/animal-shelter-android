package app.animalshelter

import android.media.Image
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView

class PetsFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_pets, container, false)

        val petsList = listOf(
            Pet("Pet1", "Breed1", 2, null),
            Pet("Pet2", "Breed2", 3, null),
            Pet("Pet3", "Breed3", 1, null)
        )

        val adapter = PetAdapter(petsList)
        val recyclerView = view.findViewById<RecyclerView>(R.id.Pets_RecyclerView)
        recyclerView.adapter = adapter

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance() {
            PetsFragment()
        }
    }

    data class Pet(
        val name: String,
        val breed: String,
        val age: Int,
        val img: Image?,
    )

    class PetAdapter(private val pets: List<Pet>) : RecyclerView.Adapter<PetAdapter.PetViewHolder>() {
        class PetViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById<TextView>(R.id.PetItem_TextView_Name)
            val image: ImageView = view.findViewById<ImageView>(R.id.PetItem_ImageView_Image)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.pet_data_item, parent, false)
            return PetViewHolder(view)
        }

        override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
            val pet = pets[position]
            holder.name.text = pet.name
        }

        override fun getItemCount() = pets.size
    }

}