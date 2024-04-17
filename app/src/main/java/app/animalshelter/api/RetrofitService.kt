package app.animalshelter.api

import android.content.SharedPreferences
import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory



class RetrofitService(private val sharedPreferences: SharedPreferences) {
    private var baseUrl: String = sharedPreferences.getString("base_url", "http://10.0.2.2:3001/")!!

    init {
        if (baseUrl.isEmpty() || !baseUrl.startsWith("http://")) {
            baseUrl = "http://10.0.2.2:3001/"
            sharedPreferences.edit().putString("base_url", "http://10.0.2.2:3001/").apply()
            Log.i("RetrofitService", "Base URL not set correctly, using default: $baseUrl")
        }
    }

    fun getRetrofitService(): Retrofit {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(TokenInterceptor(sharedPreferences))
            .build()

        val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
            .create()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
}
