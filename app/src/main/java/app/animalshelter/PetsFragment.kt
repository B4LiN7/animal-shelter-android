package app.animalshelter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.animalshelter.ApiService.Media
import app.animalshelter.ApiService.Pet
import app.animalshelter.ApiService.PetDto
import app.animalshelter.ApiService.RetrofitService
import kotlinx.coroutines.launch

class PetsFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_pets, container, false)

        lifecycleScope.launch {
            RetrofitService.cookieJar.printCookiesToLog()

            Log.i("PetsFragment", "Fetching pets")
            val petList = RetrofitService.getRetrofitService().create(Pet::class.java).getPets()

            Log.i("PetsFragment", "Fetching pet images")
            val imageList: MutableList<Bitmap> = getImagesForPets(petList)

            Log.i("PetsFragment", "Setting up RecyclerView")
            val adapter = PetAdapter(petList, imageList)
            val recyclerView = view.findViewById<RecyclerView>(R.id.Pets_RecyclerView)
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter
        }

        return view
    }

    suspend fun getImagesForPets(petList: List<PetDto>): MutableList<Bitmap> {
        val imageList: MutableList<Bitmap> = mutableListOf()
        for (pet in petList) {
            if (pet.imageUrl == null) {
                val emptyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                imageList.add(emptyBitmap)
                continue
            }
            val fullUrl = pet.imageUrl
            val startIndex = fullUrl.indexOf("/uploads")
            val shortUrl = fullUrl.substring(startIndex)
            val image = RetrofitService.getRetrofitService().create(Media::class.java).getMedia(shortUrl)
            val inputStream = image.byteStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            imageList.add(bitmap)
        }
        return imageList
    }

    class PetAdapter(private val pets: List<PetDto>, private val images: List<Bitmap>) : RecyclerView.Adapter<PetAdapter.PetViewHolder>() {
        class PetViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.PetItem_TextView_Name)
            val image: ImageView = view.findViewById(R.id.PetItem_ImageView_Image)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.pet_item, parent, false)
            return PetViewHolder(view)
        }

        override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
            val pet = pets[position]
            val image = images[position]
            holder.name.text = pet.name
            holder.image.setImageBitmap(image)
        }

        override fun getItemCount() = pets.size
    }

    companion object {
        @JvmStatic
        fun newInstance() {
            PetsFragment()
        }
    }
}