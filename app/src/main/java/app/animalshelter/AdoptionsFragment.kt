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
import app.animalshelter.api.AdoptionResponse
import app.animalshelter.api.AdoptionStatus
import app.animalshelter.api.AdoptionDto
import app.animalshelter.api.ApiService
import app.animalshelter.api.PetDto
import app.animalshelter.api.UserNameDto
import kotlinx.coroutines.launch

class AdoptionsFragment : Fragment() {
    private var recyclerView: RecyclerView? = null
    private var textView: TextView? = null
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

        Log.i("AdoptionsFragment", "Fetching adoptions")
        val adoptionList: List<AdoptionResponse> = apiSrv.fetchAdoptions()

        if (adoptionList.isEmpty()) {
            textView?.text = "Nincsenek adoptációk."
            textView?.visibility = View.VISIBLE
            return
        }

        Log.i("AdoptionsFragment", "Fetching user's names")
        val usernameMap: MutableMap<String, UserNameDto> = apiSrv.fetchUsernames(adoptionList)

        Log.i("AdoptionsFragment", "Fetching pets")
        val petList: List<PetDto> = apiSrv.fetchPetsArray(adoptionList.map { it.petId })

        Log.i("AdoptionsFragment", "Fetching pet images")
        val imageMap: MutableMap<String, Bitmap> = apiSrv.fetchImagesForPets(petList)

        Log.i("AdoptionsFragment", "Setting up RecyclerView")
        val adapter = AdoptionAdapter(adoptionList, usernameMap, petList, imageMap)
        recyclerView?.layoutManager = LinearLayoutManager(context)
        recyclerView?.adapter = adapter
    }

    private inner class AdoptionAdapter(private val adoptions: List<AdoptionResponse>, private val usernames: MutableMap<String, UserNameDto>, private val pets: List<PetDto>, private val images: Map<String, Bitmap>) : RecyclerView.Adapter<AdoptionAdapter.AdoptionViewHolder>() {
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
            val username = usernames[adoption.userId]?.name
            val pet = pets.find { it.petId == adoption.petId }
            
            val image: Bitmap = images[pet?.petId] ?: Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            holder.image.setImageBitmap(image)

            holder.pet.text = pet?.name ?: "Unknown"
            holder.user.text = username ?: "Unknown"
            holder.status.text = adoption.status.toString()

            if (adoption.status == AdoptionStatus.APPROVED || adoption.status == AdoptionStatus.CANCELLED || adoption.status == AdoptionStatus.REJECTED) {
                holder.btnFinish.visibility = View.GONE
                holder.btnCancel.visibility = View.GONE
            }


            holder.btnFinish.setOnClickListener {
                dialog?.setTitle("Adoptálás")?.setMessage("Biztosan véglegesiti a kiválasztott, ${pet?.name} nevű állat adoptálását?")
                    ?.setPositiveButton(R.string.btn_yes) { _, _ ->
                        lifecycleScope.launch {
                            val response = apiSrv.adoptionInterface.updateAdoption(adoption.adoptionId, AdoptionDto(adoption.petId, adoption.userId, AdoptionStatus.APPROVED, null))
                            if (response.isSuccessful) {
                                Toast.makeText(context, "Adoption finished", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error finishing adoption", Toast.LENGTH_SHORT).show()
                            }
                            fetchAndDisplayAdoptions()
                        }
                    }
                    ?.setNegativeButton(R.string.btn_no) { _, _ -> }
                    ?.show()
            }

            holder.btnCancel.setOnClickListener {
                dialog?.setTitle("Megszakitás")?.setMessage("Biztosan törölni megszakítja a kiválasztott, ${pet?.name} állat adoptációját?")
                    ?.setPositiveButton(R.string.btn_yes) { _, _ ->
                        lifecycleScope.launch {
                            val response = apiSrv.adoptionInterface.updateAdoption(adoption.adoptionId, AdoptionDto(adoption.petId, adoption.userId, AdoptionStatus.CANCELLED, null))
                            if (response.isSuccessful) {
                                Toast.makeText(context, "Adoption cancelled", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error cancelling adoption", Toast.LENGTH_SHORT).show()
                            }
                            fetchAndDisplayAdoptions()
                        }
                    }
                    ?.setNegativeButton(R.string.btn_no) { _, _ -> }
                    ?.show()
            }
        }

        override fun getItemCount() = adoptions.size
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.Adoptions_RecyclerView)
        textView = view.findViewById(R.id.Adoptions_TextView)
        dialog = AlertDialog.Builder(context)
    }
}