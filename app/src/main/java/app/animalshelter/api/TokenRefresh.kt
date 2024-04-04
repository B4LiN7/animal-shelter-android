package app.animalshelter.api

import android.content.SharedPreferences
import com.auth0.android.jwt.JWT
import com.google.gson.Gson

class TokenRefresh(private val authService: Auth, private val sharedPreferences: SharedPreferences) {
    suspend fun refreshTokenIfNeeded() {
        // Check if the access token is expired
        // If expired, use the refresh token to get a new access token
        val refreshToken = sharedPreferences.getString("refresh_token", null)
        if (refreshToken != null && isTokenExpired()) {
            val response = authService.refresh("Bearer $refreshToken")
            if (response.isSuccessful) {
                // Save new tokens to SharedPreferences
                val authResponse = response.body()?.let { Gson().fromJson(it.string(), AuthResponse::class.java) }
                authResponse?.let {
                    with(sharedPreferences.edit()) {
                        putString("access_token", it.accessToken)
                        putString("refresh_token", it.refreshToken)
                        apply()
                    }
                }
            }
        }
    }

    private fun isTokenExpired(): Boolean {
        val token = sharedPreferences.getString("access_token", null)
        val jwt: JWT = JWT(token!!)
        return jwt.isExpired(10)
    }
}