package app.animalshelter.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import app.animalshelter.R
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory



class RetrofitService(private val sharedPreferences: SharedPreferences) {
    private val baseUrl: String = sharedPreferences.getString("base_url", "http://10.0.2.2:3001/") ?: "http://10.0.2.2:3001/"

    init {
        var baseUrlText: String = baseUrl
        if (baseUrlText.isNullOrEmpty() || !baseUrlText.startsWith("http://")) {
            baseUrlText = "http://10.0.2.2:3001/"
        }
        sharedPreferences.edit().putString("base_url", baseUrlText).apply()
    }

    fun getRetrofitService(): Retrofit {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(TokenInterceptor(sharedPreferences))
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
