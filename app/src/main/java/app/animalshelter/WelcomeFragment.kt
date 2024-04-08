package app.animalshelter

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import app.animalshelter.api.ApiService

class WelcomeFragment : Fragment() {

    private var welcome: TextView? = null
    private lateinit var apiSrv: ApiService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_welcome, container, false)
        initViews(view)
        apiSrv = ApiService(requireContext())

        lifecycleScope.launchWhenStarted {
            val currentUser = apiSrv.fetchCurrentUser()
            welcome?.text = "Welcome ${currentUser?.name}!"
        }

        return view
    }

    private fun initViews(view: View) {
        welcome = view.findViewById(R.id.Welcome_TextView)
    }
}