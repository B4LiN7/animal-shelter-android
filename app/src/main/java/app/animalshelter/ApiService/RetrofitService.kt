package app.animalshelter.ApiService

import android.content.Context
import android.util.Log
import app.animalshelter.R
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitService {
    lateinit var BASE_URL: String

    fun initialize(context: Context) {
        BASE_URL = context.resources.getString(R.string.base_url)
        Log.i("RetrofitService", "BASE_URL set to: $BASE_URL")
    }

    val cookieJar = MyCookieJar()

    fun getRetrofitService(): Retrofit {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        val client = OkHttpClient.Builder()
        client.addInterceptor(logging)
        client.cookieJar(cookieJar)

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}