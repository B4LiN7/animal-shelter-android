package app.animalshelter

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText

class SettingsFragment : Fragment() {

    var baseUrl: EditText? = null
    var btnSubmit: Button? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        initViews(view)

        btnSubmit?.setOnClickListener {
            val baseUrlText = baseUrl?.text.toString()
            val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            sharedPreferences.edit().putString("base_url", baseUrlText).apply()

            // Reload the main activity
            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        return view
    }

    private fun initViews(view: View) {
        baseUrl = view.findViewById(R.id.Settings_EditText_BaseURL)
        btnSubmit = view.findViewById(R.id.Settings_Button_Submit)

        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val baseUrlText: String = sharedPreferences.getString("base_url", "http://10.0.2.2:3001/") ?: "http://10.0.2.2:3001/"
        baseUrl?.setText(baseUrlText)
    }
}