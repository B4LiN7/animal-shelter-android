package app.animalshelter

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
import app.animalshelter.ApiService.Auth
import app.animalshelter.ApiService.LoginDto
import app.animalshelter.ApiService.RegisterDto
import app.animalshelter.ApiService.RetrofitService
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var username: EditText? = null
    private var password: EditText? = null
    private var confirmPassword: EditText? = null
    private var email: EditText? = null
    private var LoginBtn: Button? = null
    private var RegisterBtn: Button? = null
    private var currentEvent: AccountEvent = AccountEvent.LOGIN

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)
        initViews(view)

        LoginBtn?.setOnClickListener {
            if (currentEvent != AccountEvent.LOGIN) {
                currentEvent = AccountEvent.LOGIN
                setEvent()
                clearInputs()
                return@setOnClickListener
            }

            val username = username?.text.toString()
            val password = password?.text.toString()

            if (checkInputs()) {
                lifecycleScope.launch {
                    val loginDto = LoginDto(username, password)
                    try {
                        val response = RetrofitService.getRetrofitService().create(Auth::class.java).login(loginDto)
                        if (response.isSuccessful) {
                            Log.i("Login", "Login successful: ${response.body()?.string()}")
                            Toast.makeText(requireContext(), "Login successful", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.i("Login", "Login failed: ${response.errorBody()?.string()}")
                            Toast.makeText(requireContext(), "Login failed", Toast.LENGTH_SHORT).show()
                        }
                        RetrofitService.cookieJar.printCookies()
                    } catch (e: Exception) {
                        Log.e("Login", "Error: ${e.message}")
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        RegisterBtn?.setOnClickListener {
            if (currentEvent != AccountEvent.REGISTER) {
                currentEvent = AccountEvent.REGISTER
                setEvent()
                clearInputs()
                return@setOnClickListener
            }

            val username = username?.text.toString()
            val password = password?.text.toString()
            val email = email?.text.toString()

            if (checkInputs()) {
                lifecycleScope.launch {
                    val registerDto = RegisterDto(username, password, email)
                    try {
                        val response = RetrofitService.getRetrofitService().create(Auth::class.java).register(registerDto)
                        if (response.isSuccessful) {
                            Log.i("Register", "Register successful: ${response.body()?.string()}")
                            Toast.makeText(requireContext(), "Register successful", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.i("Register", "Register failed: ${response.errorBody()?.string()}")
                            Toast.makeText(requireContext(), "Register failed", Toast.LENGTH_SHORT).show()
                        }
                        RetrofitService.cookieJar.printCookies()
                    } catch (e: Exception) {
                        Log.e("Register", "Error: ${e.message}")
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
        LoginBtn = view.findViewById(R.id.Login_Button_Login)
        RegisterBtn = view.findViewById(R.id.Login_Button_Register)
    }

    private fun setEvent() {
        when (currentEvent) {
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

    private enum class AccountEvent {
        LOGIN, REGISTER
    }
}