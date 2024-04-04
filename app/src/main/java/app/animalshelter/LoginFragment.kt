package app.animalshelter

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import app.animalshelter.api.ApiService
import app.animalshelter.api.LoginDto
import app.animalshelter.api.RegisterDto
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var username: EditText? = null
    private var password: EditText? = null
    private var confirmPassword: EditText? = null
    private var email: EditText? = null
    private var btnLogin: Button? = null
    private var btnRegister: Button? = null
    private enum class AccountEvent { LOGIN, REGISTER }
    private var currentEvent: AccountEvent = AccountEvent.LOGIN

    private lateinit var apiSrv: ApiService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)
        apiSrv = ApiService(requireContext())
        initViews(view)

        btnLogin?.setOnClickListener {
            if (currentEvent != AccountEvent.LOGIN) {
                currentEvent = AccountEvent.LOGIN
                setEvent(currentEvent)
                clearInputs()
                return@setOnClickListener
            }

            val username = username?.text.toString()
            val password = password?.text.toString()

            if (checkInputs()) {
                lifecycleScope.launch {
                    val successful = apiSrv.login(username, password)
                    if (successful != null) {
                        Toast.makeText(requireContext(), "Belépés sikeres", Toast.LENGTH_SHORT).show()

                        // Reload the main activity
                        val intent = Intent(requireContext(), MainActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    } else {
                        Toast.makeText(requireContext(), "Belépés sikertelen", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnRegister?.setOnClickListener {
            if (currentEvent != AccountEvent.REGISTER) {
                currentEvent = AccountEvent.REGISTER
                setEvent(currentEvent)
                clearInputs()
                return@setOnClickListener
            }

            val username = username?.text.toString()
            val password = password?.text.toString()
            val email = email?.text.toString()

            if (checkInputs()) {
                lifecycleScope.launch {
                    val successful = apiSrv.register(username, password, email)

                    if (successful) {
                        Toast.makeText(requireContext(), "Sikeres regisztráció", Toast.LENGTH_SHORT).show()
                        currentEvent = AccountEvent.LOGIN
                        setEvent(currentEvent)
                        clearInputs()
                    } else {
                        Toast.makeText(requireContext(), "Sikertelen regisztráció", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        return view
    }

    private fun initViews(view: View) {
        username = view.findViewById(R.id.Login_EditText_Username)
        password = view.findViewById(R.id.Login_EditText_Password)
        confirmPassword = view.findViewById(R.id.Login_EditText_ConfirmPassword)
        email = view.findViewById(R.id.Login_EditText_Email)
        btnLogin = view.findViewById(R.id.Login_Button_Login)
        btnRegister = view.findViewById(R.id.Login_Button_Register)
    }

    private fun setEvent(event: AccountEvent) {
        when (event) {
            AccountEvent.LOGIN -> {
                confirmPassword?.visibility = View.GONE
                email?.visibility = View.GONE
            }
            AccountEvent.REGISTER -> {
                confirmPassword?.visibility = View.VISIBLE
                email?.visibility = View.VISIBLE
            }
        }
    }

    private fun clearInputs() {
        username?.text?.clear()
        password?.text?.clear()
        confirmPassword?.text?.clear()
        email?.text?.clear()

        username?.error = null
        password?.error = null
        confirmPassword?.error = null
        email?.error = null
    }

    private fun checkInputs(): Boolean {
        username?.error = null
        password?.error = null
        confirmPassword?.error = null
        email?.error = null

        if (username?.text.toString().isEmpty()) {
            username?.error = "Username is required"
        }
        if (password?.text.toString().isEmpty()) {
            password?.error = "Password is required"
        }
        else if (password?.text.toString().length < 8) {
            password?.error = "Password must be at least 8 characters"
        }

        if (currentEvent == AccountEvent.REGISTER) {
            if (confirmPassword?.text.toString().isEmpty()) {
                confirmPassword?.error = "Confirm password is required"
            }
            else if (confirmPassword?.text.toString() != password?.text.toString()) {
                confirmPassword?.error = "Password and confirm password do not match"
            }
            if (email?.text.toString().isEmpty()) {
                email?.error = "Email is required"
            }
        }

        return username?.error == null && password?.error == null && confirmPassword?.error == null && email?.error == null
    }
}