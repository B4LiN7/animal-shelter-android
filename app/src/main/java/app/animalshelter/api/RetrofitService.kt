package app.animalshelter.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import app.animalshelter.R
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory



class RetrofitService(context: Context) {
    companion object {
        val BASE_URL: String = "http://10.0.2.2:3001/"
    }

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    init {
        Log.i("RetrofitService", "BASE_URL set to: $BASE_URL")
    }

    fun getRetrofitService(): Retrofit {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(TokenInterceptor(sharedPreferences)) // Disable due to can't send refresh token
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
