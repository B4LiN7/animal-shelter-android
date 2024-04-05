package app.animalshelter.api

import android.content.SharedPreferences
import android.util.Log
import com.auth0.android.jwt.JWT
import com.google.gson.Gson

class TokenRefresh(private val authService: Auth, private val sharedPreferences: SharedPreferences) {
    suspend fun refreshTokenIfNeeded() {
        val refreshToken = sharedPreferences.getString("refresh_token", null)
        if (refreshToken != null && isTokenExpired()) {
            val response = authService.refresh("Bearer $refreshToken")
            if (response.isSuccessful) {
                val authResponse = response.body()?.let { Gson().fromJson(it.string(), AuthResponse::class.java) }
                authResponse?.let {
                    with(sharedPreferences.edit()) {
                        putString("access_token", it.accessToken)
                        putString("refresh_token", it.refreshToken)
                        apply()
                    }
                }
                Log.i("TokenRefresh", "Token refreshed. New access_token: ${authResponse?.accessToken}")
            }
        }
    }

    private fun isTokenExpired(): Boolean {
        val token = sharedPreferences.getString("access_token", null)
        val jwt: JWT = JWT(token!!)
        return jwt.isExpired(10)
    }
}