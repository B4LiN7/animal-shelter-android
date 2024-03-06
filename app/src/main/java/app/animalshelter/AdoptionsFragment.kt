package app.animalshelter

import android.app.AlertDialog
import android.graphics.Bitmap
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
import app.animalshelter.api.AdoptionDto
import app.animalshelter.api.AdoptionStatus
import app.animalshelter.api.AdoptionSubmitDto
import app.animalshelter.api.ApiService
import app.animalshelter.api.PetDto
import app.animalshelter.api.Status
import app.animalshelter.api.UserNameDto
import kotlinx.coroutines.launch

class AdoptionsFragment : Fragment() {
    private var recyclerView: RecyclerView? = null
    private var dialog: AlertDialog.Builder? = null

    // ApiService
    private lateinit var apiSrv: ApiService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_adoptions, container, false)
        apiSrv = ApiService(requireContext())
        initViews(view)
        lifecycleScope.launch {
            fetchAndDisplayAdoptions()
        }
        return view
    }

    private suspend fun fetchAndDisplayAdoptions() {
        Log.i("AdoptionsFragment", "Start fetching and displaying pets")
        apiSrv.printCookiesToLog()

        Log.i("AdoptionsFragment", "Fetching adoptions")
        val adoptionList: List<AdoptionDto> = apiSrv.fetchAdoptions()

        Log.i("AdoptionsFragment", "Fetching user's names")
        val usernameMap: MutableMap<String, UserNameDto> = apiSrv.fetchUsernames(adoptionList)

        Log.i("AdoptionsFragment", "Fetching pets")
        val petList: List<PetDto> = apiSrv.fetchPets()

        Log.i("AdoptionsFragment", "Fetching pet images")
        val imageMap: MutableMap<Int, Bitmap> = apiSrv.fetchImagesForPets(petList)

        Log.i("AdoptionsFragment", "Setting up RecyclerView")
        val adapter = AdoptionAdapter(adoptionList, usernameMap, petList, imageMap)
        recyclerView?.layoutManager = LinearLayoutManager(context)
        recyclerView?.adapter = adapter
    }

    private inner class AdoptionAdapter(private val adoptions: List<AdoptionDto>, private val usernames: MutableMap<String, UserNameDto>, private val pets: List<PetDto>, private val images: Map<Int, Bitmap>) : RecyclerView.Adapter<AdoptionAdapter.AdoptionViewHolder>() {
        inner class AdoptionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val image: ImageView = view.findViewById(R.id.AdoptionItem_ImageView_Image)
            val pet: TextView = view.findViewById(R.id.AdoptionItem_TextView_Pet)
            val user: TextView = view.findViewById(R.id.AdoptionItem_TextView_User)
            val status: TextView = view.findViewById(R.id.AdoptionItem_TextView_Status)
            val btnFinish: Button = view.findViewById(R.id.AdoptionItem_Button_Finish)
            val btnCancel: Button = view.findViewById(R.id.AdoptionItem_Button_Cancel)
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
            holder.status.text = adoption.status.toString()

            if (adoption.status == Status.ADOPTED) {
                holder.btnFinish.visibility = View.GONE
                holder.btnCancel.visibility = View.GONE
            }
            holder.btnFinish.setOnClickListener {
                dialog?.setTitle("Adoptálás")?.setMessage("Biztosan véglegesiti a kiválasztott, ${pet?.name} nevű állat adoptálását?")
                    ?.setPositiveButton("Igen") { _, _ ->
                        lifecycleScope.launch {
                            val response = apiSrv.adoptionInterface.createAdoption(AdoptionSubmitDto(adoption.petId, adoption.userId, AdoptionStatus.ADOPTED))
                            if (response.isSuccessful) {
                                Toast.makeText(context, "Adoption finished", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error finishing adoption", Toast.LENGTH_SHORT).show()
                            }
                            fetchAndDisplayAdoptions()
                        }
                    }
                    ?.setNegativeButton("Nem") { _, _ -> }
                    ?.show()
            }

            holder.btnCancel.setOnClickListener {
                dialog?.setTitle("Megszakitás")?.setMessage("Biztosan törölni megszakítja a kiválasztott, ${pet?.name} állat adoptációját?")
                    ?.setPositiveButton("Igen") { _, _ ->
                        lifecycleScope.launch {
                            val response = apiSrv.adoptionInterface.createAdoption(AdoptionSubmitDto(adoption.petId, adoption.userId, AdoptionStatus.CANCELLED))
                            if (response.isSuccessful) {
                                Toast.makeText(context, "Adoption cancelled", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error cancelling adoption", Toast.LENGTH_SHORT).show()
                            }
                            fetchAndDisplayAdoptions()
                        }
                    }
                    ?.setNegativeButton("Nem") { _, _ -> }
                    ?.show()
            }
        }

        override fun getItemCount() = adoptions.size
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.Adoptions_RecyclerView)
        dialog = AlertDialog.Builder(context)
    }
}